package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.{Date, Optional}

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO, HakutoiveenValintatapajonoDTO}
import fi.vm.sade.sijoittelu.tulos.dto.{HakemuksenTila, IlmoittautumisTila, ValintatuloksenTila}
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila._
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritService
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime

class SijoittelutulosService(raportointiService: RaportointiService, ohjausparametritService: OhjausparametritService) {
  import scala.collection.JavaConversions._

  def hakemuksenTulos(haku: Haku, hakemusOid: String): Option[HakemuksenSijoitteluntulos] = {
    val aikataulu = ohjausparametritService.ohjausparametrit(haku.oid).flatMap(_.vastaanottoaikataulu)

    for (
      sijoitteluAjo <- fromOptional(raportointiService.latestSijoitteluAjoForHaku(haku.oid));
      hakija: HakijaDTO <- Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid))
    ) yield hakemuksenYhteenveto(hakija, aikataulu)
  }

  def hakemustenTulos(hakuOid: String): List[HakemuksenSijoitteluntulos] = {
    val aikataulu = ohjausparametritService.ohjausparametrit(hakuOid).flatMap(_.vastaanottoaikataulu)

    for (
      sijoitteluAjo <- fromOptional(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).toList;
      hakija <- Option(raportointiService.hakemukset(sijoitteluAjo, null, null, null, null, null, null)).map(_.getResults.toList).getOrElse(Nil)
    ) yield hakemuksenYhteenveto(hakija, aikataulu)
  }

  def hakemustenTulos(hakuOid: String, hakukohdeOid: String) = {
    val aikataulu = ohjausparametritService.ohjausparametrit(hakuOid).flatMap(_.vastaanottoaikataulu)

    for (
      sijoitteluAjo <- fromOptional(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).toList;
      hakija <- Option(raportointiService.hakemukset(sijoitteluAjo, null, null, null, List(hakukohdeOid), null, null)).map(_.getResults.toList).getOrElse(Nil)
    ) yield hakemuksenYhteenveto(hakija, aikataulu)
  }

  private def hakemuksenYhteenveto(hakija: HakijaDTO, aikataulu: Option[Vastaanottoaikataulu]): HakemuksenSijoitteluntulos = {
    val hakutoiveidenYhteenvedot = hakija.getHakutoiveet.toList.map { hakutoive: HakutoiveDTO =>
      val jono: HakutoiveenValintatapajonoDTO = merkitseväJono(hakutoive).get
      var valintatila: Valintatila = jononValintatila(jono, hakutoive)
      val viimeisinHakemuksenTilanMuutos: Option[Date] = Option(jono.getHakemuksenTilanViimeisinMuutos())
      val viimeisinValintatuloksenMuutos: Option[Date] = Option(jono.getValintatuloksenViimeisinMuutos())
      val vastaanottoDeadline: Option[DateTime] = laskeVastaanottoDeadline(aikataulu, viimeisinHakemuksenTilanMuutos, viimeisinValintatuloksenMuutos, valintatila)
      val vastaanottotila: Vastaanottotila = laskeVastaanottotila(valintatila, jono.getVastaanottotieto, aikataulu, vastaanottoDeadline)
      valintatila = vastaanottotilanVaikutusValintatilaan(valintatila, vastaanottotila)
      val vastaanotettavuustila: Vastaanotettavuustila.Value = laskeVastaanotettavuustila(valintatila, vastaanottotila)
      val julkaistavissa = jono.getVastaanottotieto != ValintatuloksenTila.KESKEN || jono.isJulkaistavissa
      val pisteet: Option[BigDecimal] = Option(jono.getPisteet).map((p: java.math.BigDecimal) => new BigDecimal(p))

      HakutoiveenSijoitteluntulos(
        hakutoive.getHakukohdeOid,
        hakutoive.getTarjoajaOid,
        jono.getValintatapajonoOid,
        valintatila,
        vastaanottotila,
        vastaanottoDeadline.map(_.toDate),
        Ilmoittautumistila.withName(Option(jono.getIlmoittautumisTila).getOrElse(IlmoittautumisTila.EI_TEHTY).name()),
        vastaanotettavuustila,
        viimeisinHakemuksenTilanMuutos,
        viimeisinValintatuloksenMuutos,
        Option(jono.getJonosija).map(_.toInt),
        Option(jono.getVarasijojaKaytetaanAlkaen),
        Option(jono.getVarasijojaTaytetaanAsti),
        Option(jono.getVarasijanNumero).map(_.toInt),
        julkaistavissa,
        jono.getTilanKuvaukset.toMap,
        pisteet
      )
    }

    HakemuksenSijoitteluntulos(hakija.getHakemusOid, Option(StringUtils.trimToNull(hakija.getHakijaOid)), hakutoiveidenYhteenvedot)
  }

  private def laskeVastaanotettavuustila(valintatila: Valintatila, vastaanottotila: Vastaanottotila): Vastaanotettavuustila.Value = {
    if (Valintatila.isHyväksytty(valintatila) && vastaanottotila == Vastaanottotila.kesken) {
      Vastaanotettavuustila.vastaanotettavissa_sitovasti
    } else {
      Vastaanotettavuustila.ei_vastaanotettavissa
    }
  }

  private def jononValintatila(jono: HakutoiveenValintatapajonoDTO, hakutoive: HakutoiveDTO) = {
    var valintatila: Valintatila = ifNull(fromHakemuksenTila(jono.getTila), Valintatila.kesken)
    if (valintatila == Valintatila.varalla && jono.isHyvaksyttyVarasijalta) {
      valintatila = Valintatila.hyväksytty
    }

    if (jono.getTila.isHyvaksytty) {
      if (jono.isHyvaksyttyHarkinnanvaraisesti) {
        valintatila = Valintatila.harkinnanvaraisesti_hyväksytty
      }
    } else if (!hakutoive.isKaikkiJonotSijoiteltu) {
      valintatila = Valintatila.kesken
    }
    valintatila
  }

  private def laskeVastaanottotila(valintatila: Valintatila, vastaanottotieto: ValintatuloksenTila, aikataulu: Option[Vastaanottoaikataulu], vastaanottoDeadline: Option[DateTime]): Vastaanottotila = {
    def convertVastaanottotila(valintatuloksenTila: ValintatuloksenTila): Vastaanottotila = {
      valintatuloksenTila match {
        case ValintatuloksenTila.ILMOITETTU =>
          Vastaanottotila.kesken
        case ValintatuloksenTila.KESKEN =>
          Vastaanottotila.kesken
        case ValintatuloksenTila.PERUNUT =>
          Vastaanottotila.perunut
        case ValintatuloksenTila.PERUUTETTU =>
          Vastaanottotila.peruutettu
        case ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA =>
          Vastaanottotila.ei_vastaanotettu_määräaikana
        case ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT =>
          Vastaanottotila.ehdollisesti_vastaanottanut
        case ValintatuloksenTila.VASTAANOTTANUT_LASNA =>
          Vastaanottotila.vastaanottanut
        case ValintatuloksenTila.VASTAANOTTANUT_POISSAOLEVA =>
          Vastaanottotila.vastaanottanut
        case ValintatuloksenTila.VASTAANOTTANUT =>
          Vastaanottotila.vastaanottanut
        case ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI =>
          Vastaanottotila.vastaanottanut
      }
    }

    val vastaanottotila = (convertVastaanottotila(ifNull(vastaanottotieto, ValintatuloksenTila.KESKEN)), vastaanottoDeadline) match {
      case (Vastaanottotila.kesken, Some(deadline)) if Valintatila.isHyväksytty(valintatila) && new DateTime().isAfter(deadline) =>
        Vastaanottotila.ei_vastaanotettu_määräaikana
      case (tila, _) =>
        tila
    }
    vastaanottotila
  }

  private def vastaanottotilanVaikutusValintatilaan(valintatila: Valintatila, vastaanottotila : Vastaanottotila) = {
    if (List(Vastaanottotila.ehdollisesti_vastaanottanut, Vastaanottotila.vastaanottanut).contains(vastaanottotila)) {
      Valintatila.hyväksytty
    } else if (Vastaanottotila.perunut == vastaanottotila) {
      Valintatila.perunut
    } else if (Vastaanottotila.peruutettu == vastaanottotila) {
      Valintatila.peruutettu
    } else {
      valintatila
    }
  }

  private def fromHakemuksenTila(tila: HakemuksenTila): Valintatila = {
    Valintatila.withName(tila.name)
  }


  private def laskeVastaanottoDeadline(aikataulu: Option[Vastaanottoaikataulu], viimeisinHakemuksenTilanMuutos: Option[Date], viimeisinValintatuloksenMuutos: Option[Date], valintatila: Valintatila): Option[DateTime] = {
    (aikataulu) match {
      case Some(Vastaanottoaikataulu(Some(deadline), buffer)) if Valintatila.isHyväksytty(valintatila) =>
        Some{
          val deadlineFromHakemuksenTilanMuutos = getDeadlineWithBuffer(viimeisinHakemuksenTilanMuutos, buffer, deadline)
          val deadlineFromValintatuloksenMuutos = getDeadlineWithBuffer(viimeisinValintatuloksenMuutos, buffer, deadline)
          (deadlineFromHakemuksenTilanMuutos, deadlineFromValintatuloksenMuutos) match {
            case (Some(hakemuksenMuutos), Some(valintatuloksenMuutos)) if hakemuksenMuutos.isAfter(deadline) && (hakemuksenMuutos.isAfter(valintatuloksenMuutos) || hakemuksenMuutos.isEqual(valintatuloksenMuutos)) => hakemuksenMuutos
            case (Some(hakemuksenMuutos), Some(valintatuloksenMuutos)) if valintatuloksenMuutos.isAfter(deadline) && valintatuloksenMuutos.isAfter(hakemuksenMuutos) => valintatuloksenMuutos
            case (Some(muutosDeadline), None) if muutosDeadline.isAfter(deadline) => muutosDeadline
            case (None, Some(muutosDeadline)) if muutosDeadline.isAfter(deadline) => muutosDeadline
            case _ => deadline
          }
        }
      case _ => None
    }
  }

  private def getDeadlineWithBuffer(viimeisinMuutosOption: Option[Date], bufferOption: Option[Int], deadline: DateTime): Option[DateTime] = {
    for {
      viimeisinMuutos <- viimeisinMuutosOption
      buffer <- bufferOption
    } yield new DateTime(viimeisinMuutos).plusDays(buffer).withTime(deadline.getHourOfDay, deadline.getMinuteOfHour, deadline.getSecondOfMinute, deadline.getMillisOfSecond)
  }

  private def ifNull[T](value: T, defaultValue: T): T = {
    if (value == null) defaultValue
    else value
  }

  private def merkitseväJono(hakutoive: HakutoiveDTO): Option[HakutoiveenValintatapajonoDTO] = {
    val ordering = Ordering.fromLessThan{ (jono1: HakutoiveenValintatapajonoDTO, jono2: HakutoiveenValintatapajonoDTO) =>
      val tila1 = fromHakemuksenTila(jono1.getTila)
      val tila2 = fromHakemuksenTila(jono2.getTila)
      if (tila1 == Valintatila.varalla && tila2 == Valintatila.varalla) {
        jono1.getVarasijanNumero < jono2.getVarasijanNumero
      } else {
        tila1.compareTo(tila2) < 0
      }
    }

    val orderedJonot: List[HakutoiveenValintatapajonoDTO] = hakutoive.getHakutoiveenValintatapajonot.toList.sorted(ordering)
    val headOption: Option[HakutoiveenValintatapajonoDTO] = orderedJonot.headOption
    headOption.map(jono => {
      val tila: Valintatila = fromHakemuksenTila(jono.getTila)
      if (tila.equals(Valintatila.hylätty)) jono.setTilanKuvaukset(orderedJonot.last.getTilanKuvaukset)
      jono
    })
  }

  def fromOptional[T](opt: Optional[T]) = {
    if (opt.isPresent) {
      Some(opt.get)
    } else {
      None
    }
  }
}

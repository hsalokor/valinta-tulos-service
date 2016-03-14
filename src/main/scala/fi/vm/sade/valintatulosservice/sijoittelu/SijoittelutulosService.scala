package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.{Date, Optional}

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO, HakutoiveenValintatapajonoDTO}
import fi.vm.sade.sijoittelu.tulos.dto.{HakemuksenTila, IlmoittautumisTila}
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila._
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritService
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, VastaanottoRecord}
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime

class SijoittelutulosService(raportointiService: RaportointiService,
                             ohjausparametritService: OhjausparametritService,
                             hakijaVastaanottoRepository: HakijaVastaanottoRepository) {
  import scala.collection.JavaConversions._

  def hakemuksenTulos(haku: Haku, hakemusOid: String, hakijaOid: String): Option[HakemuksenSijoitteluntulos] = {
    val aikataulu = ohjausparametritService.ohjausparametrit(haku.oid).flatMap(_.vastaanottoaikataulu)

    for (
      sijoitteluAjo <- fromOptional(raportointiService.latestSijoitteluAjoForHaku(haku.oid));
      hakija: HakijaDTO <- Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid))
    ) yield hakemuksenYhteenveto(hakija, aikataulu, fetchVastaanotto(hakijaOid, haku.oid))
  }

  def hakemustenTulos(hakuOid: String,
                      hakukohdeOid: Option[String] = None,
                      hakijaOidsByHakemusOids: Map[String, String],
                      hakukohteenVastaanotot: Option[Map[String,Set[VastaanottoRecord]]] = None): List[HakemuksenSijoitteluntulos] = {
    def fetchVastaanottos(h: HakijaDTO): Set[VastaanottoRecord] = hakijaOidsByHakemusOids.get(h.getHakemusOid) match {
      case Some(hakijaOid) => hakukohteenVastaanotot.flatMap(_.get(hakijaOid)).getOrElse(fetchVastaanotto(hakijaOid, hakuOid))
      case None => Set()
    }

    val aikataulu = ohjausparametritService.ohjausparametrit(hakuOid).flatMap(_.vastaanottoaikataulu)
    val hakukohde: java.util.List[String] = if (hakukohdeOid.isEmpty) null else hakukohdeOid.map(List(_)).get
    (for (
      sijoittelu <- fromOptional(raportointiService.latestSijoitteluAjoForHaku(hakuOid));
      hakijat <- Option(raportointiService.hakemukset(sijoittelu, null, null, null, hakukohde, null, null)).map(_.getResults.toList)
    ) yield {
      hakijat.map(h => hakemuksenYhteenveto(h, aikataulu, fetchVastaanottos(h)))
    }).getOrElse(Nil)
  }

  private def fetchVastaanotto(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord] = {
    hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, hakuOid)
  }

  private def hakemuksenYhteenveto(hakija: HakijaDTO, aikataulu: Option[Vastaanottoaikataulu], vastaanottoRecord: Set[VastaanottoRecord]): HakemuksenSijoitteluntulos = {

    val hakutoiveidenYhteenvedot = hakija.getHakutoiveet.toList.map { hakutoive: HakutoiveDTO =>
      val vastaanotto = vastaanottoRecord.find(v => v.hakukohdeOid == hakutoive.getHakukohdeOid).map(_.action)
      val jono: HakutoiveenValintatapajonoDTO = merkitseväJono(hakutoive).get
      var valintatila: Valintatila = jononValintatila(jono, hakutoive)
      val viimeisinHakemuksenTilanMuutos: Option[Date] = Option(jono.getHakemuksenTilanViimeisinMuutos)
      val viimeisinValintatuloksenMuutos: Option[Date] = Option(jono.getValintatuloksenViimeisinMuutos)
      val ( vastaanottotila, vastaanottoDeadline ) = laskeVastaanottotila(valintatila, vastaanotto, aikataulu, viimeisinHakemuksenTilanMuutos)
      valintatila = vastaanottotilanVaikutusValintatilaan(valintatila, vastaanottotila)
      val vastaanotettavuustila: Vastaanotettavuustila.Value = laskeVastaanotettavuustila(valintatila, vastaanottotila)
      val julkaistavissa = jono.isJulkaistavissa
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
    val valintatila: Valintatila = ifNull(fromHakemuksenTila(jono.getTila), Valintatila.kesken)
    if (jono.getTila.isHyvaksytty && jono.isHyvaksyttyHarkinnanvaraisesti) {
      Valintatila.harkinnanvaraisesti_hyväksytty
    } else if (!jono.getTila.isHyvaksytty && !hakutoive.isKaikkiJonotSijoiteltu) {
      Valintatila.kesken
    } else if (valintatila == Valintatila.varalla && jono.isHyvaksyttyVarasijalta) {
      Valintatila.hyväksytty
    } else if (valintatila == Valintatila.varalla && jono.isEiVarasijatayttoa) {
      Valintatila.kesken
    } else {
      valintatila
    }
  }

  private def laskeVastaanottotila(valintatila: Valintatila, vastaanotto: Option[VastaanottoAction], aikataulu: Option[Vastaanottoaikataulu], viimeisinHakemuksenTilanMuutos: Option[Date]): ( Vastaanottotila, Option[DateTime] ) = {
    val vastaanottotila: Vastaanottotila =
      vastaanotto match {
        case Some(Poista) | None => Vastaanottotila.kesken
        case Some(Peru) => Vastaanottotila.perunut
        case Some(VastaanotaSitovasti) => Vastaanottotila.vastaanottanut
        case Some(VastaanotaEhdollisesti) => Vastaanottotila.ehdollisesti_vastaanottanut
        case Some(Peruuta) => Vastaanottotila.peruutettu
        case Some(MerkitseMyohastyneeksi) => Vastaanottotila.ei_vastaanotettu_määräaikana
      }

    vastaanottotila match {
      case Vastaanottotila.kesken if ( Valintatila.isHyväksytty(valintatila) || valintatila == Valintatila.perunut ) =>
        laskeVastaanottoDeadline(aikataulu, viimeisinHakemuksenTilanMuutos) match {
          case Some(deadline) if new DateTime().isAfter(deadline) => ( Vastaanottotila.ei_vastaanotettu_määräaikana, Some(deadline) )
          case deadline => (vastaanottotila, deadline)
        }
      case tila => (tila, None)
    }
  }

  private def laskeVastaanottoDeadline(aikataulu: Option[Vastaanottoaikataulu], viimeisinHakemuksenTilanMuutos: Option[Date]): Option[DateTime] = {
    (aikataulu) match {
      case Some(Vastaanottoaikataulu(Some(deadlineFromHaku), buffer)) =>
        val deadlineFromHakemuksenTilanMuutos = getDeadlineWithBuffer(viimeisinHakemuksenTilanMuutos, buffer, deadlineFromHaku)
        val deadlines = Some(deadlineFromHaku) ++ deadlineFromHakemuksenTilanMuutos
        Some(deadlines.maxBy((a: DateTime) => a.getMillis))
      case _ => None
    }
  }

  private def vastaanottotilanVaikutusValintatilaan(valintatila: Valintatila, vastaanottotila : Vastaanottotila): Valintatila = {
    if (List(Vastaanottotila.ehdollisesti_vastaanottanut, Vastaanottotila.vastaanottanut).contains(vastaanottotila)) {
      if (List(Valintatila.harkinnanvaraisesti_hyväksytty, Valintatila.varasijalta_hyväksytty, Valintatila.hyväksytty).contains(valintatila)) {
        valintatila
      } else {
         Valintatila.hyväksytty
      }
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

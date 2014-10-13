package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.{Optional, Date}

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO, HakutoiveenValintatapajonoDTO}
import fi.vm.sade.sijoittelu.tulos.dto.{HakemuksenTila, ValintatuloksenTila}
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila._
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila._
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritService
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService}
import org.joda.time.{LocalDate, LocalDateTime}

protected[sijoittelu] class YhteenvetoService(raportointiService: RaportointiService, ohjausparametritService: OhjausparametritService) {
  import scala.collection.JavaConversions._

  protected[sijoittelu] def hakemuksenYhteenveto(haku: Haku, hakemusOid: String): Option[HakemuksenYhteenveto] = {
    val aikataulu = ohjausparametritService.aikataulu(haku.oid)
    val hakija = fromOptional(raportointiService.latestSijoitteluAjoForHaku(haku.oid))
      .flatMap { sijoitteluAjo => Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid)) }

    hakija.map { hakija =>
      hakemuksenYhteenveto(haku, hakija, aikataulu)
    }
  }

  private def hakemuksenYhteenveto(haku: Haku, hakija: HakijaDTO, aikataulu: Option[Vastaanottoaikataulu]): HakemuksenYhteenveto = {
    val hakutoiveidenYhteenvedot = hakija.getHakutoiveet.toList.map { hakutoive: HakutoiveDTO =>
      val jono = getFirst(hakutoive).get
      var valintatila: Valintatila = ifNull(fromHakemuksenTila(jono.getTila()), Valintatila.kesken);
      if (valintatila == Valintatila.varalla && jono.isHyvaksyttyVarasijalta()) {
        valintatila = Valintatila.hyväksytty;
      }
      var vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      // Valintatila

      if (toisenToiveenVastaanottoEstaaVastaanoton(haku, jono, hakija, hakutoive)) {
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
        valintatila = Valintatila.peruuntunut;
      } else if (jono.getTila().isHyvaksytty()) {
        if (jono.isHyvaksyttyHarkinnanvaraisesti()) {
          valintatila = Valintatila.harkinnanvaraisesti_hyväksytty;
        }
        vastaanotettavuustila = Vastaanotettavuustila.vastaanotettavissa_sitovasti;
        if (haku.korkeakoulu && haku.yhteishaku) {
          if (hakutoive.getHakutoive() > 1) {
            if (aikaparametriLauennut(jono)) {
              vastaanotettavuustila = Vastaanotettavuustila.vastaanotettavissa_ehdollisesti;
            } else {
              if (ylempiaHakutoiveitaSijoittelematta(hakija, hakutoive)) {
                valintatila = Valintatila.kesken;
                vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
              } else if (ylempiaHakutoiveitaVaralla(hakija, hakutoive)) {
                vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
              }
            }
          }
        }
      } else if (!hakutoive.isKaikkiJonotSijoiteltu()) {
        valintatila = Valintatila.kesken;
      }

      var vastaanottotila = convertVastaanottotila(ifNull(jono.getVastaanottotieto(), ValintatuloksenTila.KESKEN));

      // Vastaanottotilan vaikutus valintatilaan
      if (List(Vastaanottotila.ehdollisesti_vastaanottanut, Vastaanottotila.vastaanottanut).contains(vastaanottotila)) {
        valintatila = Valintatila.hyväksytty;
      } else if (Vastaanottotila.perunut == vastaanottotila) {
        valintatila = Valintatila.perunut;
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      } else if (ValintatuloksenTila.ILMOITETTU == jono.getVastaanottotieto()) {
        valintatila = Valintatila.hyväksytty;
      } else if (Vastaanottotila.peruutettu == vastaanottotila) {
        valintatila = Valintatila.peruutettu;
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      } else if (Vastaanottotila.ei_vastaanotetu_määräaikana == vastaanottotila) {
        valintatila = Valintatila.perunut;
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      }

      if (vastaanottotila != Vastaanottotila.kesken) {
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      }

      val viimeisinValintatuloksenMuutos: Option[Date] = Option(jono.getValintatuloksenViimeisinMuutos());

      if(Vastaanotettavuustila.isVastaanotettavissa(vastaanotettavuustila) && new LocalDateTime().isAfter(getVastaanottoDeadline(aikataulu, viimeisinValintatuloksenMuutos))) {
        vastaanottotila = Vastaanottotila.ei_vastaanotetu_määräaikana
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa
      }

      val julkaistavissa = jono.getVastaanottotieto() != ValintatuloksenTila.KESKEN || jono.isJulkaistavissa();
      new HakutoiveenYhteenveto(hakutoive, jono, valintatila, vastaanottotila, vastaanotettavuustila, julkaistavissa, viimeisinValintatuloksenMuutos);
    }
    HakemuksenYhteenveto(hakija, aikataulu, hakutoiveidenYhteenvedot)

  }

  private def toisenToiveenVastaanottoEstaaVastaanoton(haku: Haku, jono: HakutoiveenValintatapajonoDTO, hakija: HakijaDTO, hakutoive: HakutoiveDTO) = {
    if(haku.korkeakoulu && haku.yhteishaku) {
      jono.getTila().isHyvaksyttyOrVaralla() && toinenHakutoiveVastaanotettu(hakija, hakutoive.getHakutoive())
    }
    else {
      false
    }
  }

  private def ylempiaHakutoiveitaSijoittelematta(hakija: HakijaDTO, hakutoive: HakutoiveDTO) = {
    ylemmatHakutoiveet(hakija, hakutoive.getHakutoive()).filter(toive => !toive.isKaikkiJonotSijoiteltu()).size > 0
  }

  private def ylempiaHakutoiveitaVaralla(hakija: HakijaDTO, hakutoive: HakutoiveDTO) = {
    ylemmatHakutoiveet(hakija, hakutoive.getHakutoive()).find { toive =>
      getFirst(toive).get.getTila == HakemuksenTila.VARALLA
    }.isDefined
  }

  private def fromHakemuksenTila(tila: HakemuksenTila): Valintatila = {
    Valintatila.withName(tila.name)
  }

  private def toinenHakutoiveVastaanotettu(hakija: HakijaDTO, hakutoive: Integer): Boolean = {
    return hakija.getHakutoiveet.find(h =>
      !h.getHakutoive().equals(hakutoive) && getFirst(h).get.getVastaanottotieto() == ValintatuloksenTila.VASTAANOTTANUT).isDefined
  }

  private def convertVastaanottotila(valintatuloksenTila: ValintatuloksenTila): Vastaanottotila = {
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
        Vastaanottotila.ei_vastaanotetu_määräaikana
      case ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT =>
        Vastaanottotila.ehdollisesti_vastaanottanut
      case ValintatuloksenTila.VASTAANOTTANUT_LASNA =>
        Vastaanottotila.vastaanottanut
      case ValintatuloksenTila.VASTAANOTTANUT_POISSAOLEVA =>
        Vastaanottotila.vastaanottanut
      case ValintatuloksenTila.VASTAANOTTANUT =>
        Vastaanottotila.vastaanottanut
      case _ =>
        throw new IllegalArgumentException("Unknown state: " + valintatuloksenTila)
    }
  }

  private def getVastaanottoDeadline(aikataulu: Option[Vastaanottoaikataulu], viimeisinValintatuloksenMuutos: Option[Date]) = {
      aikataulu match {
        case Some(Vastaanottoaikataulu(Some(deadlineAsDate), buffer)) =>
          val deadline = new LocalDateTime(deadlineAsDate)
          viimeisinValintatuloksenMuutos.map(new LocalDateTime(_).plusDays(buffer.getOrElse(0))) match {
            case Some(muutosDeadline) if(muutosDeadline.isAfter(deadline)) => muutosDeadline
            case _ => deadline
          }
        case _ => new LocalDateTime().plusYears(100)
      }
  }

  private def ifNull[T](value: T, defaultValue: T): T = {
    if (value == null) return defaultValue
    return value
  }

  private def aikaparametriLauennut(jono: HakutoiveenValintatapajonoDTO): Boolean = {
    if (jono.getVarasijojaKaytetaanAlkaen == null || jono.getVarasijojaTaytetaanAsti == null) {
      return false
    }
    val alkaen: LocalDate = new LocalDate(jono.getVarasijojaKaytetaanAlkaen)
    val asti: LocalDate = new LocalDate(jono.getVarasijojaTaytetaanAsti)
    val today: LocalDate = new LocalDate
    return !today.isBefore(alkaen) && !today.isAfter(asti)
  }

  private def ylemmatHakutoiveet(hakija: HakijaDTO, prioriteettiRaja: Integer): Set[HakutoiveDTO] = {
    return hakija.getHakutoiveet.toSet.filter(t => t.getHakutoive() < prioriteettiRaja)
  }

  private def getFirst(hakutoive: HakutoiveDTO): Option[HakutoiveenValintatapajonoDTO] = {
    val ordering = Ordering.fromLessThan{ (jono1: HakutoiveenValintatapajonoDTO, jono2: HakutoiveenValintatapajonoDTO) =>
      val tila1 = fromHakemuksenTila(jono1.getTila)
      val tila2 = fromHakemuksenTila(jono2.getTila)
      if (tila1 == Valintatila.varalla && tila2 == Valintatila.varalla) {
        jono1.getVarasijanNumero() < jono2.getVarasijanNumero()
      } else {
        tila1.compareTo(tila2) < 0;
      }
    }

    hakutoive.getHakutoiveenValintatapajonot.toList.sorted(ordering).headOption
  }

  def fromOptional[T](opt: Optional[T]) = {
    if (opt.isPresent) {
      Some(opt.get)
    } else {
      None
    }
  }
}

protected[sijoittelu] case class HakemuksenYhteenveto(hakija: HakijaDTO, aikataulu: Option[Vastaanottoaikataulu], hakutoiveet: List[HakutoiveenYhteenveto])

protected[sijoittelu] case class HakutoiveenYhteenveto (hakutoive: HakutoiveDTO, valintatapajono: HakutoiveenValintatapajonoDTO, valintatila: Valintatila, vastaanottotila: Vastaanottotila, vastaanotettavuustila: Vastaanotettavuustila, julkaistavissa: Boolean, viimeisinValintatuloksenMuutos: Option[Date])

package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date
import java.util.stream.Collectors
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakutoiveDTO, HakutoiveenValintatapajonoDTO, HakijaDTO}
import fi.vm.sade.sijoittelu.tulos.dto.{HakemuksenTila, ValintatuloksenTila, IlmoittautumisTila}
import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila._
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila._
import Ilmoittautumistila._
import org.joda.time.LocalDate
import org.joda.time.LocalDateTime

protected object YhteenvetoService {
  import collection.JavaConversions._
  def hakutoiveidenYhteenveto(aikataulu: Option[Vastaanottoaikataulu], hakija: HakijaDTO): List[HakutoiveenYhteenveto] = {
    hakija.getHakutoiveet.toList.map { hakutoive: HakutoiveDTO =>
      val jono = getFirst(hakutoive).get
      var valintatila: Valintatila = ifNull(fromHakemuksenTila(jono.getTila()), Valintatila.kesken);
      if (valintatila == Valintatila.varalla && jono.isHyvaksyttyVarasijalta()) {
        valintatila = Valintatila.hyväksytty;
      }
      var vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      // Valintatila

      if (jono.getTila().isHyvaksyttyOrVaralla() && toinenHakutoiveVastaanotettu(hakija, hakutoive.getHakutoive())) {
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
        valintatila = Valintatila.peruuntunut;
      } else if (jono.getTila().isHyvaksytty()) {
        if (jono.isHyvaksyttyHarkinnanvaraisesti()) {
          valintatila = Valintatila.harkinnanvaraisesti_hyväksytty;
        }
        vastaanotettavuustila = Vastaanotettavuustila.vastaanotettavissa_sitovasti;
        if (hakutoive.getHakutoive() > 1) {
          if (aikaparametriLauennut(jono)) {
            vastaanotettavuustila = Vastaanotettavuustila.vastaanotettavissa_ehdollisesti;
          } else {
            val ylempiaHakutoiveitaSijoittelematta = ylemmatHakutoiveet(hakija, hakutoive.getHakutoive()).filter(toive => !toive.isKaikkiJonotSijoiteltu()).size > 0;
            if (ylempiaHakutoiveitaSijoittelematta) {
              valintatila = Valintatila.kesken;
              vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
            } else {
              val ylempiaHakutoiveitaVaralla = ylemmatHakutoiveet(hakija, hakutoive.getHakutoive()).filter { toive =>
                getFirst(toive).get.getTila == HakemuksenTila.VARALLA
              }.size > 0;
              if (ylempiaHakutoiveitaVaralla) {
                vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
              }
            }
          }
        }
      } else if (!hakutoive.isKaikkiJonotSijoiteltu()) {
        valintatila = Valintatila.kesken;
      }

      val vastaanottotila = convertVastaanottotila(ifNull(jono.getVastaanottotieto(), ValintatuloksenTila.KESKEN));

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
        valintatila = Valintatila.peruuntunut;
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      }

      if (vastaanottotila != Vastaanottotila.kesken) {
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa;
      }

      val viimeisinVastaanottotilanMuutos: Option[Date] = Option(jono.getVastaanottotilanViimeisinMuutos());

      vastaanotettavuustila = checkAikataulu(vastaanotettavuustila, aikataulu, viimeisinVastaanottotilanMuutos)

      val julkaistavissa = jono.getVastaanottotieto() != ValintatuloksenTila.KESKEN || jono.isJulkaistavissa();
      new HakutoiveenYhteenveto(hakutoive, jono, valintatila, vastaanottotila, vastaanotettavuustila, julkaistavissa, viimeisinVastaanottotilanMuutos);
    }
  }

  private def fromHakemuksenTila(tila: HakemuksenTila): Valintatila = {
    Valintatila.withName(tila.name)
  }

  private def toinenHakutoiveVastaanotettu(hakija: HakijaDTO, hakutoive: Integer): Boolean = {
    return hakija.getHakutoiveet.find(h =>
      !h.getHakutoive().equals(hakutoive) && getFirst(h).get.getVastaanottotieto() == ValintatuloksenTila.VASTAANOTTANUT).isDefined
  }

  def yhteenveto(aikataulu: Option[Vastaanottoaikataulu], hakija: HakijaDTO): Hakemuksentulos = {
    return new Hakemuksentulos(hakija.getHakemusOid, aikataulu, hakutoiveidenYhteenveto(aikataulu, hakija).map { hakutoiveenYhteenveto =>
      new Hakutoiveentulos(
        hakutoiveenYhteenveto.hakutoive.getHakukohdeOid(),
        hakutoiveenYhteenveto.hakutoive.getTarjoajaOid(),
        hakutoiveenYhteenveto.valintatila,
        hakutoiveenYhteenveto.vastaanottotila,
        Ilmoittautumistila.withName(ifNull(hakutoiveenYhteenveto.valintatapajono.getIlmoittautumisTila(), IlmoittautumisTila.EI_TEHTY).name()),
        hakutoiveenYhteenveto.vastaanotettavuustila,
        Option(hakutoiveenYhteenveto.viimeisinVastaanottotilanMuutos.getOrElse(null)),
        Option(hakutoiveenYhteenveto.valintatapajono.getJonosija()).map(_.toInt),
        Option(hakutoiveenYhteenveto.valintatapajono.getVarasijojaKaytetaanAlkaen()),
        Option(hakutoiveenYhteenveto.valintatapajono.getVarasijojaTaytetaanAsti()),
        Option(hakutoiveenYhteenveto.valintatapajono.getVarasijanNumero()).map(_.toInt),
        hakutoiveenYhteenveto.julkaistavissa
      )
    })
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

  private def checkAikataulu(vastaanotettavuustila: Vastaanotettavuustila, aikataulu: Option[Vastaanottoaikataulu], viimeisinVastaanottotilanMuutos: Option[Date]) = {
    if(vastaanotettavuustila == Vastaanotettavuustila.ei_vastaanotettavissa) {
      Vastaanotettavuustila.ei_vastaanotettavissa
    }
    else {
      aikataulu match {
        case Some(Vastaanottoaikataulu(Some(deadline), buffer)) => {
          if(LocalDateTime.now().toDate().before(deadline)) {
            vastaanotettavuustila
          }
          else {
            viimeisinVastaanottotilanMuutos match {
              case None => Vastaanotettavuustila.ei_vastaanotettavissa
              case Some(muutos) => {
                if(LocalDateTime.now().isBefore(new LocalDateTime(muutos).plusDays(buffer.getOrElse(0)))) {
                  vastaanotettavuustila
                }
                else {
                  Vastaanotettavuustila.ei_vastaanotettavissa
                }
              }
            }
          }
        }
        case _ => vastaanotettavuustila
      }
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
}

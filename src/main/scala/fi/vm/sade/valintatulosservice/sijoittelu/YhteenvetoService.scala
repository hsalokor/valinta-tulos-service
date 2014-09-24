package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date
import java.util.stream.Collectors

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakutoiveYhteenvetoDTO, HakemusYhteenvetoDTO, Vastaanotettavuustila, HakijaDTO, YhteenvedonVastaanottotila, YhteenvedonValintaTila, HakutoiveenValintatapajonoDTO, HakutoiveDTO}
import fi.vm.sade.sijoittelu.tulos.dto.{HakemuksenTila, ValintatuloksenTila, IlmoittautumisTila}

import org.joda.time.LocalDate

case class HakutoiveenYhteenveto (hakutoive: HakutoiveDTO, valintatapajono: HakutoiveenValintatapajonoDTO, valintatila: YhteenvedonValintaTila, vastaanottotila: YhteenvedonVastaanottotila, vastaanotettavuustila: Vastaanotettavuustila, julkaistavissa: Boolean, viimeisinVastaanottotilanMuutos: Option[Date])

object YhteenvetoService {
  import collection.JavaConversions._
  def hakutoiveidenYhteenveto(hakija: HakijaDTO): List[HakutoiveenYhteenveto] = {
    hakija.getHakutoiveet.toList.map { hakutoive: HakutoiveDTO =>
      val jono = getFirst(hakutoive).get
      var valintatila = ifNull(YhteenvedonValintaTila.fromHakemuksenTila(jono.getTila()), YhteenvedonValintaTila.KESKEN);
      if (valintatila == YhteenvedonValintaTila.VARALLA && jono.isHyvaksyttyVarasijalta()) {
        valintatila = YhteenvedonValintaTila.HYVAKSYTTY;
      }
      var vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
      // Valintatila

      if (jono.getTila().isHyvaksyttyOrVaralla() && toinenHakutoiveVastaanotettu(hakija, hakutoive.getHakutoive())) {
        vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
        valintatila = YhteenvedonValintaTila.PERUUNTUNUT;
      } else if (jono.getTila().isHyvaksytty()) {
        if (jono.isHyvaksyttyHarkinnanvaraisesti()) {
          valintatila = YhteenvedonValintaTila.HARKINNANVARAISESTI_HYVAKSYTTY;
        }
        vastaanotettavuustila = Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI;
        if (hakutoive.getHakutoive() > 1) {
          if (aikaparametriLauennut(jono)) {
            vastaanotettavuustila = Vastaanotettavuustila.VASTAANOTETTAVISSA_EHDOLLISESTI;
          } else {
            val ylempiaHakutoiveitaSijoittelematta = ylemmatHakutoiveet(hakija, hakutoive.getHakutoive()).filter(toive => !toive.isKaikkiJonotSijoiteltu()).size > 0;
            if (ylempiaHakutoiveitaSijoittelematta) {
              valintatila = YhteenvedonValintaTila.KESKEN;
              vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
            } else {
              val ylempiaHakutoiveitaVaralla = ylemmatHakutoiveet(hakija, hakutoive.getHakutoive()).filter { toive =>
                getFirst(toive).get.getTila == HakemuksenTila.VARALLA
              }.size > 0;
              if (ylempiaHakutoiveitaVaralla) {
                vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
              }
            }
          }
        }
      } else if (!hakutoive.isKaikkiJonotSijoiteltu()) {
        valintatila = YhteenvedonValintaTila.KESKEN;
      }

      val vastaanottotila = convertVastaanottotila(ifNull(jono.getVastaanottotieto(), ValintatuloksenTila.KESKEN));

      // Vastaanottotilan vaikutus valintatilaan
      if (List(YhteenvedonVastaanottotila.EHDOLLISESTI_VASTAANOTTANUT, YhteenvedonVastaanottotila.VASTAANOTTANUT).contains(vastaanottotila)) {
        valintatila = YhteenvedonValintaTila.HYVAKSYTTY;
      } else if (YhteenvedonVastaanottotila.PERUNUT == vastaanottotila) {
        valintatila = YhteenvedonValintaTila.PERUNUT;
        vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
      } else if (ValintatuloksenTila.ILMOITETTU == jono.getVastaanottotieto()) {
        valintatila = YhteenvedonValintaTila.HYVAKSYTTY;
      } else if (YhteenvedonVastaanottotila.PERUUTETTU == vastaanottotila) {
        valintatila = YhteenvedonValintaTila.PERUUTETTU;
        vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
      } else if (YhteenvedonVastaanottotila.EI_VASTAANOTETTU_MAARA_AIKANA == vastaanottotila) {
        valintatila = YhteenvedonValintaTila.PERUUNTUNUT;
        vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
      }

      var viimeisinVastaanottotilanMuutos: Option[Date] = None;
      if (vastaanottotila != YhteenvedonVastaanottotila.KESKEN) {
        vastaanotettavuustila = Vastaanotettavuustila.EI_VASTAANOTETTAVISSA;
        viimeisinVastaanottotilanMuutos = Option(jono.getVastaanottotilanViimeisinMuutos());
      }

      val julkaistavissa = jono.getVastaanottotieto() != ValintatuloksenTila.KESKEN || jono.isJulkaistavissa();
      new HakutoiveenYhteenveto(hakutoive, jono, valintatila, vastaanottotila, vastaanotettavuustila, julkaistavissa, viimeisinVastaanottotilanMuutos);
    }
  }

  private def toinenHakutoiveVastaanotettu(hakija: HakijaDTO, hakutoive: Integer): Boolean = {
    return hakija.getHakutoiveet.find(h =>
      !h.getHakutoive().equals(hakutoive) && getFirst(h).get.getVastaanottotieto() == ValintatuloksenTila.VASTAANOTTANUT).isDefined
  }

  def yhteenveto(hakija: HakijaDTO): HakemusYhteenvetoDTO = {
    return new HakemusYhteenvetoDTO(hakija.getHakemusOid, hakutoiveidenYhteenveto(hakija).map { hakutoiveenYhteenveto =>
      new HakutoiveYhteenvetoDTO(
        hakutoiveenYhteenveto.hakutoive.getHakukohdeOid(),
        hakutoiveenYhteenveto.hakutoive.getTarjoajaOid(),
        hakutoiveenYhteenveto.valintatila,
        hakutoiveenYhteenveto.vastaanottotila,
        ifNull(hakutoiveenYhteenveto.valintatapajono.getIlmoittautumisTila(), IlmoittautumisTila.EI_TEHTY),
        hakutoiveenYhteenveto.vastaanotettavuustila,
        hakutoiveenYhteenveto.viimeisinVastaanottotilanMuutos.getOrElse(null),
        hakutoiveenYhteenveto.valintatapajono.getJonosija(),
        hakutoiveenYhteenveto.valintatapajono.getVarasijojaKaytetaanAlkaen(),
        hakutoiveenYhteenveto.valintatapajono.getVarasijojaTaytetaanAsti(),
        hakutoiveenYhteenveto.valintatapajono.getVarasijanNumero(),
        hakutoiveenYhteenveto.julkaistavissa
      )
    })
  }

  private def convertVastaanottotila(valintatuloksenTila: ValintatuloksenTila): YhteenvedonVastaanottotila = {
    valintatuloksenTila match {
      case ValintatuloksenTila.ILMOITETTU =>
        YhteenvedonVastaanottotila.KESKEN
      case ValintatuloksenTila.KESKEN =>
        YhteenvedonVastaanottotila.KESKEN
      case ValintatuloksenTila.PERUNUT =>
        YhteenvedonVastaanottotila.PERUNUT
      case ValintatuloksenTila.PERUUTETTU =>
        YhteenvedonVastaanottotila.PERUUTETTU
      case ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA =>
        YhteenvedonVastaanottotila.EI_VASTAANOTETTU_MAARA_AIKANA
      case ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT =>
        YhteenvedonVastaanottotila.EHDOLLISESTI_VASTAANOTTANUT
      case ValintatuloksenTila.VASTAANOTTANUT_LASNA =>
        YhteenvedonVastaanottotila.VASTAANOTTANUT
      case ValintatuloksenTila.VASTAANOTTANUT_POISSAOLEVA =>
        YhteenvedonVastaanottotila.VASTAANOTTANUT
      case ValintatuloksenTila.VASTAANOTTANUT =>
        YhteenvedonVastaanottotila.VASTAANOTTANUT
      case _ =>
        throw new IllegalArgumentException("Unknown state: " + valintatuloksenTila)
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
      val tila1 = YhteenvedonValintaTila.fromHakemuksenTila(jono1.getTila)
      val tila2 = YhteenvedonValintaTila.fromHakemuksenTila(jono2.getTila)
      if (tila1 == YhteenvedonValintaTila.VARALLA && tila2 == YhteenvedonValintaTila.VARALLA) {
        jono1.getVarasijanNumero() < jono2.getVarasijanNumero()
      } else {
        tila1.compareTo(tila2) < 0;
      }
    }

    hakutoive.getHakutoiveenValintatapajonot.toList.sorted(ordering).headOption
  }
}

package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat

import fi.vm.sade.sijoittelu.domain.{LogEntry, Valintatulos, ValintatuloksenTila}
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{Vastaanotettavuustila, HakemusYhteenvetoDTO, YhteenvedonValintaTila, YhteenvedonVastaanottotila}
import org.joda.time.{DateTimeUtils, LocalDate}
import org.junit.Assert._
import org.specs2.mutable.Specification

class VastaanottoServiceSpec extends Specification with ITSetup with TimeWarp {
  sequential

  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val hakukohdeOid: String = "1.2.246.562.5.16303028779"
  val hakemusOid: String = "1.2.246.562.11.00000441369"
  val muokkaaja: String = "Teppo Testi"
  val selite: String = "Testimuokkaus"

  "uusiValintatulosVastaanotaVäärälläArvolla" in {
    useFixture("hyvaksytty-ei-valintatulosta.json")
    getYhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA, muokkaaja, selite)}
    success
  }


  "uusiValintatulosVastaanota" in {
    useFixture("hyvaksytty-ei-valintatulosta.json")
    getYhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
    vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
    getYhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.VASTAANOTTANUT
  }


  "valintaTuloksenMuutoslogi" in {
    import collection.JavaConversions._
    useFixture("hyvaksytty-ei-valintatulosta.json")
    vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
    val valintatulos: Valintatulos = valintatulosDao.loadValintatulos(hakukohdeOid, "14090336922663576781797489829887", hakemusOid)
    val logEntries: List[LogEntry] = valintatulos.getLogEntries.toList
    logEntries.size must_== 1
    val logEntry: LogEntry = logEntries.get(0)
    logEntry.getMuutos must_== "VASTAANOTTANUT"
    logEntry.getSelite must_== selite
    logEntry.getMuokkaaja must_== muokkaaja
    new LocalDate(logEntry.getLuotu) must_== new LocalDate
  }

  "uusiValintatulosPeru" in {
    useFixture("hyvaksytty-ei-valintatulosta.json")
    vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.PERUNUT, muokkaaja, selite)
    getYhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.PERUNUT
  }

  "vastaanotaEhdollisestiKunAikaparametriEiLauennut" in {
    useFixture("hyvaksytty-ylempi-varalla.json")
    expectFailure {
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, muokkaaja, selite)
    }
  }

  "vastaanotaEhdollisestiKunAikaparametriLauennut" in {
    useFixture("hyvaksytty-ylempi-varalla.json")
    withFixedDate("15.8.2014") {
      getYhteenveto.hakutoiveet.get(0).valintatila must_== YhteenvedonValintaTila.VARALLA
      getYhteenveto.hakutoiveet.get(1).valintatila must_== YhteenvedonValintaTila.HYVAKSYTTY
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, muokkaaja, selite)
      getYhteenveto.hakutoiveet.get(1).valintatila must_== YhteenvedonValintaTila.HYVAKSYTTY
      getYhteenveto.hakutoiveet.get(1).vastaanottotila must_== YhteenvedonVastaanottotila.EHDOLLISESTI_VASTAANOTTANUT
      getYhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
      getYhteenveto.hakutoiveet.get(0).valintatila must_== YhteenvedonValintaTila.VARALLA
    }
  }

  "vastaanotaSitovastiKunAikaparametriLauennut" in {
    useFixture("hyvaksytty-ylempi-varalla.json")
    withFixedDate("15.8.2014") {
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      yhteenveto.hakutoiveet.get(1).valintatila must_== YhteenvedonValintaTila.HYVAKSYTTY
      yhteenveto.hakutoiveet.get(1).vastaanottotila must_== YhteenvedonVastaanottotila.VASTAANOTTANUT
      yhteenveto.hakutoiveet.get(1).vastaanotettavuustila must_== Vastaanotettavuustila.EI_VASTAANOTETTAVISSA

      yhteenveto.hakutoiveet.get(0).valintatila must_== YhteenvedonValintaTila.PERUUNTUNUT
      yhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
      yhteenveto.hakutoiveet.get(0).vastaanotettavuustila must_== Vastaanotettavuustila.EI_VASTAANOTETTAVISSA
    }
  }

  "vastaanotaYlempiKunKaksiHyvaksyttya" in {
    useFixture("hyvaksytty-julkaisematon-hyvaksytty.json")
    vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
    val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
    yhteenveto.hakutoiveet.get(0).valintatila must_== YhteenvedonValintaTila.HYVAKSYTTY
    yhteenveto.hakutoiveet.get(1).valintatila must_== YhteenvedonValintaTila.PERUUNTUNUT
    yhteenveto.hakutoiveet.get(2).valintatila must_== YhteenvedonValintaTila.PERUUNTUNUT
    yhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.VASTAANOTTANUT
    yhteenveto.hakutoiveet.get(1).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
    yhteenveto.hakutoiveet.get(2).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
  }


  "vastaanotaAlempiKunKaksiHyvaksyttya" in {
    useFixture("hyvaksytty-julkaisematon-hyvaksytty.json")
    vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738904", ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
    val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
    yhteenveto.hakutoiveet.get(0).valintatila must_== YhteenvedonValintaTila.PERUUNTUNUT
    yhteenveto.hakutoiveet.get(1).valintatila must_== YhteenvedonValintaTila.PERUUNTUNUT
    yhteenveto.hakutoiveet.get(2).valintatila must_== YhteenvedonValintaTila.HYVAKSYTTY
    yhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
    yhteenveto.hakutoiveet.get(1).vastaanottotila must_== YhteenvedonVastaanottotila.KESKEN
    yhteenveto.hakutoiveet.get(2).vastaanottotila must_== YhteenvedonVastaanottotila.VASTAANOTTANUT
  }

  "vastaanotaAiemminVastaanotettu" in {
    useFixture("hyvaksytty-vastaanottanut.json")
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
  }

  "hakemustaEiLöydy" in {
    useFixture("hyvaksytty-ei-valintatulosta.json")
    expectFailure { vastaanota(hakuOid, hakemusOid + 1, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
  }

  "hakukohdettaEiLöydy" in {
    useFixture("hyvaksytty-ei-valintatulosta.json")
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid + 1, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
  }

  "vastaanotaIlmoitettu" in {
    useFixture("hyvaksytty-ilmoitettu.json")
    vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
    getYhteenveto.hakutoiveet.get(0).vastaanottotila must_== YhteenvedonVastaanottotila.VASTAANOTTANUT
  }

  "vastaanotaEhdollisestiVastaanotettu" in {
    useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json")
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
  }

  lazy val sijoitteluClient = appConfig.sijoitteluContext.sijoitteluClient
  lazy val valintatulosDao = appConfig.sijoitteluContext.valintatulosDao

  def getYhteenveto: HakemusYhteenvetoDTO = sijoitteluClient.yhteenveto(hakuOid, hakemusOid).get

  lazy val vastaanottoService = appConfig.sijoitteluContext.vastaanottoService


  // TODO: expect failure

  def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: ValintatuloksenTila, muokkaaja: String, selite: String) = {
    vastaanottoService.vastaanota(hakuOid, hakemusOid, hakukohdeOid, tila, muokkaaja, selite)
    success
  }

  def expectFailure[T](block: => T) = {
    try {
      block
      failure("Expected exception")
    } catch {
      case e: IllegalArgumentException => success
    }
  }
}

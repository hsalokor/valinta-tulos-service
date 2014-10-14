package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.sijoittelu.domain.{LogEntry, ValintatuloksenTila, Valintatulos}
import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila._
import fi.vm.sade.valintatulosservice.domain.{Ilmoittautuminen, _}
import fi.vm.sade.valintatulosservice.tarjonta.{HakuService, HakuFixtures}
import fi.vm.sade.valintatulosservice.{ITSetup, TimeWarp}
import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VastaanottoServiceSpec extends Specification with ITSetup with TimeWarp {
  sequential

  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val hakukohdeOid: String = "1.2.246.562.5.16303028779"
  val vastaanotettavissaHakuKohdeOid = "1.2.246.562.5.72607738902"
  val hakemusOid: String = "1.2.246.562.11.00000441369"
  val muokkaaja: String = "Teppo Testi"
  val selite: String = "Testimuokkaus"
  val hakuFixture = HakuFixtures.korkeakouluYhteishaku

  "vastaanotto ilman valintatulosta ei onnistu" in {
    useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
    hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
    success
  }

  "peruminen ilman valintatulosta ei onnistu" in {
    useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
    hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.PERUNUT, muokkaaja, selite)}
    success
  }

  "ilmoittautuminen peruttuun kohteeseen ei onnistu" in {
    useFixture("hyvaksytty-ilmoitettu.json", hakuFixture = hakuFixture)
    vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, ValintatuloksenTila.PERUNUT, muokkaaja, selite)
    hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
    expectFailure{ilmoittaudu(hakuOid, hakemusOid, hakukohdeOid, läsnä_koko_lukuvuosi, muokkaaja, selite)}
  }

  "ilmoitetun Valintatuloksen vastaanotto onnistuu jos viimeisin muutos on bufferin sisään" in {
    useFixture("hyvaksytty-kesken-julkaistavissa.json", "vastaanotto-loppunut", hakuFixture = hakuFixture)
    withFixedDateTime("9.9.2014 19:01") {
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
      vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
    }
  }

  "ilmoitetunValintatuloksenVastaanottoEiOnnistuDeadlinenjälkeenJosEiOleBufferiaAnnettu" in {
    useFixture("hyvaksytty-kesken-julkaistavissa.json", "ei-vastaanotto-bufferia", hakuFixture = hakuFixture)
    withFixedDateTime("1.9.2014 12:01") {
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ei_vastaanotetu_määräaikana
      hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.ei_vastaanotettavissa
      expectFailure {
        vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      }
    }
  }

  "ilmoitetunValintatuloksenVastaanottoEiOnnistuJosViimeisinMuutosOnBufferinJälkeen" in {
    useFixture("hyvaksytty-kesken-julkaistavissa.json", "vastaanotto-loppunut", hakuFixture = hakuFixture)
    withFixedDateTime("9.9.2014 19:10") {
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ei_vastaanotetu_määräaikana
      hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.ei_vastaanotettavissa
      expectFailure {
        vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      }
    }
  }

  "valintaTuloksenMuutoslogi"  in {
    import scala.collection.JavaConversions._
    useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
    vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
    val valintatulos: Valintatulos = valintatulosDao.loadValintatulos(vastaanotettavissaHakuKohdeOid, "14090336922663576781797489829886", hakemusOid)
    val logEntries: List[LogEntry] = valintatulos.getLogEntries.toList
    logEntries.size must_== 2
    val logEntry: LogEntry = logEntries(1)
    logEntry.getMuutos must_== "VASTAANOTTANUT"
    logEntry.getSelite must_== selite
    logEntry.getMuokkaaja must_== muokkaaja
    new LocalDate(logEntry.getLuotu) must_== new LocalDate
  }

  "vastaanotaEhdollisestiKunAikaparametriEiLauennut" in {
    useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
    expectFailure {
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, muokkaaja, selite)
    }
  }

  "vastaanotaEhdollisestiKunAikaparametriLauennut" in {
    useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
    withFixedDateTime("15.8.2014 12:00") {
      hakemuksenTulos.hakutoiveet(0).valintatila must_== Valintatila.varalla
      hakemuksenTulos.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT, muokkaaja, selite)
      hakemuksenTulos.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      hakemuksenTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaanottanut
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      hakemuksenTulos.hakutoiveet(0).valintatila must_== Valintatila.varalla
    }
  }

  "vastaanotaSitovastiKunAikaparametriLauennut" in {
    useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
    withFixedDateTime("15.8.2014 12:00") {
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      val yhteenveto = hakemuksenTulos
      yhteenveto.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.vastaanottanut
      yhteenveto.hakutoiveet(1).vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa

      yhteenveto.hakutoiveet(0).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(0).vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
    }
  }

  "korkeakoulujen yhteishaku" in {
    "vastaanota ylempi kun kaksi hyvaksyttyä -> alemmat peruuntuvat" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture, hakemusFixture = "00000441369-3")
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      val yhteenveto = hakemuksenTulos
      yhteenveto.hakutoiveet(0).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(1).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(2).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
      yhteenveto.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(2).vastaanottotila must_== Vastaanottotila.kesken
    }

    "vastaanota alempi kun kaksi hyväksyttyä -> muut peruuntuvat" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture, hakemusFixture = "00000441369-3")
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738904", ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      val yhteenveto = hakemuksenTulos
      yhteenveto.hakutoiveet(0).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(1).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(2).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(2).vastaanottotila must_== Vastaanottotila.vastaanottanut
    }

    "vastaanota ehdollisesti vastaanotettu -> ERROR" in {
      useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json", hakuFixture = hakuFixture)
      expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
    }
  }

  "toisen asteen oppilaitosten yhteishaku" in {
    "vastaanota alempi kun kaksi hyvaksyttya -> muut eivät peruunnut" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = HakuFixtures.toinenAsteYhteishaku, hakemusFixture = "00000441369-3")
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738904", ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
      val yhteenveto = hakemuksenTulos
      yhteenveto.hakutoiveet(0).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(1).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(2).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(2).vastaanottotila must_== Vastaanottotila.vastaanottanut
    }
  }


  "vastaanotaAiemminVastaanotettu" in {
    useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
  }

  "hakemustaEiLöydy" in {
    useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
    expectFailure { vastaanota(hakuOid, hakemusOid + 1, hakukohdeOid, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
  }

  "hakukohdettaEiLöydy" in {
    useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
    expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid + 1, ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)}
  }

  "vastaanotaIlmoitettu" in {
    useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
    vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", ValintatuloksenTila.VASTAANOTTANUT, muokkaaja, selite)
    hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
    ilmoittaudu(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", läsnä_koko_lukuvuosi, muokkaaja, selite)
    hakemuksenTulos.hakutoiveet(0).ilmoittautumistila must_== läsnä_koko_lukuvuosi
  }

  private lazy val valintatulosDao = appConfig.sijoitteluContext.valintatulosDao
  private lazy val valintatulosService = appConfig.sijoitteluContext.valintatulosService
  private lazy val vastaanottoService = appConfig.sijoitteluContext.vastaanottoService

  private def hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).get

  private def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: ValintatuloksenTila, muokkaaja: String, selite: String) = {
    vastaanottoService.vastaanota(hakuOid, hakemusOid, hakukohdeOid, tila, muokkaaja, selite)
    success
  }

  private def ilmoittaudu(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: Ilmoittautumistila, muokkaaja: String, selite: String) = {
    vastaanottoService.ilmoittaudu(hakuOid, hakemusOid, Ilmoittautuminen(hakukohdeOid, tila, muokkaaja, selite))
    success
  }

  private def expectFailure[T](block: => T) = {
    try {
      block
      failure("Expected exception")
    } catch {
      case e: IllegalArgumentException => success
    }
  }
}

package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.sijoittelu.domain.{LogEntry, ValintatuloksenTila, Valintatulos}
import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila._
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritFixtures
import fi.vm.sade.valintatulosservice.sijoittelu.SijoittelutulosService
import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakukohdeRecordService, ValintarekisteriDb}
import org.joda.time.{DateTime, LocalDate}
import org.junit.runner.RunWith
import org.specs2.execute.Result
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VastaanottoServiceSpec extends ITSpecification with TimeWarp {
  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val hakukohdeOid: String = "1.2.246.562.5.16303028779"
  val vastaanotettavissaHakuKohdeOid = "1.2.246.562.5.72607738902"
  val hakemusOid: String = "1.2.246.562.11.00000441369"
  val muokkaaja: String = "Teppo Testi"
  val personOid: String = "1.2.246.562.24.14229104472"
  val selite: String = "Testimuokkaus"
  val ilmoittautumisaikaPaattyy2100: Ilmoittautumisaika = Ilmoittautumisaika(None, Some(new DateTime(2100, 1, 10, 23, 59, 59, 999)))

  def kaikkienHakutyyppienTestit(hakuFixture: String) = {
    "vastaanota hyväksytty julkaistu tulos" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
    }

    "virhetilanteet" in {
      "vastaanota aiemmin vastaanotettu" in {
        useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
        expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)}
      }

      "hakemusta ei löydy" in {
        useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
        expectFailure { vastaanota(hakuOid, hakemusOid + 1, hakukohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)}
      }

      "hakukohdetta ei löydy" in {
        useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
        expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid + 1, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)}
      }

      "vastaanotto ilman valintatulosta" in {
        useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
        hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
        expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)}
        success
      }

      "peruminen ilman valintatulosta " in {
        useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
        hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
        expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.perunut, muokkaaja, selite, personOid)}
        success
      }

      "paikan peruminen varsinaisessa haussa, kun lisähaussa vastaanottavissa, ei peru lisähaun paikkoja" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanotettavissa.json"), hakuFixture = hakuFixture)
        hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230").hakutoiveet(0).vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
        vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.perunut, muokkaaja, selite, personOid)
        hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230").hakutoiveet(0).vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
        val lisaHaunTulos = hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230")
        lisaHaunTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
        lisaHaunTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.kesken
      }
    }

    "vastaanoton aikataulu" in {
      "vastaanotto onnistuu jos viimeisin valintatuloksen muutos on bufferin sisään" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", ohjausparametritFixture = "vastaanotto-loppunut", hakuFixture = hakuFixture)
        withFixedDateTime("9.9.2014 12:00") {
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
          hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
          vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
        }
      }

      "vastaanotto ei onnistu jos ei bufferia annettu ollenkaan, vaikka vastaanotto samana päivänä kuin muutos" in {
        useFixture("hyvaksytty-varasijalta-kesken-julkaistavissa.json", ohjausparametritFixture = "ei-vastaanotto-bufferia", hakuFixture = hakuFixture)
        withFixedDateTime("02.9.2014 12:00") {
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ei_vastaanotettu_määräaikana
          hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.ei_vastaanotettavissa
          expectFailure {
            vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
          }
        }
      }

      "vastaanotto onnistuu jos viimeisin hakemuksen tilan muutos on bufferin sisään" in {
        useFixture("hyvaksytty-varasijalta-kesken-julkaistavissa.json", ohjausparametritFixture = "vastaanotto-loppunut", hakuFixture = hakuFixture)
        withFixedDateTime("16.9.2014 12:00") {
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
          hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
          vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
        }
      }

      "vastaanotto ei onnistu jos viimeisin hakemuksen tilan muutos ei ole bufferin sisään" in {
        useFixture("hyvaksytty-varasijalta-kesken-julkaistavissa.json", ohjausparametritFixture = "vastaanotto-loppunut", hakuFixture = hakuFixture)
        withFixedDateTime("16.9.2014 12:01") {
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ei_vastaanotettu_määräaikana
          hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.ei_vastaanotettavissa
          expectFailure {
            vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
          }
        }
      }

      "vastaanotto ei onnistu deadlinen jälkeen jos vastaanottobufferia ei ole annettu" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", ohjausparametritFixture = "ei-vastaanotto-bufferia", hakuFixture = hakuFixture)
        withFixedDateTime("1.9.2014 12:01") {
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ei_vastaanotettu_määräaikana
          hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.ei_vastaanotettavissa
          expectFailure {
            vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
          }
        }
      }

      "vastaanotto ei onnistu jos viimeisin valintatuloksen muutos on bufferin jälkeen" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", ohjausparametritFixture = "vastaanotto-loppunut", hakuFixture = hakuFixture)
        withFixedDateTime("9.9.2014 12:01") {
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ei_vastaanotettu_määräaikana
          hakemuksenTulos.hakutoiveet(0).vastaanotettavuustila  must_== Vastaanotettavuustila.ei_vastaanotettavissa
          expectFailure {
            vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
          }
        }
      }
    }

    "ilmoittautuminen" in {
      "virhetilanteet" in {
        "ilmoittautuminen peruttuun kohteeseen" in {
          useFixture("hyvaksytty-ilmoitettu.json", hakuFixture = hakuFixture)
          vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.perunut, muokkaaja, selite, personOid)
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
          expectFailure(Some("""Hakutoive 1.2.246.562.5.72607738902 ei ole ilmoittauduttavissa: ilmoittautumisaika: {"loppu":"2100-01-10T21:59:59Z"}, ilmoittautumistila: EI_TEHTY, valintatila: PERUNUT, vastaanottotila: PERUNUT"""))
            {ilmoittaudu(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, läsnä_koko_lukuvuosi, muokkaaja, selite)}
        }

        "ilmoittautuminen ilman vastaanottoa" in {
          useFixture("hyvaksytty-ilmoitettu.json", hakuFixture = hakuFixture)
          expectFailure(Some("""Hakutoive 1.2.246.562.5.72607738902 ei ole ilmoittauduttavissa: ilmoittautumisaika: {"loppu":"2100-01-10T21:59:59Z"}, ilmoittautumistila: EI_TEHTY, valintatila: HYVAKSYTTY, vastaanottotila: KESKEN"""))
            {ilmoittaudu(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, läsnä_koko_lukuvuosi, muokkaaja, selite)}
        }

        "kahteen kertaan ilmoittautuminen" in {
          useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
          vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
          ilmoittaudu(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, läsnä_koko_lukuvuosi, muokkaaja, selite)
          hakemuksenTulos.hakutoiveet(0).ilmoittautumistila.ilmoittautumistila must_== Ilmoittautumistila.läsnä_koko_lukuvuosi
          hakemuksenTulos.hakutoiveet(0).ilmoittautumistila.ilmoittauduttavissa must_== false
          expectFailure(Some("""Hakutoive 1.2.246.562.5.72607738902 ei ole ilmoittauduttavissa: ilmoittautumisaika: {"loppu":"2100-01-10T21:59:59Z"}, ilmoittautumistila: LASNA_KOKO_LUKUVUOSI, valintatila: HYVAKSYTTY, vastaanottotila: VASTAANOTTANUT_SITOVASTI"""))
            {ilmoittaudu(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, läsnä_koko_lukuvuosi, muokkaaja, selite)}
        }
      }
    }
  }

  "korkeakoulujen yhteishaku" in {
    val hakuFixture = HakuFixtures.korkeakouluYhteishaku

    kaikkienHakutyyppienTestit(hakuFixture)

    "vastaanota ylempi kun kaksi hyvaksyttyä -> alemmat peruuntuvat" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture, hakemusFixtures = List("00000441369-3"))
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      val yhteenveto = hakemuksenTulos
      yhteenveto.hakutoiveet(0).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(1).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(2).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
      valintatulosDao.loadValintatulos("1.2.246.562.5.72607738902", "14090336922663576781797489829886", hakemusOid).getTila must_== ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI
      yhteenveto.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(2).vastaanottotila must_== Vastaanottotila.kesken
    }

    "vastaanota alempi kun kaksi hyväksyttyä -> muut peruuntuvat" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture, hakemusFixtures = List("00000441369-3"))
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738904", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
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
      expectFailure { vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)}
    }

    // Tilat tarkistetaan suoraan kannasta jotta varmistutaan siitä että virkailijakäyttöliittymä saa identtiset tilat lukiessaan
    // suoraan kantaa, historiallisesti OHP tekee kannan tilan pohjalta päättelyä siitä mitä oppijalle näytetään
    "yhden paikan sääntö" in {
      "vastaanota varsinaisessa haussa, kun lisähaussa vastaanottavissa -> lisähaun paikka peruuntuu" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanotettavissa.json"), hakuFixture = hakuFixture)
        hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230").hakutoiveet(0).vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
        vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
        val lisaHaunTulos = hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230")
        lisaHaunTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
        lisaHaunTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.perunut
        val valintatulos: Valintatulos = valintatulosDao.loadValintatulos(lisaHaunTulos.hakutoiveet(0).hakukohdeOid, lisaHaunTulos.hakutoiveet(0).valintatapajonoOid, lisaHaunTulos.hakemusOid)
        assertSecondLogEntry(valintatulos, "tila: KESKEN -> PERUNUT", "VASTAANOTTANUT_SITOVASTI paikan 1.2.246.562.5.72607738902 toisesta hausta 1.2.246.562.5.2013080813081926341928")
      }

      "vastaanota lisähaussa, kun varsinaisessa haussa vastaanottavissa -> varsinaisen haun paikka peruuntuu" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanotettavissa.json"), hakuFixture = hakuFixture)
        val tila = (hakukohdeOid: String, jonoOid: String, hakemusOid: String) => () => valintatulosDao.loadValintatulos(hakukohdeOid, jonoOid, hakemusOid).getTila
        val lisahakuHakemusOid = "1.2.246.562.11.00000878230"
        val lisahaunVastaanotettavaHakukohdeOid = "1.2.246.562.14.2014022408541751568934"
        val lisahaunVastaanotettavaTila = tila(lisahaunVastaanotettavaHakukohdeOid, "14090336922663576781797489829886", lisahakuHakemusOid)
        val lisahaunPeruuntuvaTila = tila("1.2.246.562.14.2013120515524070995659", "14090336922663576781797489829887", lisahakuHakemusOid)
        val varsinaisenHaunPeruuntuvaTila = tila(vastaanotettavissaHakuKohdeOid, "14090336922663576781797489829886", hakemusOid)

        lisahaunVastaanotettavaTila() must_== ValintatuloksenTila.KESKEN
        lisahaunPeruuntuvaTila() must_== ValintatuloksenTila.KESKEN
        varsinaisenHaunPeruuntuvaTila() must_== ValintatuloksenTila.KESKEN

        vastaanota("korkeakoulu-lisahaku1", lisahakuHakemusOid, lisahaunVastaanotettavaHakukohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)

        lisahaunVastaanotettavaTila() must_== ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI
        lisahaunPeruuntuvaTila() must_== ValintatuloksenTila.PERUNUT
        varsinaisenHaunPeruuntuvaTila() must_== ValintatuloksenTila.PERUNUT

        val varsinaisenHaunValintatulos = valintatulosDao.loadValintatulos(vastaanotettavissaHakuKohdeOid, "14090336922663576781797489829886", hakemusOid)
        assertSecondLogEntry(varsinaisenHaunValintatulos, "tila: KESKEN -> PERUNUT", s"VASTAANOTTANUT_SITOVASTI paikan $lisahaunVastaanotettavaHakukohdeOid toisesta hausta korkeakoulu-lisahaku1")

        expectFailure(Some(s"Väärä vastaanottotila toisen haun korkeakoulu-lisahaku1 kohteella $lisahaunVastaanotettavaHakukohdeOid: VASTAANOTTANUT_SITOVASTI (yritetty muutos: VASTAANOTTANUT_SITOVASTI $hakukohdeOid)")) {
          tarkistaVastaanotettavuus(vastaanotettavissaHakuKohdeOid, hakemusOid, hakukohdeOid)
        }
      }
      "vastaanota lisahaussa kahdesta hakutoiveesta toinen -> ei-vastaanotettu paikka peruuntuu" in {
        useFixture("lisahaku-vastaanotettavissa.json", hakuFixture = HakuFixtures.korkeakouluLisahaku1, hakemusFixtures = List("00000878230"))
        vastaanota("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230", "1.2.246.562.14.2013120515524070995659", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
        val lisaHaunTulos = hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230")

        val tulos0: Valintatulos = valintatulosDao.loadValintatulos(lisaHaunTulos.hakutoiveet(0).hakukohdeOid, lisaHaunTulos.hakutoiveet(0).valintatapajonoOid, lisaHaunTulos.hakemusOid)
        tulos0.getTila must_== ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI
        assertSecondLogEntry(tulos0, "tila: KESKEN -> VASTAANOTTANUT_SITOVASTI", "Testimuokkaus")

        val tulos1: Valintatulos = valintatulosDao.loadValintatulos(lisaHaunTulos.hakutoiveet(1).hakukohdeOid, lisaHaunTulos.hakutoiveet(1).valintatapajonoOid, lisaHaunTulos.hakemusOid)
        tulos1.getTila must_== ValintatuloksenTila.PERUNUT
        assertSecondLogEntry(tulos1, "tila: KESKEN -> PERUNUT", "VASTAANOTTANUT_SITOVASTI paikan 1.2.246.562.14.2013120515524070995659 toisesta hausta korkeakoulu-lisahaku1")
      }

      "vastaanota ehdollisesti varsinaisessa haussa, kun lisähaussa vastaanottavissa -> lisähaun paikka peruuntuu" in {
        useFixture("hyvaksytty-ylempi-varalla.json", List("lisahaku-vastaanotettavissa.json"), hakuFixture = hakuFixture)
        hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230").hakutoiveet(0).vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
        vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja, selite, personOid)
        val lisaHaunTulos = hakemuksenTulos("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230")
        lisaHaunTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
        lisaHaunTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.perunut
        val valintatulos: Valintatulos = valintatulosDao.loadValintatulos(lisaHaunTulos.hakutoiveet(0).hakukohdeOid, lisaHaunTulos.hakutoiveet(0).valintatapajonoOid, lisaHaunTulos.hakemusOid)
        assertSecondLogEntry(valintatulos, "tila: KESKEN -> PERUNUT", "EHDOLLISESTI_VASTAANOTTANUT paikan 1.2.246.562.5.16303028779 toisesta hausta 1.2.246.562.5.2013080813081926341928")
      }

      "vastaanota lisähaussa, kun varsinaisessa haussa jo vastaanottanut -> ERROR" in {
        useFixture("hyvaksytty-vastaanottanut.json", List("lisahaku-vastaanotettavissa.json"), hakuFixture = hakuFixture)
        expectFailure(Some("Väärä vastaanottotila toisen haun 1.2.246.562.5.2013080813081926341928 kohteella 1.2.246.562.5.72607738902: VASTAANOTTANUT_SITOVASTI (yritetty muutos: VASTAANOTTANUT_SITOVASTI 1.2.246.562.14.2014022408541751568934)")) {
          vastaanota("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230", "1.2.246.562.14.2014022408541751568934", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
        }
      }

      "vastaanota lisähaussa, kun varsinaisessa haussa jo ehdollisesti vastaanottanut -> ERROR" in {
        useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json", List("lisahaku-vastaanotettavissa.json"), hakuFixture = hakuFixture)
        expectFailure(Some("Väärä vastaanottotila toisen haun 1.2.246.562.5.2013080813081926341928 kohteella 1.2.246.562.5.16303028779: EHDOLLISESTI_VASTAANOTTANUT (yritetty muutos: VASTAANOTTANUT_SITOVASTI 1.2.246.562.14.2014022408541751568934)")) {
          vastaanota("korkeakoulu-lisahaku1", "1.2.246.562.11.00000878230", "1.2.246.562.14.2014022408541751568934", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
        }
      }

      "vastaanota varsinaisessa haussa, kun lisähaussa jo vastaanottanut -> ERROR" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanottanut.json"), hakuFixture = hakuFixture)
        expectFailure(Some("Väärä vastaanottotila toisen haun korkeakoulu-lisahaku1 kohteella 1.2.246.562.14.2014022408541751568934: VASTAANOTTANUT_SITOVASTI (yritetty muutos: VASTAANOTTANUT_SITOVASTI 1.2.246.562.5.72607738902)")) {
          vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
        }
      }

      "peruminen varsinaisessa haussa, kun lisähaussa jo vastaanottanut, onnistuu" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanottanut.json"), hakuFixture = hakuFixture)
        vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.perunut, muokkaaja, selite, personOid)
      }

      "vastaanota ehdollisesti varsinaisessa haussa, kun lisähaussa jo vastaanottanut -> ERROR" in {
        useFixture("hyvaksytty-ylempi-varalla.json", List("lisahaku-vastaanottanut.json"), hakuFixture = hakuFixture)
        expectFailure(Some("Väärä vastaanottotila toisen haun korkeakoulu-lisahaku1 kohteella 1.2.246.562.14.2014022408541751568934: VASTAANOTTANUT_SITOVASTI (yritetty muutos: EHDOLLISESTI_VASTAANOTTANUT 1.2.246.562.5.16303028779)")) {
          vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja, selite, personOid)
        }
      }

      "vastaanota varsinaisessa haussa, kun lisähaussa jo vastaanottanut ehdollisesti -> ERROR" in {
        useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanottanut-ehdollisesti.json"), hakuFixture = hakuFixture)
        expectFailure(Some("Väärä vastaanottotila toisen haun korkeakoulu-lisahaku1 kohteella 1.2.246.562.14.2014022408541751568934: EHDOLLISESTI_VASTAANOTTANUT (yritetty muutos: VASTAANOTTANUT_SITOVASTI 1.2.246.562.5.72607738902)")) {
          vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
        }
      }

    }

    "vastaanota ehdollisesti kun varasijasäännöt eivät ole vielä voimassa" in {
      useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture, ohjausparametritFixture = OhjausparametritFixtures.varasijasaannotEiVielaVoimassa)
      expectFailure {
        vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja, selite, personOid)
      }
    }

    "vastaanota ehdollisesti kun varasijasäännöt voimassa" in {
      useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
      hakemuksenTulos.hakutoiveet(0).valintatila must_== Valintatila.varalla
      hakemuksenTulos.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja, selite, personOid)
      hakemuksenTulos.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      hakemuksenTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaanottanut
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      hakemuksenTulos.hakutoiveet(0).valintatila must_== Valintatila.varalla
    }

    "vastaanota sitovasti kun varasijasäännöt voimassa" in {
      useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
      vastaanota(hakuOid, hakemusOid, hakukohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      val yhteenveto = hakemuksenTulos
      yhteenveto.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.vastaanottanut
      yhteenveto.hakutoiveet(1).vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa

      yhteenveto.hakutoiveet(0).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(0).vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
    }

    "Valintatuloksen muutoslogi"  in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
      vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      val valintatulos: Valintatulos = valintatulosDao.loadValintatulos(vastaanotettavissaHakuKohdeOid, "14090336922663576781797489829886", hakemusOid)
      assertSecondLogEntry(valintatulos, "tila: KESKEN -> VASTAANOTTANUT_SITOVASTI", selite)
    }

    "ilmoittautuminen" in {
      "onnistuu ja tarjotaaan oilia, jos vastaanottanut" in {
        useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
        hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
        hakemuksenTulos.hakutoiveet(0).ilmoittautumistila must_== HakutoiveenIlmoittautumistila(ilmoittautumisaikaPaattyy2100, Some(HakutoiveenIlmoittautumistila.oili), Ilmoittautumistila.ei_tehty, true)
        ilmoittaudu(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", läsnä_koko_lukuvuosi, muokkaaja, selite)
        hakemuksenTulos.hakutoiveet(0).ilmoittautumistila must_== HakutoiveenIlmoittautumistila(ilmoittautumisaikaPaattyy2100, Some(HakutoiveenIlmoittautumistila.oili), Ilmoittautumistila.läsnä_koko_lukuvuosi, false)
      }
      "ei onnistu, jos vastaanottanut ehdollisesti" in {
        useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json", hakuFixture = hakuFixture)
        hakemuksenTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaanottanut
        hakemuksenTulos.hakutoiveet(1).ilmoittautumistila must_== HakutoiveenIlmoittautumistila(ilmoittautumisaikaPaattyy2100, Some(HakutoiveenIlmoittautumistila.oili), Ilmoittautumistila.ei_tehty, false)
        expectFailure{ilmoittaudu(hakuOid, hakemusOid, "1.2.246.562.5.16303028779", läsnä_koko_lukuvuosi, muokkaaja, selite)}
      }
      "onnistuu viime hetkeen asti" in {
        withFixedDateTime(ilmoittautumisaikaPaattyy2100.loppu.get.minusMinutes(1).getMillis) {
          useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
          hakemuksenTulos.hakutoiveet(0).ilmoittautumistila.ilmoittauduttavissa must_== true
          ilmoittaudu(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", läsnä_koko_lukuvuosi, muokkaaja, selite)
          hakemuksenTulos.hakutoiveet(0).ilmoittautumistila.ilmoittautumistila must_== Ilmoittautumistila.läsnä_koko_lukuvuosi
          hakemuksenTulos.hakutoiveet(0).ilmoittautumistila.ilmoittauduttavissa must_== false
        }
      }
      "ei onnistu päättymisen jälkeen" in {
        withFixedDateTime(ilmoittautumisaikaPaattyy2100.loppu.get.plusMinutes(1).getMillis) {
          useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
          hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
          hakemuksenTulos.hakutoiveet(0).ilmoittautumistila.ilmoittauduttavissa must_== false
          expectFailure{ilmoittaudu(hakuOid, hakemusOid, "1.2.246.562.5.16303028779", läsnä_koko_lukuvuosi, muokkaaja, selite)}
        }
      }
    }
  }

  "toisen asteen oppilaitosten yhteishaku" in {
    val hakuFixture = HakuFixtures.toinenAsteYhteishaku

    kaikkienHakutyyppienTestit(hakuFixture)

    "vastaanota alempi kun kaksi hyvaksyttya -> muut eivät peruunnut" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture, hakemusFixtures = List("00000441369-3"))
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738904", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      val yhteenveto = hakemuksenTulos
      yhteenveto.hakutoiveet(0).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(1).valintatila must_== Valintatila.peruuntunut
      yhteenveto.hakutoiveet(2).valintatila must_== Valintatila.hyväksytty
      yhteenveto.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.kesken
      yhteenveto.hakutoiveet(2).vastaanottotila must_== Vastaanottotila.vastaanottanut
      valintatulosDao.loadValintatulos("1.2.246.562.5.72607738904", "14090336922663576781797489829888", hakemusOid).getTila must_== ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI
    }

    "vastaanota varsinaisessa haussa, kun lisähaussa jo vastaanottanut, onnistuu" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanottanut.json"), hakuFixture = hakuFixture)
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
    }

    "Valintatuloksen muutoslogi"  in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
      vastaanota(hakuOid, hakemusOid, vastaanotettavissaHakuKohdeOid, Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      val valintatulos: Valintatulos = valintatulosDao.loadValintatulos(vastaanotettavissaHakuKohdeOid, "14090336922663576781797489829886", hakemusOid)
      assertSecondLogEntry(valintatulos, "tila: KESKEN -> VASTAANOTTANUT_SITOVASTI", selite)
    }

    "ilmoittautuminen" in {
      "onnistuu jos vastaanottanut, ei tarjota ilmoittautumistapaa" in {
        useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
        hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
        hakemuksenTulos.hakutoiveet(0).ilmoittautumistila must_== HakutoiveenIlmoittautumistila(ilmoittautumisaikaPaattyy2100, None, Ilmoittautumistila.ei_tehty, true)
        ilmoittaudu(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", läsnä_koko_lukuvuosi, muokkaaja, selite)
        hakemuksenTulos.hakutoiveet(0).ilmoittautumistila must_== HakutoiveenIlmoittautumistila(ilmoittautumisaikaPaattyy2100, None, Ilmoittautumistila.läsnä_koko_lukuvuosi, false)
      }
    }
  }

  private lazy val valintatulosDao = appConfig.sijoitteluContext.valintatulosDao

  lazy val hakuService = HakuService(appConfig)
  lazy val valintarekisteriDb = new ValintarekisteriDb(appConfig.settings.valintaRekisteriDbConfig)
  lazy val hakukohdeRecordService = new HakukohdeRecordService(hakuService, valintarekisteriDb)
  lazy val sijoittelutulosService = new SijoittelutulosService(appConfig.sijoitteluContext.raportointiService, appConfig.ohjausparametritService, valintarekisteriDb)
  lazy val valintatulosService = new ValintatulosService(sijoittelutulosService, hakuService)(appConfig)
  lazy val vastaanottoService = new VastaanottoService(hakuService, valintatulosService,
    valintarekisteriDb, hakukohdeRecordService, appConfig.sijoitteluContext.valintatulosRepository)
  lazy val ilmoittautumisService = new IlmoittautumisService(valintatulosService,
    appConfig.sijoitteluContext.valintatulosRepository, valintarekisteriDb)

  private def hakemuksenTulos: Hakemuksentulos = hakemuksenTulos(hakuOid, hakemusOid)
  private def hakemuksenTulos(hakuOid: String, hakemusOid: String) = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).get

  private def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: Vastaanottotila, muokkaaja: String, selite: String, personOid: String) = {
    vastaanottoService.vastaanotaHakukohde(VastaanottoEvent(personOid, hakukohdeOid, tila match {
      case Vastaanottotila.perunut => Peru
      case Vastaanottotila.vastaanottanut => VastaanotaSitovasti
      case Vastaanottotila.ehdollisesti_vastaanottanut => VastaanotaEhdollisesti
    })).get
    success
  }

  private def tarkistaVastaanotettavuus(hakuOid: String, hakemusOid: String, hakukohdeOid: String) = {
    vastaanottoService.tarkistaVastaanotettavuus(hakuOid, hakemusOid, hakukohdeOid)
    success
  }

  private def ilmoittaudu(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: Ilmoittautumistila, muokkaaja: String, selite: String) = {
    ilmoittautumisService.ilmoittaudu(hakuOid, hakemusOid, Ilmoittautuminen(hakukohdeOid, tila, muokkaaja, selite))
    success
  }

  private def expectFailure[T](block: => T): Result = expectFailure[T](None)(block)

  private def expectFailure[T](assertErrorMsg: Option[String])(block: => T): Result = {
    try {
      block
      failure("Expected exception")
    } catch {
      case e: Exception => assertErrorMsg match {
        case Some(msg) => e.getMessage must_== msg
        case None => success
      }
    }
  }

  private def assertSecondLogEntry(valintatulos: Valintatulos, tila: String, selite: String): Result = {
    import scala.collection.JavaConversions._
    val logEntries: List[LogEntry] = valintatulos.getLogEntries.toList
    logEntries.size must_== 2
    val logEntry: LogEntry = logEntries(1)
    logEntry.getMuutos must_== tila
    logEntry.getSelite must_== selite
    logEntry.getMuokkaaja must_== muokkaaja
    new LocalDate(logEntry.getLuotu) must_== new LocalDate(System.currentTimeMillis())
  }
}

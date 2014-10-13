package fi.vm.sade.valintatulosservice.local

import java.text.SimpleDateFormat
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures
import fi.vm.sade.valintatulosservice.{TimeWarp, ITSetup}
import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain.{Hakutoiveentulos, Valintatila, Vastaanotettavuustila, Vastaanottotila}
import org.joda.time.{DateTime, DateTimeUtils}
import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SijoitteluTulosServiceSpec extends Specification with ITSetup with TimeWarp {
  sequential

  "SijoitteluTulosService" should {

    "yhteishaku korkeakouluihin" in {
      val hakuFixture = HakuFixtures.korkeakouluYhteishaku

      testitKaikilleHakutyypeille(hakuFixture)

      "hyväksytty, korkeakoulujen yhteishaku" in {
        "ylemmat sijoiteltu -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-sijoiteltu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
          checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
        }

        "ylempi varalla -> ei vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "ylempi sijoittelematon -> kesken" in {
          useFixture("hyvaksytty-ylempi-sijoittelematon.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
          checkHakutoiveState(getHakutoive(1), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }

        "ylempi varalla, aikaparametri lauennut -> ehdollisesti vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          withFixedDateTime("15.8.2014 01:00") {
            checkHakutoiveState(getHakutoive(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
            checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, true)
          }
        }
      }
    }

    "erillishaku korkeakouluihin" in {
      testitKaikilleHakutyypeille(HakuFixtures.korkeakouluErillishaku)
      testitTyypillisilleHauille(HakuFixtures.korkeakouluErillishaku)
    }

    "yhteishaku toisen asteen oppilaitoksiin" in {
      testitKaikilleHakutyypeille(HakuFixtures.toinenAsteYhteishaku)
      testitTyypillisilleHauille(HakuFixtures.toinenAsteYhteishaku)
    }

    def testitTyypillisilleHauille(hakuFixture: String) = {
      "hyväksytty, tyypilliset haut" in {
        "ylemmat sijoiteltu -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-sijoiteltu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
          checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
        }

        "hyväksytty, ylempi varalla -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "ylempi sijoittelematon -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-sijoittelematon.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
          checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
        }

        "ylempi varalla, aikaparametri lauennut -> vastaanotettavissa sitovasti" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          withFixedDateTime("15.8.2014 01:00") {
            checkHakutoiveState(getHakutoive(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
            checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
          }
        }
      }
    }

    def testitKaikilleHakutyypeille(hakuFixture: String) = {
      "hyväksytty" in {
        "valintatulos ilmoitettu (Legacy)" in {
          useFixture("hyvaksytty-ilmoitettu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "hyvaksytty varasijalta, Valintatulos julkaistavissa" in {
          useFixture("hyvaksytty-varasijalta-julkaistavissa.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.varasijalta_hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }


        "Valintatulos kesken" in {
          useFixture("hyvaksytty-kesken.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
        }

        "Valintatulos julkaistavissa" in {
          useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "ei Valintatulosta" in {
          useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
          getHakutoive(0).julkaistavissa must_== false
        }

        "hyvaksytty Valintatulos peruutettu" in {
          useFixture("hyvaksytty-valintatulos-peruutettu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.peruutettu, Vastaanottotila.peruutettu, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "hyvaksytty Valintatulos perunut" in {
          useFixture("hyvaksytty-valintatulos-perunut.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(1), Valintatila.perunut, Vastaanottotila.perunut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "ei vastaanottanut määräaikana" in {
          "sijoittelu ei ole ehtinyt muuttamaan tulosta" in {
            useFixture("hyvaksytty-valintatulos-ei-vastaanottanut-maaraaikana.json", hakuFixture = hakuFixture)
            checkHakutoiveState(getHakutoive(1), Valintatila.perunut, Vastaanottotila.ei_vastaanotetu_määräaikana, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          }
          "sijoittelu on muuttanut tuloksen" in {
            useFixture("perunut-ei-vastaanottanut-maaraaikana.json", hakuFixture = hakuFixture)
            val hakutoive = getHakutoive(0)
            checkHakutoiveState(hakutoive, Valintatila.perunut, Vastaanottotila.ei_vastaanotetu_määräaikana, Vastaanotettavuustila.ei_vastaanotettavissa, true)
            hakutoive.tilanKuvaukset("FI") must_== "Peruuntunut, ei vastaanottanut määräaikana"
          }
        }

        "vastaanottanut" in {
          useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.vastaanottanut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "vastaanottanut ehdollisesti" in {
          useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.ehdollisesti_vastaanottanut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "hyväksytty + julkaisematon + hyväksytty" in {
          useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
          checkHakutoiveState(getHakutoive(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
          checkHakutoiveState(getHakutoive(2), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }
      }

      "hyvaksytty harkinnanvaraisesti" in {
        useFixture("harkinnanvaraisesti-hyvaksytty.json", hakuFixture = hakuFixture)
        checkHakutoiveState(getHakutoive(0), Valintatila.harkinnanvaraisesti_hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
      }

      "varalla" in {
        "käytetään parasta varasijaa, jos useammassa jonossa varalla" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          getHakutoive(0).varasijanumero must_== Some(2)
        }

        "varasijojen käsittelypäivämäärät näytetään" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          getHakutoive(1).varasijojaKaytetaanAlkaen must_== Some(new DateTime("2014-08-01T16:00:00.000Z").toDate)
          getHakutoive(1).varasijojaTaytetaanAsti must_== Some(new DateTime("2014-08-31T16:00:00.000Z").toDate)
        }

        "Valintatulos ilmoitettu (Legacy)" in {
          useFixture("varalla-valintatulos-ilmoitettu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "Valintatulos kesken" in {
          useFixture("varalla-valintatulos-kesken.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }

        "Valintatulos hyvaksytty varasijalta" in {
          useFixture("varalla-valintatulos-hyvaksytty-varasijalta-flag.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }
      }

      "hylätty" in {
        "jonoja sijoittelematta" in {
          useFixture("hylatty-jonoja-kesken.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }

        "jonot sijoiteltu" in {
          useFixture("hylatty-jonot-valmiit.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }

        "julkaistavissa" in {
          useFixture("hylatty-julkaistavissa.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(0), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }
      }
    }
  }

  lazy val sijoittelutulosService = appConfig.sijoitteluContext.sijoittelutulosService

  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val sijoitteluAjoId: String = "latest"
  val hakemusOid: String = "1.2.246.562.11.00000441369"

  def getHakutoive(index: Int) = sijoittelutulosService.hakemuksenTulos(hakuOid, hakemusOid).get.hakutoiveet(index)

  def checkHakutoiveState(hakuToive: Hakutoiveentulos, expectedTila: Valintatila, vastaanottoTila: Vastaanottotila, vastaanotettavuustila: Vastaanotettavuustila, julkaistavissa: Boolean) = {
    hakuToive.valintatila must_== expectedTila
    hakuToive.vastaanottotila must_== vastaanottoTila
    hakuToive.vastaanotettavuustila must_== vastaanotettavuustila
    hakuToive.julkaistavissa must_== julkaistavissa
  }
}

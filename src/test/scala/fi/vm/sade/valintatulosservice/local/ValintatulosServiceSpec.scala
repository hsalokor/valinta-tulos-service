package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.domain.Valintatila
import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import fi.vm.sade.valintatulosservice.tarjonta.{HakuService, HakuFixtures}
import fi.vm.sade.valintatulosservice.{ValintatulosService, ITSetup, TimeWarp}
import org.joda.time.DateTime
import org.json4s.jackson.Serialization
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValintatulosServiceSpec extends Specification with ITSetup with TimeWarp {
  sequential

  "ValintaTulosService" should {

    "yhteishaku korkeakouluihin" in {
      val hakuFixture = HakuFixtures.korkeakouluYhteishaku

      testitKaikilleHakutyypeille(hakuFixture)
      testitSijoittelunPiirissäOlevilleHakutyypeille(hakuFixture)

      "hyväksytty, korkeakoulujen yhteishaku" in {
        "ylemmat sijoiteltu -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-sijoiteltu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "ylempi varalla -> ei vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "ylempi sijoittelematon -> kesken" in {
          useFixture("hyvaksytty-ylempi-sijoittelematon.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "ylempi varalla, aikaparametri lauennut -> ehdollisesti vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          withFixedDateTime("15.8.2014 01:00") {
            checkHakutoiveState(getHakutoive(("1.2.246.562.5.72607738902")), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
            checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, true)
          }
        }
      }
    }

    "erillishaku korkeakouluihin" in {
      val hakuFixture: String = HakuFixtures.korkeakouluErillishaku
      testitKaikilleHakutyypeille(hakuFixture)
      testitTyypillisilleHauille(hakuFixture)
      testitSijoittelunPiirissäOlevilleHakutyypeille(hakuFixture)
    }

    "yhteishaku toisen asteen oppilaitoksiin" in {
      val hakuFixture: String = HakuFixtures.toinenAsteYhteishaku
      testitKaikilleHakutyypeille(hakuFixture)
      testitTyypillisilleHauille(hakuFixture)
      testitSijoittelunPiirissäOlevilleHakutyypeille(hakuFixture)
    }

    "erillishaku toisen asteen oppilaitoksiin (ei sijoittelua)" in {
      val hakuFixture: String = HakuFixtures.toinenAsteErillishakuEiSijoittelua
      testitKaikilleHakutyypeille(hakuFixture)
      testitTyypillisilleHauille(hakuFixture)
      testitSijoittelunPiirissäOlemattomilleHakutyypeille(hakuFixture)
    }

    def testitTyypillisilleHauille(hakuFixture: String) = {
      "hyväksytty, useimmat haut" in {
        "ylemmat sijoiteltu -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-sijoiteltu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(("1.2.246.562.5.72607738902")), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "hyväksytty, ylempi varalla -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(("1.2.246.562.5.72607738902")), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "ylempi sijoittelematon -> vastaanotettavissa" in {
          useFixture("hyvaksytty-ylempi-sijoittelematon.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive(("1.2.246.562.5.72607738902")), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "ylempi varalla, aikaparametri lauennut -> vastaanotettavissa sitovasti" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          withFixedDateTime("15.8.2014 01:00") {
            checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
            checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
          }
        }
      }
    }

    def testitSijoittelunPiirissäOlevilleHakutyypeille(hakuFixture: String) = {
      "hyväksyttyä hakutoivetta alemmat julkaisemattomat merkitään tilaan PERUUNTUNUT" in {
        useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture, hakemusFixture = "00000441369-3")

        checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738903"), Valintatila.peruuntunut, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738904"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
      }
    }

    def testitSijoittelunPiirissäOlemattomilleHakutyypeille(hakuFixture: String) = {
      "hyväksyttyä hakutoivetta alempia ei merkitä tilaan PERUUNTUNUT" in {
        useFixture("hyvaksytty-julkaisematon-hyvaksytty.json", hakuFixture = hakuFixture, hakemusFixture = "00000441369-3")

        checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738903"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738904"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
      }
    }

    def testitKaikilleHakutyypeille(hakuFixture: String) = {
      "sijoittelusta puuttuvat hakutoiveet" in {
        "näytetään keskeneräisinä" in {
          useFixture("hylatty-jonot-valmiit.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }
      }

      "hyväksytty" in {
        "hyvaksytty varasijalta, Valintatulos julkaistavissa" in {
          useFixture("hyvaksytty-varasijalta-julkaistavissa.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.varasijalta_hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "Valintatulos kesken (ei julkaistavissa)" in {
          useFixture("hyvaksytty-kesken.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }

        "Valintatulos julkaistavissa" in {
          useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }

        "ei Valintatulosta" in {
          useFixture("hyvaksytty-ei-valintatulosta.json", hakuFixture = hakuFixture)
          val toiveet: List[Hakutoiveentulos] = hakemuksenTulos.hakutoiveet
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }

        "hyvaksytty Valintatulos peruutettu" in {
          useFixture("hyvaksytty-valintatulos-peruutettu.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.peruutettu, Vastaanottotila.peruutettu, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "hyvaksytty Valintatulos perunut" in {
          useFixture("hyvaksytty-valintatulos-perunut.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.perunut, Vastaanottotila.perunut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "ei vastaanottanut määräaikana" in {
          "sijoittelu ei ole ehtinyt muuttamaan tulosta" in {
            useFixture("hyvaksytty-valintatulos-ei-vastaanottanut-maaraaikana.json", hakuFixture = hakuFixture)
            checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.perunut, Vastaanottotila.ei_vastaanotetu_määräaikana, Vastaanotettavuustila.ei_vastaanotettavissa, true)
          }
          "sijoittelu on muuttanut tuloksen" in {
            useFixture("perunut-ei-vastaanottanut-maaraaikana.json", hakuFixture = hakuFixture)
            val hakutoive = getHakutoive("1.2.246.562.5.72607738902")
            checkHakutoiveState(hakutoive, Valintatila.perunut, Vastaanottotila.ei_vastaanotetu_määräaikana, Vastaanotettavuustila.ei_vastaanotettavissa, true)
            hakutoive.tilanKuvaukset("FI") must_== "Peruuntunut, ei vastaanottanut määräaikana"
          }
        }

        "vastaanottanut" in {
          useFixture("hyvaksytty-vastaanottanut.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hyväksytty, Vastaanottotila.vastaanottanut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "vastaanottanut ehdollisesti" in {
          useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json", hakuFixture = hakuFixture, hakemusFixture = "00000441369-flipped")
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.ehdollisesti_vastaanottanut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }
      }

      "hyvaksytty harkinnanvaraisesti" in {
        useFixture("harkinnanvaraisesti-hyvaksytty.json", hakuFixture = hakuFixture)
        checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.harkinnanvaraisesti_hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
      }

      "varalla" in {
        "käytetään parasta varasijaa, jos useammassa jonossa varalla" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          getHakutoive("1.2.246.562.5.72607738902").varasijanumero must_== Some(2)
        }

        "varasijojen käsittelypäivämäärät näytetään" in {
          useFixture("hyvaksytty-ylempi-varalla.json", hakuFixture = hakuFixture)
          getHakutoive("1.2.246.562.5.16303028779").varasijojaKaytetaanAlkaen must_== Some(new DateTime("2014-08-01T16:00:00.000Z").toDate)
          getHakutoive("1.2.246.562.5.16303028779").varasijojaTaytetaanAsti must_== Some(new DateTime("2014-08-31T16:00:00.000Z").toDate)
        }

        "Valintatulos kesken" in {
          useFixture("varalla-valintatulos-kesken.json", hakuFixture = hakuFixture, hakemusFixture = "00000441369-flipped")
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "Valintatulos hyvaksytty varasijalta" in {
          useFixture("varalla-valintatulos-hyvaksytty-varasijalta-flag.json", hakuFixture = hakuFixture, hakemusFixture = "00000441369-flipped")
          checkHakutoiveState(getHakutoive("1.2.246.562.5.16303028779"), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
        }
      }

      "hylätty" in {
        "jonoja sijoittelematta" in {
          useFixture("hylatty-jonoja-kesken.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "jonot sijoiteltu" in {
          useFixture("hylatty-jonot-valmiit.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "julkaistavissa" in {
          useFixture("hylatty-julkaistavissa.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        }

        "ei Valintatulosta" in {
          useFixture("hylatty-ei-valintatulosta.json", hakuFixture = hakuFixture)
          checkHakutoiveState(getHakutoive("1.2.246.562.5.72607738902"), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
        }

      }
    }
  }

  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)

  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val sijoitteluAjoId: String = "latest"
  val hakemusOid: String = "1.2.246.562.11.00000441369"

  def getHakutoive(idSuffix: String) = hakemuksenTulos.hakutoiveet.find{_.hakukohdeOid.endsWith(idSuffix)}.get

  def hakemuksenTulos = {
    valintatulosService.hakemuksentulos(hakuOid, hakemusOid).get
  }

  def checkHakutoiveState(hakuToive: Hakutoiveentulos, expectedTila: Valintatila, vastaanottoTila: Vastaanottotila, vastaanotettavuustila: Vastaanotettavuustila, julkaistavissa: Boolean) = {
    hakuToive.valintatila must_== expectedTila
    hakuToive.vastaanottotila must_== vastaanottoTila
    hakuToive.vastaanotettavuustila must_== vastaanotettavuustila
    hakuToive.julkaistavissa must_== julkaistavissa
  }
}
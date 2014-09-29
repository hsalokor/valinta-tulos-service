package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat

import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain.{Hakutoiveentulos, Vastaanotettavuustila, Vastaanottotila, Valintatila}
import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import Valintatila._
import org.joda.time.{DateTime, DateTimeUtils}
import org.specs2.mutable.Specification

class YhteenvetoServiceSpec extends Specification with ITSetup {
  sequential

  "YhteenvetoService" should {
    "hyvaksyttyValintatulosIlmoitettuLegacy" in {
      useFixture("hyvaksytty-ilmoitettu.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
    }


    "hyvaksyttyVarasijaltaValintatulosJulkaistavissa" in {
      useFixture("hyvaksytty-varasijalta-julkaistavissa.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.varasijalta_hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
    }


    "hyvaksyttyValintatulosKesken" in {
      useFixture("hyvaksytty-kesken.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
    }


    "hyvaksyttyValintatulosJulkaistavissa" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
    }


    "hyvaksyttyEiValintatulosta" in {
      useFixture("hyvaksytty-ei-valintatulosta.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
      yhteenveto.hakutoiveet(0).julkaistavissa must_== false
    }


    "hyvaksyttyYlempiSijoittelematon" in {
      useFixture("hyvaksytty-ylempi-sijoittelematon.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
    }


    "hyvaksyttyValintatulosPeruutettu" in {
      useFixture("hyvaksytty-valintatulos-peruutettu.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.peruutettu, Vastaanottotila.peruutettu, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "hyvaksyttyValintatulosPerunut" in {
      useFixture("hyvaksytty-valintatulos-perunut.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.perunut, Vastaanottotila.perunut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "hyvaksyttyValintatulosEiVastaanottanutMaaraaikana" in {
        useFixture("hyvaksytty-valintatulos-ei-vastaanottanut-maaraaikana.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(1), Valintatila.peruuntunut, Vastaanottotila.ei_vastaanotetu_määräaikana, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "hyvaksyttyYlemmatSijoiteltu" in {
      useFixture("hyvaksytty-ylempi-sijoiteltu.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
      checkHakutoiveState(yhteenveto.hakutoiveet(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
    }


    "hyvaksyttyVastaanottanut" in {
      useFixture("hyvaksytty-vastaanottanut.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.vastaanottanut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "hyvaksyttyVastaanottanutEhdollisesti" in {
      useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.ehdollisesti_vastaanottanut, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "hyvaksyttyYlempiVaralla" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
      checkHakutoiveState(yhteenveto.hakutoiveet(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "hyvaksyttyHarkinnanvaraisesti" in {
      useFixture("harkinnanvaraisesti-hyvaksytty.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.harkinnanvaraisesti_hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
    }


    "varallaKäytetäänParastaVarasijaa" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      val yhteenveto = getYhteenveto
      yhteenveto.hakutoiveet(0).varasijanumero must_== Some(2)
    }


    "varasijojenKasittelypaivamaaratNaytetaan" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      val yhteenveto = getYhteenveto
      yhteenveto.hakutoiveet(1).varasijojaKaytetaanAlkaen must_== Some(new DateTime("2014-08-01T16:00:00.000Z").toDate)
      yhteenveto.hakutoiveet(1).varasijojaTaytetaanAsti must_== Some(new DateTime("2014-08-31T16:00:00.000Z").toDate)
    }


    "hyvaksyttyYlempiVarallaAikaparametriLauennut" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      DateTimeUtils.setCurrentMillisFixed(new SimpleDateFormat("d.M.yyyy").parse("15.8.2014").getTime)
      try {
        val yhteenveto = getYhteenveto
        checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
        checkHakutoiveState(yhteenveto.hakutoiveet(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, true)
      }
      finally {
        DateTimeUtils.setCurrentMillisSystem
      }
    }


    "varallaValintatulosIlmoitettuLegacy" in {
      useFixture("varalla-valintatulos-ilmoitettu.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "varallaValintatulosKesken" in {
      useFixture("varalla-valintatulos-kesken.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.varalla, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
    }


    "varallaValintatulosHyvaksyttyVarasijalta" in {
      useFixture("varalla-valintatulos-hyvaksytty-varasijalta-flag.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
    }


    "hakutoiveHylattyKunSijoitteluKesken" in {
      useFixture("hylatty-jonoja-kesken.json")
      val hakuToive = getHakuToive
      checkHakutoiveState(hakuToive, Valintatila.kesken, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
    }


    "hakutoiveHylattyKunSijoitteluValmis" in {
      useFixture("hylatty-jonot-valmiit.json")
      val hakuToive = getHakuToive
      checkHakutoiveState(hakuToive, Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, false)
    }

    "hakutoiveHylattyJulkaistavissa" in {
      useFixture("hylatty-julkaistavissa.json")
      val hakuToive = getHakuToive
      checkHakutoiveState(hakuToive, Valintatila.hylätty, Vastaanottotila.kesken, Vastaanotettavuustila.ei_vastaanotettavissa, true)
    }


    "hyvaksyttyPlusJulkaisematonPlusHyvaksytty" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json")
      val yhteenveto = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet(0), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
      checkHakutoiveState(yhteenveto.hakutoiveet(1), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, false)
      checkHakutoiveState(yhteenveto.hakutoiveet(2), Valintatila.hyväksytty, Vastaanottotila.kesken, Vastaanotettavuustila.vastaanotettavissa_sitovasti, true)
    }
  }

  import scala.collection.JavaConversions._
  lazy val sijoitteluClient = appConfig.sijoitteluContext.sijoittelutulosService

  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val sijoitteluAjoId: String = "latest"
  val hakemusOid: String = "1.2.246.562.11.00000441369"

  def getYhteenveto = sijoitteluClient.hakemuksentulos(hakuOid, hakemusOid).get
  def getHakuToive = getYhteenveto.hakutoiveet(0)

  def checkHakutoiveState(hakuToive: Hakutoiveentulos, expectedTila: Valintatila, vastaanottoTila: Vastaanottotila, vastaanotettavuustila: Vastaanotettavuustila, julkaistavissa: Boolean) = {
    hakuToive.valintatila must_== expectedTila
    hakuToive.vastaanottotila must_== vastaanottoTila
    hakuToive.vastaanotettavuustila must_== vastaanotettavuustila
    hakuToive.julkaistavissa must_== julkaistavissa
  }



}

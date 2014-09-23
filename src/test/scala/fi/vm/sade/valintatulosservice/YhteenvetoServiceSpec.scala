package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat

import fi.vm.sade.sijoittelu.tulos.dto.raportointi._
import org.joda.time.{DateTime, DateTimeUtils}
import org.specs2.mutable.Specification

class YhteenvetoServiceSpec extends Specification with ITSetup {
  sequential

  "YhteenvetoService" should {
    "hyvaksyttyValintatulosIlmoitettuLegacy" in {
      useFixture("hyvaksytty-ilmoitettu.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, true)
    }


    "hyvaksyttyVarasijaltaValintatulosJulkaistavissa" in {
      useFixture("hyvaksytty-varasijalta-julkaistavissa.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.VARASIJALTA_HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, true)
    }


    "hyvaksyttyValintatulosKesken" in {
      useFixture("hyvaksytty-kesken.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, false)
    }


    "hyvaksyttyValintatulosJulkaistavissa" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, true)
    }


    "hyvaksyttyEiValintatulosta" in {
      useFixture("hyvaksytty-ei-valintatulosta.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, false)
      yhteenveto.hakutoiveet.get(0).julkaistavissa must_== false
    }


    "hyvaksyttyYlempiSijoittelematon" in {
      useFixture("hyvaksytty-ylempi-sijoittelematon.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.KESKEN, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, false)
    }


    "hyvaksyttyValintatulosPeruutettu" in {
      useFixture("hyvaksytty-valintatulos-peruutettu.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.PERUUTETTU, YhteenvedonVastaanottotila.PERUUTETTU, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
    }


    "hyvaksyttyValintatulosPerunut" in {
      useFixture("hyvaksytty-valintatulos-perunut.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.PERUNUT, YhteenvedonVastaanottotila.PERUNUT, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
    }


    "hyvaksyttyValintatulosEiVastaanottanutMaaraaikana" in {
      useFixture("hyvaksytty-valintatulos-ei-vastaanottanut-maaraaikana.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.PERUUNTUNUT, YhteenvedonVastaanottotila.EI_VASTAANOTETTU_MAARA_AIKANA, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
    }


    "hyvaksyttyYlemmatSijoiteltu" in {
      useFixture("hyvaksytty-ylempi-sijoiteltu.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYLATTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, false)
      checkHakutoiveState(yhteenveto.hakutoiveet.get(1), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, false)
    }


    "hyvaksyttyVastaanottanut" in {
      useFixture("hyvaksytty-vastaanottanut.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.VASTAANOTTANUT, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
    }


    "hyvaksyttyVastaanottanutEhdollisesti" in {
      useFixture("hyvaksytty-vastaanottanut-ehdollisesti.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.EHDOLLISESTI_VASTAANOTTANUT, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
    }


    "hyvaksyttyYlempiVaralla" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.VARALLA, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
      checkHakutoiveState(yhteenveto.hakutoiveet.get(1), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
    }


    "hyvaksyttyHarkinnanvaraisesti" in {
      useFixture("harkinnanvaraisesti-hyvaksytty.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HARKINNANVARAISESTI_HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, true)
    }


    "varallaKäytetäänParastaVarasijaa" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      yhteenveto.hakutoiveet.get(0).varasijanumero must_== 2
    }


    "varasijojenKasittelypaivamaaratNaytetaan" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      yhteenveto.hakutoiveet.get(1).varasijojaKaytetaanAlkaen must_== new DateTime("2014-08-01T16:00:00.000Z").toDate
      yhteenveto.hakutoiveet.get(1).varasijojaTaytetaanAsti must_== new DateTime("2014-08-31T16:00:00.000Z").toDate
    }


    "hyvaksyttyYlempiVarallaAikaparametriLauennut" in {
      useFixture("hyvaksytty-ylempi-varalla.json")
      DateTimeUtils.setCurrentMillisFixed(new SimpleDateFormat("d.M.yyyy").parse("15.8.2014").getTime)
      try {
        val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
        checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.VARALLA, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
        checkHakutoiveState(yhteenveto.hakutoiveet.get(1), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_EHDOLLISESTI, true)
      }
      finally {
        DateTimeUtils.setCurrentMillisSystem
      }
    }


    "varallaValintatulosIlmoitettuLegacy" in {
      useFixture("varalla-valintatulos-ilmoitettu.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, true)
    }


    "varallaValintatulosKesken" in {
      useFixture("varalla-valintatulos-kesken.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.VARALLA, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, false)
    }


    "varallaValintatulosHyvaksyttyVarasijalta" in {
      useFixture("varalla-valintatulos-hyvaksytty-varasijalta-flag.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, false)
    }


    "hakutoiveHylattyKunSijoitteluKesken" in {
      useFixture("hylatty-jonoja-kesken.json")
      val hakuToive: HakutoiveYhteenvetoDTO = getHakuToive
      checkHakutoiveState(hakuToive, YhteenvedonValintaTila.KESKEN, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, false)
    }


    "hakutoiveHylattyKunSijoitteluValmis" in {
      useFixture("hylatty-jonot-valmiit.json")
      val hakuToive: HakutoiveYhteenvetoDTO = getHakuToive
      checkHakutoiveState(hakuToive, YhteenvedonValintaTila.HYLATTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.EI_VASTAANOTETTAVISSA, false)
    }


    "hyvaksyttyPlusJulkaisematonPlusHyvaksytty" in {
      useFixture("hyvaksytty-julkaisematon-hyvaksytty.json")
      val yhteenveto: HakemusYhteenvetoDTO = getYhteenveto
      checkHakutoiveState(yhteenveto.hakutoiveet.get(0), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, true)
      checkHakutoiveState(yhteenveto.hakutoiveet.get(1), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, false)
      checkHakutoiveState(yhteenveto.hakutoiveet.get(2), YhteenvedonValintaTila.HYVAKSYTTY, YhteenvedonVastaanottotila.KESKEN, Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI, true)
    }
  }

  import fi.vm.sade.sijoittelu.tulos.testfixtures.{FixtureImporter => SijoitteluFixtureImporter}

  def useFixture(fixtureName: String) {
    SijoitteluFixtureImporter.importFixture(appConfig.sijoitteluContext.database, fixtureName, true)
  }


  import scala.collection.JavaConversions._
  lazy val sijoitteluClient = appConfig.sijoitteluContext.sijoitteluClient

  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val sijoitteluAjoId: String = "latest"
  val hakemusOid: String = "1.2.246.562.11.00000441369"

  def getYhteenveto: HakemusYhteenvetoDTO = sijoitteluClient.yhteenveto(hakuOid, hakemusOid).get
  def getHakuToive: HakutoiveYhteenvetoDTO = getYhteenveto.hakutoiveet(0)

  def checkHakutoiveState(hakuToive: HakutoiveYhteenvetoDTO, expectedTila: YhteenvedonValintaTila, vastaanottoTila: YhteenvedonVastaanottotila, vastaanotettavuustila: Vastaanotettavuustila, julkaistavissa: Boolean) = {
    hakuToive.valintatila must_== expectedTila
    hakuToive.vastaanottotila must_== vastaanottoTila
    hakuToive.vastaanotettavuustila must_== vastaanotettavuustila
    hakuToive.julkaistavissa must_== julkaistavissa
  }



}

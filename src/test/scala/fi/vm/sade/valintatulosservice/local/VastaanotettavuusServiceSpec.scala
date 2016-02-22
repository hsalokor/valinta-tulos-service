package fi.vm.sade.valintatulosservice.local

import java.util.Date

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService, Hakukohde, YhdenPaikanSaanto}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}
import fi.vm.sade.valintatulosservice.{ValintatulosService, VastaanotettavuusService}
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.specs2.matcher.MustThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope

@RunWith(classOf[JUnitRunner])
class VastaanotettavuusServiceSpec extends Specification {
  "VastaanotettavuusService" >> {
    "haeVastaanotettavuus" >> {
      val hakemusOid = "1.2.246.562.11.00000441784"
      "kun hakijalla on aiempia vastaanottoja" in new YhdenPaikanSaantoVoimassa with HakutoiveLoytyy {
        val previousVastaanottoRecord = VastaanottoRecord(
          henkiloOid,
          haku.oid,
          hakukohde.oid,
          VastaanotaSitovasti,
          ilmoittaja = "",
          new Date(0)
        )

        hakuService.getHaku(haku.oid) returns Some(haku)
        hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set(previousVastaanottoRecord)
        val vastaanotettavuus = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions must beEmpty
        vastaanotettavuus.reason.isDefined must beTrue
        vastaanotettavuus.reason.get.getMessage must contain("aiempi vastaanotto")
      }
      "kun hakijalla ei ole aiempia vastaanottoja, mutta hakemusta ei ole hyväksytty" in new YhdenPaikanSaantoVoimassa with MockedHakemuksenTulos {
        hakuService.getHaku(haku.oid) returns Some(haku)
        hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set()
        val hakutoiveenTulos = mock[Hakutoiveentulos]
        hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
        hakutoiveenTulos.valintatila returns Valintatila.hylätty

        val vastaanotettavuus = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions must beEmpty
        vastaanotettavuus.reason.isDefined must beTrue
        vastaanotettavuus.reason.get.getMessage must contain("hakutoiveen valintatila ei ole hyväksytty")
        vastaanotettavuus.reason.get.getMessage must contain(hakutoiveenTulos.valintatila.toString)
      }
      "kun hakijalla ei ole aiempia vastaanottoja ja hakemus on hyväksytty eikä paikka ole vastaanotettavissa ehdollisesti" in new HyvaksyttyHakemus(false) {
        val vastaanotettavuus = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions mustEqual List(Peru, VastaanotaSitovasti)
      }
      "kun hakijalla ei ole aiempia vastaanottoja ja hakemus on hyväksytty ja paikka on vastaanotettavissa ehdollisesti" in new HyvaksyttyHakemus(true) {
        val vastaanotettavuus = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions mustEqual List(Peru, VastaanotaSitovasti, VastaanotaEhdollisesti)
      }
    }
  }

  trait YhdenPaikanSaantoVoimassa extends VastaanottoServiceWithMocks with Mockito with Scope with MustThrownExpectations {
    val haku = Haku("1.2.246.562.29.00000000000", true, true, true, false, true, None, Set(), List(),
      YhdenPaikanSaanto(true, "kk haku ilman kohdejoukon tarkennetta"))
    val koulutusOid = "1.2.246.562.17.00000000000"
    val hakukohde = Hakukohde("1.2.246.562.20.00000000000", haku.oid, List(koulutusOid), "KORKEAKOULUTUS", "TUTKINTO")
    val kausi = Syksy(2015)
    hakukohdeRecordService.getHakukohdeRecord(hakukohde.oid) returns HakukohdeRecord(hakukohde.oid, haku.oid, true, true, kausi)
    hakuService.getHaku(haku.oid) returns Some(haku)
  }

  trait MockedHakemuksenTulos extends Mockito {
    this: VastaanottoServiceWithMocks =>
    val hakemuksenTulos = mock[Hakemuksentulos]
    valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)
    valintatulosService.hakemuksentuloksetByPerson(haku.oid, henkiloOid) returns List(hakemuksenTulos)
  }

  class HyvaksyttyHakemus(vastaanotettavissaEhdollisesti: Boolean) extends YhdenPaikanSaantoVoimassa with MockedHakemuksenTulos {
    hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set()
    val hakutoiveenTulos = mock[Hakutoiveentulos]
    hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
    hakutoiveenTulos.valintatila returns Valintatila.hyväksytty
    valintatulosService.onkoVastaanotettavissaEhdollisesti(hakutoiveenTulos, haku) returns vastaanotettavissaEhdollisesti
  }

  trait HakutoiveLoytyy extends MockedHakemuksenTulos {
    this: VastaanottoServiceWithMocks =>
    val hakutoiveenTulos = mock[Hakutoiveentulos]
    hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
    hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
  }

  trait VastaanottoServiceWithMocks extends Mockito with Scope with MustThrownExpectations {
    val haku: Haku
    val hakukohde: Hakukohde
    val hakukohdeRecordService = mock[HakukohdeRecordService]
    val valintatulosService = mock[ValintatulosService]
    val hakijaVastaanottoRepository = mock[HakijaVastaanottoRepository]
    val hakuService = mock[HakuService]
    val v = new VastaanotettavuusService(valintatulosService, hakuService, hakukohdeRecordService, hakijaVastaanottoRepository)
    val henkiloOid = "1.2.246.562.24.00000000000"
    val hakemusOid = "1.2.246.562.99.00000000000"
  }

}

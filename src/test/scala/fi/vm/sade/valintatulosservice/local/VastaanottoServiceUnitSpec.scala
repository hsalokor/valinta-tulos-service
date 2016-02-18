package fi.vm.sade.valintatulosservice.local

import java.util.Date

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.{HakuService, Haku, Hakukohde, YhdenPaikanSaanto}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}
import fi.vm.sade.valintatulosservice.{PriorAcceptanceException, ValintatulosService, VastaanottoService}
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.specs2.matcher.MustThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class VastaanottoServiceUnitSpec extends Specification {
  "VastaanottoService" >> {
    "vastaanotaHakukohde" >> {
      "haulle jossa yhden paikan sääntö voimassa" >> {
        "hakukohteelle joka ei ole hakijan hakutoive" in new YhdenPaikanSaantoVoimassa with EiHakutoivetta {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)

          v.vastaanotaHakukohde(vastaanottoEvent) must beFailedTry.withThrowable[IllegalStateException]
          there was no(hakijaVastaanottoRepository).findHenkilonVastaanototHaussa(Matchers.any[String], Matchers.any[String])
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
          there was no(hakijaVastaanottoRepository).store(vastaanottoEvent)
        }
        "kun hakijalla ei aiempia vastaanottoja samalle koulutuksen alkamiskaudelle" in new YhdenPaikanSaantoVoimassaHakutoiveLoytyy {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)

          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set.empty[VastaanottoRecord]
          v.vastaanotaHakukohde(vastaanottoEvent) must beEqualTo(Success(()))
          there was one(hakijaVastaanottoRepository).store(vastaanottoEvent)
        }
        "kun hakijalla yksi aiempi vastaanotto samalle koulutuksen alkamiskaudelle" in new YhdenPaikanSaantoVoimassaHakutoiveLoytyy {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)
          val previousVastaanottoRecord = VastaanottoRecord(
            henkiloOid,
            haku.oid,
            hakukohde.oid,
            VastaanotaSitovasti,
            ilmoittaja = "",
            new Date(0)
          )

          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set(previousVastaanottoRecord)
          v.vastaanotaHakukohde(vastaanottoEvent) must beFailedTry.withThrowable[PriorAcceptanceException]
          there was no(hakijaVastaanottoRepository).store(vastaanottoEvent)
        }
        "hakijalla ei voi olla useita aiempia vastaanottoja samalle alkamiskaudelle" in new YhdenPaikanSaantoVoimassaHakutoiveLoytyy {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)
          val previousVastaanottoRecord = VastaanottoRecord(
            henkiloOid,
            haku.oid,
            hakukohde.oid,
            VastaanotaSitovasti,
            ilmoittaja = "",
            new Date(0)
          )
          val anotherPreviousVastaanottoRecord = previousVastaanottoRecord.copy(hakukohdeOid = hakukohde.oid + "1")

          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set(previousVastaanottoRecord, anotherPreviousVastaanottoRecord)
          v.vastaanotaHakukohde(vastaanottoEvent) must beFailedTry.withThrowable[IllegalStateException]
          there was no(hakijaVastaanottoRepository).store(vastaanottoEvent)
        }
      }
      "haulle ilman yhden paikan sääntöä" >> {
        "kun hakijalla ei aiempia vastaanottoja samassa haussa" in new IlmanYhdenPaikanSaantoaHakutoiveLoytyy {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)

          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set()
          v.vastaanotaHakukohde(vastaanottoEvent) must beEqualTo(Success(()))
          there was one(hakijaVastaanottoRepository).store(vastaanottoEvent)
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla yksi aiempi vastaanotto samassa haussa" in new IlmanYhdenPaikanSaantoaHakutoiveLoytyy {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)
          val previousVastaanottoRecord = VastaanottoRecord(
            henkiloOid,
            haku.oid,
            hakukohde.oid,
            VastaanotaSitovasti,
            ilmoittaja = "",
            new Date(0)
          )

          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set(previousVastaanottoRecord)
          v.vastaanotaHakukohde(vastaanottoEvent) must beFailedTry.withThrowable[PriorAcceptanceException]
          there was no(hakijaVastaanottoRepository).store(vastaanottoEvent)
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakija on perunut aiemman paikan samassa haussa" in new IlmanYhdenPaikanSaantoaHakutoiveLoytyy {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)
          val previousVastaanottoRecord = VastaanottoRecord(henkiloOid, haku.oid, hakukohde.oid,
            Peru, ilmoittaja = "", new Date(0))

          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set(previousVastaanottoRecord)
          v.vastaanotaHakukohde(vastaanottoEvent) must beEqualTo(Success(()))
          there was one(hakijaVastaanottoRepository).store(vastaanottoEvent)
        }
        "hakijalla ei voi olla useita vastaanottoja samassa haussa" in new IlmanYhdenPaikanSaantoaHakutoiveLoytyy {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)
          val previousVastaanottoRecord = VastaanottoRecord(
            henkiloOid,
            haku.oid,
            hakukohde.oid,
            VastaanotaSitovasti,
            ilmoittaja = "",
            new Date(0)
          )
          val anotherPreviousVastaanottoRecord = previousVastaanottoRecord.copy(hakukohdeOid = hakukohde.oid + "1")

          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set(previousVastaanottoRecord, anotherPreviousVastaanottoRecord)
          v.vastaanotaHakukohde(vastaanottoEvent) must beFailedTry.withThrowable[IllegalStateException]
          there was no(hakijaVastaanottoRepository).store(vastaanottoEvent)
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
      }
    }
    "haeVastaanotettavuus" >> {
      val hakemusOid = "1.2.246.562.11.00000441784"
      "kun hakijalla on aiempia vastaanottoja" in new YhdenPaikanSaantoVoimassa with MockedHakemuksenTulos {
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
        val vastaanotettavuus = v.paatteleVastaanotettavuus(haku.oid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions must beEmpty
        vastaanotettavuus.reason.isDefined must beTrue
        vastaanotettavuus.reason.get must contain("aiempi vastaanotto")
      }
      "kun hakijalla ei ole aiempia vastaanottoja, mutta hakemusta ei ole hyväksytty" in new YhdenPaikanSaantoVoimassa with MockedHakemuksenTulos {
        hakuService.getHaku(haku.oid) returns Some(haku)
        hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set()
        val hakutoiveenTulos = mock[Hakutoiveentulos]
        hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
        hakutoiveenTulos.valintatila returns Valintatila.hylätty

        val vastaanotettavuus = v.paatteleVastaanotettavuus(haku.oid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions must beEmpty
        vastaanotettavuus.reason.isDefined must beTrue
        vastaanotettavuus.reason.get must contain("hakutoiveen valintatila ei ole hyväksytty")
        vastaanotettavuus.reason.get must contain(hakutoiveenTulos.valintatila.toString)
      }
      "kun hakijalla ei ole aiempia vastaanottoja ja hakemus on hyväksytty eikä paikka ole vastaanotettavissa ehdollisesti" in new HyvaksyttyHakemus(false) {
        val vastaanotettavuus = v.paatteleVastaanotettavuus(haku.oid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions mustEqual List(Peru, VastaanotaSitovasti)
      }
      "kun hakijalla ei ole aiempia vastaanottoja ja hakemus on hyväksytty ja paikka on vastaanotettavissa ehdollisesti" in new HyvaksyttyHakemus(true) {
        val vastaanotettavuus = v.paatteleVastaanotettavuus(haku.oid, hakemusOid, hakukohde.oid)
        vastaanotettavuus.allowedActions mustEqual List(Peru, VastaanotaSitovasti, VastaanotaEhdollisesti)
      }
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
}

trait IlmanYhdenPaikanSaantoa extends VastaanottoServiceWithMocks with Mockito with Scope with MustThrownExpectations {
  val haku = Haku("1.2.246.562.29.00000000001", true, true, true, false, true, None, Set(), List(),
    YhdenPaikanSaanto(false, "ei kk haku"))
  val koulutusOid = "1.2.246.562.17.00000000001"
  val hakukohde = Hakukohde("1.2.246.562.20.00000000001", haku.oid, List(koulutusOid), "KORKEAKOULUTUS", "TUTKINTO")
  val kausi = Syksy(2015)
  hakukohdeRecordService.getHakukohdeRecord(hakukohde.oid) returns HakukohdeRecord(hakukohde.oid, haku.oid, false, true, kausi)
}

trait VastaanottoServiceWithMocks extends Mockito with Scope with MustThrownExpectations {
  val haku: Haku
  val hakukohde: Hakukohde
  val hakukohdeRecordService = mock[HakukohdeRecordService]
  val valintatulosService = mock[ValintatulosService]
  val hakijaVastaanottoRepository = mock[HakijaVastaanottoRepository]
  val valintatulosRepository = mock[ValintatulosRepository]
  val hakuService = mock[HakuService]
  val v = new VastaanottoService(hakuService, valintatulosService, hakijaVastaanottoRepository, hakukohdeRecordService,
    valintatulosRepository)
  val henkiloOid = "1.2.246.562.24.00000000000"
}

trait MockedHakemuksenTulos extends Mockito { this: VastaanottoServiceWithMocks =>
  val hakemuksenTulos = mock[Hakemuksentulos]
  valintatulosService.hakemuksentulos(Matchers.eq(haku.oid), Matchers.any[String]) returns Some(hakemuksenTulos)
  valintatulosService.hakemuksentuloksetByPerson(haku.oid, henkiloOid) returns List(hakemuksenTulos)
}

class HyvaksyttyHakemus(vastaanotettavissaEhdollisesti: Boolean) extends YhdenPaikanSaantoVoimassa with MockedHakemuksenTulos{
  hakuService.getHaku(haku.oid) returns Some(haku)
  hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set()
  val hakutoiveenTulos = mock[Hakutoiveentulos]
  hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
  hakutoiveenTulos.valintatila returns Valintatila.hyväksytty
  valintatulosService.onkoVastaanotettavissaEhdollisesti(hakutoiveenTulos, haku) returns vastaanotettavissaEhdollisesti
}

trait HakutoiveLoytyy extends MockedHakemuksenTulos { this: VastaanottoServiceWithMocks =>
  val hakutoiveenTulos = mock[Hakutoiveentulos]
  hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
}

trait EiHakutoivetta extends MockedHakemuksenTulos { this: VastaanottoServiceWithMocks =>
  hakemuksenTulos.findHakutoive(hakukohde.oid) returns None
}

class YhdenPaikanSaantoVoimassaHakutoiveLoytyy extends YhdenPaikanSaantoVoimassa with HakutoiveLoytyy {}

class IlmanYhdenPaikanSaantoaHakutoiveLoytyy extends IlmanYhdenPaikanSaantoa with HakutoiveLoytyy {}

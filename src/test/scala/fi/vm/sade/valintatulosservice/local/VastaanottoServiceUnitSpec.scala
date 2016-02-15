package fi.vm.sade.valintatulosservice.local

import java.util.Date

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, Hakukohde, YhdenPaikanSaanto}
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
        "kun hakijalla ei aiempia vastaanottoja samalle koulutuksen alkamiskaudelle" in new YhdenPaikanSaantoVoimassa {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)

          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.eq(kausi)) returns Set.empty[VastaanottoRecord]
          v.vastaanotaHakukohde(vastaanottoEvent) must beEqualTo(Success(()))
          there was one(hakijaVastaanottoRepository).store(vastaanottoEvent)
        }
        "kun hakijalla yksi aiempi vastaanotto samalle koulutuksen alkamiskaudelle" in new YhdenPaikanSaantoVoimassa {
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
        "hakijalla ei voi olla useita aiempia vastaanottoja samalle alkamiskaudelle" in new YhdenPaikanSaantoVoimassa {
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
        "kun hakijalla ei aiempia vastaanottoja samassa haussa" in new IlmanYhdenPaikanSaantoa {
          val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakukohde.oid, VastaanotaSitovasti)

          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set()
          v.vastaanotaHakukohde(vastaanottoEvent) must beEqualTo(Success(()))
          there was one(hakijaVastaanottoRepository).store(vastaanottoEvent)
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla yksi aiempi vastaanotto samassa haussa" in new IlmanYhdenPaikanSaantoa {
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
        "hakijalla ei voi olla useita vastaanottoja samassa haussa" in new IlmanYhdenPaikanSaantoa {
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
  val hakukohdeRecordService = mock[HakukohdeRecordService]
  val valintatulosService = mock[ValintatulosService]
  val hakijaVastaanottoRepository = mock[HakijaVastaanottoRepository]
  val valintatulosRepository = mock[ValintatulosRepository]
  val v = new VastaanottoService(null, valintatulosService, hakijaVastaanottoRepository, hakukohdeRecordService,
    valintatulosRepository)
  val henkiloOid = "1.2.246.562.24.00000000000"
}

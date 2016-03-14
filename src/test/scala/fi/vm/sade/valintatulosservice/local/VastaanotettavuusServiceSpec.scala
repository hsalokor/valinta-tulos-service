package fi.vm.sade.valintatulosservice.local

import java.util.Date

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, Hakukohde, YhdenPaikanSaanto}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService, VastaanottoRecord}
import fi.vm.sade.valintatulosservice.{PriorAcceptanceException, VastaanotettavuusService}
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.specs2.matcher.MustThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.mock.mockito.{MockitoMatchers, MockitoStubs}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope
import slick.dbio.{DBIO, DBIOAction, FailureAction, SuccessAction}

import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class VastaanotettavuusServiceSpec extends Specification with MockitoMatchers with MockitoStubs {
  "VastaanotettavuusService" in {
    "tarkistaAiemmatVastaanotot" in {
      "kun haussa yhden paikan sääntö voimassa" in {
        "kun hakijalla useita aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          returnRunBlockingResult[Option[VastaanottoRecord]](hakijaVastaanottoRepository)
          hakijaVastaanottoRepository.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, kausi) returns DBIOAction.failed(new RuntimeException("test msg"))
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must throwA("test msg")
        }
        "kun hakijalla yksi aiempi vastaanotto" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          returnRunBlockingResult[Option[VastaanottoRecord]](hakijaVastaanottoRepository)
          hakijaVastaanottoRepository.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, kausi) returns DBIOAction.successful(Some(previousVastaanottoRecord))
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beFailedTry.withThrowable[PriorAcceptanceException]
        }
        "kun hakijalla ei aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          returnRunBlockingResult[Option[VastaanottoRecord]](hakijaVastaanottoRepository)
          hakijaVastaanottoRepository.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, kausi) returns DBIOAction.successful(None)
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beSuccessfulTry
        }
      }
      "kun yhden paikan sääntö ei voimassa" in {


        "kun hakijalla useita aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          returnRunBlockingResult[Option[VastaanottoRecord]](hakijaVastaanottoRepository)
          hakijaVastaanottoRepository.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohde.oid) returns DBIOAction.failed(new RuntimeException("test msg"))
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must throwA("test msg")
          there was no(hakijaVastaanottoRepository).findYhdenPaikanSaannonPiirissaOlevatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla yksi aiempi vastaanotto" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          returnRunBlockingResult[Option[VastaanottoRecord]](hakijaVastaanottoRepository)
          hakijaVastaanottoRepository.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohde.oid) returns DBIOAction.successful(Some(previousVastaanottoRecord))
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beFailedTry.withThrowable[PriorAcceptanceException]
          there was no(hakijaVastaanottoRepository).findYhdenPaikanSaannonPiirissaOlevatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla ei aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          returnRunBlockingResult[Option[VastaanottoRecord]](hakijaVastaanottoRepository)
          hakijaVastaanottoRepository.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohde.oid) returns DBIOAction.successful(None)
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beSuccessfulTry
          there was no(hakijaVastaanottoRepository).findYhdenPaikanSaannonPiirissaOlevatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
      }
    }
  }

  trait YhdenPaikanSaantoVoimassa extends Mockito with Scope with MustThrownExpectations { this: VastaanotettavuusServiceWithMocks =>
    val henkiloOid = "1.2.246.562.24.00000000000"
    val haku = Haku("1.2.246.562.29.00000000000", true, true, true, false, true, None, Set(), List(),
      YhdenPaikanSaanto(true, "kk haku ilman kohdejoukon tarkennetta"))
    val koulutusOid = "1.2.246.562.17.00000000000"
    val hakukohde = Hakukohde("1.2.246.562.20.00000000000", haku.oid, List(koulutusOid), "KORKEAKOULUTUS", "TUTKINTO")
    val kausi = Syksy(2015)
    val previousVastaanottoRecord = VastaanottoRecord(
      henkiloOid,
      haku.oid,
      hakukohde.oid,
      VastaanotaSitovasti,
      ilmoittaja = "",
      new Date(0)
    )
    hakukohdeRecordService.getHakukohdeRecord(hakukohde.oid) returns HakukohdeRecord(hakukohde.oid, haku.oid, true, true, kausi)
  }

  trait IlmanYhdenPaikanSaantoa extends Mockito with Scope with MustThrownExpectations { this: VastaanotettavuusServiceWithMocks =>
    val henkiloOid = "1.2.246.562.24.00000000000"
    val hakemusOid = "1.2.246.562.99.00000000000"
    val haku = Haku("1.2.246.562.29.00000000001", true, true, true, false, true, None, Set(), List(),
      YhdenPaikanSaanto(false, "ei kk haku"))
    val koulutusOid = "1.2.246.562.17.00000000001"
    val hakukohde = Hakukohde("1.2.246.562.20.00000000001", haku.oid, List(koulutusOid), "AMMATILLINEN_PERUSKOULUTUS", "TUTKINTO_OHJELMA")
    val kausi = Syksy(2015)
    val previousVastaanottoRecord = VastaanottoRecord(
      henkiloOid,
      haku.oid,
      hakukohde.oid,
      VastaanotaSitovasti,
      ilmoittaja = "",
      new Date(0)
    )
    hakukohdeRecordService.getHakukohdeRecord(hakukohde.oid) returns HakukohdeRecord(hakukohde.oid, haku.oid, false, true, kausi)
  }

  trait VastaanotettavuusServiceWithMocks extends Mockito with Scope with MustThrownExpectations {
    val hakukohdeRecordService = mock[HakukohdeRecordService]
    val hakijaVastaanottoRepository = mock[HakijaVastaanottoRepository]
    val v = new VastaanotettavuusService(hakukohdeRecordService, hakijaVastaanottoRepository)
  }

  private def returnRunBlockingResult[T](hakijaVastaanottoRepository: HakijaVastaanottoRepository): T = {
    hakijaVastaanottoRepository.runBlocking[T](any[DBIO[T]], any[Duration]).answers { (params, mock) =>
      params match {
        case Array(action, _) => action match {
          case SuccessAction(v) => v.asInstanceOf[T]
          case FailureAction(t) => throw t
        }
        case x => throw new IllegalArgumentException(s"Broken arguments: $x")
      }
    }
  }
}

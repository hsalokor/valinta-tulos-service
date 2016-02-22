package fi.vm.sade.valintatulosservice.local

import java.util.Date

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, Hakukohde, YhdenPaikanSaanto}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}
import fi.vm.sade.valintatulosservice.{PriorAcceptanceException, VastaanotettavuusService}
import org.junit.runner.RunWith
import org.mockito.Matchers
import org.specs2.matcher.MustThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope

@RunWith(classOf[JUnitRunner])
class VastaanotettavuusServiceSpec extends Specification {
  "VastaanotettavuusService" in {
    "tarkistaAiemmatVastaanotot" in {
      "kun haussa yhden paikan sääntö voimassa" in {
        "kun hakijalla useita aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          val anotherPreviousVastaanottoRecord = previousVastaanottoRecord.copy(
            hakukohdeOid = hakukohde.oid + "1",
            hakuOid = haku.oid + "1"
          )
          val aiemmatVastaanotot = Set(previousVastaanottoRecord, anotherPreviousVastaanottoRecord)
          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns aiemmatVastaanotot
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beFailedTry.withThrowable[IllegalStateException]
        }
        "kun hakijalla yksi aiempi vastaanotto" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns Set(previousVastaanottoRecord)
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beFailedTry.withThrowable[PriorAcceptanceException]
        }
        "kun hakijalla aiempi peruttu hakutoive" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns Set(previousVastaanottoRecord.copy(action = Peru))
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beSuccessfulTry
        }
        "kun hakijalla ei aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns Set()
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beSuccessfulTry
        }
      }
      "kun yhden paikan sääntö ei voimassa" in {
        "kun hakijalla useita aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          val anotherPreviousVastaanottoRecord = previousVastaanottoRecord.copy(hakukohdeOid = hakukohde.oid + "1")
          val aiemmatVastaanotot = Set(previousVastaanottoRecord, anotherPreviousVastaanottoRecord)
          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns aiemmatVastaanotot
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beFailedTry.withThrowable[IllegalStateException]
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla yksi aiempi vastaanotto" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set(previousVastaanottoRecord)
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beFailedTry.withThrowable[PriorAcceptanceException]
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla aiempi peruttu hakutoive" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set(previousVastaanottoRecord.copy(action = Peru))
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beSuccessfulTry
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla ei aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set()
          v.tarkistaAiemmatVastaanotot(henkiloOid, hakukohde.oid) must beSuccessfulTry
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
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

}

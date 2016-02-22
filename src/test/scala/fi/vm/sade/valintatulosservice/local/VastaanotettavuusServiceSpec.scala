package fi.vm.sade.valintatulosservice.local

import java.util.Date

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService, Hakukohde, YhdenPaikanSaanto}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}
import fi.vm.sade.valintatulosservice.{PriorAcceptanceException, ValintatulosService, VastaanotettavuusService}
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
    "vastaanotettavuus" in {
      val hakemusOid = "1.2.246.562.11.00000441784"
      "kun hakukohdetta ei löydy" in new VastaanotettavuusServiceWithMocks {
        hakukohdeRecordService.getHakukohdeRecord("non-existent-oid") throws new RuntimeException("test msg")
        v.vastaanotettavuus("henkilo-oid", hakemusOid, "non-existent-oid") must throwA[RuntimeException](message = "test msg")
      }
      "kun hakemusta ei löydy" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
        valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns None
        val r = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
        r.allowedActions must be(Nil)
        r.reason must beSome[Exception].which(_.getMessage == s"Hakemusta $hakemusOid ei löydy")
      }
      "kun hakukohde ei löydy hakemukselta hakutoiveena" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
        hakemuksenTulos.findHakutoive(hakukohde.oid) returns None
        valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)
        val r = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
        r.allowedActions must be(Nil)
        r.reason must beSome[Exception].which(_.getMessage == s"Ei löydy kohteen ${hakukohde.oid} tulosta hakemuksen tuloksesta $hakemuksenTulos")
      }
      "kun hakijaa ei ole hyväksytty hakukohteeseen" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
        hakutoiveenTulos.valintatila returns (Valintatila.hylätty)
        hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
        valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)
        val r = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
        r.allowedActions must be(Nil)
        r.reason must beSome[Exception].which(_.getMessage == s"Ei voida ottaa vastaan, koska hakutoiveen valintatila ei ole hyväksytty: ${hakutoiveenTulos.valintatila}")
      }
      "kun haussa yhden paikan sääntö voimassa" in {
        "kun hakijalla useita aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
          hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
          valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)

          val anotherPreviousVastaanottoRecord = previousVastaanottoRecord.copy(
            hakukohdeOid = hakukohde.oid + "1",
            hakuOid = haku.oid + "1"
          )
          val aiemmatVastaanotot = Set(previousVastaanottoRecord, anotherPreviousVastaanottoRecord)
          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns aiemmatVastaanotot
          val r = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
          r.allowedActions must be(Nil)
          r.reason must beSome[Exception].which(_.getMessage == s"Hakijalla ${henkiloOid} useita vastaanottoja: $aiemmatVastaanotot")
        }
        "kun hakijalla yksi aiempi vastaanotto" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
          hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
          hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
          valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)

          hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns Set(previousVastaanottoRecord)
          val r = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
          r.allowedActions must be(Nil)
          r.reason must beSome[Exception].which(_.isInstanceOf[PriorAcceptanceException])
        }
      }
      "kun yhden paikan sääntö ei voimassa" in {
        "kun hakijalla useita aiempia vastaanottoja" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
          hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
          valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)

          val anotherPreviousVastaanottoRecord = previousVastaanottoRecord.copy(hakukohdeOid = hakukohde.oid + "1")
          val aiemmatVastaanotot = Set(previousVastaanottoRecord, anotherPreviousVastaanottoRecord)
          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns aiemmatVastaanotot
          val r = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
          r.allowedActions must be(Nil)
          r.reason must beSome[Exception].which(_.getMessage == s"Hakijalla ${henkiloOid} useita vastaanottoja: $aiemmatVastaanotot")
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
        "kun hakijalla yksi aiempi vastaanotto" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
          hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
          hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
          valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)

          hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set(previousVastaanottoRecord)
          val r = v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid)
          r.allowedActions must be(Nil)
          r.reason must beSome[Exception].which(_.isInstanceOf[PriorAcceptanceException])
          there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
        }
      }
      "kun hakutoive ei ole vastaanotettavissa ehdollisesti ja yhden paikan sääntö ei voimassa" in new VastaanotettavuusServiceWithMocks with IlmanYhdenPaikanSaantoa {
        hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
        hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
        valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)

        hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, haku.oid) returns Set()
        valintatulosService.onkoVastaanotettavissaEhdollisesti(hakutoiveenTulos, haku) returns false
        v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid) must beEqualTo(Vastaanotettavuus(List(Peru, VastaanotaSitovasti), None))
        there was no(hakijaVastaanottoRepository).findKkTutkintoonJohtavatVastaanotot(Matchers.any[String], Matchers.any[Kausi])
      }
      "kun hakutoive ei ole vastaanotettavissa ehdollisesti" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
        hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
        hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
        valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)

        hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns Set()
        valintatulosService.onkoVastaanotettavissaEhdollisesti(hakutoiveenTulos, haku) returns false
        v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid) must beEqualTo(Vastaanotettavuus(List(Peru, VastaanotaSitovasti), None))
        there was no(hakijaVastaanottoRepository).findHenkilonVastaanototHaussa(Matchers.any[String], Matchers.any[String])
      }
      "kun hakutoive on vastaanotettavissa ehdollisesti" in new VastaanotettavuusServiceWithMocks with YhdenPaikanSaantoVoimassa {
        hakutoiveenTulos.valintatila returns (Valintatila.hyväksytty)
        hakemuksenTulos.findHakutoive(hakukohde.oid) returns Some(hakutoiveenTulos)
        valintatulosService.hakemuksentulos(haku.oid, hakemusOid) returns Some(hakemuksenTulos)

        hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(henkiloOid, kausi) returns Set()
        valintatulosService.onkoVastaanotettavissaEhdollisesti(hakutoiveenTulos, haku) returns true
        v.vastaanotettavuus(henkiloOid, hakemusOid, hakukohde.oid) must beEqualTo(Vastaanotettavuus(List(Peru, VastaanotaSitovasti, VastaanotaEhdollisesti), None))
        there was no(hakijaVastaanottoRepository).findHenkilonVastaanototHaussa(Matchers.any[String], Matchers.any[String])
      }
    }
  }

  trait YhdenPaikanSaantoVoimassa extends Mockito with Scope with MustThrownExpectations { this: VastaanotettavuusServiceWithMocks =>
    val henkiloOid = "1.2.246.562.24.00000000000"
    val hakemusOid = "1.2.246.562.99.00000000000"
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
    hakuService.getHaku(haku.oid) returns Some(haku)
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
    hakuService.getHaku(haku.oid) returns Some(haku)
  }

  trait VastaanotettavuusServiceWithMocks extends Mockito with Scope with MustThrownExpectations {
    val hakukohdeRecordService = mock[HakukohdeRecordService]
    val valintatulosService = mock[ValintatulosService]
    val hakijaVastaanottoRepository = mock[HakijaVastaanottoRepository]
    val hakuService = mock[HakuService]
    val v = new VastaanotettavuusService(valintatulosService, hakuService, hakukohdeRecordService, hakijaVastaanottoRepository)
    val hakemuksenTulos = mock[Hakemuksentulos]
    val hakutoiveenTulos = mock[Hakutoiveentulos]
  }

}

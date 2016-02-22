package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}
import fi.vm.sade.valintatulosservice.{ValintatulosService, VastaanotettavuusService, VastaanottoService}
import org.junit.runner.RunWith
import org.specs2.matcher.MustThrownExpectations
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.Scope

import scala.util.matching.Regex

@RunWith(classOf[JUnitRunner])
class VastaanottoServiceUnitSpec extends Specification {
  "VastaanottoService" in {
    "vastaanotaHakukohde" in {
      "kun hakutoive ei ole vastaanotettavissa halutulla tavalla ja ei syytä" in new VastaanottoServiceWithMocks {
        val allowedActions = List(Peru, VastaanotaSitovasti)
        val vastaanottoEvent = VastaanottoEvent(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti)
        vastaanotettavuusService.vastaanotettavuus(henkiloOid, hakemusOid, hakukohdeOid) returns Vastaanotettavuus(allowedActions, None)
        v.vastaanotaHakukohde(vastaanottoEvent) must beFailedTry.withThrowable[IllegalStateException](s"${vastaanottoEvent.action} ei ole sallittu. Sallittuja ovat ${Regex.quote(allowedActions.toString)}")
      }
      "kun hakutoive ei ole vastaanotettavissa halutulla tavalla ja syy" in new VastaanottoServiceWithMocks {
        val reason = new Exception("test reason")
        vastaanotettavuusService.vastaanotettavuus(henkiloOid, hakemusOid, hakukohdeOid) returns Vastaanotettavuus(Nil, Some(reason))
        v.vastaanotaHakukohde(VastaanottoEvent(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti)) must beFailedTry[Unit].withThrowable[Exception]("test reason")
      }
      "kun hakutoive on vastaanotettavissa" in new VastaanottoServiceWithMocks {
        vastaanotettavuusService.vastaanotettavuus(henkiloOid, hakemusOid, hakukohdeOid) returns Vastaanotettavuus(List(Peru, VastaanotaSitovasti), None)
        v.vastaanotaHakukohde(VastaanottoEvent(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti)) must beSuccessfulTry.withValue(())
      }
    }
  }

  trait VastaanottoServiceWithMocks extends Mockito with Scope with MustThrownExpectations {
    val hakukohdeRecordService = mock[HakukohdeRecordService]
    val valintatulosService = mock[ValintatulosService]
    val hakijaVastaanottoRepository = mock[HakijaVastaanottoRepository]
    val valintatulosRepository = mock[ValintatulosRepository]
    val hakuService = mock[HakuService]
    val vastaanotettavuusService = mock[VastaanotettavuusService]
    val v = new VastaanottoService(hakuService, vastaanotettavuusService, valintatulosService, hakijaVastaanottoRepository, hakukohdeRecordService,
      valintatulosRepository)
    val henkiloOid = "1.2.246.562.24.00000000000"
    val hakemusOid = "1.2.246.562.99.00000000000"
    val hakukohdeOid = "1.2.246.562.20.00000000000"
  }
}

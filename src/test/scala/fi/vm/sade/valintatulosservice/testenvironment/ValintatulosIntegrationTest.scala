package fi.vm.sade.valintatulosservice.testenvironment

import fi.vm.sade.valintatulosservice.ValintatulosService
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Hakemuksentulos
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import org.specs2.mutable.Specification

class ValintatulosIntegrationTest extends Specification {
  "in luokka environment" should {
    "return valintatulos for " in {
      implicit val appConfig = new AppConfig.LocalTestingWithTemplatedVars("../deploy/vars/environments/ophitest_vars.yml")

      val valintatulosService = new ValintatulosService()

      val tulos: Hakemuksentulos = valintatulosService.hakemuksentulos("1.2.246.562.29.92175749016", "1.2.246.562.11.00000000330").get

      tulos.hakutoiveet.length must_== 2
    }
  }
}

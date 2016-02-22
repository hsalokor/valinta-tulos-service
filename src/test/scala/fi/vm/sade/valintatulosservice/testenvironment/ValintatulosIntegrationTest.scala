package fi.vm.sade.valintatulosservice.testenvironment

import java.io.File

import fi.vm.sade.valintatulosservice.ValintatulosService
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.domain.Hakemuksentulos
import fi.vm.sade.valintatulosservice.sijoittelu.SijoittelutulosService
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import org.junit.runner.RunWith
import org.specs2.execute._
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValintatulosIntegrationTest extends Specification {
  "in luokka environment" should {
    "return valintatulos for " in {
      val varsFile = "../environment-ophitest/deploy/ophitest_vars.yml"
      if (new File(varsFile).exists()) {
        implicit val appConfig = new AppConfig.LocalTestingWithTemplatedVars(varsFile)

        val hakuService = HakuService(appConfig)
        val sijoittelutulosService = new SijoittelutulosService(appConfig.sijoitteluContext.raportointiService, appConfig.ohjausparametritService, null)
        val valintatulosService = new ValintatulosService(null, sijoittelutulosService, hakuService)

        val tulos: Hakemuksentulos = valintatulosService.hakemuksentulos("1.2.246.562.29.92175749016", "1.2.246.562.11.00000000330").get

        tulos.hakutoiveet.length must_== 2
      } else {
        throw new SkipException(Skipped("Variables file not found at " + varsFile))
      }
    }
  }
}

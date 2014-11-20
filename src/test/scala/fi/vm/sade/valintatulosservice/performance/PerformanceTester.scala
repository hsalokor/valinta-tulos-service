package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.fixture.{ExampleFixture, LargerFixture}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService

object PerformanceTester extends App with Logging {
  implicit val appConfig: AppConfig = new AppConfig.IT
  val hakuService = HakuService(appConfig)
  appConfig.start

  new LargerFixture(5, 1000, randomize = true).fixture.apply(appConfig)

  logger.info("fixture applied")

  val haku = hakuService.getHaku("1").get

  println(new ValintatulosService(hakuService).hakemuksentulos("1", "1"))
}
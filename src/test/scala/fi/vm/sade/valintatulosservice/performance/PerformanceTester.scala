package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.generatedfixtures.{GeneratedFixture, SimpleGeneratedHakuFixture}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}

object PerformanceTester extends App with Logging {
  implicit val appConfig: AppConfig = new AppConfig.IT
  val hakuService = HakuService(appConfig)
  appConfig.start

  new GeneratedFixture(new SimpleGeneratedHakuFixture(5, 1000)).apply(appConfig)

  logger.info("fixture applied")

  val haku = hakuService.getHaku("1").get

  println(new ValintatulosService(hakuService).hakemuksentulos("1", "1"))

  // TODO: tämä vielä ihan vaiheessa
}
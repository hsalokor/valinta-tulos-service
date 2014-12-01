package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.generatedfixtures.{RandomizedGeneratedHakuFixture, GeneratedFixture, SimpleGeneratedHakuFixture}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}

object PerformanceTestFixtureCreator extends App with Logging {
  implicit val appConfig: AppConfig = new AppConfig.Dev
  val hakuService = HakuService(appConfig)
  appConfig.start

  private val randomData: RandomizedGeneratedHakuFixture = new RandomizedGeneratedHakuFixture(100, 100000, kohteitaPerHakemus = 5)

  new GeneratedFixture(randomData).apply(appConfig)

  logger.info("fixture applied")
}
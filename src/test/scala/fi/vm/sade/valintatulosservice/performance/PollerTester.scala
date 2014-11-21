package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.MailPoller
import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}

object PollerTester extends App with Logging {
  implicit val appConfig: AppConfig = AppConfig.fromSystemProperty
  appConfig.start

  val hakuService = HakuService(appConfig)
  val valintatulosService: ValintatulosService = new ValintatulosService(hakuService)


  val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, limit = 3)

  println(poller.pollForMailables)
}
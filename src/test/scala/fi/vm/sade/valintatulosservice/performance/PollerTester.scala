package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.MailPoller
import fi.vm.sade.valintatulosservice.{TimeWarp, Logging, ValintatulosService}

object PollerTester extends App with Logging with TimeWarp {
  implicit val appConfig: AppConfig = AppConfig.fromSystemProperty
  appConfig.start

  val hakuService = HakuService(appConfig)
  val valintatulosService: ValintatulosService = new ValintatulosService(hakuService)

  val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, limit = 100)

  logger.info("Polling...")

  withFixedDateTime("22.11.2014 15:00") {
    val mailables = poller.pollForMailables
    logger.info("Got stuff")

    mailables.foreach { mailStatus =>
      println(mailStatus.hakemusOid + " -> " + mailStatus.anyMailToBeSent)
    }
  }
}
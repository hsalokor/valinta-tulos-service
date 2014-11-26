package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.MailPoller
import fi.vm.sade.valintatulosservice.{Timer, TimeWarp, Logging, ValintatulosService}

object PollerTester extends App with Logging with TimeWarp {
  implicit val appConfig: AppConfig = AppConfig.fromSystemProperty
  appConfig.start

  val hakuService = HakuService(appConfig)
  val valintatulosService: ValintatulosService = new ValintatulosService(hakuService)

  val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 100)

  logger.info("Polling...")

  withFixedDateTime("22.11.2014 15:00") {
    (1 to 5) foreach { num =>
      val mailables = Timer.timed(0, "pollForMailables") {
        poller.pollForMailables()
      }

      logger.info("Got mailables")

      mailables.foreach { mailStatus =>
        println(mailStatus.hakemusOid + " -> " + mailStatus.anyMailToBeSent)
      }
    }
  }
}
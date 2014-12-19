package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.utils.Timer
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.{HakemusMailStatus, MailPoller}
import fi.vm.sade.valintatulosservice.{TimeWarp, ValintatulosService}

object PollerTester extends App with Logging with TimeWarp {
  implicit val appConfig: AppConfig = AppConfig.fromSystemProperty
  appConfig.start

  val hakuService = HakuService(appConfig)
  val valintatulosService: ValintatulosService = new ValintatulosService(hakuService)

  val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 100)

  logger.info("Polling...")
  var total = 0
  var added = 0

  //withFixedDateTime("22.11.2014 15:00") {
    do {
      val mailables: List[HakemusMailStatus] = Timer.timed("pollForMailables") {
        poller.pollForMailables()
      }
      added = mailables.size
      total = total + added
      mailables.foreach { mailable =>
        println(mailable)
      }
      logger.info("Got mailables: " + added + ", total so far "+ total)

    } while (added > 0)
  //}
}
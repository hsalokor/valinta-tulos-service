package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}
import fi.vm.sade.valintatulosservice.vastaanottomeili.{LahetysKuittaus, HakemusMailStatus, MailPoller}
import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}

object MailPollerPerformanceTester extends App with Logging {
  implicit val appConfig: AppConfig = new AppConfig.Dev
  val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val mailPoller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 1000)

  HakuFixtures.useFixture(HakuFixtures.korkeakouluYhteishaku, List("1"))

  while(true) {
    logger.info("Polling for mailables...")
    val mailables: List[HakemusMailStatus] = mailPoller.pollForMailables()
    logger.info("Got " + mailables.size)
    mailables.toStream
      .map(mailable => LahetysKuittaus(mailable.hakemusOid, mailable.hakukohteet.map(_.hakukohdeOid), List("email")))
      .foreach(mailPoller.markAsSent(_))
    logger.info("Marked as sent")
  }

}
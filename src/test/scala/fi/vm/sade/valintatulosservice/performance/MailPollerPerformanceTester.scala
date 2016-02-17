package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.sijoittelu.SijoittelutulosService
import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}
import fi.vm.sade.valintatulosservice.vastaanottomeili.{ValintatulosMongoCollection, LahetysKuittaus, HakemusMailStatus, MailPoller}
import fi.vm.sade.valintatulosservice.ValintatulosService

object MailPollerPerformanceTester extends App with Logging {
  implicit val appConfig: AppConfig = new AppConfig.Dev
  val hakuService = HakuService(appConfig)
  lazy val sijoittelutulosService = new SijoittelutulosService(appConfig.sijoitteluContext.raportointiService, appConfig.ohjausparametritService, null)
  lazy val valintatulosService = new ValintatulosService(sijoittelutulosService, hakuService)
  lazy val valintatulokset = new ValintatulosMongoCollection(appConfig.settings.valintatulosMongoConfig)
  lazy val mailPoller = new MailPoller(valintatulokset, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 1000)

  HakuFixtures.useFixture(HakuFixtures.korkeakouluYhteishaku, List("1"))

  while(true) {
    logger.info("Polling for mailables...")
    val mailables: List[HakemusMailStatus] = mailPoller.pollForMailables()
    logger.info("Got " + mailables.size)
    mailables.toStream
      .map(mailable => LahetysKuittaus(mailable.hakemusOid, mailable.hakukohteet.map(_.hakukohdeOid), List("email")))
      .foreach(valintatulokset.markAsSent(_))
    logger.info("Marked as sent")
  }

}
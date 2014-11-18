package fi.vm.sade.valintatulosservice.experimental

import fi.vm.sade.valintatulosservice.ValintatulosService
import fi.vm.sade.valintatulosservice.config.AppConfig.LocalTestingWithTemplatedVars
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.MailPoller

object MailPollerQA extends App {
  val url = System.getProperty("mongo.uri")
  val mongoConfig: MongoConfig = MongoConfig(url, "sijoitteludb")

  val appConfig = new LocalTestingWithTemplatedVars("../deploy/vars/environments/ophp_vars.yml")

  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)

  private val list = new MailPoller(mongoConfig, valintatulosService, hakuService).pollForMailables(List("1.2.246.562.5.2013080813081926341927"))

  list.foreach { mailable =>
    println(mailable)
  }
}

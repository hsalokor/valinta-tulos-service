package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.{HakemusMailStatus, MailPoller}
import fi.vm.sade.valintatulosservice.{ITSetup, ValintatulosService}
import org.specs2.mutable.Specification

class MailPollerSpec extends Specification with ITSetup {
  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService)

  "Finds mailables" in {
    val mailables: List[HakemusMailStatus] = poller.pollForMailables("1.2.246.562.5.2013080813081926341928")
    mailables.size must_== 1
    mailables(0).hakemusOid must_== "1.2.246.562.11.00000441369"
    mailables(0).hakukohteet.size must_== 2
    mailables(0).hakukohteet(0).shouldMail must_== true
    mailables(0).hakukohteet(1).shouldMail must_== false
  }
}

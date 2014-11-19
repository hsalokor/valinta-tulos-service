package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}
import fi.vm.sade.valintatulosservice.vastaanottomeili.{HakemusIdentifier, HakemusMailStatus, MailPoller}
import fi.vm.sade.valintatulosservice.{ITSpecification, ValintatulosService}

class MailPollerSpec extends ITSpecification {
  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"

  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService)

  "Finds candidates" in {
    useFixture("hyvaksytty-hylatty-toisessa-jonossa.json")
    val hakemusId = HakemusIdentifier(hakuOid,"1.2.246.562.11.00000441369")
    poller.pollForCandidates must_== List(hakemusId, hakemusId)
  }

  "Finds mailables" in {
    useFixture("hyvaksytty-hylatty-toisessa-jonossa.json")
    val mailables: List[HakemusMailStatus] = poller.pollForMailables
    mailables.size must_== 1
    mailables(0).hakemusOid must_== "1.2.246.562.11.00000441369"
    mailables(0).hakukohteet.size must_== 2
    mailables(0).hakukohteet(0).shouldMail must_== true
    mailables(0).hakukohteet(1).shouldMail must_== false
    mailables(0).anyMailToBeSent must_== true
  }

  "Marks mails sent" in {
    useFixture("hyvaksytty-hylatty-toisessa-jonossa.json")
    var mailables: List[HakemusMailStatus] = poller.pollForMailables
    mailables.foreach(poller.markAsHandled(_))
    mailables = poller.pollForMailables
    mailables.size must_== 1
    mailables(0).anyMailToBeSent must_== false

    poller.pollForCandidates.size must_== 1 // <- se jono, jonka tulosta ei lähetetty
  }


  // TODO: hae relevantit haut (miten?)
  // TODO: (hyväksytty+hylätty)         -> 2 candidates, 1 status (dups removed), 1 to be sent
  // TODO: (hyväksytty+hylätty) -> mark -> 1 candidate,  1 status,              , 0 to be sent
  // TODO: testaa: vain korkeakouluhaku
  // TODO: testaa limittaus ja sorttaus
}
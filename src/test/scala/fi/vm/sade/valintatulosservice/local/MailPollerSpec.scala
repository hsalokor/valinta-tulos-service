package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.performance.ExampleFixture
import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}
import fi.vm.sade.valintatulosservice.vastaanottomeili.{LahetysKuittaus, HakemusIdentifier, HakemusMailStatus, MailPoller}
import fi.vm.sade.valintatulosservice.{ITSpecification, ValintatulosService}

class MailPollerSpec extends ITSpecification {
  val hakuOid = ExampleFixture.hakuOid
  val hakemusOid = ExampleFixture.hakemusOid

  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService)

  "Finds candidates" in {
    ExampleFixture.fixture.apply

    val hakemusId = HakemusIdentifier(hakuOid, hakemusOid)
    poller.pollForCandidates must_== List(hakemusId, hakemusId)
  }

  "Finds mailables" in {
    val mailables: List[HakemusMailStatus] = poller.pollForMailables
    mailables.size must_== 1
    mailables(0).hakemusOid must_== hakemusOid
    mailables(0).hakukohteet.size must_== 1
    mailables(0).hakukohteet(0).shouldMail must_== true
    mailables(0).anyMailToBeSent must_== true
  }

  "Marks mails sent" in {
    var mailables: List[HakemusMailStatus] = poller.pollForMailables
    mailables
      .map{ mail => LahetysKuittaus(mail.hakemusOid, mail.hakukohteet.map(_.hakukohdeOid), List("email")) }
      .foreach(poller.markAsSent(_))
    mailables = poller.pollForMailables
    mailables.size must_== 1
    mailables(0).anyMailToBeSent must_== false

    poller.pollForCandidates.size must_== 1 // <- se jono, jonka tulosta ei lähetetty
  }


  // TODO: testaa previosCheck-päivitys (tähän auttaisi se isompi datasetti)
  // TODO: hae tarkemmin relevantit haut (miten?)
  // TODO: (hyväksytty+hylätty)         -> 2 candidates, 1 status (dups removed), 1 to be sent
  // TODO: (hyväksytty+hylätty) -> mark -> 1 candidate,  1 status,              , 0 to be sent
  // TODO: testaa: vain korkeakouluhaku
  // TODO: testaa limittaus ja sorttaus
}
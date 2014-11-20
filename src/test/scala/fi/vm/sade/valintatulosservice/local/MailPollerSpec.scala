package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.fixture.LargerFixture
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.{HakemusIdentifier, HakemusMailStatus, LahetysKuittaus, MailPoller}
import fi.vm.sade.valintatulosservice.{ITSpecification, ValintatulosService}

class MailPollerSpec extends ITSpecification {
  val fixture = new LargerFixture(5, 5)


  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, limit = 3)

  "Finds candidates (limited number)" in {
    fixture.fixture.apply

    poller.pollForCandidates must_== List(HakemusIdentifier("1","1"), HakemusIdentifier("1","2"), HakemusIdentifier("1","3"))
  }

  "Finds mailables" in {
    val mailables: List[HakemusMailStatus] = poller.pollForMailables
    mailables.size must_== 3
    mailables(0).hakukohteet.size must_== 5
    mailables(0).hakukohteet(0).shouldMail must_== true
    mailables(0).anyMailToBeSent must_== true
  }

  "Marks mails sent" in {
    var mailables: List[HakemusMailStatus] = poller.pollForMailables
    mailables
      .map{ mail => LahetysKuittaus(mail.hakemusOid, mail.hakukohteet.map(_.hakukohdeOid), List("email")) }
      .foreach(poller.markAsSent(_))
    mailables = poller.pollForMailables
    mailables.size must_== 2
    mailables(0).anyMailToBeSent must_== true
  }


  // TODO: testaa previosCheck-päivitys (tähän auttaisi se isompi datasetti)
  // TODO: hae tarkemmin relevantit haut (miten?)
  // TODO: (hyväksytty+hylätty)         -> 2 candidates, 1 status (dups removed), 1 to be sent
  // TODO: (hyväksytty+hylätty) -> mark -> 1 candidate,  1 status,              , 0 to be sent
  // TODO: testaa: vain korkeakouluhaku
  // TODO: testaa limittaus ja sorttaus
}
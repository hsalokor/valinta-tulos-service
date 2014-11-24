package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.sijoittelu.domain.HakemuksenTila
import fi.vm.sade.valintatulosservice.fixture._
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.{HakemusIdentifier, HakemusMailStatus, LahetysKuittaus, MailPoller}
import fi.vm.sade.valintatulosservice.{TimeWarp, ITSpecification, ValintatulosService}

class MailPollerSpec extends ITSpecification with TimeWarp {
  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, limit = 3)

  "Kun päässyt kaikkiin hakukohteisiin" should {
    val fixture = new LargerFixture(5, 5)

    "Finds candidates (limited number, cycles through candidates)" in {
      fixture.apply

      val result1 = poller.pollForCandidates
      result1.size must_== 3

      val result2 = poller.pollForCandidates
      result2.size must_== 3
      result2 must_!= result1
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
        .map { mail => LahetysKuittaus(mail.hakemusOid, mail.hakukohteet.map(_.hakukohdeOid), List("email"))}
        .foreach(poller.markAsSent(_))
      mailables = poller.pollForMailables
      mailables.size must_== 2
      mailables(0).anyMailToBeSent must_== true
    }
  }

  "Kun hyväksytty yhteen kohteeseen ja hylätty toisessa" in {
    val fixture = new GeneratedFixture {
      override def hakemukset = List(
        HakemuksenTulosFixture("H1", List(
          HakemuksenHakukohdeFixture("1", "1"),
          HakemuksenHakukohdeFixture("1", "2", List(ValintatapaJonoFixture(HakemuksenTila.HYLATTY)))
        )),
        HakemuksenTulosFixture("H2", List(
          HakemuksenHakukohdeFixture("1", "1", List(ValintatapaJonoFixture(HakemuksenTila.HYLATTY)))
        ))
      )
    }


    "Meili lähetetään" in {
      fixture.apply

      val mailables: List[HakemusMailStatus] = poller.pollForMailables

      mailables.map(_.hakemusOid) must_== List("H2", "H1")
      mailables.filter(_.anyMailToBeSent).map(_.hakemusOid) must_== List("H1")
    }

    // TODO: testaa Kesken -> Hyväksytty muutoksesta aiheutuva meili

    "Samaa ei tarkisteta uudelleen ennen 24H kulumista" in {
      fixture.apply
      withFixedDateTime("10.10.2014 0:00") {
        poller.pollForCandidates.map(_.hakemusOid) must_== Set("H1", "H2")
        poller.pollForCandidates.map(_.hakemusOid) must_== Set.empty
        withFixedDateTime("11.10.2014 1:00") {
          poller.pollForCandidates.map(_.hakemusOid) must_== Set("H1", "H2")
        }
      }
    }
  }


  // TODO: hae tarkemmin relevantit haut (miten?)
  // TODO: (hyväksytty+hylätty)         -> 2 candidates, 1 status (dups removed), 1 to be sent
  // TODO: (hyväksytty+hylätty) -> mark -> 1 candidate,  1 status,              , 0 to be sent
  // TODO: testaa: vain korkeakouluhaku
  // TODO: kantaan lista lähetysmedioista
}
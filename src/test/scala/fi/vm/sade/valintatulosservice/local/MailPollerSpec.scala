package fi.vm.sade.valintatulosservice.local

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.TypeImports._
import fi.vm.sade.sijoittelu.domain.HakemuksenTila
import fi.vm.sade.valintatulosservice.generatedfixtures._
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}
import fi.vm.sade.valintatulosservice.vastaanottomeili.{HakemusMailStatus, LahetysKuittaus, MailPoller}
import fi.vm.sade.valintatulosservice.{ITSpecification, TimeWarp, ValintatulosService}
import org.bson.BSONObject

class MailPollerSpec extends ITSpecification with TimeWarp {
  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 3)

  "Hakujen filtteröinti" in {
    "korkeakouluhaku -> mukaan" in {
      new GeneratedFixture(new SingleHakemusFixture()).apply
      poller.etsiHaut must_== List("1")
    }

    "2.asteen haku -> ei mukaan" in {
      new GeneratedFixture(new SingleHakemusFixture()) {
        override def hakuFixture = HakuFixtures.toinenAsteYhteishaku
      }.apply
      poller.etsiHaut must_== Nil
    }

    "Jos hakuaika ei alkanut -> ei mukaan" in {
      new GeneratedFixture(new SingleHakemusFixture()) {
        override def hakuFixture = "korkeakoulu-yhteishaku-hakuaika-tulevaisuudessa"
      }.apply
      poller.etsiHaut must_== Nil
    }

    "Jos hakukierros päättynyt -> ei mukaan" in {
      new GeneratedFixture(new SingleHakemusFixture()){
        override def ohjausparametritFixture = "hakukierros-loppuu-2010"
      }.apply
      poller.etsiHaut must_== Nil
    }
  }

  "Kun päässyt kaikkiin hakukohteisiin" in {
    lazy val fixture = new GeneratedFixture(new SimpleGeneratedHakuFixture(5, 5))

    "Finds candidates (limited number, cycles through candidates)" in {
      fixture.apply

      val result1 = poller.pollForCandidates
      result1.size must_== 3

      val result2 = poller.pollForCandidates
      result2.size must_== 2 // next list of candidates contains the rest
      result2 must_!= result1
    }

    "Finds mailables" in {
      fixture.apply
      val mailables: List[HakemusMailStatus] = poller.pollForMailables()
      mailables.size must_== 3
      mailables(0).hakukohteet.size must_== 5
      poller.pollForMailables().size must_== 2 // the rest of the mailables returned on next call
    }

    "Marks mails sent" in {
      val timestamp = System.currentTimeMillis()
      withFixedDateTime(timestamp) {
        fixture.apply
        val mailables: List[HakemusMailStatus] = poller.pollForMailables()
        mailables
          .map { mail => LahetysKuittaus(mail.hakemusOid, mail.hakukohteet.map(_.hakukohdeOid), List("email"))}
          .foreach(poller.markAsSent(_))

        verifyMailSent(mailables(0).hakemusOid, mailables(0).hakukohteet(0).hakukohdeOid, timestamp)

        val nextMailables = poller.pollForMailables()
        nextMailables.size must_== 2
      }
    }
  }

  "Kun hyväksytty yhteen kohteeseen ja hylätty toisessa" in {
    lazy val fixture = new GeneratedFixture(List(new GeneratedHakuFixture() {
      override def hakemukset = List(
        HakemuksenTulosFixture("H1", List(
          HakemuksenHakukohdeFixture("1", "1"),
          HakemuksenHakukohdeFixture("1", "2", List(ValintatapaJonoFixture(HakemuksenTila.HYLATTY)))
        )),
        HakemuksenTulosFixture("H2", List(
          HakemuksenHakukohdeFixture("1", "1", List(ValintatapaJonoFixture(HakemuksenTila.HYLATTY)))
        ))
      )
    }))

    "Meili lähetetään" in {
      fixture.apply

      val mailables: List[HakemusMailStatus] = poller.pollForMailables()

      mailables.map(_.hakemusOid).toSet must_== Set("H1")
    }

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

  "Kun hakemus KESKEN-tilassa" in {
    lazy val fixture = new GeneratedFixture(List(new GeneratedHakuFixture() {
      override def hakemukset = List(
        HakemuksenTulosFixture("H1", List(
          HakemuksenHakukohdeFixture("1", "1", List(ValintatapaJonoFixture(HakemuksenTila.HYVAKSYTTY)), julkaistavissa = false)
        ))
      )
    }))

    "Meiliä ei lähetetä" in {
      fixture.apply
      poller.pollForMailables() must_== Nil
    }
  }

  "Kun Hakemuksia on useammassa Haussa" in {
    val fixture = new GeneratedFixture(List(new SimpleGeneratedHakuFixture(1, 4, "1"), new SimpleGeneratedHakuFixture(1, 4, "2")))
    "Tuloksia haetaan molemmista" in {
      val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 8)
      fixture.apply
      poller.pollForMailables().size must_== 4 // <- molemmista hauista tulee 2 hyväksyttyä
    }

    "Määrärajoitus koskee kaikkia Hakuja yhteensä" in {
      val poller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 3)
      fixture.apply
      poller.pollForMailables().size must_== 3
    }
  }

  private def verifyMailSent(hakemusOid: String, hakukohdeOid: String, timestamp: Long) {
    val valintatulosCollection = MongoFactory.createDB(appConfig.settings.valintatulosMongoConfig)("Valintatulos")
    val query = MongoDBObject(
      "hakemusOid" -> hakemusOid,
      "hakukohdeOid" -> hakukohdeOid
    )
    val result: DBObject = valintatulosCollection.findOne(query).get
    result.expand[Long]("mailStatus.sent") must_== Some(timestamp)
    result.expand[List[String]]("mailStatus.media") must_== Some(List("email"))
  }
}
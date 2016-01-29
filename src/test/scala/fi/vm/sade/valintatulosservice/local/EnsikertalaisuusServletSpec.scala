package fi.vm.sade.valintatulosservice.local

import java.net.URLEncoder
import java.util.concurrent.TimeUnit

import fi.vm.sade.valintatulosservice.ServletSpecification
import fi.vm.sade.valintatulosservice.ensikertalaisuus.EnsikertalaisuusServlet._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{EnsikertalaisuusServlet, EiEnsikertalainen, Ensikertalainen, Ensikertalaisuus}
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.jackson.Serialization._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class EnsikertalaisuusServletSpec extends ServletSpecification {
  override implicit val formats = EnsikertalaisuusServlet.ensikertalaisuusJsonFormats
  val henkilo = "1.2.246.562.24.00000000001"
  val hakukohde = "1.2.246.561.20.00000000001"
  val haku = "1.2.246.561.29.00000000001"
  val koulutus = "1.2.246.561.21.00000000001"
  val timestamp = new DateTime(2014, 7, 1, 0, 0, 10, DateTimeZone.forID("Europe/Helsinki"))

  step({
    Await.ready(valintarekisteriDb.run(DBIOAction.seq(
      sqlu"""insert into hakufamilies (name) values ('testfamily')""",
      sqlu"""insert into haut ("hakuOid", "familyId") values ($haku, currval('hakufamilies_id_seq'))""",
      sqlu"""insert into hakukohteet ("hakukohdeOid", "hakuOid", "familyId", "tutkintoonJohtava")
             values ($hakukohde, $haku, currval('hakufamilies_id_seq'), true)""",
      sqlu"""insert into kaudet (kausi, ajanjakso) values ('2015K', '["2014-12-31 22:00:00","2015-07-31 21:00:00")')""",
      sqlu"""insert into koulutukset ("koulutusOid", alkamiskausi) values ($koulutus, '2015K')""",
      sqlu"""insert into koulutushakukohde ("koulutusOid", "hakukohdeOid") values ($koulutus, $hakukohde)""",
      sqlu"""insert into vastaanotot
             (henkilo, hakukohde, "familyId", active, "kkTutkintoonJohtava", ilmoittaja, "timestamp", deleted)
             values ($henkilo, $hakukohde, currval('hakufamilies_id_seq'), true, true, 'ilmoittaja', ${timestamp.getMillis}, null)"""
    ).transactionally), Duration(1, TimeUnit.SECONDS))
  })

  "GET /ensikertalaisuus/:henkiloOid" should {
    "return 200 OK" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        status mustEqual 200
      }
    }

    "return 200 OK with shorter ISO 8601 koulutuksenAlkamispvm" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00Z"), Map("Content-Type" -> "application/json")) {
        status mustEqual 200
        body mustEqual """{"personOid":"1.2.246.562.24.00000000001","paattyi":"2014-07-01T00:00:10+03"}"""
      }
    }

    "return EiEnsikertalainen" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        body mustEqual """{"personOid":"1.2.246.562.24.00000000001","paattyi":"2014-07-01T00:00:10+03"}"""
        read[EiEnsikertalainen](body) mustEqual EiEnsikertalainen("1.2.246.562.24.00000000001", timestamp.toDate)
      }
    }

    "return Ensikertalainen" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000002", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        read[Ensikertalaisuus](body) mustEqual Ensikertalainen("1.2.246.562.24.00000000002")
      }
    }

    "return 400 Bad Request for invalid henkilo oid" in {
      get("ensikertalaisuus/foo", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for invalid koulutuksenAlkamispvm" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamispvm" -> "foo"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for missing koulutuksenAlkamispvm" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map(), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }
  }

  "POST /ensikertalaisuus" should {
    "return 200 OK" in {
      postJSON(s"ensikertalaisuus?koulutuksenAlkamispvm=${URLEncoder.encode("2014-07-01T00:00:00.000+03:00", "UTF-8")}", write(Seq("1.2.246.562.24.00000000001")), Map()) {
        status mustEqual 200
      }
    }

    "return a sequence of EiEnsikertalainen" in {
      val personOidsToQuery = Seq("1.2.246.562.24.00000000001", "1.2.246.562.24.00000000002")
      postJSON(s"ensikertalaisuus?koulutuksenAlkamispvm=${URLEncoder.encode("2014-07-01T00:00:00.000+03:00", "UTF-8")}", write(personOidsToQuery), Map()) {
        val ensikertalaisuuses = read[Seq[Ensikertalaisuus]](body).sortBy(_.personOid)
        ensikertalaisuuses must have size 2
        ensikertalaisuuses.head mustEqual EiEnsikertalainen("1.2.246.562.24.00000000001", formats.dateFormat.parse("2014-07-01T00:00:10+03").get)
        ensikertalaisuuses(1) mustEqual Ensikertalainen("1.2.246.562.24.00000000002")
      }
    }

    "return 400 Bad Request if too many henkilo oids is sent" in {
      postJSON(s"ensikertalaisuus?koulutuksenAlkamispvm=${URLEncoder.encode("2014-07-01T00:00:00.000+03:00", "UTF-8")}", write((1 to (maxHenkiloOids + 1)).map(i => s"1.2.246.562.24.$i")), Map()) {
        status mustEqual 400
      }
    }
  }
}

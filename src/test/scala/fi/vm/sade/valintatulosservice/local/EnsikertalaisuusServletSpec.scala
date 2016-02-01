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
      sqlu"""insert into haut ("hakuOid") values ($haku)""",
      sqlu"""insert into hakukohteet ("hakukohdeOid", "hakuOid", kktutkintoonjohtava)
             values ($hakukohde, $haku, true)""",
      sqlu"""insert into koulutukset ("koulutusOid", alkamiskausi) values ($koulutus, '2015K')""",
      sqlu"""insert into koulutushakukohde ("koulutusOid", "hakukohdeOid") values ($koulutus, $hakukohde)""",
      sqlu"""insert into vastaanotot
             (henkilo, hakukohde, active, ilmoittaja, "timestamp", deleted)
             values ($henkilo, $hakukohde, true, 'ilmoittaja', ${timestamp.getMillis}, null)"""
    ).transactionally), Duration(1, TimeUnit.SECONDS))
  })

  "GET /ensikertalaisuus/:henkiloOid" should {
    "return 200 OK" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamiskausi" -> "2014K"), Map("Content-Type" -> "application/json")) {
        status mustEqual 200
      }
    }

    "return EiEnsikertalainen" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamiskausi" -> "2014K"), Map("Content-Type" -> "application/json")) {
        body mustEqual """{"personOid":"1.2.246.562.24.00000000001","paattyi":"2014-07-01T00:00:10.000+03"}"""
        read[EiEnsikertalainen](body) mustEqual EiEnsikertalainen("1.2.246.562.24.00000000001", timestamp.toDate)
      }
    }

    "return Ensikertalainen" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000002", Map("koulutuksenAlkamiskausi" -> "2014K"), Map("Content-Type" -> "application/json")) {
        read[Ensikertalaisuus](body) mustEqual Ensikertalainen("1.2.246.562.24.00000000002")
      }
    }

    "return 400 Bad Request for invalid henkilo oid" in {
      get("ensikertalaisuus/foo", Map("koulutuksenAlkamiskausi" -> "2014K"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for invalid koulutuksenAlkamiskausi" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamiskausi" -> "foo"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for missing koulutuksenAlkamiskausi" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map(), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }
  }

  "POST /ensikertalaisuus" should {
    "return 200 OK" in {
      postJSON("ensikertalaisuus?koulutuksenAlkamiskausi=2014K", write(Seq("1.2.246.562.24.00000000001")), Map()) {
        status mustEqual 200
      }
    }

    "return a sequence of EiEnsikertalainen" in {
      val personOidsToQuery = Seq("1.2.246.562.24.00000000001", "1.2.246.562.24.00000000002")
      postJSON("ensikertalaisuus?koulutuksenAlkamiskausi=2014K", write(personOidsToQuery), Map()) {
        val ensikertalaisuuses = read[Seq[Ensikertalaisuus]](body).sortBy(_.personOid)
        ensikertalaisuuses must have size 2
        ensikertalaisuuses.head mustEqual EiEnsikertalainen("1.2.246.562.24.00000000001", formats.dateFormat.parse("2014-07-01T00:00:10.000+03").get)
        ensikertalaisuuses(1) mustEqual Ensikertalainen("1.2.246.562.24.00000000002")
      }
    }

    "return 400 Bad Request if too many henkilo oids is sent" in {
      postJSON("ensikertalaisuus?koulutuksenAlkamiskausi=2014K", write((1 to (maxHenkiloOids + 1)).map(i => s"1.2.246.562.24.$i")), Map()) {
        status mustEqual 400
      }
    }
  }
}

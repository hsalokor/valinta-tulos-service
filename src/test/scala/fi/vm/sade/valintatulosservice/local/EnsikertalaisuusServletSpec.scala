package fi.vm.sade.valintatulosservice.local

import java.net.URLEncoder
import java.text.SimpleDateFormat

import fi.vm.sade.valintatulosservice.ServletSpecification
import fi.vm.sade.valintatulosservice.ensikertalaisuus.EnsikertalaisuusServlet._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{Ensikertalainen, EiEnsikertalainen, Ensikertalaisuus}
import org.json4s.jackson.Serialization._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EnsikertalaisuusServletSpec extends ServletSpecification {

  "GET /ensikertalaisuus/:henkiloOid" should {
    "return 200 OK" in {
      get("ensikertalaisuus/1.2.246.561.24.00000000001", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        status mustEqual 200
      }
    }

    "return EiEnsikertalainen" in {
      get("ensikertalaisuus/1.2.246.561.24.00000000001", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        read[Ensikertalaisuus](body) mustEqual EiEnsikertalainen("1.2.246.561.24.00000000001", new SimpleDateFormat(dateFormat).parse("2014-07-01T00:00:10.000+03:00"))
      }
    }

    "return Ensikertalainen" in {
      get("ensikertalaisuus/1.2.246.561.24.00000000002", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        read[Ensikertalaisuus](body) mustEqual Ensikertalainen("1.2.246.561.24.00000000002")
      }
    }

    "return 400 Bad Request for invalid henkilo oid" in {
      get("ensikertalaisuus/foo", Map("koulutuksenAlkamispvm" -> "2014-07-01T00:00:00.000+03:00"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for invalid koulutuksenAlkamispvm" in {
      get("ensikertalaisuus/1.2.246.561.24.00000000001", Map("koulutuksenAlkamispvm" -> "foo"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for missing koulutuksenAlkamispvm" in {
      get("ensikertalaisuus/1.2.246.561.24.00000000001", Map(), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }
  }

  "POST /ensikertalaisuus" should {
    "return 200 OK" in {
      postJSON(s"ensikertalaisuus?koulutuksenAlkamispvm=${URLEncoder.encode("2014-07-01T00:00:00.000+03:00", "UTF-8")}", write(Seq("1.2.246.561.24.00000000001")), Map()) {
        status mustEqual 200
      }
    }

    "return a sequence of EiEnsikertalainen" in {
      postJSON(s"ensikertalaisuus?koulutuksenAlkamispvm=${URLEncoder.encode("2014-07-01T00:00:00.000+03:00", "UTF-8")}", write(Seq("1.2.246.561.24.00000000001")), Map()) {
        read[Seq[Ensikertalaisuus]](body).head mustEqual EiEnsikertalainen("1.2.246.561.24.00000000001", new SimpleDateFormat(dateFormat).parse("2014-07-01T00:00:10.000+03:00"))
      }
    }

    "return 400 Bad Request if too many henkilo oids is sent" in {
      postJSON(s"ensikertalaisuus?koulutuksenAlkamispvm=${URLEncoder.encode("2014-07-01T00:00:00.000+03:00", "UTF-8")}", write((1 to (maxHenkiloOids + 1)).map(i => s"1.2.246.561.24.$i")), Map()) {
        status mustEqual 400
      }
    }

  }

}

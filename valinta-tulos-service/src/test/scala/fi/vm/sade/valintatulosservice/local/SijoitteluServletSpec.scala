package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SijoitteluServletSpec extends ServletSpecification {
  "POST /sijoittelu/sijoitteluajo" should {
    skipped //TODO sijoitteluajon parametrit jää nulliksi + tämä rajapinta poistunee tulevaisuudessa
    "Lisää sijoitteluajon" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      post("sijoittelu/sijoitteluajo", """{"sijoitteluajoId": 1, "hakuOid": 1, "startMils": 111111, "endMils": 222222}""", Map[String, String]("Content-type" -> "Application/json")) {
        status must_== 200
        body must_== ""
      }
    }
  }
}

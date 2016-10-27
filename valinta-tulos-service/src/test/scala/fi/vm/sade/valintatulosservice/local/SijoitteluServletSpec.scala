package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SijoitteluServletSpec extends ServletSpecification {
  "POST /sijoittelu/sijoitteluajo" should {
    "Lisää sijoitteluajon" in {
      skipped //TODO sijoitteluajon parametrit jää nulliksi + tämä rajapinta poistunee tulevaisuudessa
      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      post("sijoittelu/sijoitteluajo", """{"sijoitteluajoId": 1, "hakuOid": 1, "startMils": 111111, "endMils": 222222}""", Map[String, String]("Content-type" -> "Application/json")) {
        status must_== 200
        body must_== ""
      }
    }
  }

  "GET /sijoittelu/:hakuOid/sijoitteluajo/:sijoitteluajoOid/hakemus/:hakemusOid" should {
    "Hakee sijoittelun hakemuksen" in {
      skipped // TODO data ei mene kantaan?
      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      get("sijoittelu/1.2.246.562.29.173465377510/sijoitteluajo/1409055160622/hakemus/1.2.246.562.11.00000877688") {
        status must_== 200
        body must_== ""
      }
    }
  }
}

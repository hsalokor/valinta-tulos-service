package fi.vm.sade.valintatulosservice.production

import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import org.specs2.mutable.Specification
import org.json4s.JsonAST.JValue
import org.json4s.DefaultFormats
import org.json4s.Formats
import fi.vm.sade.valintatulosservice.domain.Hakemuksentulos
import fi.vm.sade.valintatulosservice.domain.Valintatila

class ProductionSmokeTest extends Specification {
  import org.json4s.jackson.JsonMethods._
  implicit val jsonFormats: Formats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all

  val hakuOid = "1.2.246.562.5.2014022711042555034240"
  val hyväksytty = "1.2.246.562.11.00000923132"
  val perunut = "1.2.246.562.11.00000901332"
  val url = "https://virkailija.opintopolku.fi/valinta-tulos-service/haku/" + hakuOid + "/hakemus/"

  "tuotantoympäristön valintatuloksissa " should {
    "hakija hakemuksella " + hyväksytty + " on hyväksytty ylempään hakutoiveeseen ja alempi on peruuntunut" in {
      val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url + hyväksytty).responseWithHeaders
      responseCode must_== 200
      val tulos = parse(resultString).extractOpt[JValue].map(_.extract[Hakemuksentulos]).get
      tulos.hakemusOid must_== hyväksytty
      tulos.hakutoiveet(0).valintatila must_== Valintatila.hyväksytty
      tulos.hakutoiveet(1).valintatila must_== Valintatila.peruuntunut
    }

    "hakija hakemuksella " + perunut + " on perunut paikan kahdessa ylimmässä hakutoiveessa ja alin on peruuntunut" in {
      val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url + perunut).responseWithHeaders
      responseCode must_== 200
      val tulos = parse(resultString).extractOpt[JValue].map(_.extract[Hakemuksentulos]).get
      tulos.hakemusOid must_== perunut
      tulos.hakutoiveet(0).valintatila must_== Valintatila.perunut
      tulos.hakutoiveet(1).valintatila must_== Valintatila.perunut
      tulos.hakutoiveet(2).valintatila must_== Valintatila.peruuntunut
    }
  }
}

package fi.vm.sade.valintatulosservice.ohjausparametrit

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import fi.vm.sade.valintatulosservice.memoize.TTLOptionalMemoize
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.ext.JodaTimeSerializers
import fi.vm.sade.valintatulosservice.domain.Vastaanottoaikataulu
import fi.vm.sade.valintatulosservice.JsonFormats
import java.util.Date

trait OhjausparametritService {
  def aikataulu(asId: String): Option[Vastaanottoaikataulu]
}

class StubbedOhjausparametritService extends OhjausparametritService {
  def aikataulu(asId: String) = {
    val fileName = "/fixtures/ohjausparametrit/" + OhjausparametritFixtures.activeFixture + ".json"
    Option(getClass.getResourceAsStream(fileName))
      .map(io.Source.fromInputStream(_).mkString)
      .map(parse(_).asInstanceOf[JObject])
      .flatMap(OhjausparametritParser.parseValintatulokset(_))
  }
}

object CachedRemoteOhjausparametritService {
  def apply(implicit appConfig: AppConfig): OhjausparametritService = {
    val service = new RemoteOhjausparametritService()
    val valintatuloksetMemo = TTLOptionalMemoize.memoize(service.aikataulu _, 60 * 60)

    new OhjausparametritService() {
      override def aikataulu(asId: String) = valintatuloksetMemo(asId)
    }
  }
}

class RemoteOhjausparametritService(implicit appConfig: AppConfig) extends OhjausparametritService with JsonFormats {
  import org.json4s.jackson.JsonMethods._

  def aikataulu(asId: String) = {
    val (responseCode, _, resultString) = DefaultHttpClient.httpGet(appConfig.settings.ohjausparametritUrl + "/" + asId)
      .responseWithHeaders

    responseCode match {
      case 200 =>
        parse(resultString).extractOpt[JValue].flatMap(OhjausparametritParser.parseValintatulokset(_))
      case _ => None
    }
  }
}

private object OhjausparametritParser extends JsonFormats {
  def parseValintatulokset(json: JValue) = {
    val vastaanottoEnd = for {
      obj <- (json \ "PH_OPVP").toOption
      end <- (obj \ "date").extractOpt[Long].map(new Date(_))
    } yield end
    val vastaanottoBufferDays = for {
      obj <- (json \ "PH_HPVOA").toOption
      end <- (obj \ "value").extractOpt[Int]
    } yield end
    Some(Vastaanottoaikataulu(vastaanottoEnd, vastaanottoBufferDays))
  }
}


package fi.vm.sade.valintatulosservice.ohjausparametrit

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.memoize.TTLOptionalMemoize
import org.json4s._
import org.json4s.jackson.JsonMethods._
import fi.vm.sade.valintatulosservice.domain.Vastaanottoaikataulu
import java.util.Date

case class Ohjausparametrit(vastaanottoaikataulu: Option[Vastaanottoaikataulu], ilmoittautuminenPaattyy: Option[Date])

trait OhjausparametritService {
  def ohjausparametrit(asId: String): Option[Ohjausparametrit]
}

class StubbedOhjausparametritService extends OhjausparametritService {
  def ohjausparametrit(asId: String) = {
    val fileName = "/fixtures/ohjausparametrit/" + OhjausparametritFixtures.activeFixture + ".json"
    Option(getClass.getResourceAsStream(fileName))
      .map(io.Source.fromInputStream(_).mkString)
      .map(parse(_).asInstanceOf[JObject])
      .flatMap(OhjausparametritParser.parseOhjausparametrit(_))
  }
}

object CachedRemoteOhjausparametritService {
  def apply(implicit appConfig: AppConfig): OhjausparametritService = {
    val service = new RemoteOhjausparametritService()
    val ohjausparametritMemo = TTLOptionalMemoize.memoize(service.ohjausparametrit _, 60 * 60)

    new OhjausparametritService() {
      override def ohjausparametrit(asId: String) = ohjausparametritMemo(asId)
    }
  }
}

class RemoteOhjausparametritService(implicit appConfig: AppConfig) extends OhjausparametritService with JsonFormats {
  import org.json4s.jackson.JsonMethods._

  def ohjausparametrit(asId: String) = {
    val (responseCode, _, resultString) = DefaultHttpClient.httpGet(appConfig.settings.ohjausparametritUrl + "/" + asId)
      .responseWithHeaders

    responseCode match {
      case 200 =>
        parse(resultString).extractOpt[JValue].flatMap(OhjausparametritParser.parseOhjausparametrit(_))
      case _ => None
    }
  }
}

private object OhjausparametritParser extends JsonFormats {

  def parseOhjausparametrit(json: JValue) = {
    Some(Ohjausparametrit(parseValintatulokset(json), parseIlmoittautuminenPaattyy(json)))
  }

  private def parseValintatulokset(json: JValue) = {
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

  private def parseIlmoittautuminenPaattyy(json: JValue) = {
    for {
      obj <- (json \ "PH_IP").toOption
      end <- (obj \ "date").extractOpt[Long].map(new Date(_))
    } yield end
  }
}


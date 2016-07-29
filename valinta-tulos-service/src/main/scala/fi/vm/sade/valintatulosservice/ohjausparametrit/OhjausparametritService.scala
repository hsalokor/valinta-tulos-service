package fi.vm.sade.valintatulosservice.ohjausparametrit

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.utils.http.DefaultHttpClient
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.memoize.TTLOptionalMemoize
import org.joda.time.DateTime
import org.json4s._
import org.json4s.jackson.JsonMethods._
import fi.vm.sade.valintatulosservice.domain.Vastaanottoaikataulu

import scala.util.{Failure, Success, Try}

case class Ohjausparametrit(vastaanottoaikataulu: Option[Vastaanottoaikataulu], varasijaSaannotAstuvatVoimaan: Option[DateTime], ilmoittautuminenPaattyy: Option[DateTime], hakukierrosPaattyy: Option[DateTime], tulostenJulkistusAlkaa: Option[DateTime], kaikkiJonotSijoittelussa: Option[DateTime])

trait OhjausparametritService {
  def ohjausparametrit(asId: String): Option[Ohjausparametrit]
}

class StubbedOhjausparametritService extends OhjausparametritService {
  def ohjausparametrit(asId: String): Option[Ohjausparametrit] = {
    val fileName = "/fixtures/ohjausparametrit/" + OhjausparametritFixtures.activeFixture + ".json"
    Option(getClass.getResourceAsStream(fileName))
      .map(io.Source.fromInputStream(_).mkString)
      .map(parse(_).asInstanceOf[JObject])
      .map(OhjausparametritParser.parseOhjausparametrit)
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
    val url = appConfig.settings.ohjausparametritUrl + "/" + asId
    DefaultHttpClient.httpGet(url).responseWithHeaders match {
      case (200, _, resultString) =>
        Try(OhjausparametritParser.parseOhjausparametrit(parse(resultString))) match {
          case Success(r) => Some(r)
          case Failure(t) => throw new RuntimeException(s"Error parsing response $resultString", t)
        }
      case (404, _, _) => None
      case (status, _, _) => throw new RuntimeException(s"Fetching $url failed with HTTP status $status")
    }
  }
}

private object OhjausparametritParser extends JsonFormats {

  def parseOhjausparametrit(json: JValue): Ohjausparametrit = {
    Ohjausparametrit(
      parseVastaanottoaikataulu(json),
      parseVarasijaSaannotAstuvatVoimaan(json),
      parseIlmoittautuminenPaattyy(json),
      parseHakukierrosPaattyy(json),
      parseTulostenJulkistus(json),
      parseKaikkiJonotSijoittelussa(json))
  }

  private def parseDateTime(json: JValue, key: String): Option[DateTime] = {
    for {
      obj <- (json \ key).toOption
      date <- (obj \ "date").extractOpt[Long].map(new DateTime(_))
    } yield date
  }

  private def parseVastaanottoaikataulu(json: JValue) = {
    val vastaanottoEnd = parseDateTime(json, "PH_OPVP")
    val vastaanottoBufferDays = for {
      obj <- (json \ "PH_HPVOA").toOption
      end <- (obj \ "value").extractOpt[Int]
    } yield end
    Some(Vastaanottoaikataulu(vastaanottoEnd, vastaanottoBufferDays))
  }

  private def parseTulostenJulkistus(json: JValue) = {
    for {
      obj <- (json \ "PH_VTJH").toOption
      dateStart <- (obj \ "dateStart").extractOpt[Long].map(new DateTime(_))
    } yield dateStart
  }

  private def parseVarasijaSaannotAstuvatVoimaan(json: JValue) = parseDateTime(json, "PH_VSSAV")

  private def parseIlmoittautuminenPaattyy(json: JValue) = parseDateTime(json, "PH_IP")

  private def parseHakukierrosPaattyy(json: JValue) =  parseDateTime(json, "PH_HKP")

  private def parseKaikkiJonotSijoittelussa(json: JValue) =  parseDateTime(json, "PH_VTSSV")
}


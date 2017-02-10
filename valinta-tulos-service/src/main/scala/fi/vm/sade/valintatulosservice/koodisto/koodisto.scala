package fi.vm.sade.valintatulosservice.koodisto

import fi.vm.sade.utils.http.DefaultHttpClient.httpGet
import fi.vm.sade.valintatulosservice.config.OphUrlProperties.ophProperties
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats, Formats}

import scala.util.Try
import scala.util.control.NonFatal
import scalaj.http.HttpOptions

case class KoodistoUri(uri: String) {
  override def toString: String = uri
}
case class KoodiUri(uri: String) {
  val koodistoUri: KoodistoUri = KoodistoUri(uri.split("_").head)
  override def toString: String = uri
}
case class Koodi(uri: KoodiUri, versio: Int, relaatiot: Option[Relaatiot])
case class Relaatiot(within: List[Koodi], levelsWith: List[Koodi], includes: List[Koodi])

class KoodiSerializer extends CustomSerializer[Koodi]((formats: Formats) => {
  implicit val f = formats
  ({
    case o: JObject =>
      val uri = (o \ "koodiUri").extractOrElse[String]((o \ "codeElementUri").extract[String])
      val versio = (o \ "versio").extractOrElse[Int]((o \ "codeElementVersion").extract[Int])
      val relaatiot = for {
        within <- (o \ "withinCodeElements").toOption.map(_.extract[List[Koodi]])
        levelsWith <- (o \ "levelsWithCodeElements").toOption.map(_.extract[List[Koodi]])
        includes <- (o \ "includesCodeElements").toOption.map(_.extract[List[Koodi]])
      } yield Relaatiot(within, levelsWith, includes)
      Koodi(KoodiUri(uri), versio, relaatiot)
  }, { case o => ??? })
})

class KoodistoService(appConfig: AppConfig) {
  implicit val formats = DefaultFormats ++ List(new KoodiSerializer)

  def fetchLatest(uri: KoodiUri): Either[Throwable, Koodi] = {
    val url = ophProperties.url("koodisto-service.rest.codeelement.latest", uri)
    get(url).right.flatMap(k => fetch(k.uri, k.versio))
  }

  def fetch(uri: KoodiUri, versio: Int): Either[Throwable, Koodi] = {
    val url = ophProperties.url("koodisto-service.rest.codeelement", uri, versio.toString)
    get(url)
  }

  private def get(url: String): Either[Throwable, Koodi] = {
    Try(httpGet(url, HttpOptions.connTimeout(30000), HttpOptions.readTimeout(120000)).responseWithHeaders match {
      case (200, _, body) =>
        Try(Right(parse(body).extract[Koodi])).recover {
          case NonFatal(e) => Left(new IllegalStateException(s"Parsing result $body of GET $url failed"))
        }.get
      case (404, _, body) =>
        Left(new IllegalArgumentException(s"GET $url failed with status 404: $body"))
      case (500, _, body) if body == "error.codeelement.not.found" =>
        Left(new IllegalArgumentException(s"GET $url failed with status 500: $body"))
      case (status, _, body) =>
        Left(new RuntimeException(s"GET $url failed with status $status: $body"))
    }).recover {
      case NonFatal(e) => Left(new RuntimeException(s"GET $url failed", e))
    }.get
  }
}

object KoodistoService {
  val TutkintooJohtavaKoulutus: KoodistoUri = KoodistoUri("tutkintoonjohtavakoulutus")
  val OnTutkinto: KoodiUri = KoodiUri("tutkintoonjohtavakoulutus_1")
}

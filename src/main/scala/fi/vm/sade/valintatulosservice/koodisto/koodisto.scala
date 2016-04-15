package fi.vm.sade.valintatulosservice.koodisto

import fi.vm.sade.utils.http.DefaultHttpClient.httpGet
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats, Formats}

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

  def fetch(uri: KoodiUri, versio: Int): Option[Koodi] = {
    val url = appConfig.settings.koodistoUrl + s"/rest/codeelement/$uri/$versio"
    httpGet(url, HttpOptions.connTimeout(30000), HttpOptions.readTimeout(120000)).responseWithHeaders match {
      case (200, _, body) => Some(parse(body).extract[Koodi])
      case (404, _, _) => None
      case (500, _, body) if body == "error.codeelement.not.found" => None
      case (status, _, body) => throw new RuntimeException(s"GET $url failed, status: $status, body: $body")
    }
  }
}

object KoodistoService {
  val TutkintooJohtavaKoulutus: KoodistoUri = KoodistoUri("tutkintoonjohtavakoulutus")
  val OnTukinto: KoodiUri = KoodiUri("tutkintoonjohtavakoulutus_1")
}

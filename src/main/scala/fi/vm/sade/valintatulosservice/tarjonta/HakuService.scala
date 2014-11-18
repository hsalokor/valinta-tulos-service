package fi.vm.sade.valintatulosservice.tarjonta

import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.{AppConfig, StubbedExternalDeps}
import fi.vm.sade.valintatulosservice.config.ApplicationSettings
import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import fi.vm.sade.valintatulosservice.memoize.TTLOptionalMemoize
import org.json4s.jackson.JsonMethods._

import scala.util.Try

trait HakuService {
  def getHaku(oid: String): Option[Haku]
  def kaikkiHaut: List[Haku]
}

object HakuService {
  def apply(appConfig: AppConfig): HakuService = appConfig match {
    case _:StubbedExternalDeps => new StubbedHakuService(appConfig)
    case _ => new CachedHakuService(new TarjontaHakuService(appConfig))
  }
}

case class Haku(oid: String, korkeakoulu: Boolean, yhteishaku: Boolean, käyttääSijoittelua: Boolean)

protected trait JsonHakuService {
  import org.json4s._
  implicit val formats = DefaultFormats
}

class CachedHakuService(wrapperService: HakuService) extends HakuService {
  private val byOid = TTLOptionalMemoize.memoize(wrapperService.getHaku _, 60 * 60)
  private val all: (String) => Option[List[Haku]] = TTLOptionalMemoize.memoize({any : String => Some(wrapperService.kaikkiHaut)}, 60 * 60)

  override def getHaku(oid: String) = byOid(oid)
  def kaikkiHaut: List[Haku] = all("").toList.flatten
}

private case class HakuTarjonnassa(oid: String, hakutapaUri: String, hakutyyppiUri: String, kohdejoukkoUri: String, sijoittelu: Boolean) {
  def toHaku = {
    val korkeakoulu: Boolean = kohdejoukkoUri.startsWith("haunkohdejoukko_12#")
    val yhteishaku: Boolean = hakutapaUri.startsWith("hakutapa_01#")
    Haku(oid, korkeakoulu, yhteishaku, sijoittelu)
  }
}

class TarjontaHakuService(appConfig: AppConfig) extends HakuService with JsonHakuService with Logging {
  def getHaku(oid: String) = {
    val url = appConfig.settings.tarjontaUrl + "/rest/v1/haku/" + oid
    fetch(url) { response =>
      val hakuTarjonnassa = (parse(response) \ "result").extract[HakuTarjonnassa]
      hakuTarjonnassa.toHaku
    }
  }

  def kaikkiHaut = {
    val url = appConfig.settings.tarjontaUrl + "/rest/v1/haku/find"
    fetch(url) { response =>
      val haut = (parse(response) \ "result").extract[List[HakuTarjonnassa]]
      haut.map(_.toHaku)
    }.getOrElse(Nil)
  }

  private def fetch[T](url: String)(parse: (String => T)) = {
    val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url)
      .responseWithHeaders

    responseCode match {
      case 200 =>
        val parsed = Try(parse(resultString))
        if (parsed.isFailure) logger.error(s"Error parsing response from: $resultString", parsed.failed.get)
        parsed.toOption
      case _ =>
        logger.warn("Get haku from " + url + " failed with status " + responseCode)
        None
    }
  }
}

class StubbedHakuService(appConfig: AppConfig) extends HakuService with JsonHakuService {
  override def getHaku(oid: String) = {
    val fileName = "/fixtures/tarjonta/haku/" + HakuFixtures.activeFixture + ".json"
    Option(getClass.getResourceAsStream(fileName))
      .map(io.Source.fromInputStream(_).mkString)
      .map { response =>
        val hakuTarjonnassa = (parse(response) \ "result").extract[HakuTarjonnassa]
        hakuTarjonnassa.toHaku.copy(oid = oid)
      }
  }

  override def kaikkiHaut = List("1.2.246.562.5.2013080813081926341928").flatMap(getHaku(_))
}
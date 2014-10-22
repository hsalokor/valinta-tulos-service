package fi.vm.sade.valintatulosservice.tarjonta

import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.{AppConfig, StubbedExternalDeps}
import fi.vm.sade.valintatulosservice.config.ApplicationSettings
import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import fi.vm.sade.valintatulosservice.memoize.TTLOptionalMemoize

import scala.util.Try

trait HakuService {
  def getHaku(oid: String): Option[Haku]
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
  import org.json4s.jackson.JsonMethods._
  implicit val formats = DefaultFormats

  protected def parseResponse(oid: String, response: String, settings: ApplicationSettings): Haku = {
    val hakuTarjonnassa = (parse(response) \ "result").extract[HakuTarjonnassa]
    val korkeakoulu: Boolean = hakuTarjonnassa.kohdejoukkoUri.startsWith("haunkohdejoukko_12#")
    val yhteishaku: Boolean = hakuTarjonnassa.hakutapaUri.startsWith("hakutapa_01#")

    Haku(oid, korkeakoulu, yhteishaku, hakuTarjonnassa.sijoittelu)
  }
}

class CachedHakuService(wrapperService: HakuService) extends HakuService {
  private val memo = TTLOptionalMemoize.memoize(wrapperService.getHaku _, 60 * 60)
  override def getHaku(oid: String) = memo(oid)
}

private case class HakuTarjonnassa(oid: String, hakutapaUri: String, hakutyyppiUri: String, kohdejoukkoUri: String, sijoittelu: Boolean) {}

class TarjontaHakuService(appConfig: AppConfig) extends HakuService with JsonHakuService with Logging {
  def getHaku(oid: String) = {
    val url = appConfig.settings.tarjontaUrl + "/rest/v1/haku/" + oid
    val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url)
      .responseWithHeaders

    responseCode match {
      case 200 =>
        val parsed = Try(parseResponse(oid, resultString, appConfig.settings))
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
      .map(parseResponse(oid, _, appConfig.settings))
  }
}
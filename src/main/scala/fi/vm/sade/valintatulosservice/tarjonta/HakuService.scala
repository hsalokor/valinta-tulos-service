package fi.vm.sade.valintatulosservice.tarjonta

import fi.vm.sade.valintatulosservice.config.AppConfig.{StubbedExternalDeps, AppConfig}
import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import fi.vm.sade.valintatulosservice.Logging

trait HakuService {
  def getHaku(oid: String): Option[Haku]
}

object HakuService {
  def apply(appConfig: AppConfig): HakuService = appConfig match {
    case _:StubbedExternalDeps => new StubbedHakuService
    case _ => new TarjontaHakuService(appConfig)
  }
}

trait JsonHakuService {
  import org.json4s._
  import org.json4s.jackson.JsonMethods._
  implicit val formats = DefaultFormats

  protected def parseResponse(response: String): Haku = {
    (parse(response) \ "result").extract[HakuTarjonnassa]
  }
}

class TarjontaHakuService(appConfig: AppConfig) extends HakuService with JsonHakuService with Logging {
  def getHaku(oid: String) = {
    val url = appConfig.settings.tarjontaUrl + "/rest/v1/haku/" + oid
    val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url)
      .responseWithHeaders

    responseCode match {
      case 200 =>
        Some(parseResponse(resultString))
      case _ => {
        logger.warn("Get haku from " + url + " failed with status " + responseCode)
        None
      }
    }
  }
}

class StubbedHakuService extends HakuService with JsonHakuService {
  override def getHaku(oid: String) = {
    val fileName = "/fixtures/tarjonta/haku/" + HakuFixtures.activeFixture + ".json"
    Option(getClass.getResourceAsStream(fileName))
      .map(io.Source.fromInputStream(_).mkString)
      .map(parseResponse(_))
  }
}

trait Haku {
  def toinenAste: Boolean
  def varsinainenHaku: Boolean
}

case class HakuTarjonnassa(oid: String, hakutapaUri: String, hakutyyppiUri: String, kohdejoukkoUri: String) extends Haku {
  def toinenAste: Boolean = kohdejoukkoUri == "haunkohdejoukko_11#1"
  def varsinainenHaku: Boolean = hakutyyppiUri == "hakutyyppi_01#1"
}
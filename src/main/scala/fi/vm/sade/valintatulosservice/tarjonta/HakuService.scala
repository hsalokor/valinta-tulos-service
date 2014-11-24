package fi.vm.sade.valintatulosservice.tarjonta

import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.{AppConfig, StubbedExternalDeps}
import fi.vm.sade.valintatulosservice.http.{DefaultHttpRequest, DefaultHttpClient}
import fi.vm.sade.valintatulosservice.memoize.TTLOptionalMemoize
import org.json4s.jackson.JsonMethods._

import scala.util.Try
import scalaj.http.{Http, HttpOptions}

trait HakuService {
  def getHaku(oid: String): Option[Haku]
  def findLiittyvatHaut(haku: Haku): Set[String] = {
    val parentHaut = haku.varsinaisenHaunOid.flatMap(getHaku(_).map(parentHaku => parentHaku.sisältyvätHaut + parentHaku.oid)).getOrElse(Nil)
    (haku.sisältyvätHaut ++ parentHaut).filterNot(_ == haku.oid)
  }
  def kaikkiHaut: List[Haku]
}

object HakuService {
  def apply(appConfig: AppConfig): HakuService = appConfig match {
    case _:StubbedExternalDeps => HakuFixtures
    case _ => new CachedHakuService(new TarjontaHakuService(appConfig))
  }
}

case class Haku(oid: String, korkeakoulu: Boolean, yhteishaku: Boolean, käyttääSijoittelua: Boolean, varsinaisenHaunOid: Option[String], sisältyvätHaut: Set[String] )

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

private case class HakuTarjonnassa(oid: String, hakutapaUri: String, hakutyyppiUri: String, kohdejoukkoUri: String, sijoittelu: Boolean, parentHakuOid: Option[String], sisaltyvatHaut: Set[String]) {
  def toHaku = {
    val korkeakoulu: Boolean = kohdejoukkoUri.startsWith("haunkohdejoukko_12#")
    val yhteishaku: Boolean = hakutapaUri.startsWith("hakutapa_01#")
    Haku(oid, korkeakoulu, yhteishaku, sijoittelu, parentHakuOid, sisaltyvatHaut)
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

  private def fetch[T](url: String)(parse: (String => T)): Option[T] = {
    val (responseCode, _, resultString) = new DefaultHttpRequest(Http.get(url)
      .options(HttpOptions.connTimeout(30000))
      .option(HttpOptions.readTimeout(120000)))
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
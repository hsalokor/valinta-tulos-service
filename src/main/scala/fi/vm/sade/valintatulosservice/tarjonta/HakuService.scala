package fi.vm.sade.valintatulosservice.tarjonta

import fi.vm.sade.utils.http.DefaultHttpClient
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.{AppConfig, StubbedExternalDeps}
import fi.vm.sade.valintatulosservice.memoize.TTLOptionalMemoize
import org.joda.time.DateTime
import org.json4s.jackson.JsonMethods._

import scala.util.Try
import scalaj.http.HttpOptions

trait HakuService {
  def getHaku(oid: String): Option[Haku]
  def findLiittyvatHaut(haku: Haku): Set[String] = {
    val parentHaut = haku.varsinaisenHaunOid.flatMap(getHaku(_).map(parentHaku => parentHaku.sisältyvätHaut + parentHaku.oid)).getOrElse(Nil)
    (haku.sisältyvätHaut ++ parentHaut).filterNot(_ == haku.oid)
  }
  def kaikkiJulkaistutHaut: List[Haku]
}

object HakuService {
  def apply(appConfig: AppConfig): HakuService = appConfig match {
    case _:StubbedExternalDeps => HakuFixtures
    case _ => new CachedHakuService(new TarjontaHakuService(appConfig))
  }
}

case class Haku(oid: String, korkeakoulu: Boolean, yhteishaku: Boolean, varsinainenhaku: Boolean, lisähaku: Boolean, käyttääSijoittelua: Boolean, varsinaisenHaunOid: Option[String], sisältyvätHaut: Set[String], hakuAjat: List[Hakuaika] )
case class Hakuaika(hakuaikaId: String, alkuPvm: Option[Long], loppuPvm: Option[Long]) {
  def hasStarted = alkuPvm match {
    case Some(alku) => new DateTime().isAfter(new DateTime(alku))
    case _ => true
  }
}

protected trait JsonHakuService {
  import org.json4s._
  implicit val formats = DefaultFormats

  protected def toHaku(haku: HakuTarjonnassa) = {
    val korkeakoulu: Boolean = haku.kohdejoukkoUri.startsWith("haunkohdejoukko_12#")
    val yhteishaku: Boolean = haku.hakutapaUri.startsWith("hakutapa_01#")
    val varsinainenhaku: Boolean = haku.hakutyyppiUri.startsWith("hakutyyppi_01#1")
    val lisähaku: Boolean = haku.hakutyyppiUri.startsWith("hakutyyppi_03#1")
    Haku(haku.oid, korkeakoulu, yhteishaku, varsinainenhaku, lisähaku, haku.sijoittelu, haku.parentHakuOid, haku.sisaltyvatHaut, haku.hakuaikas)
  }
}

class CachedHakuService(wrapperService: HakuService) extends HakuService {
  private val byOid = TTLOptionalMemoize.memoize(wrapperService.getHaku _, 4 * 60 * 60)
  private val all: (String) => Option[List[Haku]] = TTLOptionalMemoize.memoize({any : String => Some(wrapperService.kaikkiJulkaistutHaut)}, 4 * 60 * 60)

  override def getHaku(oid: String) = byOid(oid)
  def kaikkiJulkaistutHaut: List[Haku] = all("").toList.flatten
}

private case class HakuTarjonnassa(oid: String, hakutapaUri: String, hakutyyppiUri: String, kohdejoukkoUri: String, sijoittelu: Boolean,
                                   parentHakuOid: Option[String], sisaltyvatHaut: Set[String], tila: String,
                                   hakuaikas: List[Hakuaika]) {
  def julkaistu = {
    tila == "JULKAISTU"
  }
}

class TarjontaHakuService(appConfig: AppConfig) extends HakuService with JsonHakuService with Logging {

  def parseStatus(json: String): Option[String] = {
    for {
      status <- (parse(json) \ "status").extractOpt[String]
    } yield status
  }

  def getHaku(oid: String): Option[Haku] = {
    val url = appConfig.settings.tarjontaUrl + "/rest/v1/haku/" + oid
    fetch(url) { response =>
      val hakuTarjonnassa = (parse(response) \ "result").extract[HakuTarjonnassa]
      toHaku(hakuTarjonnassa)
    }
  }

  def kaikkiJulkaistutHaut = {
    val url = appConfig.settings.tarjontaUrl + "/rest/v1/haku/find?addHakuKohdes=false"
    fetch(url) { response =>
      val haut = (parse(response) \ "result").extract[List[HakuTarjonnassa]]
      haut.filter(_.julkaistu).map(toHaku(_))
    }.getOrElse(Nil)
  }

  private def fetch[T](url: String)(parse: (String => T)): Option[T] = {
    val (responseCode, _, resultString) = DefaultHttpClient.httpGet(
      url,
      HttpOptions.connTimeout(30000),
      HttpOptions.readTimeout(120000)
    ).responseWithHeaders

    responseCode match {
      case 200 => {
        parseStatus(resultString) match {
          case Some(status) if status.equals("NOT_FOUND") => None
          case _ => {
            val parsed = Try(parse(resultString))
            if(parsed.isFailure) logger.error(s"Error parsing response from: $resultString", parsed.failed.get)
            parsed.toOption
          }
        }
      }
      case _ =>
        logger.warn("Get haku from " + url + " failed with status " + responseCode)
        None
    }
  }
}
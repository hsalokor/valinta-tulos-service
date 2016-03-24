package fi.vm.sade.valintatulosservice.migraatio

import java.net.URLEncoder

import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.json.JsonFormats
import org.http4s.{Method, Request, Uri}
import org.json4s.JsonAST.{JArray, JValue}
import org.json4s.{Reader, _}
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Success, Try}
import scalaz.concurrent.Task

class MissingHakijaOidResolver(appConfig: AppConfig) extends JsonFormats with Logging {
  private val hakuClient = createCasClient(appConfig, "/haku-app")
  private val henkiloClient = createCasClient(appConfig, "/authentication-service")
  private val hakuUrlBase = appConfig.settings.config.getString("valinta-tulos-service.haku-app-url") + "/applications?q="
  private val henkiloPalveluUrlBase = appConfig.settings.config.getString("valinta-tulos-service.authentication-service-url") + "/resources/henkilo?q="

  def findPersonOidByHakemusOid(hakemusOid: String): Option[String] = {
    case class Hetu(personOid: Option[String], hetu: String)

    implicit val hetuReader = new Reader[Hetu] {
      override def read(v: JValue): Hetu = {
        val result: JValue = (v \ "results").asInstanceOf[JArray](0)
        Hetu((result \ "henkiloOid").extractOpt[String], (result \ "ssn").extract[String])
      }
    }
    implicit val hetuDecoder = org.http4s.json4s.native.jsonOf[Hetu]

    Try[Hetu](hakuClient.prepare(Request(method = Method.GET, uri = createUri(hakuUrlBase, hakemusOid))).flatMap {
      case r if 200 == r.status.code => r.as[Hetu]
      case r => Task.fail(new RuntimeException(r.toString))
    }.run) match {
      case Success(hetu: Hetu) => hetu.personOid.orElse(findPersonOidByHetu(hetu.hetu))
      case Failure(f) =>
        logger.error(s"Got failure $f", f)
        None
    }
  }

  private def findPersonOidByHetu(hetu: String): Option[String] = {
    def parsePersonOid(response: String): Option[String] = (parse(response) \ "results" \\ "oppijanumero").extractOpt[String]

    Try(henkiloClient.prepare(createUri(henkiloPalveluUrlBase, hetu)).flatMap {
      case r if 200 == r.status.code => r.as[String]
      case r => Task.fail(new RuntimeException(r.toString))
    }.run) match {
      case Success(response) => parsePersonOid(response)
      case Failure(t) =>
        logger.error(s"Got failure $t", t)
        None
    }
  }

  private def createUri(base: String, rest: String): Uri = {
    val stringUri = base + URLEncoder.encode(rest, "UTF-8")
    Uri.fromString(stringUri).getOrElse(throw new RuntimeException(s"Invalid uri: $stringUri"))
  }

  private def createCasClient(appConfig: AppConfig, targetService: String): CasAuthenticatingClient = {
    val params = CasParams(targetService, appConfig.settings.securitySettings.casUsername, appConfig.settings.securitySettings.casPassword)
    new CasAuthenticatingClient(appConfig.securityContext.casClient, params, org.http4s.client.blaze.defaultClient)
  }
}

package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat
import java.util.Properties

import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.http4s.{Method, Request, Uri}
import org.json4s.DefaultReaders.StringReader
import org.json4s.JsonAST.JValue
import org.json4s.Reader

import scala.util.Try
import scalaz.concurrent.Task

case class Henkiloviite(masterOid: String, henkiloOid: String)

class HenkiloviiteClient(config: Properties) {
  private val resourceUrl = parseResourceUrl()
  private val client = createCasClient()

  implicit val henkiloviiteReader = new Reader[Henkiloviite] {
    override def read(v: JValue): Henkiloviite = {
      Henkiloviite(StringReader.read(v \ "masterOid"), StringReader.read(v \ "henkiloOid"))
    }
  }
  import org.json4s.DefaultReaders.arrayReader
  implicit val henkiloviiteDecoder = org.http4s.json4s.native.jsonOf[Array[Henkiloviite]]

  def fetchHenkiloviitteet(): Try[List[Henkiloviite]] = {
    Try(client.prepare(Request(
      method = Method.GET,
      uri = resourceUrl
    )).flatMap {
      case r if 200 == r.status.code => r.as[Array[Henkiloviite]].map(_.toList)
      case r => Task.fail(new RuntimeException(r.toString))
    }.run)
  }

  private def parseResourceUrl(): Uri = {
    val date = config.getProperty("authentication.service.duplicatehenkilos.date")
    val url = config.getProperty("authentication.service.duplicatehenkilos.url")
    Try(new SimpleDateFormat("yyyy-MM-dd").parse(date))
      .getOrElse(throw new RuntimeException(s"Invalid authentication.service.duplicatehenkilos.date $date"))
    Try(Uri.fromString(url).map(_.withQueryParam("date", date)).toOption.get)
      .getOrElse(throw new RuntimeException(s"Invalid authentication.service.duplicatehenkilos.url $url"))
  }

  private def getConfiguration(key:String): String = {
    Option(config.getProperty(key)).getOrElse(throw new RuntimeException(s"Configuration $key is missing"))
  }

  private def createCasClient(): CasAuthenticatingClient = {
    val username = getConfiguration("authentication.service.username")
    val password = getConfiguration("authentication.service.password")
    val casUrl = getConfiguration("host.cas")
    val casParams = CasParams("/authentication-service", username, password)
    new CasAuthenticatingClient(
      new CasClient(casUrl, org.http4s.client.blaze.defaultClient),
      casParams, org.http4s.client.blaze.defaultClient)
  }
}

package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat

import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.http4s.{Method, Request, Uri}
import org.json4s.DefaultReaders.StringReader
import org.json4s.JsonAST.JValue
import org.json4s.Reader

import scala.util.Try
import scalaz.concurrent.Task

case class Henkiloviite(masterOid: String, henkiloOid: String)

class HenkiloviiteClient(configuration: AuthenticationConfiguration) {
  private val dateFormater = new SimpleDateFormat("yyyy-MM-dd")
  private val resourceUrl = configuration.url.withQueryParam("date", dateFormater.format(configuration.since))
  private val client = createCasClient()

  implicit val henkiloviiteReader = new Reader[Henkiloviite] {
    override def read(v: JValue): Henkiloviite = {
      Henkiloviite(StringReader.read(v \ "masterOid"), StringReader.read(v \ "henkiloOid"))
    }
  }
  import org.json4s.DefaultReaders.arrayReader
  implicit val henkiloviiteDecoder = org.http4s.json4s.native.jsonOf[Array[Henkiloviite]]

  def fetchHenkiloviitteet(): Try[List[Henkiloviite]] = {
    val request = Request(
      method = Method.GET,
      uri = resourceUrl
    )
    Try(client.prepare(request).flatMap {
      case r if 200 == r.status.code => r.as[Array[Henkiloviite]].map(_.toList)
      case r => Task.fail(new RuntimeException("Request " + request + " failed with response " + r))
    }.run)
  }

  private def createCasClient(): CasAuthenticatingClient = {
    val casParams = CasParams("/authentication-service", configuration.cas.user, configuration.cas.password)
    new CasAuthenticatingClient(
      new CasClient(configuration.cas.host, org.http4s.client.blaze.defaultClient),
      casParams, org.http4s.client.blaze.defaultClient)
  }
}

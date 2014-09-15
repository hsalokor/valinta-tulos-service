package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Vastaanotto, Vastaanottotila}
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import org.json4s.JValue
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

class ValintatulosServlet(implicit val appConfig: AppConfig) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats {
  val valintatulosService: ValintatulosService = new ValintatulosService()

  get("/") {
    "valinta-tulos-service"
  }

  get("/haku/:hakuOid/hakemus/:hakemusOid") {
    contentType = formats("json")
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    valintatulosService.hakemuksentulos(hakuOid, hakemusOid)
  }

  // POST vastaanottotila = vastaanottanut/ehdollisesti_vastaanottanut/perunut
  post("/haku/:hakuOid/hakemus/:hakemusOid/vastaanota") {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    val vastaanotto = parsedBody.extract[Vastaanotto]

    // TODO: käsittele
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }

  error {
    case e => {
      logger.error(request.getMethod + " " + requestPath, e);
      e.printStackTrace()
      response.setStatus(500)
      "500 Internal Server Error"
    }
  }
}

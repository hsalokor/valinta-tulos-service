package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Vastaanotto
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

class ValintatulosServlet(implicit val appConfig: AppConfig) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats {
  lazy val valintatulosService: ValintatulosService = new ValintatulosService(appConfig.sijoitteluContext, new HakemusRepository())
  lazy val vastaanottoService: VastaanottoService = new VastaanottoService(appConfig.sijoitteluContext)


  get("/:hakuOid/hakemus/:hakemusOid", operation(getHakemusSwagger)) {
    contentType = formats("json")
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    valintatulosService.hakemuksentulos(hakuOid, hakemusOid) match {
      case Some(tulos) => tulos
      case _ =>
        response.setStatus(404)
        "Not found"
    }
  }

  post("/:hakuOid/hakemus/:hakemusOid/vastaanota") {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    val vastaanotto = parsedBody.extract[Vastaanotto]

    vastaanottoService.vastaanota(hakuOid, hakemusOid, vastaanotto)
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

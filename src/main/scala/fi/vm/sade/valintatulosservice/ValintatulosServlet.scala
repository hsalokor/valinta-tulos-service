package fi.vm.sade.valintatulosservice

import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

class ValintatulosServlet extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats {
  get("/") {
    "valinta-tulos-service"
  }

  get("/hakemus/:hakemusOid") {
    contentType = formats("json")
    val hakemusOid = params("hakemusOid")
    Hakemuksentulos(hakemusOid, List(Hakutoiveentulos(
    "2.3.4.5", "3.4.5.6", "HYVAKSYTTY", "ILMOITETTU", None, "EI_VASTAANOTETTAVISSA", Some(1), None
    )))
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

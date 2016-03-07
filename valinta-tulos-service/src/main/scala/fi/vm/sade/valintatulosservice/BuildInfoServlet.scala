package fi.vm.sade.valintatulosservice

import fi.vm.sade.utils.slf4j.Logging
import org.scalatra._

class BuildInfoServlet extends ScalatraServlet with Logging {

  get("/") {
    "valinta-tulos-service"
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

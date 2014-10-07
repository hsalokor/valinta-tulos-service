package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Vastaanotto
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport
import fi.vm.sade.valintatulosservice.ohjausparametrit.StubbedOhjausparametritService
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritFixtures

class TestUtilServlet (implicit val appConfig: AppConfig) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats {
  options("/fixtures/apply") {
    response.addHeader("Access-Control-Allow-Origin", "*")
    response.addHeader("Access-Control-Allow-Methods", "PUT")
    response.addHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Allow-Headers"))
  }

  put("/fixtures/apply") {
    response.addHeader("Access-Control-Allow-Origin", "*")
    val fixturename = params("fixturename")
    SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database,  fixturename + ".json", true)
    val ohjausparametrit = paramOption("ohjausparametrit").getOrElse(OhjausparametritFixtures.DEFAULT_FIXTURE)
    OhjausparametritFixtures.activeFixture = ohjausparametrit
  }

  error {
    case e => {
      logger.error(request.getMethod + " " + requestPath, e);
      response.setStatus(500)
      "500 Internal Server Error"
    }
  }

  protected def paramOption(name: String): Option[String] = {
    try {
      Option(params(name))
    } catch {
      case e: Exception => None
    }
  }

}

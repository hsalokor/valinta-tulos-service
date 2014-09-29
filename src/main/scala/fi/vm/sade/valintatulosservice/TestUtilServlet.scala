package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Vastaanotto
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

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
  }
}

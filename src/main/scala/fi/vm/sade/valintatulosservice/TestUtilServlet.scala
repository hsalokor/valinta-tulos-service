package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Vastaanotto
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

class TestUtilServlet (implicit val appConfig: AppConfig) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats {
  put("/fixtures/apply") {
    val fixturename = params("fixturename")
    SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database,  fixturename + ".json", true)
  }
}

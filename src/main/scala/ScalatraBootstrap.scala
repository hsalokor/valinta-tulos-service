import javax.servlet.ServletContext

import fi.vm.sade.valintatulosservice.ValintatulosServlet
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  implicit val config: AppConfig = AppConfig.fromSystemProperty

  override def init(context: ServletContext) {
    context.mount(new ValintatulosServlet, "/")
  }
}
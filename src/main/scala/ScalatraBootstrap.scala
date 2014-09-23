import javax.servlet.ServletContext
import fi.vm.sade.valintatulosservice.ValintatulosServlet
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.scalatra._
import fi.vm.sade.valintatulosservice.SwaggerServlet
import fi.vm.sade.valintatulosservice.ValintatulosSwagger

class ScalatraBootstrap extends LifeCycle {
  implicit val config: AppConfig = AppConfig.fromSystemProperty
  implicit val swagger = new ValintatulosSwagger

  override def init(context: ServletContext) {
    config.start
    context.mount(new ValintatulosServlet, "/")
    context.mount(new SwaggerServlet, "/swagger/*")
  }

  override def destroy(context: ServletContext) = {
    config.stop
    super.destroy(context)
  }
}
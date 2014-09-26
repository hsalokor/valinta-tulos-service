import javax.servlet.ServletContext
import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.{IT, AppConfig}
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  implicit val config: AppConfig = AppConfig.fromSystemProperty
  implicit val swagger = new ValintatulosSwagger

  override def init(context: ServletContext) {
    config.start
    context.mount(new BuildInfoServlet, "/")
    context.mount(new ValintatulosServlet, "/haku")
    context.mount(new SwaggerServlet, "/swagger/*")
    if (config.isInstanceOf[IT])
      context.mount(new TestUtilServlet, "/util")
  }

  override def destroy(context: ServletContext) = {
    config.stop
    super.destroy(context)
  }
}
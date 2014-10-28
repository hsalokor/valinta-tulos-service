import java.util
import javax.servlet.{DispatcherType, ServletContext}

import fi.vm.sade.security.CasLdapFilter
import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.{StubbedExternalDeps, AppConfig, IT_externalHakemus}
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ValintatulosSwagger

  var globalConfig: Option[AppConfig] = None

  override def init(context: ServletContext) {
    implicit val appConfig: AppConfig = AppConfig.fromOptionalString(Option(context.getAttribute("valintatulos.profile").asInstanceOf[String]))
    globalConfig = Some(appConfig)
    appConfig.start
    context.mount(new BuildInfoServlet, "/")
    context.mount(new ValintatulosServlet, "/haku")
    context.mount(new SwaggerServlet, "/swagger/*")

    val securityFilter = SecurityContext(appConfig).securityFilter

    context.addFilter("cas", securityFilter)
      .addMappingForUrlPatterns(util.EnumSet.allOf(classOf[DispatcherType]), true, "/cas/*")

    context.mount(new ValintatulosServlet, "/cas/haku")

    if (appConfig.isInstanceOf[IT_externalHakemus])
      context.mount(new TestUtilServlet, "/util")
  }

  override def destroy(context: ServletContext) = {
    globalConfig.foreach(_.stop)
    super.destroy(context)
  }
}
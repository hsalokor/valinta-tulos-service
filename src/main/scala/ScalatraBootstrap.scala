import java.util
import javax.servlet.{DispatcherType, ServletContext}
import fi.vm.sade.security.CasLdapFilter
import fi.vm.sade.security.cas.CasConfig
import fi.vm.sade.security.ldap.LdapConfig
import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.{IT_externalHakemus, IT, AppConfig}
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  implicit val appConfig: AppConfig = AppConfig.fromSystemProperty
  implicit val swagger = new ValintatulosSwagger

  override def init(context: ServletContext) {
    appConfig.start
    context.mount(new BuildInfoServlet, "/")
    context.mount(new ValintatulosServlet, "/haku")
    context.mount(new SwaggerServlet, "/swagger/*")

    context.addFilter("cas", securityFilter)
      .addMappingForUrlPatterns(util.EnumSet.allOf(classOf[DispatcherType]), true, "/cas/*")

    context.mount(new ValintatulosServlet, "/cas/haku")

    if (appConfig.isInstanceOf[IT_externalHakemus])
      context.mount(new TestUtilServlet, "/util")
  }

  override def destroy(context: ServletContext) = {
    appConfig.stop
    super.destroy(context)
  }

  private def securityFilter = {
    val securityConfig = appConfig.settings.securitySettings
    new CasLdapFilter(securityConfig.casConfig, securityConfig.ldapConfig, securityConfig.casServiceIdentifier, securityConfig.requiredLdapRoles)
  }
}
import java.util
import javax.servlet.{DispatcherType, ServletContext}

import fi.vm.sade.security.CasLdapFilter
import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.{IT, StubbedExternalDeps, AppConfig, IT_externalHakemus}
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.{MailDecorator, MailPoller}
import org.scalatra._
import org.scalatra.swagger.Swagger

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ValintatulosSwagger

  var globalConfig: Option[AppConfig] = None

  override def init(context: ServletContext) {
    implicit val appConfig: AppConfig = AppConfig.fromOptionalString(Option(context.getAttribute("valintatulos.profile").asInstanceOf[String]))
    lazy val hakuService = HakuService(appConfig)
    lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
    lazy val vastaanottoService = new VastaanottoService(hakuService, valintatulosService, appConfig.sijoitteluContext.valintatulosRepository)
    lazy val ilmoittautumisService = new IlmoittautumisService(valintatulosService, appConfig.sijoitteluContext.valintatulosRepository)
    lazy val mailPoller = new MailPoller(appConfig.settings.valintatulosMongoConfig, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 100)

    globalConfig = Some(appConfig)
    appConfig.start
    context.mount(new BuildInfoServlet, "/")

    val securityFilter = appConfig.securityContext.securityFilter
    context.addFilter("cas", securityFilter)
      .addMappingForUrlPatterns(util.EnumSet.allOf(classOf[DispatcherType]), true, "/cas/*")

    context.mount(new ValintatulosServlet(valintatulosService, vastaanottoService, ilmoittautumisService), "/cas/haku")
    context.mount(new EmailStatusServlet(mailPoller, new MailDecorator(new HakemusRepository())), "/vastaanottoposti")

    context.mount(new ValintatulosServlet(valintatulosService, vastaanottoService, ilmoittautumisService), "/haku")
    context.mount(new SwaggerServlet, "/swagger/*")

    if (appConfig.isInstanceOf[IT])
      context.mount(new TestUtilServlet, "/util")
  }

  override def destroy(context: ServletContext) = {
    globalConfig.foreach(_.stop)
    super.destroy(context)
  }
}
import java.util
import javax.servlet.{DispatcherType, ServletContext}

import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.vastaanottomeili.{ValintatulosMongoCollection, MailDecorator, MailPoller}
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ValintatulosSwagger

  var globalConfig: Option[AppConfig] = None

  override def init(context: ServletContext) {
    implicit val appConfig: AppConfig = AppConfig.fromOptionalString(Option(context.getAttribute("valintatulos.profile").asInstanceOf[String]))
    lazy val hakuService = HakuService(appConfig)
    lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
    lazy val vastaanottoService = new VastaanottoService(hakuService, valintatulosService, appConfig.sijoitteluContext.valintatulosRepository)
    lazy val ilmoittautumisService = new IlmoittautumisService(valintatulosService, appConfig.sijoitteluContext.valintatulosRepository)
    lazy val valintatulosCollection = new ValintatulosMongoCollection(appConfig.settings.valintatulosMongoConfig)
    lazy val mailPoller = new MailPoller(valintatulosCollection, valintatulosService, hakuService, appConfig.ohjausparametritService, limit = 100)

    globalConfig = Some(appConfig)
    appConfig.start
    context.mount(new BuildInfoServlet, "/")

    context.mount(new PrivateValintatulosServlet(valintatulosService, vastaanottoService, ilmoittautumisService), "/haku")
    context.mount(new EmailStatusServlet(mailPoller, valintatulosCollection, new MailDecorator(new HakemusRepository(), valintatulosCollection)), "/vastaanottoposti")
    context.mount(new VastaanottoServlet(vastaanottoService), "/vastaanotto")


    val securityFilter = appConfig.securityContext.securityFilter
    context.addFilter("cas", securityFilter)
      .addMappingForUrlPatterns(util.EnumSet.allOf(classOf[DispatcherType]), true, "/cas/*")
    context.mount(new PublicValintatulosServlet(valintatulosService, vastaanottoService, ilmoittautumisService), "/cas/haku")

    context.mount(new SwaggerServlet, "/swagger/*")

    if (appConfig.isInstanceOf[IT] || appConfig.isInstanceOf[Dev])
      context.mount(new FixtureServlet, "/util")
  }

  override def destroy(context: ServletContext) = {
    globalConfig.foreach(_.stop)
    super.destroy(context)
  }
}
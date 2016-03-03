package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.scalatra.swagger._

class PublicValintatulosServlet(valintatulosService: ValintatulosService, vastaanottoService: VastaanottoService, ilmoittautumisService: IlmoittautumisService)(override implicit val swagger: Swagger, appConfig: AppConfig) extends ValintatulosServlet(valintatulosService, vastaanottoService, ilmoittautumisService)(swagger, appConfig) {

  override val applicationName = Some("cas/haku")

  protected val applicationDescription = "Julkinen valintatulosten REST API"

}

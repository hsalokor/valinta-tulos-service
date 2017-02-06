package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.VtsAppConfig.VtsAppConfig
import org.scalatra.swagger._

class PrivateValintatulosServlet(valintatulosService: ValintatulosService, vastaanottoService: VastaanottoService, ilmoittautumisService: IlmoittautumisService)(override implicit val swagger: Swagger, appConfig: VtsAppConfig) extends ValintatulosServlet(valintatulosService, vastaanottoService, ilmoittautumisService)(swagger, appConfig) {

  override val applicationName = Some("haku")

  protected val applicationDescription = "Sisäinen valintatulosten REST API"

}

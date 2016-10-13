package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain.Sijoitteluajo
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.json4s.jackson.Serialization._
import org.scalatra.Ok
import org.scalatra.swagger._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

class SijoitteluServlet(sijoitteluService: SijoitteluService) (implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("sijoittelu")

  override protected def applicationDescription: String = "Sijoittelun REST API"

  lazy val postSijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("postSijoitteluajoSwagger")
    summary "Tallentaa sijoitteluajon"
    parameter bodyParam[Sijoitteluajo]("sijoitteluajo").description("Sijoitteluajon data"))
  post("/sijoitteluajo", operation(postSijoitteluajoSwagger)) {
    val sijoitteluajo = read[Sijoitteluajo](request.body)
    Ok(sijoitteluService.luoSijoitteluajo(sijoitteluajo))
  }

}

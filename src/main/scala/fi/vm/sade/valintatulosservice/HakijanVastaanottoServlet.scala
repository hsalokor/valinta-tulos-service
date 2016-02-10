package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{VastaanottoAction, Vastaanottotila, Vastaanotto}
import org.json4s.Extraction
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra._
import org.scalatra.swagger.Swagger

import scala.util.Try

class HakijanVastaanottoServlet(vastaanottoService: VastaanottoService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("hakija-vastaanotto")

  override protected def applicationDescription: String = "Opiskelupaikan vastaanoton REST API"

  val postVastaanottoSwagger: OperationBuilder = (apiOperation[Unit]("postVastaanotto")
    summary "Tallenna hakukohteelle uusi vastaanottotila"
    parameter pathParam[String]("henkiloOid").description("Hakijan henkilönumero")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid")
    parameter bodyParam[VastaanottoAction].description("Vastaanoton tyyppi"))
  post("/henkilo/:henkiloOid/hakukohde/:hakukohdeOid", operation(postVastaanottoSwagger)) {
    val personOid = params("henkiloOid")
    val hakukohdeOid = params("hakukohdeOid")
    val action = parsedBody.extract[VastaanottoAction]

    Try(vastaanottoService.vastaanotaHakukohde(personOid, hakukohdeOid, action)).map((_) => Ok()).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get

  }

}

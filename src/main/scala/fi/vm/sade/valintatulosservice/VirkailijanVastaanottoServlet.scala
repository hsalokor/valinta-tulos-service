package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{VastaanottoAction, Vastaanotettavuus}
import org.scalatra.swagger.Swagger
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

class VirkailijanVastaanottoServlet(vastaanotettavuusService: VastaanotettavuusService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {
  override val applicationName = Some("vastaanotto")

  override protected def applicationDescription: String = "Virkailijan vastaanotto REST API"

  private val getVastaanotettavuusSwagger: OperationBuilder = (apiOperation[Vastaanotettavuus]("getVastaanotettavuus")
    summary s"Palauttaa tietyn hakemksen hakutoiveelle mahdolliset vastaanottotoimenpiteet (0-kaikki näistä: ${VastaanottoAction.values}))"
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid"))
  get("/henkilo/:henkiloOid/hakemus/:hakemusOid/hakukohde/:hakukohdeOid/vastaanotettavuus", operation(getVastaanotettavuusSwagger)) {
    vastaanotettavuusService.vastaanotettavuus(params("henkiloOid"), params("hakemusOid"), params("hakukohdeOid"))
  }
}

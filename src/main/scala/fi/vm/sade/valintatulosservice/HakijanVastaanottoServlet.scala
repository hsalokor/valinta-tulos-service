package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import org.json4s._
import org.scalatra._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._

import scala.util.Try

class HakijanVastaanottoServlet(vastaanottoService: VastaanottoService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("vastaanotto")

  override protected def applicationDescription: String = "Opiskelupaikan vastaanoton REST API"

  private val hakijanVastaanottoActionModel = Model(
    id = classOf[HakijanVastaanottoAction].getSimpleName,
    name = classOf[HakijanVastaanottoAction].getSimpleName,
    properties = List("action" -> ModelProperty(`type` = DataType.String, required = true, allowableValues = AllowableValues(HakijanVastaanottoAction.values))))

  val postVastaanottoSwagger: OperationBuilder = (apiOperation[Unit]("postVastaanotto")
    summary "Tallenna hakukohteelle uusi vastaanottotila"
    parameter pathParam[String]("henkiloOid").description("Hakijan henkilönumero")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid")
    parameter bodyParam(hakijanVastaanottoActionModel))
  post("/henkilo/:henkiloOid/hakemus/:hakemusOid/hakukohde/:hakukohdeOid", operation(postVastaanottoSwagger)) {

    val personOid = params("henkiloOid")
    val hakemusOid = params("hakemusOid")
    val hakukohdeOid = params("hakukohdeOid")
    val action = parsedBody.extract[HakijanVastaanottoAction]

    vastaanottoService.vastaanotaHakijana(HakijanVastaanotto(personOid, hakemusOid, hakukohdeOid, action)).get
  }
}

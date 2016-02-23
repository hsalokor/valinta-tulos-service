package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import org.scalatra._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._

import scala.util.Try

class HakijanVastaanottoServlet(vastaanottoService: VastaanottoService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("vastaanotto")

  override protected def applicationDescription: String = "Opiskelupaikan vastaanoton REST API"

  registerModel(Model(id = classOf[VastaanottoAction].getSimpleName, name = classOf[VastaanottoAction].getSimpleName,
          properties = List("action" -> ModelProperty(`type` = DataType.String, position = 0, required = true,
            allowableValues = AllowableValues(VastaanottoAction.values)))))

  val postVastaanottoSwagger: OperationBuilder = (apiOperation[Unit]("postVastaanotto")
    summary "Tallenna hakukohteelle uusi vastaanottotila"
    parameter pathParam[String]("henkiloOid").description("Hakijan henkilönumero")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid")
    parameter bodyParam[VastaanottoAction])
  post("/henkilo/:henkiloOid/hakemus/:hakemusOid/hakukohde/:hakukohdeOid", operation(postVastaanottoSwagger)) {

    val personOid = params("henkiloOid")
    val hakemusOid = params("hakemusOid")
    val hakukohdeOid = params("hakukohdeOid")
    val action = parsedBody.extract[VastaanottoAction]

    Try(vastaanottoService.vastaanota(hakemusOid, Vastaanotto(hakukohdeOid, action.vastaanottotila, personOid, "Hakijan tekemä vastaanotto"))).map((_) => Ok()).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get
  }
}

package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Vastaanottotila, Hakemuksentulos, Vastaanotto, VastaanottoAction}
import org.scalatra.{Forbidden, Ok}
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._
import scala.collection.breakOut

import scala.util.Try

class VirkailijanVastaanottoServlet(valintatulosService: ValintatulosService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("vastaanotto")

  override protected def applicationDescription: String = "Virkailijan vastaanottotietojen käsittely REST API"

  registerModel(Model(id = classOf[VastaanottoAction].getSimpleName, name = classOf[VastaanottoAction].getSimpleName,
    properties = List("action" -> ModelProperty(`type` = DataType.String, position = 0, required = true,
      allowableValues = AllowableValues(VastaanottoAction.values)))))

  val getVastaanottoTilatByHakukohdeSwagger: OperationBuilder = (apiOperation[Unit]("getVastaanottoTilatByHakukohde")
    summary "Hakee vastaanoton tilat hakukohteen hakijoille"
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid"))
  get("/haku/:hakuOid/hakukohde/:hakukohdeOid", operation(getVastaanottoTilatByHakukohdeSwagger)) {

    val hakuOid = params("hakuOid")
    val hakukohdeOid = params("hakukohdeOid")

    Try(valintatulosService.hakemustenTulosByHakukohde(hakuOid, hakukohdeOid).getOrElse(List())).map(hakemuksentulos => {
      val v =
      hakemuksentulos
          .map(t => t.hakemusOid -> t.findHakutoive(hakukohdeOid).map(_.vastaanottotila).getOrElse(Vastaanottotila.kesken)) toMap

      Ok(v)
    }).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get
  }
}

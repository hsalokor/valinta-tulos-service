package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._
import org.scalatra.{Forbidden, Ok}

import scala.util.Try

class VirkailijanVastaanottoServlet(valintatulosService: ValintatulosService, vastaanottoService: VastaanottoService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("virkailija")

  override protected def applicationDescription: String = "Virkailijan vastaanottotietojen käsittely REST API"

  val getVastaanottoTilatByHakukohdeSwagger: OperationBuilder = (apiOperation[Unit]("getVastaanottoTilatByHakukohde")
    summary "Hakee vastaanoton tilat hakukohteen hakijoille"
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid"))
  get("/haku/:hakuOid/hakukohde/:hakukohdeOid", operation(getVastaanottoTilatByHakukohdeSwagger)) {

    val hakuOid = params("hakuOid")
    val hakukohdeOid = params("hakukohdeOid")

    Try(valintatulosService.hakemustenTulosByHakukohde(hakuOid, hakukohdeOid).getOrElse(List())).map(
      h => Ok(h.map(t => {
        val hakutoive = t.findHakutoive(hakukohdeOid)
        HakemuksenVastaanottotila(t.hakemusOid, hakutoive.map(_.valintatapajonoOid), hakutoive.map(_.vastaanottotila))
      }).toList)
    ).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get
  }

  val vastaanottoEventModel = Model(
    id = classOf[VastaanottoEvent].getSimpleName,
    name = classOf[VastaanottoEvent].getSimpleName,
    properties = List(
      "henkiloOid" -> ModelProperty(`type` = DataType.String, required = true),
      "hakemusOid" -> ModelProperty(`type` = DataType.String, required = true),
      "hakukohdeOid" -> ModelProperty(`type` = DataType.String, required = true),
      "action" -> ModelProperty(`type` = DataType.String, required = true, allowableValues = AllowableValues(VastaanottoAction.values))
    ))

  val postVastaanottoActionsSwagger: OperationBuilder = (apiOperation[List[VastaanottoResult]]("postVastaanotto")
    summary "Tallenna vastaanottotapahtumat"
    parameter bodyParam(vastaanottoEventModel))
  post("/", operation(postVastaanottoActionsSwagger)) {

    val vastaanottoEvents = parsedBody.extract[List[VastaanottoEvent]]
    vastaanottoService.virkailijanVastaanota(vastaanottoEvents)
  }
}

case class Result(status: Int, message: Option[String])
case class VastaanottoResult(henkiloOid: String, hakemusOid: String, hakukohdeOid: String, result: Result)

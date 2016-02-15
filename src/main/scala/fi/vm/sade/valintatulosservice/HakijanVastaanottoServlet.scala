package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.Ensikertalaisuus
import org.json4s.JsonAST.{JField, JValue, JString}
import org.json4s.jackson.compactJson
import org.json4s.{MappingException, JObject, Formats, CustomSerializer}
import org.scalatra._
import org.scalatra.swagger._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

import scala.util.Try

class HakijanVastaanottoServlet(vastaanottoService: VastaanottoService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("vastaanotto")

  override protected def applicationDescription: String = "Opiskelupaikan vastaanoton REST API"

  val postVastaanottoSwagger: OperationBuilder = (apiOperation[Unit]("postVastaanotto")
    summary "Tallenna hakukohteelle uusi vastaanottotila"
    parameter pathParam[String]("henkiloOid").description("Hakijan henkilönumero")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid")
    parameter bodyParam(Model(id = classOf[VastaanottoAction].getSimpleName, name = classOf[VastaanottoAction].getSimpleName,
        properties = List("action" -> ModelProperty(`type` = DataType.String, position = 0, required = true,
          allowableValues = AllowableValues("Peru", "VastaanotaSitovasti", "VastaanotaEhdollisesti")))
    )))
  post("/henkilo/:henkiloOid/hakukohde/:hakukohdeOid", operation(postVastaanottoSwagger)) {
    val personOid = params("henkiloOid")
    val hakukohdeOid = params("hakukohdeOid")
    val action = parsedBody.extract[VastaanottoAction]

    Try(vastaanottoService.vastaanotaHakukohde(VastaanottoEvent(personOid, hakukohdeOid, action))).map((_) => Ok()).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get

  }
}

class VastaanottoActionSerializer extends CustomSerializer[VastaanottoAction]((formats: Formats) => {
  def throwMappingException(json: String) = throw new MappingException(
    s"Can't convert $json to ${classOf[VastaanottoAction].getSimpleName}. " +
      s"Expected one of Peru, VastaanotaSitovasti, VastaanotaEhdollisesti.")
  ( {
    case json@JObject(JField("action", JString(action)) :: Nil) => action match {
      case "Peru" => Peru
      case "VastaanotaSitovasti" => VastaanotaSitovasti
      case "VastaanotaEhdollisesti" => VastaanotaEhdollisesti
      case _ => throwMappingException(compactJson(json))
    }
    case json: JValue => throwMappingException(compactJson(json))
  }, {
    case x: VastaanottoAction => JObject(JField("action", JString(x.toString)))
  })
}
)

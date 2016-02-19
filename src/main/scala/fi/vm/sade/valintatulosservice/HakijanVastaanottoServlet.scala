package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import org.json4s.JsonAST.{JField, JString, JValue}
import org.json4s.jackson.compactJson
import org.json4s.{CustomSerializer, Formats, JObject, MappingException}
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

    Try(vastaanottoService.vastaanotaHakukohde(VastaanottoEvent(personOid, hakemusOid, hakukohdeOid, action))).map((_) => Ok()).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get
  }

  private val getVastaanotettavuusSwagger: OperationBuilder = (apiOperation[Vastaanotettavuus]("getVastaanotettavuus")
    summary s"Palauttaa tietyn hakemksen hakutoiveelle mahdolliset vastaanottotoimenpiteet (0-kaikki näistä: ${VastaanottoAction.values}))"
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid"))
  get("/henkilo/:henkiloOid/hakemus/:hakemusOid/hakukohde/:hakukohdeOid/vastaanotettavuus", operation(getVastaanotettavuusSwagger)) {

    vastaanottoService.paatteleVastaanotettavuus(params("henkiloOid"), params("hakemusOid"), params("hakukohdeOid"))
  }
}

class VastaanottoActionSerializer extends CustomSerializer[VastaanottoAction]((formats: Formats) => {
  def throwMappingException(json: String, cause: Option[Exception] = None) = {
    val message = s"Can't convert $json to ${classOf[VastaanottoAction].getSimpleName}."
    cause match {
      case Some(e) => throw new MappingException(s"$message : ${e.getMessage}", e)
      case None => throw new MappingException(message)
    }
  }
  ( {
    case json@JObject(JField("action", JString(action)) :: Nil) => Try(VastaanottoAction(action)).recoverWith {
      case cause: Exception => throwMappingException(compactJson(json), Some(cause)) }.get
    case json: JValue => throwMappingException(compactJson(json))
  }, {
    case x: VastaanottoAction => JObject(JField("action", JString(x.toString)))
  })
}
)

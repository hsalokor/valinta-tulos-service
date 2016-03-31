package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.Valintatulos
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.json.JsonFormats.javaObjectToJsonString
import fi.vm.sade.valintatulosservice.valintarekisteri.VastaanottoEvent
import org.json4s.MappingException
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

  val getValintatuloksetByHakemusSwagger: OperationBuilder = (apiOperation[Unit]("getValintatuloksetByHakemus")
    summary "Hakee hakemuksen valintatulokset hakukohteeseen"
    parameter pathParam[String]("hakuOid").description("Haku oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid"))
  get("/valintatulos/haku/:hakuOid/hakemus/:hakemusOid", operation(getValintatuloksetByHakemusSwagger)) {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    Ok(javaObjectToJsonString(valintatulosService.findValintaTuloksetForVirkailijaByHakemus(hakuOid, hakemusOid)))
  }

  val getValintatuloksetByHakukohdeSwagger: OperationBuilder = (apiOperation[Unit]("getValintatuloksetByHakukohde")
    summary "Hakee valintatulokset hakukohteen hakijoille"
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid"))
  get("/valintatulos/haku/:hakuOid/hakukohde/:hakukohdeOid", operation(getValintatuloksetByHakukohdeSwagger)) {
    val hakuOid = params("hakuOid")
    val hakukohdeOid = params("hakukohdeOid")
    Ok(javaObjectToJsonString(valintatulosService.findValintaTuloksetForVirkailija(hakuOid, hakukohdeOid)))
  }

  val getValintatuloksetByHakuSwagger: OperationBuilder = (apiOperation[Unit]("getValintatuloksetByHaku")
    summary "Hakee valintatulokset haun hakijoille"
    parameter pathParam[String]("hakuOid").description("Haun oid"))
  get("/valintatulos/haku/:hakuOid", operation(getValintatuloksetByHakuSwagger)) {
    val hakuOid = params("hakuOid")
    Ok(javaObjectToJsonString(valintatulosService.findValintaTuloksetForVirkailija(hakuOid)))
  }

  val vastaanottoEventModel = Model(
    id = classOf[VastaanottoEventDto].getSimpleName,
    name = classOf[VastaanottoEventDto].getSimpleName,
    properties = List(
      "henkiloOid" -> ModelProperty(`type` = DataType.String, required = true),
      "hakemusOid" -> ModelProperty(`type` = DataType.String, required = true),
      "hakukohdeOid" -> ModelProperty(`type` = DataType.String, required = true),
      "hakuOid" -> ModelProperty(`type` = DataType.String, required = true),
      "ilmoittaja" -> ModelProperty(`type` = DataType.String, required = true),
      "tila" -> ModelProperty(`type` = DataType.String, required = true, allowableValues = AllowableValues(Vastaanottotila.values.toList)),
      "selite" -> ModelProperty(`type` = DataType.String, required = true)
    ))
  registerModel(vastaanottoEventModel)

  val postVirkailijanVastaanottoActionsSwagger: OperationBuilder = (apiOperation[List[VastaanottoResult]]("postVastaanotto")
    summary "Tallenna vastaanottotapahtumat"
    parameter bodyParam[List[VastaanottoEventDto]])
  post("/vastaanotto", operation(postVirkailijanVastaanottoActionsSwagger)) {

    val vastaanottoEvents = parsedBody.extract[List[VastaanottoEventDto]]
    vastaanottoService.vastaanotaVirkailijana(vastaanottoEvents)
  }

  val postTransactionalVirkailijanVastaanottoActionsSwagger: OperationBuilder = (apiOperation[Unit]("postVastaanotto")
    summary "Tallenna vastaanottotapahtumat transaktiossa"
    parameter bodyParam[List[VastaanottoEventDto]])
  post("/transactional-vastaanotto", operation(postTransactionalVirkailijanVastaanottoActionsSwagger)) {

    val vastaanottoEvents = parsedBody.extract[List[VastaanottoEventDto]]
    vastaanottoService.vastaanotaVirkailijanaInTransaction(vastaanottoEvents).get
  }
}

case class Result(status: Int, message: Option[String])
case class VastaanottoResult(henkiloOid: String, hakemusOid: String, hakukohdeOid: String, result: Result)
case class VastaanottoEventDto(henkiloOid: String, hakemusOid: String, hakukohdeOid: String, hakuOid: String,
                               tila: Vastaanottotila, ilmoittaja: String, selite: String)

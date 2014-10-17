package fi.vm.sade.valintatulosservice

import java.util.Date

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import org.json4s.Extraction
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._

class ValintatulosServlet(implicit val appConfig: AppConfig, val swagger: Swagger) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats with SwaggerSupport {
  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)
  lazy val vastaanottoService = new VastaanottoService(valintatulosService, appConfig.sijoitteluContext.valintatulosRepository)
  lazy val ilmoittautumisService = new IlmoittautumisService(valintatulosService, appConfig.sijoitteluContext.valintatulosRepository)

  override def applicationName = Some("haku")
  protected val applicationDescription = "Valintatulosten REST API"

  // Real return type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
  val getHakemusSwagger: OperationBuilder = (apiOperation[Unit]("getHakemus")
    summary "Hae hakemuksen tulokset."
    notes "Palauttaa tyyppiä Hakemuksentulos. Esim:\n" +
      pretty(Extraction.decompose(
        Hakemuksentulos("4.3.2.1",
          "1.3.3.1",
          Some(Vastaanottoaikataulu(Some(new Date()), Some(14))),
          List(
            Hakutoiveentulos.kesken("1.2.3.4", "4.4.4.4")
          )
        )
      ))
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka tulokset halutaan")
  )
  get("/:hakuOid/hakemus/:hakemusOid", operation(getHakemusSwagger)) {
    contentType = formats("json")
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    valintatulosService.hakemuksentulos(hakuOid, hakemusOid) match {
      case Some(tulos) => tulos
      case _ =>
        response.setStatus(404)
        "Not found"
    }
  }

  get("/:hakuOid") {
    contentType = formats("json")
    val hakuOid = params("hakuOid")
    valintatulosService.hakemustenTulos(hakuOid) match {
      case Some(tulos) => tulos
      case _ =>
        response.setStatus(404)
        "Not found"
    }
  }

  val postVastaanottoSwagger: OperationBuilder = (apiOperation[Unit]("getHakemus")
    summary "Tallenna hakukohteelle uusi vastaanottotila"
    // Real body param type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
    notes "Bodyssä tulee antaa tieto hakukohteen vastaanotttilan muutoksesta Vastaanotto tyyppinä. Esim:\n" +
      pretty(Extraction.decompose(
        Vastaanotto(
            "1.2.3.4",
            Vastaanottotila.vastaanottanut,
            "henkilö: 5.5.5.5",
            "kuvaus mitä kautta muokkaus tehty"
        )
      ))
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka vastaanottotilaa ollaan muokkaamassa")
  )
  post("/:hakuOid/hakemus/:hakemusOid/vastaanota", operation(postVastaanottoSwagger)) {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    val vastaanotto = parsedBody.extract[Vastaanotto]

    vastaanottoService.vastaanota(hakuOid, hakemusOid, vastaanotto)
  }

  val postIlmoittautuminenSwagger: OperationBuilder = (apiOperation[Unit]("ilmoittaudu")
    summary "Tallenna hakukohteelle uusi ilmoittautumistila"
    // Real body param type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
    notes "Bodyssä tulee antaa tieto hakukohteen ilmoittautumistilan muutoksesta Ilmoittautuminen tyyppinä. Esim:\n" +
    pretty(Extraction.decompose(
      Ilmoittautuminen(
        "1.2.3.4",
        Ilmoittautumistila.läsnä_koko_lukuvuosi,
        "henkilö: 5.5.5.5",
        "kuvaus mitä kautta muokkaus tehty"
      )
    ))
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka vastaanottotilaa ollaan muokkaamassa")
    )
  post("/:hakuOid/hakemus/:hakemusOid/ilmoittaudu", operation(postIlmoittautuminenSwagger)) {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    val ilmoittautuminen = parsedBody.extract[Ilmoittautuminen]

    ilmoittautumisService.ilmoittaudu(hakuOid, hakemusOid, ilmoittautuminen)
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }

  error {
    case e => {
      logger.error(request.getMethod + " " + requestPath, e);
      response.setStatus(500)
      "500 Internal Server Error"
    }
  }
}

package fi.vm.sade.valintatulosservice


import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.ohjausparametrit.Ohjausparametrit
import fi.vm.sade.valintatulosservice.tarjonta.{Hakuaika, Haku}
import org.joda.time.DateTime
import org.json4s.{MappingException, Extraction}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._

class ValintatulosServlet(valintatulosService: ValintatulosService, vastaanottoService: VastaanottoService, ilmoittautumisService: IlmoittautumisService)(implicit val swagger: Swagger, appConfig: AppConfig) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats with SwaggerSupport {

  override def applicationName = Some("haku")
  protected val applicationDescription = "Valintatulosten REST API"

  lazy val exampleHakemuksenTulos = Hakemuksentulos(
    "2.2.2.2",
    "4.3.2.1",
    "1.3.3.1",
    Some(Vastaanottoaikataulu(Some(new DateTime()), Some(14))),
    List(
      Hakutoiveentulos.julkaistavaVersio(HakutoiveenSijoitteluntulos.kesken("1.2.3.4", "4.4.4.4"), Haku("5.5.5.5", true, true, true, None, Set(), List(Hakuaika("12345", Some(System.currentTimeMillis()), Some(System.currentTimeMillis())))), Some(Ohjausparametrit(None, Some(DateTime.now().plusDays(10)), Some(DateTime.now().plusDays(30)), Some(DateTime.now().plusDays(60)))))
    )
  )

  // Real return type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
  lazy val getHakemusSwagger: OperationBuilder = (apiOperation[Unit]("getHakemus")
    summary "Hae hakemuksen tulokset."
    notes "Palauttaa tyyppiä Hakemuksentulos. Esim:\n" +
      pretty(Extraction.decompose(exampleHakemuksenTulos))
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka tulokset halutaan")
  )
  get("/:hakuOid/hakemus/:hakemusOid", operation(getHakemusSwagger)) {
    contentType = formats("json")
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    valintatulosService.hakemuksentulos(hakuOid, hakemusOid) match {
      case Some(tulos) => tulos
      case _ => NotFound("error" -> "Not found")
    }
  }

  lazy val getHakemuksetSwagger: OperationBuilder = (apiOperation[Unit]("getHakemukset")
    summary "Hae haun kaikkien hakemusten tulokset."
    notes "Palauttaa tyyppiä Seq[Hakemuksentulos]. Esim:\n" +
      pretty(Extraction.decompose(Seq(exampleHakemuksenTulos)))
    parameter pathParam[String]("hakuOid").description("Haun oid")
  )
  get("/:hakuOid", operation(getHakemuksetSwagger)) {
    contentType = formats("json")
    val hakuOid = params("hakuOid")
    valintatulosService.hakemustenTulos(hakuOid) match {
      case Some(tulos) => tulos
      case _ => NotFound("error" -> "Not found")
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
      )) + ".\nMahdolliset vastaanottotilat: " + vastaanottoService.sallitutVastaanottotilat
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka vastaanottotilaa ollaan muokkaamassa")
  )
  post("/:hakuOid/hakemus/:hakemusOid/vastaanota", operation(postVastaanottoSwagger)) {
    contentType = formats("json")
    checkJsonContentType
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
    )) + ".\nMahdolliset ilmoittautumistilat: " + Ilmoittautumistila.values

    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka vastaanottotilaa ollaan muokkaamassa")
    )
  post("/:hakuOid/hakemus/:hakemusOid/ilmoittaudu", operation(postIlmoittautuminenSwagger)) {
    contentType = formats("json")
    checkJsonContentType
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

  def checkJsonContentType = {
    request.contentType match {
      case Some(ct) if ct.startsWith("application/json") =>
      case _ => halt(415, "error" -> "Only application/json accepted")
    }
  }

  error {
    case e => {
      val desc = request.getMethod + " " + requestPath + (if (request.body.length > 0) { " (body: " + request.body + ")"} else "")
      if (e.isInstanceOf[IllegalStateException] || e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[MappingException]) {
        logger.warn(desc + ": " + e.toString);
        BadRequest("error" -> e.getMessage)
      } else {
        logger.error(desc, e);
        InternalServerError("error" -> "500 Internal Server Error")
      }
    }
  }
}

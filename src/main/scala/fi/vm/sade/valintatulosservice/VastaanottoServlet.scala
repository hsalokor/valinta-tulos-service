package fi.vm.sade.valintatulosservice

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Vastaanottotila, Vastaanotto}
import fi.vm.sade.valintatulosservice.json.JsonFormats
import org.json4s.{Extraction, MappingException}
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.{SwaggerSupport, Swagger}

import scala.util.Try

class VastaanottoServlet(vastaanottoService: VastaanottoService)(implicit val swagger: Swagger, appConfig: AppConfig) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats with SwaggerSupport {

  override val applicationName = Some("vastaanotto")

  override protected def applicationDescription: String = "Opiskelupaikan vastaanoton REST API"

  val postVastaanottoSwagger: OperationBuilder = (apiOperation[Unit]("getHakemus")
    summary "Tallenna hakukohteelle uusi vastaanottotila"
    // Real body param type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
    notes "Bodyssä tulee antaa tieto hakukohteen vastaanottotilan muutoksesta Vastaanotto tyyppinä. Esim:\n" +
    pretty(Extraction.decompose(
      Vastaanotto(
        "1.2.3.4",
        Vastaanottotila.vastaanottanut,
        "henkilö:5.5.5.5",
        "kuvaus mitä kautta muokkaus tehty"
      )
    )) + ".\nMahdolliset vastaanottotilat: " + vastaanottoService.sallitutVastaanottotilat
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka vastaanottotilaa ollaan muokkaamassa")
    )

  post("/:henkilo", operation(postVastaanottoSwagger)) {
    contentType = formats("json")
    checkJsonContentType
    val vastaanotto = parsedBody.extract[Vastaanotto]

    val personOid:String = params("henkilo")

    Try(vastaanottoService.vastaanotaHakukohde(personOid, vastaanotto)).map((_) => Ok()).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get

  }

  def checkJsonContentType {
    request.contentType match {
      case Some(ct) if ct.startsWith("application/json") =>
      case _ => halt(415, "error" -> "Only application/json accepted")
    }
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }

  error {
    case e => {
      val desc = request.getMethod + " " + requestPath + (if (request.body.length > 0) {" (body: " + request.body + ")"} else {
        ""
      })
      if (e.isInstanceOf[IllegalStateException] || e.isInstanceOf[IllegalArgumentException] || e.isInstanceOf[MappingException]) {
        logger.warn(desc + ": " + e.toString)
        BadRequest("error" -> e.getMessage)
      } else {
        logger.error(desc, e)
        InternalServerError("error" -> "500 Internal Server Error")
      }
    }
  }

}

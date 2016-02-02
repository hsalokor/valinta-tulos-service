package fi.vm.sade.valintatulosservice

import java.text.ParseException

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.json.JsonFormats
import org.json4s.MappingException
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.{Post, InternalServerError, BadRequest, ScalatraServlet}

trait VtsServletBase extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats with SwaggerSupport {
  private val maxBodyLengthToLog = 500000

  before() {
    contentType = formats("json")
    checkJsonContentType()
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    serveStaticResource() getOrElse resourceNotFound()
  }

  error {
    case t: Throwable => {
      t match {
        case e: IllegalStateException =>
          badRequest(e)
        case e: IllegalArgumentException =>
          badRequest(e)
        case e: MappingException =>
          badRequest(e)
        case e: ParseException =>
          badRequest(e)
        case e: NoSuchElementException =>
          badRequest(e)
        case e =>
          logger.error(errorDescription, e)
          InternalServerError("error" -> "500 Internal Server Error")
      }
    }
  }

  private def errorDescription: String = {
    val bodyLength = request.body.length
    def bodyToLog(): String = {
      if (bodyLength > maxBodyLengthToLog) {
        request.body.substring(0, maxBodyLengthToLog) + s"[TRUNCATED from $bodyLength to $maxBodyLengthToLog characters]"
      } else {
        request.body
      }
    }

    "%s %s%s".format(
      request.getMethod,
      requestPath,
      if (bodyLength > 0) {
        s" (body: ${bodyToLog()})"
      } else {
        ""
      }
    )
  }

  private def badRequest(e: Throwable) = {
    logger.warn(errorDescription + ": " + e.toString)
    BadRequest("error" -> e.getMessage)
  }

  private def checkJsonContentType() {
    if (request.requestMethod == Post && request.contentType.forall(!_.contains("application/json"))) {
      halt(415, "error" -> "Only application/json accepted")
    }
  }

}

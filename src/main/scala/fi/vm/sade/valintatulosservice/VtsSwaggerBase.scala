package fi.vm.sade.valintatulosservice

import org.scalatra.swagger.{SwaggerSupport, ResponseMessage}

trait VtsSwaggerBase { this: SwaggerSupport =>
  case class ErrorResponse(error: String)

  registerModel[ErrorResponse]()

  case class ModelResponseMessage(code: Int, message: String, responseModel: String = "ErrorResponse") extends ResponseMessage[String]
}

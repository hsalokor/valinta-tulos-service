package fi.vm.sade.valintatulosservice

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{JacksonSwaggerBase, Swagger}

class SwaggerServlet(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase

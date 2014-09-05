package fi.vm.sade.valintatulosservice

import org.scalatra._

class ValintatulosServlet extends ScalatraServlet {
  get("/") {
    "valinta-tulos-service"
  }
}

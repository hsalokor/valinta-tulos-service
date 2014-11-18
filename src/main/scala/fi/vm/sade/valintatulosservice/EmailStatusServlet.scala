package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.vastaanottomeili.MailPoller
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

class EmailStatusServlet(mailPoller: MailPoller) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats {
  get("/") {
    contentType = formats("json")
    mailPoller.pollForMailables
  }
}

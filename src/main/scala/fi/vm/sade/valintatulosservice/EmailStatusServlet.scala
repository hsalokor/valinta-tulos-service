package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.vastaanottomeili.{MailDecorator, HakemusMailStatus, MailPoller}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

class EmailStatusServlet(mailPoller: MailPoller, mailDecorator: MailDecorator) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats {
  get("/") {
    contentType = formats("json")
    val mailStatii: List[HakemusMailStatus] = mailPoller.pollForMailables
    mailStatii.flatMap(mailDecorator.statusToMail(_))
  }

  post("/") {
    val kuitatut = parsedBody.extract[List[HakemusMailStatus]]
    kuitatut.foreach(mailPoller.markAsHandled(_))
  }
}

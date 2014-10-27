package fi.vm.sade.security

import fi.vm.sade.security.cas._
import fi.vm.sade.valintatulosservice.config.AppConfig.LocalTestingWithTemplatedVars
import org.specs2.mutable.Specification

class CasClientIntegrationTester extends Specification {
  lazy val appConfig = new LocalTestingWithTemplatedVars("../deploy/vars/environments/oph_vars.yml")
  lazy val security = appConfig.settings.securitySettings
  lazy val client = new CasClient(security.casConfig)

  "get service ticket from CAS" in {
    val ticket = client.getServiceTicket(security.ticketRequest)
    ticket.isDefined must_== true
  }

  "validate ticket" in {
    "invalid ticket" in {
      val ticket = CasTicket("lol", "asdf")
      val response = client.validateServiceTicket(ticket).asInstanceOf[CasResponseFailure]
      response.errorMessage must_== "Service not allowed to validate tickets."
    }
    "valid ticket" in {
      val ticket = client.getServiceTicket(security.ticketRequest).get
      val response = client.validateServiceTicket(CasTicket(security.casServiceIdentifier, ticket)).asInstanceOf[CasResponseSuccess]
      response.username must_== "reaktor"
    }
  }
}

package fi.vm.sade.security

import fi.vm.sade.security.ldap.{DirectoryClient, LdapUser}
import fi.vm.sade.utils.cas._

class MockSecurityContext(val casServiceIdentifier: String, val requiredLdapRoles: List[String], users: Map[String, LdapUser]) extends SecurityContext {
  val directoryClient = new DirectoryClient {
    override def findUser(userid: String) = users.get(userid)
    override def authenticate(userid: String, password: String) = ???
  }

  val ticketClient = new TicketClient {
    override def validateServiceTicket(ticket: CasTicket) = {
      if (ticket.ticket.startsWith(ticketPrefix(ticket.service))) {
        val username = ticket.ticket.stripPrefix(ticketPrefix(ticket.service))
        CasResponseSuccess(username)
      } else {
        CasResponseFailure("unrecognized ticket: " + ticket.ticket)
      }
    }

    override def getServiceTicket(service: CasTicketRequest): Option[String] = Some(ticketFor(service.service, service.username))

    def ticketFor(service: String, username: String) = ticketPrefix(service) + username

    private def ticketPrefix(service: String) = "mock-ticket-" + service + "-"
  }
}


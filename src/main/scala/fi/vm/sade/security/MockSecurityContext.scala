package fi.vm.sade.security.mock

import fi.vm.sade.security.SecurityContext
import fi.vm.sade.security.ldap.{DirectoryClient, LdapUser, MockDirectoryClient}
import fi.vm.sade.utils.cas.CasClient._
import fi.vm.sade.utils.cas._
import org.http4s.{ParseFailure, ParseException}

import scalaz.concurrent.Task

class MockSecurityContext(val casServiceIdentifier: String, val requiredLdapRoles: List[String], users: Map[String, LdapUser]) extends SecurityContext {
  val directoryClient = new MockDirectoryClient(users)

  val casClient = new CasClient("",null) {
    override def validateServiceTicket(service : scala.Predef.String)(ticket : fi.vm.sade.utils.cas.CasClient.ST): Task[Username] = {
      if (ticket.startsWith(ticketPrefix(service))) {
        val username = ticket.stripPrefix(ticketPrefix(service))
        Task.now(username)
      } else {
        Task.fail(new RuntimeException("unrecognized ticket: " + ticket))
      }
    }

    override def getServiceTicket(service : org.http4s.Uri)(tgtUrl : fi.vm.sade.utils.cas.CasClient.TGTUrl): Task[ST] = Task.now(service.toString())

    def ticketFor(service: String, username: String) = ticketPrefix(service) + username

    private def ticketPrefix(service: String) = "mock-ticket-" + service + "-"
  }
}


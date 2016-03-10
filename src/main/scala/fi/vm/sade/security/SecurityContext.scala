package fi.vm.sade.security

import fi.vm.sade.security.ldap.DirectoryClient
import fi.vm.sade.utils.cas.TicketClient
import org.scalatra.ScalatraFilter
import fi.vm.sade.security.ldap.{LdapClient, LdapConfig}
import fi.vm.sade.utils.cas.{CasClient, CasConfig}

trait SecurityContext {
   def casServiceIdentifier: String
   def requiredLdapRoles: List[String]
   def directoryClient: DirectoryClient
   def ticketClient: TicketClient

   def securityFilter: ScalatraFilter = new CasLdapFilter(ticketClient, directoryClient, casServiceIdentifier, requiredLdapRoles)
}

class ProductionSecurityContext(ldapConfig: LdapConfig, casConfig: CasConfig, val casServiceIdentifier: String, val requiredLdapRoles: List[String]) extends SecurityContext {
  lazy val directoryClient = new LdapClient(ldapConfig)
  lazy val ticketClient = new CasClient(casConfig)
}
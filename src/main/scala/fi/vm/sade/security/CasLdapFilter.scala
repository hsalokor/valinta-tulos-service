package fi.vm.sade.security

import fi.vm.sade.security.cas._
import fi.vm.sade.security.ldap.{LdapClient, LdapConfig}
import org.scalatra.ScalatraFilter

class CasLdapFilter(casConfig: CasConfig, ldapConfig: LdapConfig, casServiceIdentifier: String, requiredRoles: List[String]) extends ScalatraFilter {
  val casClient = new CasClient(casConfig)
  val ldapClient = new LdapClient(ldapConfig)

  before() {
    params.get("ticket") match {
      case Some(ticket) =>
        casClient.validateServiceTicket(CasTicket(casServiceIdentifier, ticket)) match {
          case CasResponseSuccess(uid) =>
            ldapClient.findUser(uid) match {
              case Some(user) if (requiredRoles.forall(user.hasRole(_))) =>
                // Pass!
              case _ => halt(status = 401, body = "LDAP access denied")
            }
          case CasResponseFailure(error) =>
            halt(status = 401, body = "CAS ticket rejected")
        }
      case _ => halt(status = 401, body = "CAS ticket required")
    }
  }
}

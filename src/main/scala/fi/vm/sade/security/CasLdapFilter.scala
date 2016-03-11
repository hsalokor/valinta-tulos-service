package fi.vm.sade.security

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.utils.cas._
import fi.vm.sade.security.ldap.{DirectoryClient, LdapClient, LdapConfig}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Unauthorized, ScalatraFilter}
import org.scalatra.json._

import scala.util.{Failure, Success, Try}

/**
 * Filter that verifies CAS service ticket and checks user permissions from LDAP.
 *
 * @param casClient             CAS client
 * @param ldapClient            LDAP client
 * @param casServiceIdentifier  The "service" parameter used when verifying the CAS ticket
 * @param requiredRoles         Required roles. Roles are stored in LDAP user's "description" field.
 */
class CasLdapFilter(casClient: CasClient, ldapClient: DirectoryClient, casServiceIdentifier: String, requiredRoles: List[String]) extends ScalatraFilter with JacksonJsonSupport with LazyLogging {

  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    params.get("ticket").orElse(request.header("ticket")) match {
      case Some(ticket) =>
        Try(casClient.validateServiceTicket(casServiceIdentifier)(ticket).run) match {
          case Success(uid) =>
            ldapClient.findUser(uid) match {
              case Some(user) if (requiredRoles.forall(user.hasRole(_))) =>
              // Pass!
              case _ => halt(Unauthorized("error" -> "LDAP access denied"))
            }
          case Failure(t) =>
            logger.warn("Cas ticket rejected", t)
            halt(Unauthorized("error" -> "CAS ticket rejected"))
        }
      case _ =>
        halt(Unauthorized("error" -> "CAS ticket required"))
    }
  }

}
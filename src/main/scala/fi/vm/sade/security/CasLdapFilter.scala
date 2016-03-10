package fi.vm.sade.security

import fi.vm.sade.security.ldap.{DirectoryClient, LdapClient, LdapConfig}
import fi.vm.sade.utils.cas._
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._
import org.scalatra.{ScalatraFilter, Unauthorized}

/**
  * Filter that verifies CAS service ticket and checks user permissions from LDAP.
  *
  * @param casClient             CAS client
  * @param ldapClient            LDAP client
  * @param casServiceIdentifier  The "service" parameter used when verifying the CAS ticket
  * @param requiredRoles         Required roles. Roles are stored in LDAP user's "description" field.
  */
class CasLdapFilter(casClient: TicketClient, ldapClient: DirectoryClient, casServiceIdentifier: String, requiredRoles: List[String]) extends ScalatraFilter with JacksonJsonSupport with Logging {
   /**
    * @param casConfig
    * @param ldapConfig
    * @param casServiceIdentifier
    * @param requiredRoles
    */
   def this(casConfig: CasConfig, ldapConfig: LdapConfig, casServiceIdentifier: String, requiredRoles: List[String]) {
     this(new CasClient(casConfig), new LdapClient(ldapConfig), casServiceIdentifier, requiredRoles)
   }

   protected implicit val jsonFormats: Formats = DefaultFormats

   before() {
     contentType = formats("json")
     params.get("ticket").orElse(request.header("ticket")) match {
       case Some(ticket) =>
         casClient.validateServiceTicket(CasTicket(casServiceIdentifier, ticket)) match {
           case CasResponseSuccess(uid) =>
             ldapClient.findUser(uid) match {
               case Some(user) if (requiredRoles.forall(user.hasRole(_))) =>
                 // Pass!
               case _ => halt(Unauthorized("error" -> "LDAP access denied"))
             }
           case CasResponseFailure(error) =>
             logger.warn("Cas ticket rejected: " + error)
             halt(Unauthorized("error" -> "CAS ticket rejected"))
         }
       case _ =>
         halt(Unauthorized("error" -> "CAS ticket required"))
     }
   }

 }
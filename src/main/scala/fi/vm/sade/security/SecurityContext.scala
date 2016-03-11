package fi.vm.sade.security

import fi.vm.sade.utils.cas.CasClient
import fi.vm.sade.security.ldap.{LdapConfig, LdapClient, DirectoryClient}
import org.scalatra.ScalatraFilter

trait SecurityContext {
  def casServiceIdentifier: String
  def requiredLdapRoles: List[String]
  def directoryClient: DirectoryClient
  def casClient: CasClient

  def securityFilter: ScalatraFilter = new CasLdapFilter(casClient, directoryClient, casServiceIdentifier, requiredLdapRoles)
}

class ProductionSecurityContext(ldapConfig: LdapConfig, cc: CasClient, val casServiceIdentifier: String, val requiredLdapRoles: List[String]) extends SecurityContext {
  lazy val directoryClient = new LdapClient(ldapConfig)
  lazy val casClient = cc
}
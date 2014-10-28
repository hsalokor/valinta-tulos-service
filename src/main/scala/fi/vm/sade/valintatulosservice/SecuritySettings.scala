package fi.vm.sade.valintatulosservice

import com.typesafe.config.Config
import fi.vm.sade.security.cas.{CasConfig, CasTicketRequest}
import fi.vm.sade.security.ldap.LdapConfig

class SecuritySettings(c: Config) {
  val casConfig = CasConfig(c.getString("cas.url"))
  val casServiceIdentifier = c.getString("valinta-tulos-service.cas.service")

  private val casUsername = c.getString("valinta-tulos-service.cas.username")
  private val casPassword = c.getString("valinta-tulos-service.cas.password")

  val ldapConfig = LdapConfig(
    c.getString("ldap.server.host"),
    c.getString("ldap.user.dn"),
    c.getString("ldap.user.password"))

  val requiredLdapRoles = List("APP_VALINTATULOSSERVICE_CRUD")

  val ticketRequest: CasTicketRequest = CasTicketRequest(casServiceIdentifier, casUsername, casPassword)
}

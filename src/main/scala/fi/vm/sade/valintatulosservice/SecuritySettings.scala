package fi.vm.sade.valintatulosservice

import com.typesafe.config.Config
import fi.vm.sade.security.cas.{CasConfig, CasTicketRequest}
import fi.vm.sade.security.ldap.{LdapConfig, LdapUser}
import fi.vm.sade.security.mock.MockSecurityContext
import fi.vm.sade.security.{ProductionSecurityContext, SecurityContext}
import fi.vm.sade.valintatulosservice.config.AppConfig.{AppConfig, StubbedExternalDeps}

object SecurityContext {
  def apply(appConfig: AppConfig): SecurityContext = {
    val settings: SecuritySettings = appConfig.settings.securitySettings
    if (appConfig.isInstanceOf[StubbedExternalDeps])
      new MockSecurityContext(settings.casServiceIdentifier, settings.requiredLdapRoles, Map((settings.ticketRequest.username -> LdapUser(settings.requiredLdapRoles))))
    else
      new ProductionSecurityContext(settings.ldapConfig, settings.casConfig, settings.casServiceIdentifier, settings.requiredLdapRoles)
  }
}

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

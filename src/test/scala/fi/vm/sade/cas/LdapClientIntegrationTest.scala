package fi.vm.sade.cas

import fi.vm.sade.ldap.{LdapConfig, LdapClient}
import fi.vm.sade.valintatulosservice.config.AppConfig.LocalTestingWithTemplatedVars
import org.specs2.mutable.Specification

class LdapClientIntegrationTest extends Specification {
  val appConfig = new LocalTestingWithTemplatedVars("../deploy/vars/environments/oph_vars.yml")

  val ldapConfig = LdapConfig(
    appConfig.settings.config.getString("ldap.server.host"),
    appConfig.settings.config.getString("ldap.user.dn"),
    appConfig.settings.config.getString("ldap.user.password"))

  "Get user's roles from LDAP" in {
    val result = new LdapClient(ldapConfig).findUser("reaktor")
    result.get.hasRole("APP_VALINTAREKISTERI") must_== true
    result.get.hasRole("lolwat") must_== false
  }
}

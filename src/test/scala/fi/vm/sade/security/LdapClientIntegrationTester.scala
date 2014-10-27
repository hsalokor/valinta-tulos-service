package fi.vm.sade.security

import fi.vm.sade.security.ldap.{LdapClient, LdapConfig}
import fi.vm.sade.valintatulosservice.SecuritySettings
import fi.vm.sade.valintatulosservice.config.AppConfig.LocalTestingWithTemplatedVars
import org.specs2.mutable.Specification

class LdapClientIntegrationTester extends Specification {
  val appConfig = new LocalTestingWithTemplatedVars("../deploy/vars/environments/oph_vars.yml")

  "Get user's roles from LDAP" in {
    val result = new LdapClient(appConfig.settings.securitySettings.ldapConfig).findUser("reaktor")
    result.get.hasRole("APP_VALINTAREKISTERI") must_== true
    result.get.hasRole("lolwat") must_== false
  }
}

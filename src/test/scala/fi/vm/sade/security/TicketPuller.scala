package fi.vm.sade.security

import fi.vm.sade.security.cas.CasClient
import fi.vm.sade.valintatulosservice.SecuritySettings
import fi.vm.sade.valintatulosservice.config.AppConfig.LocalTestingWithTemplatedVars

object TicketPuller extends App {
  val appConfig = new LocalTestingWithTemplatedVars("../deploy/vars/environments/oph_vars.yml")
  val security: SecuritySettings = appConfig.settings.securitySettings
  println(new CasClient(security.casConfig).getServiceTicket(security.ticketRequest))
}

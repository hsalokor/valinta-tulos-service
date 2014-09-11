package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Hakemuksentulos
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluClient

trait ValintatulosService {
  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos]
}

object ValintatulosService {
  def apply(implicit appConfig: AppConfig) = SijoitteluClient()
}
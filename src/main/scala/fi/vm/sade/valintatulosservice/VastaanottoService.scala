package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig

class VastaanottoService(implicit appConfig: AppConfig) {
  def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: String, muokkaaja: String, selite: String) {
    appConfig.springContext.vastaanottoService.vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.valueOf(tila), muokkaaja, selite)
  }
}

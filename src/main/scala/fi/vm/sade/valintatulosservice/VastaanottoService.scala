package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluSpringContext

class VastaanottoService(sijoitteluContext: SijoitteluSpringContext) {
  def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: Vastaanottotila, muokkaaja: String, selite: String) {
    sijoitteluContext.vastaanottoService.vastaanota(hakuOid, hakemusOid, hakukohdeOid, ValintatuloksenTila.valueOf(tila.toString), muokkaaja, selite)
  }
}

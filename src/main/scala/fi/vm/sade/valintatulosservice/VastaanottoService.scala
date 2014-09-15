package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila
import fi.vm.sade.valintatulosservice.domain.Vastaanotto
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluSpringContext

class VastaanottoService(sijoitteluContext: SijoitteluSpringContext) {
  def vastaanota(hakuOid: String, hakemusOid: String, vastaanotto: Vastaanotto) {
    sijoitteluContext.vastaanottoService.vastaanota(hakuOid, hakemusOid, vastaanotto.hakukohdeOid, ValintatuloksenTila.valueOf(vastaanotto.tila.toString), vastaanotto.muokkaaja, vastaanotto.selite)
  }
}

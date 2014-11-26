package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.Haku

class SijoittelutulosService(yhteenvetoService: YhteenvetoService) {
  def hakemuksenTulos(haku: Haku, hakemusOid: String): Option[HakemuksenSijoitteluntulos] = {
    yhteenvetoService.hakemuksenYhteenveto(haku, hakemusOid)
  }

  def hakemustenTulos(haku: Haku): List[HakemuksenSijoitteluntulos] = {
    (for (
      tulokset <- yhteenvetoService.hakemustenYhteenveto(haku)
    ) yield for (
        yhteenveto <- tulokset
      ) yield yhteenveto).getOrElse(List())
  }
}

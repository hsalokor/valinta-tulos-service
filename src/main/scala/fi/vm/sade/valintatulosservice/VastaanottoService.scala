package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.{EHDOLLISESTI_VASTAANOTTANUT, PERUNUT, VASTAANOTTANUT}
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.valintatulosservice.domain.{Hakutoiveentulos, Vastaanotettavuustila, Vastaanotto}
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository

class VastaanottoService(valintatulosService: ValintatulosService, tulokset: ValintatulosRepository) {
  def vastaanota(hakuOid: String, hakemusOid: String, vastaanotto: Vastaanotto) {
    val hakutoive = valintatulosService.hakutoive(hakuOid, hakemusOid, vastaanotto.hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakemusta tai hakutoivetta ei löydy"))
    val tila: ValintatuloksenTila = ValintatuloksenTila.valueOf(vastaanotto.tila.toString)
    tarkistaVastaanotettavuus(hakutoive, tila)
    tulokset.modifyValintatulos(vastaanotto.hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid, tila.name, vastaanotto.muokkaaja, vastaanotto.selite) { valintatulos =>
      valintatulos.setTila(tila)
    }
  }

  private def tarkistaVastaanotettavuus(hakutoive: Hakutoiveentulos, tila: ValintatuloksenTila) {
    if (!List(VASTAANOTTANUT, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT).contains(tila)) {
      throw new IllegalArgumentException("Ei-hyväksytty vastaanottotila: " + tila)
    }
    if (List(VASTAANOTTANUT, PERUNUT).contains(tila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila: " + hakutoive.vastaanotettavuustila.toString + " (tavoitetila " + tila + ")")
    }
    if (tila == EHDOLLISESTI_VASTAANOTTANUT && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
      throw new IllegalArgumentException(tila.toString())
    }
  }
}
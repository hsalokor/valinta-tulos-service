package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ohjausparametrit.Ohjausparametrit
import fi.vm.sade.valintatulosservice.tarjonta.Haku

object ValintatulosKonvertteri {
  def julkaistavaVersioSijoittelunTuloksesta(tulos: HakutoiveenSijoitteluntulos, hakutoive: Hakutoive, haku: Haku, ohjausparametrit: Option[Ohjausparametrit])(implicit appConfig: AppConfig): Hakutoiveentulos = {
    Hakutoiveentulos(
      tulos.hakukohdeOid,
      hakutoive.nimi,
      tulos.tarjoajaOid,
      hakutoive.tarjoajaNimi,
      tulos.valintatapajonoOid,
      tulos.valintatila,
      tulos.vastaanottotila,
      IlmoittautumistilaKonvertteri.getIlmoittautumistila(tulos, haku, ohjausparametrit),
      tulos.vastaanotettavuustila,
      tulos.vastaanottoDeadline,
      tulos.viimeisinHakemuksenTilanMuutos,
      tulos.viimeisinValintatuloksenMuutos,
      tulos.jonosija,
      tulos.varasijojaKaytetaanAlkaen,
      tulos.varasijojaTaytetaanAsti,
      tulos.varasijanumero,
      tulos.julkaistavissa,
      tulos.tilanKuvaukset,
      tulos.pisteet
    ).julkaistavaVersio
  }
}

package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.ohjausparametrit.Ohjausparametrit
import fi.vm.sade.valintatulosservice.tarjonta.Haku

case class Hakemuksentulos(hakuOid: String, hakemusOid: String, hakijaOid: String, aikataulu: Option[Vastaanottoaikataulu], hakutoiveet: List[Hakutoiveentulos]) {
  def findHakutoive(hakukohdeOid: String): Option[Hakutoiveentulos] = hakutoiveet.find(_.hakukohdeOid == hakukohdeOid)
}

case class Hakutoiveentulos(hakukohdeOid: String,
                            tarjoajaOid: String,
                            valintatapajonoOid: String,
                            valintatila: Valintatila,
                            vastaanottotila: Vastaanottotila,
                            ilmoittautumistila: HakutoiveenIlmoittautumistila,
                            vastaanotettavuustila: Vastaanotettavuustila,
                            vastaanottoDeadline: Option[Date],
                            viimeisinHakemuksenTilanMuutos: Option[Date],
                            viimeisinValintatuloksenMuutos: Option[Date],
                            jonosija: Option[Int],
                            varasijojaKaytetaanAlkaen: Option[Date],
                            varasijojaTaytetaanAsti: Option[Date],
                            varasijanumero: Option[Int],
                            julkaistavissa: Boolean,
                            tilanKuvaukset: Map[String, String],
                            pisteet: Option[BigDecimal]
                            )

object Hakutoiveentulos {
  def julkaistavaVersio(tulos: HakutoiveenSijoitteluntulos, haku: Haku, ohjausparametrit: Option[Ohjausparametrit])(implicit appConfig: AppConfig) = {
    if(tulos.julkaistavissa)
      Hakutoiveentulos(
        tulos.hakukohdeOid,
        tulos.tarjoajaOid,
        tulos.valintatapajonoOid,
        tulos.valintatila,
        tulos.vastaanottotila,
        HakutoiveenIlmoittautumistila.getIlmoittautumistila(tulos, haku, ohjausparametrit),
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
      )
    else
      Hakutoiveentulos(
        tulos.hakukohdeOid,
        tulos.tarjoajaOid,
        tulos.valintatapajonoOid,
        Valintatila.kesken,
        tulos.vastaanottotila,
        HakutoiveenIlmoittautumistila.getIlmoittautumistila(tulos, haku, ohjausparametrit),
        Vastaanotettavuustila.ei_vastaanotettavissa,
        None,
        None,
        None,
        None,
        tulos.varasijojaKaytetaanAlkaen,
        tulos.varasijojaTaytetaanAsti,
        None,
        false,
        Map(),
        tulos.pisteet
      )
  }
}
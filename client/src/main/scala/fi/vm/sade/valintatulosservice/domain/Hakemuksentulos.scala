package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

case class Hakemuksentulos(hakuOid: String, hakemusOid: String, hakijaOid: String, aikataulu: Option[Vastaanottoaikataulu], hakutoiveet: List[Hakutoiveentulos]) {
  def findHakutoive(hakukohdeOid: String): Option[Hakutoiveentulos] = hakutoiveet.find(_.hakukohdeOid == hakukohdeOid)
}

case class Hakutoiveentulos(hakukohdeOid: String,
                            hakukohdeNimi: String,
                            tarjoajaOid: String,
                            tarjoajaNimi: String,
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
                            ) {

  def toKesken = {
    copy(
        valintatila = Valintatila.kesken,
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa,
        vastaanottoDeadline = None,
        viimeisinValintatuloksenMuutos = None,
        jonosija = None,
        varasijanumero = None,
        julkaistavissa = false,
        tilanKuvaukset = Map(),
        pisteet = None
    )
  }

  def julkaistavaVersio = {
    if (julkaistavissa) {
      this
    } else {
      toKesken
    }
  }
}
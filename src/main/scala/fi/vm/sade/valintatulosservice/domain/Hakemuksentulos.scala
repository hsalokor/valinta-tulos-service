package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

case class Hakemuksentulos(hakemusOid: String, hakijaOid: String, aikataulu: Option[Vastaanottoaikataulu], hakutoiveet: List[Hakutoiveentulos])

case class Hakutoiveentulos(hakukohdeOid: String,
                            tarjoajaOid: String,
                            valintatapajonoOid: String,
                            valintatila: Valintatila,
                            vastaanottotila: Vastaanottotila,
                            ilmoittautumistila: Ilmoittautumistila,
                            vastaanotettavuustila: Vastaanotettavuustila,
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
  def julkaistavaVersio(tulos: HakutoiveenSijoitteluntulos) = {
    if(tulos.julkaistavissa)
      Hakutoiveentulos(
        tulos.hakukohdeOid,
        tulos.tarjoajaOid,
        tulos.valintatapajonoOid,
        tulos.valintatila,
        tulos.vastaanottotila,
        tulos.ilmoittautumistila,
        tulos.vastaanotettavuustila,
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
        tulos.ilmoittautumistila,
        Vastaanotettavuustila.ei_vastaanotettavissa,
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
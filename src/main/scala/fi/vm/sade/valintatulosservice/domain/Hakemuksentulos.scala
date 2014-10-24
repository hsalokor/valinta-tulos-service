package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

case class Hakemuksentulos(hakemusOid: String, hakijaOid: String, aikataulu: Option[Vastaanottoaikataulu], hakutoiveet: List[Hakutoiveentulos]) {
  def julkaistavaVersio = Hakemuksentulos(hakemusOid, hakijaOid, aikataulu, hakutoiveet.toList.map(_.julkaistavaVersio))
}

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
                            ) {
  def julkaistavaVersio = {
    if (julkaistavissa) {
      this
    } else {
      Hakutoiveentulos(hakukohdeOid,
        tarjoajaOid,
        valintatapajonoOid,
        Valintatila.kesken,
        vastaanottotila,
        ilmoittautumistila,
        Vastaanotettavuustila.ei_vastaanotettavissa,
        None,
        None,
        varasijojaKaytetaanAlkaen,
        varasijojaTaytetaanAsti,
        None,
        false,
        Map(),
        pisteet
      )
    }
  }
}

object Hakutoiveentulos {
  def kesken(hakukohdeOid: String, tarjoajaOid: String) = {
    Hakutoiveentulos(
      hakukohdeOid,
      tarjoajaOid,
      "",
      Valintatila.kesken,
      Vastaanottotila.kesken,
      Ilmoittautumistila.ei_tehty,
      Vastaanotettavuustila.ei_vastaanotettavissa,
      None,
      None,
      None,
      None,
      None,
      true,
      Map(),
      None
    )
  }
}
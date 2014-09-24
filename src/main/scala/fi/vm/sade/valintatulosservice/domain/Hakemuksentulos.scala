package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

case class Hakemuksentulos(hakemusOid: String, hakutoiveet: List[Hakutoiveentulos]) {
  def julkaistavaVersio = Hakemuksentulos(hakemusOid, hakutoiveet.toList.map(_.julkaistavaVersio))
}

case class Hakutoiveentulos(hakukohdeOid: String,
                            tarjoajaOid: String,
                            valintatila: Valintatila,
                            vastaanottotila: Vastaanottotila,
                            ilmoittautumistila: Ilmoittautumistila,
                            vastaanotettavuustila: Vastaanotettavuustila,
                            viimeisinVastaanottotilanMuutos: Option[Date],
                            jonosija: Option[Int],
                            varasijojaKaytetaanAlkaen: Option[Date],
                            varasijojaTaytetaanAsti: Option[Date],
                            varasijanumero: Option[Int],
                            julkaistavissa: Boolean
                            ) {
  def julkaistavaVersio = {
    if (julkaistavissa) {
      this
    } else {
      Hakutoiveentulos(hakukohdeOid,
        tarjoajaOid,
        Valintatila.kesken,
        vastaanottotila,
        ilmoittautumistila,
        Vastaanotettavuustila.ei_vastaanotettavissa,
        None,
        None,
        varasijojaKaytetaanAlkaen,
        varasijojaTaytetaanAsti,
        None,
        false
      )
    }
  }
}

object Hakutoiveentulos {
  def kesken(hakukohdeOid: String, tarjoajaOid: String) = {
    Hakutoiveentulos(
      hakukohdeOid,
      tarjoajaOid,
      Valintatila.kesken,
      Vastaanottotila.kesken,
      Ilmoittautumistila.ei_tehty,
      Vastaanotettavuustila.ei_vastaanotettavissa,
      None,
      None,
      None,
      None,
      None,
      true)
  }
}
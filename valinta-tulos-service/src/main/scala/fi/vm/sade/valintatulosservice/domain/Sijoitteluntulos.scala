package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila._
import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila._
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila._

case class HakemuksenSijoitteluntulos (
  hakemusOid: String,
  hakijaOid: Option[String],
  hakutoiveet: List[HakutoiveenSijoitteluntulos]
)
case class HakutoiveenSijoitteluntulos(
  hakukohdeOid: String,
  tarjoajaOid: String,
  valintatapajonoOid: String,
  valintatila: Valintatila,
  vastaanottotila: Vastaanottotila,
  vastaanottoDeadline: Option[Date],
  ilmoittautumistila: Ilmoittautumistila,
  vastaanotettavuustila: Vastaanotettavuustila,
  viimeisinHakemuksenTilanMuutos: Option[Date],
  viimeisinValintatuloksenMuutos: Option[Date],
  jonosija: Option[Int],
  varasijojaKaytetaanAlkaen: Option[Date],
  varasijojaTaytetaanAsti: Option[Date],
  varasijanumero: Option[Int],
  julkaistavissa: Boolean,
  ehdollisestiHyvaksyttavissa: Boolean,
  tilanKuvaukset: Map[String, String],
  pisteet: Option[BigDecimal]
)

object HakutoiveenSijoitteluntulos {
  def kesken(hakukohdeOid: String, tarjoajaOid: String) = {
    HakutoiveenSijoitteluntulos(
      hakukohdeOid,
      tarjoajaOid,
      valintatapajonoOid = "",
      Valintatila.kesken,
      Vastaanottotila.kesken,
      vastaanottoDeadline = None,
      Ilmoittautumistila.ei_tehty,
      Vastaanotettavuustila.ei_vastaanotettavissa,
      viimeisinHakemuksenTilanMuutos = None,
      viimeisinValintatuloksenMuutos = None,
      jonosija = None,
      varasijojaKaytetaanAlkaen = None,
      varasijojaTaytetaanAsti = None,
      varasijanumero = None,
      julkaistavissa = true,
      ehdollisestiHyvaksyttavissa = false,
      tilanKuvaukset = Map(),
      pisteet = None
    )
  }
}

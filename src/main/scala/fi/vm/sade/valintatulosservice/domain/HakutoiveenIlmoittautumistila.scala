package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.LanguageMap.LanguageMap
import fi.vm.sade.valintatulosservice.tarjonta.Haku

case class HakutoiveenIlmoittautumistila(
  ilmoittautumisaika: Ilmoittautumisaika,
  ilmoittautumistapa: Option[Ilmoittautumistapa],
  ilmoittautumistila: Ilmoittautumistila,
  ilmoittauduttavissa: Boolean
)

case class Ilmoittautumisaika(alku: Option[Date], loppu: Option[Date], aktiivinen: Boolean)

sealed trait Ilmoittautumistapa {}

case class UlkoinenJärjestelmä(nimi: LanguageMap, url: String) extends Ilmoittautumistapa

object HakutoiveenIlmoittautumistila {

  val oili = UlkoinenJärjestelmä(Map(Language.fi -> "Oili", Language.sv -> "Oili", Language.en -> "Oili"), "/oili/")
  val ilmottautumisaika = Ilmoittautumisaika(None, None, true)

  def getIlmoittautumistila(sijoitteluTila: HakutoiveenSijoitteluntulos, haku: Haku): HakutoiveenIlmoittautumistila = {
    val ilmoittautumistapa = if(haku.korkeakoulu && haku.yhteishaku) {
      Some(oili)
    }
    else {
      None
    }
    val ilmottauduttavissa = sijoitteluTila.vastaanottotila == Vastaanottotila.vastaanottanut && ilmottautumisaika.aktiivinen && sijoitteluTila.ilmoittautumistila == Ilmoittautumistila.ei_tehty
    HakutoiveenIlmoittautumistila(ilmottautumisaika, ilmoittautumistapa, sijoitteluTila.ilmoittautumistila, ilmottauduttavissa)
  }
}
package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.LanguageMap.LanguageMap
import fi.vm.sade.valintatulosservice.tarjonta.Haku

case class HakutoiveenIlmoittautumistila(
  ilmoittautumisaika: Ilmoittautumisaika,
  ilmoittautumistapa: Option[Ilmoittautumistapa],
  ilmoittautumistila: Ilmoittautumistila
)

case class Ilmoittautumisaika(alku: Option[Date], loppu: Option[Date], aktiivinen: Boolean)

sealed trait Ilmoittautumistapa {}

case class UlkoinenJärjestelmä(nimi: LanguageMap, url: String) extends Ilmoittautumistapa

object HakutoiveenIlmoittautumistila {
  val oili = UlkoinenJärjestelmä(Map(Language.fi -> "Oili", Language.sv -> "Oili", Language.en -> "Oili"), "/oili/")
  def getIlmoittautumistila(sijoitteluTila: HakutoiveenSijoitteluntulos, haku: Haku): HakutoiveenIlmoittautumistila = {
    if(haku.korkeakoulu) {
      HakutoiveenIlmoittautumistila(Ilmoittautumisaika(None, None, true), Some(oili), sijoitteluTila.ilmoittautumistila)
    }
    else {
      HakutoiveenIlmoittautumistila(Ilmoittautumisaika(None, None, true), None, sijoitteluTila.ilmoittautumistila)
    }
  }
}
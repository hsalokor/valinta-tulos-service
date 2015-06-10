package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.LanguageMap.LanguageMap
import org.joda.time.DateTime

case class HakutoiveenIlmoittautumistila(
  ilmoittautumisaika: Ilmoittautumisaika,
  ilmoittautumistapa: Option[Ilmoittautumistapa],
  ilmoittautumistila: Ilmoittautumistila,
  ilmoittauduttavissa: Boolean
)

case class Ilmoittautumisaika(alku: Option[DateTime], loppu: Option[DateTime]) {
  def aktiivinen = {
    val now = new DateTime
    now.isAfter(alku.getOrElse(now.minusYears(100))) &&
    now.isBefore(loppu.getOrElse(now.plusYears(100)))
  }
}

sealed trait Ilmoittautumistapa {}

case class UlkoinenJärjestelmä(nimi: LanguageMap, url: String) extends Ilmoittautumistapa

object Oili extends UlkoinenJärjestelmä(Map(Language.fi -> "Oili", Language.sv -> "Oili", Language.en -> "Oili"), "/oili/")
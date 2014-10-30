package fi.vm.sade.valintatulosservice.domain

import java.util.Date

case class HakutoiveenIlmoittautumistila(
  ilmoittautumisAika: IlmoittautumisAika,
  ilmoittautumisTapa: Ilmoittautumistapa,
  ilmoittauduttavissa: Boolean
)

case class IlmoittautumisAika(alku: Date, loppu: Date, aktiivinen: Boolean)

sealed trait Ilmoittautumistapa {}

case class UlkoinenJärjestelmä(nimi: String, url: String)
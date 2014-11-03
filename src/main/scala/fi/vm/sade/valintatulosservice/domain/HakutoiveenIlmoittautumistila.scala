package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Language.Language

case class HakutoiveenIlmoittautumistila(
  ilmoittautumisAika: IlmoittautumisAika,
  ilmoittautumisTapa: Option[Ilmoittautumistapa]
)

case class IlmoittautumisAika(alku: Option[Date], loppu: Option[Date], aktiivinen: Boolean)

sealed trait Ilmoittautumistapa {}

case class UlkoinenJärjestelmä(nimi: Map[Language,String], url: String) extends Ilmoittautumistapa
package fi.vm.sade.valintatulosservice.ensikertalaisuus

import java.util.Date

sealed trait Ensikertalaisuus {
  val personOid: String
}

object Ensikertalaisuus {
  def apply(personOid: String, paattyi: Option[Date]): Ensikertalaisuus = paattyi match {
    case Some(paattyiJo) => EiEnsikertalainen(personOid, paattyiJo)
    case None => Ensikertalainen(personOid)
  }
}

case class Ensikertalainen(personOid: String) extends Ensikertalaisuus

case class EiEnsikertalainen(personOid: String, paattyi: Date) extends Ensikertalaisuus

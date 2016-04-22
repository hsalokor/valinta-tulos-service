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

sealed trait Vastaanottotieto {
  val personOid: String
  val kkTutkintoonJohtava: Boolean
  val vastaanottoaika: Date
}

case class VastaanottoHistoria (
  val uudet: List[UusiVastaanottotieto],
  val vanhat: List[VanhaVastaanottotieto]
)

case class UusiVastaanottotieto (
   personOid: String,
   hakuOid: String,
   hakukohdeOid: String,
   kkTutkintoonJohtava: Boolean,
   yhdenPaikanSaantoVoimassa: Boolean,
   vastaanottotila: String,
   vastaanottoaika: Date) extends Vastaanottotieto

case class VanhaVastaanottotieto(
    personOid: String,
    hakukohde: String,
    kkTutkintoonJohtava: Boolean,
    vastaanottoaika: Date) extends Vastaanottotieto

package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.valintarekisteri.VastaanottoEvent

@Deprecated //Used by old Vastaanotto API
case class Vastaanotto(hakukohdeOid: String, tila: Vastaanottotila, muokkaaja: String, selite: String)

case class HakijanVastaanotto(henkiloOid: String, hakemusOid: String, hakukohdeOid: String, action: HakijanVastaanottoAction) extends VastaanottoEvent {
  val ilmoittaja = henkiloOid
}

case class VirkailijanVastaanotto(henkiloOid: String, hakemusOid: String, hakukohdeOid: String, action: VirkailijanVastaanottoAction, ilmoittaja: String) extends VastaanottoEvent

sealed trait VastaanottoAction {
  def vastaanottotila: Vastaanottotila
}

sealed trait HakijanVastaanottoAction extends VastaanottoAction {
  def vastaanottotila: Vastaanottotila
}

sealed trait VirkailijanVastaanottoAction extends VastaanottoAction {
  def vastaanottotila: Vastaanottotila
}

case object Peru extends VirkailijanVastaanottoAction with HakijanVastaanottoAction {
  val vastaanottotila = Vastaanottotila.perunut
}
case object VastaanotaSitovasti extends VirkailijanVastaanottoAction with HakijanVastaanottoAction {
  val vastaanottotila = Vastaanottotila.vastaanottanut
}
case object VastaanotaEhdollisesti extends VirkailijanVastaanottoAction with HakijanVastaanottoAction {
  val vastaanottotila = Vastaanottotila.ehdollisesti_vastaanottanut
}
case object Peruuta extends VirkailijanVastaanottoAction {
  val vastaanottotila = Vastaanottotila.peruutettu
}
case object Poista extends VirkailijanVastaanottoAction {
  val vastaanottotila = Vastaanottotila.kesken
}

object HakijanVastaanottoAction {
  private val valueMapping = Map(
    "Peru" -> Peru,
    "VastaanotaSitovasti" -> VastaanotaSitovasti,
    "VastaanotaEhdollisesti" -> VastaanotaEhdollisesti)
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): HakijanVastaanottoAction = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown action '$value', expected one of $values")
  })

  def getHakijanVastaanottoAction(vastaanottotila: Vastaanottotila): HakijanVastaanottoAction = vastaanottotila match {
    case Vastaanottotila.ehdollisesti_vastaanottanut => VastaanotaEhdollisesti
    case Vastaanottotila.vastaanottanut => VastaanotaSitovasti
    case Vastaanottotila.perunut => Peru
    case x  => throw new IllegalArgumentException(s"Tila ${x} ei ole sallittu")
  }
}

object VirkailijanVastaanottoAction {
  private val valueMapping = Map(
    "Peru" -> Peru,
    "VastaanotaSitovasti" -> VastaanotaSitovasti,
    "VastaanotaEhdollisesti" -> VastaanotaEhdollisesti,
    "Peruuta" -> Peruuta,
    "Poista" -> Poista)
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): VirkailijanVastaanottoAction = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown action '$value', expected one of $values")
  })

  def getVirkailijanVastaanottoAction(vastaanottotila: Vastaanottotila): VirkailijanVastaanottoAction = vastaanottotila match {
    case Vastaanottotila.ehdollisesti_vastaanottanut => VastaanotaEhdollisesti
    case Vastaanottotila.vastaanottanut => VastaanotaSitovasti
    case Vastaanottotila.peruutettu => Peruuta
    case Vastaanottotila.perunut => Peru
    case Vastaanottotila.kesken => Poista
    case x  => throw new IllegalArgumentException(s"Tila ${x} ei ole sallittu")
  }
}
package fi.vm.sade.valintatulosservice.domain

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

case class VastaanottoRecord(henkiloOid: String, hakuOid: String, hakukohdeOid: String, action: VastaanottoAction, ilmoittaja: String, timestamp: Date)
case class VastaanottoEvent(henkiloOid: String, hakemusOid: String, hakukohdeOid: String, action: VastaanottoAction, ilmoittaja: String)

sealed trait VastaanottoAction {
  def vastaanottotila: Vastaanottotila
}

case object Peru extends VastaanottoAction {
  val vastaanottotila = Vastaanottotila.perunut
}
case object VastaanotaSitovasti extends VastaanottoAction {
  val vastaanottotila = Vastaanottotila.vastaanottanut
}
case object VastaanotaEhdollisesti extends VastaanottoAction {
  val vastaanottotila = Vastaanottotila.ehdollisesti_vastaanottanut
}

object VastaanottoAction {
  private val valueMapping = Map(
    "Peru" -> Peru,
    "VastaanotaSitovasti" -> VastaanotaSitovasti,
    "VastaanotaEhdollisesti" -> VastaanotaEhdollisesti)
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): VastaanottoAction = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown action '$value', expected one of $values")
  })
}

case class Vastaanotettavuus(allowedActions: List[VastaanottoAction], reason: Option[Exception] = None)

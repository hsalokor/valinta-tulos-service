package fi.vm.sade.valintatulosservice.domain

import java.util.Date

case class VastaanottoRecord(henkiloOid: String, hakuOid: String, hakukohdeOid: String, action: VastaanottoAction, ilmoittaja: String, timestamp: Date)
case class VastaanottoEvent(henkiloOid: String, hakukohdeOid: String, action: VastaanottoAction)

sealed trait VastaanottoAction

case object Peru extends VastaanottoAction
case object VastaanotaSitovasti extends VastaanottoAction
case object VastaanotaEhdollisesti extends VastaanottoAction

object VastaanottoAction {
  private val valueMapping = Map(
    "Peru" -> Peru,
    "VastaanotaSitovasti" -> VastaanotaSitovasti,
    "VastaanotaEhdollisesti" -> VastaanotaEhdollisesti)
  val values: Seq[String] = valueMapping.keysIterator.toList
  def apply(value: String): VastaanottoAction = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown action '$value', expected one of $values")
  })
}

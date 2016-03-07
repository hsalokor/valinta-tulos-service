package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.valintatulosservice.domain._

case class VastaanottoRecord(henkiloOid: String, hakuOid: String, hakukohdeOid: String, action: VastaanottoAction, ilmoittaja: String, timestamp: Date)

trait VastaanottoEvent {
  def henkiloOid: String
  def hakemusOid: String
  def hakukohdeOid: String
  def action: VastaanottoAction
  def ilmoittaja: String
  def selite: String
}
object VastaanottoEvent {
  def unapply(vastaanottoEvent: VastaanottoEvent): Option[(String, String, String, VastaanottoAction, String, String)] = {
    Some((vastaanottoEvent.henkiloOid, vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid,
      vastaanottoEvent.action, vastaanottoEvent.ilmoittaja, vastaanottoEvent.selite))
  }
}

object VastaanottoAction {
  private val valueMapping = Map(
    "Peru" -> Peru,
    "VastaanotaSitovasti" -> VastaanotaSitovasti,
    "VastaanotaEhdollisesti" -> VastaanotaEhdollisesti,
    "Peruuta" -> Peruuta,
    "Poista" -> Poista)
  val values: List[String] = valueMapping.keysIterator.toList
  def apply(value: String): VastaanottoAction = valueMapping.getOrElse(value, {
    throw new IllegalArgumentException(s"Unknown action '$value', expected one of $values")
  })
}

case class Vastaanotettavuus(allowedActions: List[VastaanottoAction], reason: Option[Exception] = None)

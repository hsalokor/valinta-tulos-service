package fi.vm.sade.valintatulosservice.domain

import java.util.Date

case class VastaanottoRecord(henkiloOid: String, hakuOid: String, hakukohdeOid: String, ilmoittaja: String, timestamp: Date)
case class VastaanottoEvent(henkiloOid: String, hakukohdeOid: String, action: VastaanottoAction)

sealed trait VastaanottoAction

case object Peru extends VastaanottoAction
case object VastaanotaSitovasti extends VastaanottoAction
case object VastaanotaEhdollisesti extends VastaanottoAction

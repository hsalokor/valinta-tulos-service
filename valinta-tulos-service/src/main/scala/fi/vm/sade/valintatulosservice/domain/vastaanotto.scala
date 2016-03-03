package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.VastaanottoEventDto
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.{ehdollisesti_vastaanottanut, Vastaanottotila}
import fi.vm.sade.valintatulosservice.valintarekisteri.VastaanottoEvent

@Deprecated //Used by old Vastaanotto API
case class Vastaanotto(hakukohdeOid: String, tila: Vastaanottotila, muokkaaja: String, selite: String)

case class HakijanVastaanotto(henkiloOid: String, hakemusOid: String, hakukohdeOid: String, action: HakijanVastaanottoAction) extends VastaanottoEvent {
  val ilmoittaja = henkiloOid
  val selite = "Hakijan oma vastaanotto"
}

case class VirkailijanVastaanotto(henkiloOid: String, hakemusOid: String, hakukohdeOid: String,
                                  action: VirkailijanVastaanottoAction, ilmoittaja: String, selite: String) extends VastaanottoEvent

object VirkailijanVastaanotto {
  def apply(dto: VastaanottoEventDto): VirkailijanVastaanotto = {
    VirkailijanVastaanotto(dto.henkiloOid, dto.hakemusOid, dto.hakukohdeOid,
      VirkailijanVastaanottoAction.getVirkailijanVastaanottoAction(dto.tila), dto.ilmoittaja, dto.selite)
  }
}

sealed trait VastaanottoAction

sealed trait HakijanVastaanottoAction extends VastaanottoAction

sealed trait VirkailijanVastaanottoAction extends VastaanottoAction

case object Peru extends VirkailijanVastaanottoAction with HakijanVastaanottoAction
case object VastaanotaSitovasti extends VirkailijanVastaanottoAction with HakijanVastaanottoAction
case object VastaanotaEhdollisesti extends VirkailijanVastaanottoAction with HakijanVastaanottoAction
case object Peruuta extends VirkailijanVastaanottoAction
case object Poista extends VirkailijanVastaanottoAction

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

  def getVirkailijanVastaanottoAction(vastaanottotila: Vastaanottotila): VirkailijanVastaanottoAction = vastaanottotila.toString match {
    case x if x == Vastaanottotila.ehdollisesti_vastaanottanut.toString => VastaanotaEhdollisesti
    case x if x == Vastaanottotila.vastaanottanut.toString => VastaanotaSitovasti
    case x if x == Vastaanottotila.peruutettu.toString => Peruuta
    case x if x == Vastaanottotila.perunut.toString => Peru
    case x if x == Vastaanottotila.kesken.toString => Poista
    case x  => throw new IllegalArgumentException(s"Tila ${x} ei ole sallittu")
  }
}

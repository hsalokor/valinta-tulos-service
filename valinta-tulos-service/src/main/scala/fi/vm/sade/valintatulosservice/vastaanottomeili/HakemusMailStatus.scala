package fi.vm.sade.valintatulosservice.vastaanottomeili

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Valintatila._
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.Vastaanottotila
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.Vastaanottotila._

case class HakemusMailStatus(hakijaOid: String, hakemusOid: String, hakukohteet: List[HakukohdeMailStatus], hakuOid: String) {
  def anyMailToBeSent = hakukohteet.find(_.shouldMail).nonEmpty
}

case class HakukohdeMailStatus(hakukohdeOid: String, valintatapajonoOid: String,
                               status: MailStatus.Value,
                               reasonToMail: Option[MailReason.Value],
                               deadline: Option[Date], message: String,
                               valintatila: Valintatila,
                               vastaanottotila: Vastaanottotila,
                               ehdollisestiHyvaksyttavissa: Boolean) {
  def shouldMail = status == MailStatus.SHOULD_MAIL
}

case class HakemusIdentifier(hakuOid: String, hakemusOid: String, lastSent: Option[Date])

object MailStatus extends Enumeration {
  val NOT_MAILED, MAILED, SHOULD_MAIL, NEVER_MAIL = Value
}

object MailReason extends Enumeration {
  val VASTAANOTTOILMOITUS,
  EHDOLLISEN_PERIYTYMISEN_ILMOITUS,
  SITOVAN_VASTAANOTON_ILMOITUS = Value
}
package fi.vm.sade.valintatulosservice.vastaanottomeili

import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.domain.{Hakemus, Henkilotiedot}
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository

class MailDecorator(hakemusRepository: HakemusRepository) extends Logging {
  def statusToMail(status: HakemusMailStatus): Option[VastaanotettavuusIlmoitus] = {
    status.anyMailToBeSent match {
      case true => {
        hakemusRepository.findHakemus(status.hakemusOid) match {
          case Some(Hakemus(_, henkiloOid, asiointikieli, _, Henkilotiedot(Some(kutsumanimi), Some(email)))) =>
            val mailables = status.hakukohteet.filter(_.shouldMail)
            val deadline = mailables.flatMap(_.deadline).min
            Some(VastaanotettavuusIlmoitus(
              status.hakemusOid, henkiloOid, asiointikieli, kutsumanimi, email, deadline, mailables.map(_.hakukohdeOid)
            ))
          case Some(hakemus) =>
            logger.debug("Hakemukselta puuttuu kutsumanimi tai email: " + status.hakemusOid)
            None
          case _ =>
            logger.error("Hakemusta ei löydy: " + status.hakemusOid)
            None
        }
      }
      case _ => None
    }
  }
}

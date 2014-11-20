package fi.vm.sade.valintatulosservice.vastaanottomeili

import java.util.Date

import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.domain.{Henkilotiedot, Hakemus}
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import org.joda.time.DateTime

class MailDecorator(hakemusRepository: HakemusRepository) extends Logging {
  def statusToMail(status: HakemusMailStatus): Option[VastaanotettavuusIlmoitus] = {
    status.anyMailToBeSent match {
      case true => {
        hakemusRepository.findHakemus(status.hakemusOid) match {
          case Some(Hakemus(_, henkiloOid, _, Henkilotiedot(Some(kutsumanimi), Some(email)))) =>
            val deadline = new DateTime().plusMonths(1).toDate
            Some(VastaanotettavuusIlmoitus(
              status.hakemusOid, henkiloOid, kutsumanimi, email, deadline, status.hakukohteet.filter(_.shouldMail).map(_.hakukohdeOid)
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

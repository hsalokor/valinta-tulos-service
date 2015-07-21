package fi.vm.sade.valintatulosservice.vastaanottomeili

import java.util.Date
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.{Hakemus, Henkilotiedot}
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository

class MailDecorator(hakemusRepository: HakemusRepository, valintatulosCollection: ValintatulosMongoCollection) extends Logging {
  def statusToMail(status: HakemusMailStatus): Option[VastaanotettavuusIlmoitus] = {
    status.anyMailToBeSent match {
      case true => {
        hakemusRepository.findHakemus(status.hakemusOid) match {
          case Some(Hakemus(_, _, henkiloOid, asiointikieli, _, Henkilotiedot(Some(kutsumanimi), Some(email), true))) =>
            val mailables = status.hakukohteet.filter(_.shouldMail)
            val deadline: Option[Date] = mailables.flatMap(_.deadline).sorted.headOption
            Some(VastaanotettavuusIlmoitus(
              status.hakemusOid, henkiloOid, asiointikieli, kutsumanimi, email, deadline, mailables.map(_.hakukohdeOid)
            ))
          case Some(hakemus) =>
            logger.warn("Hakemukselta puuttuu kutsumanimi tai email: " + status.hakemusOid)
            status.hakukohteet.filter(_.shouldMail).foreach {
              valintatulosCollection.addMessage(status, _,  "Hakemukselta puuttuu kutsumanimi tai email")
            }
            None
          case _ =>
            logger.error("Hakemusta ei löydy: " + status.hakemusOid)
            status.hakukohteet.filter(_.shouldMail).foreach {
              valintatulosCollection.addMessage(status, _,  "Hakemusta ei löydy")
            }
            None
        }
      }
      case _ => None
    }
  }
}

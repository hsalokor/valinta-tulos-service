package fi.vm.sade.valintatulosservice.vastaanottomeili

import java.util.Date

case class HakemusMailStatus(hakemusOid: String, hakukohteet: List[HakukohdeMailStatus]) {
  def anyMailToBeSent = hakukohteet.find(_.shouldMail).nonEmpty
}

case class HakukohdeMailStatus(hakukohdeOid: String, valintatapajonoOid: String, shouldMail: Boolean, deadline: Option[Date])

case class HakemusIdentifier(hakuOid: String, hakemusOid: String)
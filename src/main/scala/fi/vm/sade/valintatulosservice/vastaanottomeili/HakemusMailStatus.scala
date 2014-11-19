package fi.vm.sade.valintatulosservice.vastaanottomeili

case class HakemusMailStatus(hakemusOid: String, hakukohteet: List[HakukohdeMailStatus]) {
  def anyMailToBeSent = hakukohteet.find(_.shouldMail).nonEmpty
}

case class HakukohdeMailStatus(hakukohdeOid: String, valintatapajonoOid: String, shouldMail: Boolean)

case class HakemusIdentifier(hakuOid: String, hakemusOid: String)
package fi.vm.sade.valintatulosservice.vastaanottomeili

import java.util.Date

case class VastaanotettavuusIlmoitus(
  hakemusOid: String,
  hakijaOid: String,
  asiointikieli: String,
  etunimi: String,
  email: String,
  deadline: Option[Date],
  hakukohteet: List[String]
)

case class LahetysKuittaus(
  hakemusOid: String,
  hakukohteet: List[String],
  mediat: List[String]
)
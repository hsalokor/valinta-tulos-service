package fi.vm.sade.valintatulosservice.vastaanottomeili

import java.util.Date

case class VastaanotettavuusIlmoitus(
  hakemusOid: String,
  hakijaOid: String,
  etunimi: String,
  email: String,
  deadline: Date
)
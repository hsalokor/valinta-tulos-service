package fi.vm.sade.valintatulosservice

case class Hakemuksentulos(hakemusOid: String, hakutoiveet: List[Hakutoiveentulos])

case class Hakutoiveentulos(hakukohdeOid: String,
                            tarjoajaOid: String,
                            valintatila: String,
                            vastaanottotila: Option[String],
                            ilmoittautumistila: Option[String],
                            vastaanotettavuustila: String,
                            jonosija: Option[Int],
                            varasijanumero: Option[Int]
                            )

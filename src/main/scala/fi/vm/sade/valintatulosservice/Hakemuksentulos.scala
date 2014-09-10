package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

case class Hakemuksentulos(hakemusOid: String, hakutoiveet: List[Hakutoiveentulos])

case class Hakutoiveentulos(hakukohdeOid: String,
                            tarjoajaOid: String,
                            valintatila: String,
                            vastaanottotila: Vastaanottotila,
                            ilmoittautumistila: Ilmoittautumistila,
                            vastaanotettavuustila: String,
                            jonosija: Option[Int],
                            varasijanumero: Option[Int]
                            )

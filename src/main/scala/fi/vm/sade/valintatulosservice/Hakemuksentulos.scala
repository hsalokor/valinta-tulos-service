package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila

case class Hakemuksentulos(hakemusOid: String, hakutoiveet: List[Hakutoiveentulos])

case class Hakutoiveentulos(hakukohdeOid: String,
                            tarjoajaOid: String,
                            valintatila: Valintatila,
                            vastaanottotila: Vastaanottotila,
                            ilmoittautumistila: Ilmoittautumistila,
                            vastaanotettavuustila: Vastaanotettavuustila,
                            jonosija: Option[Int],
                            varasijanumero: Option[Int]
                            )

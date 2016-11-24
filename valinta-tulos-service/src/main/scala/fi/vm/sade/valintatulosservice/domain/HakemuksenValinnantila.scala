package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.valintarekisteri.domain.Vastaanottotila
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.Vastaanottotila._

case class HakemuksenVastaanottotila(val hakemusOid: String, valintatapajonoOid: Option[String], vastaanottotila: Option[Vastaanottotila]) {

}

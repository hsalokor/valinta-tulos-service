package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila._

case class HakemuksenValinnantila(val hakemusOid: String, valintatapajonoOid: Option[String], vastaanottotila: Option[Vastaanottotila]) {

}

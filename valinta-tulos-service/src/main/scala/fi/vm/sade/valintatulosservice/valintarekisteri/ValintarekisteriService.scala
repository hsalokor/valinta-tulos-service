package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.Kausi
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{VastaanottoHistoria, Ensikertalaisuus}

trait ValintarekisteriService {
  def findEnsikertalaisuus(personOid: String, koulutuksenAlkamisKausi: Kausi): Ensikertalaisuus
  def findVastaanottoHistory(personOid: String): VastaanottoHistoria
  def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamisKausi: Kausi): Set[Ensikertalaisuus]
}

package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.Kausi
import fi.vm.sade.valintatulosservice.ensikertalaisuus.Ensikertalaisuus

trait ValintarekisteriService {
  def findEnsikertalaisuus(personOid: String, koulutuksenAlkamisKausi: Kausi): Ensikertalaisuus
  def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamisKausi: Kausi): Set[Ensikertalaisuus]
}

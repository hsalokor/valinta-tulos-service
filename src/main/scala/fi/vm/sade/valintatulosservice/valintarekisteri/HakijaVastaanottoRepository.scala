package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.VastaanottoEvent

trait HakijaVastaanottoRepository {
  def store(vastaanottoEvent: VastaanottoEvent): Unit
}

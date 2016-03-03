package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.Kausi

trait HakijaVastaanottoRepository {
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHenkilonVastaanottoHakukohteeseen(henkiloOid: String, hakukohdeOid: String): Option[VastaanottoRecord]
  def findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid: String, koulutuksenAlkamiskausi: Kausi): Option[VastaanottoRecord]
  def store(vastaanottoEvent: VastaanottoEvent): Unit
  def store(vastaanottoEvents: List[VastaanottoEvent]): Unit
}

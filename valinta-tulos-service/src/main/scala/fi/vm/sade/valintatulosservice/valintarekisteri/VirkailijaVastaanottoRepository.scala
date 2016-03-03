package fi.vm.sade.valintatulosservice.valintarekisteri

trait VirkailijaVastaanottoRepository {
  def findHakukohteenVastaanotot(hakukohdeOid: String): Set[VastaanottoRecord]
}

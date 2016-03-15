package fi.vm.sade.valintatulosservice.valintarekisteri

trait VirkailijaVastaanottoRepository extends VastaanottoRepository {
  def findHakukohteenVastaanotot(hakukohdeOid: String): Set[VastaanottoRecord]
  def findHaunVastaanotot(hakuOid: String): Set[VastaanottoRecord]
}

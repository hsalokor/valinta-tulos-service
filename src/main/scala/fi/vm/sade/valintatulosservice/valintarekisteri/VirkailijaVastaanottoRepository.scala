package fi.vm.sade.valintatulosservice.valintarekisteri

trait VirkailijaVastaanottoRepository extends VastaanottoRepository {
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHakukohteenVastaanotot(hakukohdeOid: String): Set[VastaanottoRecord]
  def findHaunVastaanotot(hakuOid: String): Set[VastaanottoRecord]
}

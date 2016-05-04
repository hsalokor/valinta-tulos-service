package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.Kausi

trait VirkailijaVastaanottoRepository extends VastaanottoRepository {
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHakukohteenVastaanotot(hakukohdeOid: String): Set[VastaanottoRecord]
  def findHaunVastaanotot(hakuOid: String): Set[VastaanottoRecord]
  def findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(kausi: Kausi): Set[VastaanottoRecord]
}

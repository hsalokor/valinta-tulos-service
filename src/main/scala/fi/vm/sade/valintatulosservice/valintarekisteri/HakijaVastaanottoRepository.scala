package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.{VastaanottoRecord, Kausi, VastaanottoEvent}

trait HakijaVastaanottoRepository {
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHenkilonVastaanottoHakukohteeseen(henkiloOid: String, hakukohdeOid: String): Option[VastaanottoRecord]
  def findKkTutkintoonJohtavatVastaanotot(henkiloOid: String, koulutuksenAlkamiskausi: Kausi): Set[VastaanottoRecord]
  def store(vastaanottoEvent: VastaanottoEvent): Unit
}

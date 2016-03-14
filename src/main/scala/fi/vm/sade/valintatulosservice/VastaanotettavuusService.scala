package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService, VastaanottoRecord}

import scala.util.{Failure, Success, Try}

class VastaanotettavuusService(hakukohdeRecordService: HakukohdeRecordService,
                               hakijaVastaanottoRepository: HakijaVastaanottoRepository) {
  def tarkistaAiemmatVastaanotot(henkiloOid: String, hakukohdeOid: String): Try[Unit] = {
    val hakukohdeRecord = hakukohdeRecordService.getHakukohdeRecord(hakukohdeOid)
    haeAiemmatVastaanotot(hakukohdeRecord, henkiloOid).map(aiempiVastaanotto => {
      Failure(PriorAcceptanceException(aiempiVastaanotto))
    }).getOrElse(Success(()))
  }

  private def haeAiemmatVastaanotot(hakukohdeRecord: HakukohdeRecord, hakijaOid: String): Option[VastaanottoRecord] = {
    val HakukohdeRecord(hakukohdeOid, _, yhdenPaikanSaantoVoimassa, _, koulutuksenAlkamiskausi) = hakukohdeRecord
    val checkAction = if (yhdenPaikanSaantoVoimassa) {
      hakijaVastaanottoRepository.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(hakijaOid, koulutuksenAlkamiskausi)
    } else {
      hakijaVastaanottoRepository.findHenkilonVastaanottoHakukohteeseen(hakijaOid, hakukohdeOid)
    }
    hakijaVastaanottoRepository.runBlocking(checkAction)
  }
}

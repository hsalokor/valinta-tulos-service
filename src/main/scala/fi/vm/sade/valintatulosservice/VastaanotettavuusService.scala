package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}

import scala.util.{Failure, Success, Try}

class VastaanotettavuusService(hakukohdeRecordService: HakukohdeRecordService,
                               hakijaVastaanottoRepository: HakijaVastaanottoRepository) {
  def tarkistaAiemmatVastaanotot(henkiloOid: String, hakukohdeOid: String): Try[Unit] = {
    val hakukohdeRecord = hakukohdeRecordService.getHakukohdeRecord(hakukohdeOid)
    val aiemmatVastaanotot = haeAiemmatVastaanotot(hakukohdeRecord, henkiloOid)
    if (aiemmatVastaanotot.isEmpty) {
      Success(())
    } else if (aiemmatVastaanotot.size == 1) {
      val aiempiVastaanotto = aiemmatVastaanotot.head
      Failure(PriorAcceptanceException(aiempiVastaanotto))
    } else {
      Failure(new IllegalStateException(s"Hakijalla ${henkiloOid} useita vastaanottoja: $aiemmatVastaanotot"))
    }
  }

  private def haeAiemmatVastaanotot(hakukohdeRecord: HakukohdeRecord, hakijaOid: String): Set[VastaanottoRecord] = {
    val HakukohdeRecord(hakukohdeOid, _, yhdenPaikanSaantoVoimassa, _, koulutuksenAlkamiskausi) = hakukohdeRecord
    val aiemmatVastaanotot = if (yhdenPaikanSaantoVoimassa) {
      hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(hakijaOid, koulutuksenAlkamiskausi)
    } else {
      hakijaVastaanottoRepository.findHenkilonVastaanottoHakukohteeseen(hakijaOid, hakukohdeOid).toSet
    }
    aiemmatVastaanotot.filter(_.action != Peru)
  }
}

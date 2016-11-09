package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.{PriorAcceptanceException, HakukohdeRecord}
import fi.vm.sade.valintatulosservice.valintarekisteri.db.{VastaanottoRecord, HakijaVastaanottoRepository}
import fi.vm.sade.valintatulosservice.valintarekisteri.hakukohde.HakukohdeRecordService
import slick.dbio.{DBIO, DBIOAction}

import scala.concurrent.ExecutionContext.Implicits.global

class VastaanotettavuusService(hakukohdeRecordService: HakukohdeRecordService,
                               hakijaVastaanottoRepository: HakijaVastaanottoRepository) {
  private val defaultPriorAcceptanceHandler: VastaanottoRecord => DBIO[Unit] = aiempiVastaanotto => DBIOAction.failed(PriorAcceptanceException(aiempiVastaanotto))

  def tarkistaAiemmatVastaanotot(henkiloOid: String, hakukohdeOid: String, priorAcceptanceHandler: VastaanottoRecord => DBIO[Unit]): DBIO[Unit] = {
    val hakukohdeRecord = hakukohdeRecordService.getHakukohdeRecord(hakukohdeOid) match {
      case Right(h) => h
      case Left(e) => throw e
    }
    haeAiemmatVastaanotot(hakukohdeRecord, henkiloOid).flatMap {
      case None => DBIOAction.successful()
      case Some(aiempiVastaanotto) => priorAcceptanceHandler(aiempiVastaanotto)
    }
  }

  def tarkistaAiemmatVastaanotot(henkiloOid: String, hakukohdeOid: String): DBIO[Unit] = tarkistaAiemmatVastaanotot(henkiloOid, hakukohdeOid, defaultPriorAcceptanceHandler)

  private def haeAiemmatVastaanotot(hakukohdeRecord: HakukohdeRecord, hakijaOid: String): DBIO[Option[VastaanottoRecord]] = {
    val HakukohdeRecord(hakukohdeOid, _, yhdenPaikanSaantoVoimassa, _, koulutuksenAlkamiskausi) = hakukohdeRecord
    if (yhdenPaikanSaantoVoimassa) {
      hakijaVastaanottoRepository.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(hakijaOid, koulutuksenAlkamiskausi)
    } else {
      hakijaVastaanottoRepository.findHenkilonVastaanottoHakukohteeseen(hakijaOid, hakukohdeOid)
    }
  }
}

package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.valintatulosservice.domain.Kausi
import fi.vm.sade.valintatulosservice.ensikertalaisuus.OpintopolunVastaanottotieto
import slick.dbio.DBIO
import slick.driver.PostgresDriver.backend.Database

trait HakijaVastaanottoRepository extends VastaanottoRepository {
  val db: Database
  def findVastaanottoHistoryHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHenkilonVastaanottoHakukohteeseen(henkiloOid: String, hakukohdeOid: String): DBIO[Option[VastaanottoRecord]]
  def findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid: String, koulutuksenAlkamiskausi: Kausi): DBIO[Option[VastaanottoRecord]]
  def store(vastaanottoEvent: VastaanottoEvent): Unit
  def store[T](vastaanottoEvents: List[VastaanottoEvent], postCondition: DBIO[T]): T
  def store(vastaanottoEvent: VastaanottoEvent, vastaanottoDate: Date): Unit
}

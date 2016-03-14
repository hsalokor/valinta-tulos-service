package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.Kausi
import slick.dbio.Effect.All
import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.driver.PostgresDriver.backend.Database

trait HakijaVastaanottoRepository extends VastaanottoRepository {
  val db: Database
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHenkilonVastaanottoHakukohteeseen(henkiloOid: String, hakukohdeOid: String): DBIOAction[Option[VastaanottoRecord], NoStream, Effect]
  def findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid: String, koulutuksenAlkamiskausi: Kausi): DBIOAction[Option[VastaanottoRecord], NoStream, Effect]
  def store(vastaanottoEvent: VastaanottoEvent): Unit
  def store(vastaanottoEvents: List[VastaanottoEvent], postCondition: DBIOAction[Any, NoStream, All]): Unit
}

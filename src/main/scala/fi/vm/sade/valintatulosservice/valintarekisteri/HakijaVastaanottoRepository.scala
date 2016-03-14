package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.concurrent.TimeUnit

import fi.vm.sade.valintatulosservice.domain.Kausi
import slick.dbio.{DBIO, DBIOAction, NoStream, Effect}
import slick.driver.PostgresDriver.backend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait HakijaVastaanottoRepository {
  val db: Database
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord]
  def findHenkilonVastaanottoHakukohteeseen(henkiloOid: String, hakukohdeOid: String): DBIOAction[Option[VastaanottoRecord], NoStream, Effect]
  def findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid: String, koulutuksenAlkamiskausi: Kausi): DBIOAction[Option[VastaanottoRecord], NoStream, Effect]
  def store(vastaanottoEvent: VastaanottoEvent): Unit
  def store(vastaanottoEvents: List[VastaanottoEvent]): Unit

  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(2, TimeUnit.SECONDS)) = Await.result(db.run(operations), timeout)
}

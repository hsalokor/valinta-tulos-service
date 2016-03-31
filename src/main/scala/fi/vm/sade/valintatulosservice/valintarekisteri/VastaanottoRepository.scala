package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.concurrent.TimeUnit

import slick.dbio._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.driver.PostgresDriver.backend.Database

trait VastaanottoRepository {
  val db: Database
  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(2, TimeUnit.SECONDS)) = Await.result(db.run(operations), timeout)
}

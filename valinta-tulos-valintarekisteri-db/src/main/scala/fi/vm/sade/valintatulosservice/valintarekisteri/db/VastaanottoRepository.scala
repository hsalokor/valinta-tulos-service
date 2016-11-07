package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.util.concurrent.TimeUnit

import slick.dbio._
import slick.driver.PostgresDriver.backend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait VastaanottoRepository {
  val db: Database
  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(20, TimeUnit.SECONDS)) = Await.result(db.run(operations), timeout)
}
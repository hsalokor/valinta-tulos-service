package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.util.concurrent.TimeUnit

import slick.dbio._
import slick.driver.PostgresDriver.api.jdbcActionExtensionMethods
import slick.driver.PostgresDriver.backend.Database

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait VastaanottoRepository {
  val db: Database
  def runBlocking[R](operations: DBIO[R], timeout: Duration = Duration(20, TimeUnit.SECONDS)): R = {
    Await.result(
      db.run(operations.withStatementParameters(statementInit = st => st.setQueryTimeout(timeout.toSeconds.toInt))),
      timeout + Duration(1, TimeUnit.SECONDS)
    )
  }
}

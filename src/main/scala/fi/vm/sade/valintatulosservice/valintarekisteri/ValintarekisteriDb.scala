package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.concurrent.TimeUnit

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig

import slick.driver.PostgresDriver.api.Database
import slick.driver.PostgresDriver.api.actionBasedSQLInterpolation

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class ValintarekisteriDb(appConfig: AppConfig) {
  println(s"Database configuration: ${appConfig.settings.valintaRekisteriDbConfig}")
  val db = Database.forConfig("db", appConfig.settings.valintaRekisteriDbConfig.toConfig)

  def doSomething(): Unit = {
    val result = Await.result(db.run(sql"select 'Hello from PostgreSQL!'".as[String]), Duration(1, TimeUnit.SECONDS))
    println(s"Got from db: $result")
  }
}

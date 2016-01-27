package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigValueFactory
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import slick.driver.PostgresDriver.api.{Database, actionBasedSQLInterpolation}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class ValintarekisteriDb(appConfig: AppConfig) extends Logging {
  println(s"Database configuration: ${appConfig.settings.valintaRekisteriDbConfig.withValue("password", ConfigValueFactory.fromAnyRef("***"))}")
  val db = Database.forConfig("", appConfig.settings.valintaRekisteriDbConfig)

  def doSomething(): Unit = {
    val result = Await.result(db.run(sql"select 'Hello from PostgreSQL!'".as[String]), Duration(1, TimeUnit.SECONDS))
    println(s"Got from db: $result")
  }
}

package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigValueFactory}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.ensikertalaisuus.Ensikertalaisuus
import org.flywaydb.core.Flyway
import slick.driver.PostgresDriver.api.{Database, actionBasedSQLInterpolation}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class ValintarekisteriDb(dbConfig: Config) extends ValintarekisteriService with Logging {
  val user = if (dbConfig.hasPath("user")) dbConfig.getString("user") else null
  val password = if (dbConfig.hasPath("password")) dbConfig.getString("password") else null
  logger.info(s"Database configuration: ${dbConfig.withValue("password", ConfigValueFactory.fromAnyRef("***"))}")
  val flyway = new Flyway()
  flyway.setDataSource(dbConfig.getString("url"), user, password)
  flyway.migrate()
  val db = Database.forConfig("", dbConfig)


  def doSomething(): Unit = {
    val result = Await.result(db.run(sql"select 'Hello from PostgreSQL!'".as[String]), Duration(1, TimeUnit.SECONDS))
    logger.info(s"Got from db: $result")
  }

  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus = ???

  override def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamispvm: Date): Set[Ensikertalaisuus] = ???
}

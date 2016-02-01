package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigValueFactory}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.Kausi
import fi.vm.sade.valintatulosservice.ensikertalaisuus.Ensikertalaisuus
import org.flywaydb.core.Flyway
import slick.driver.PostgresDriver.api.{Database, actionBasedSQLInterpolation}
import slick.driver.PostgresDriver.api._

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

  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamisKausi: Kausi): Ensikertalaisuus = {
    val d = Await.result(db.run(sql"""select min("timestamp") from vastaanotot
          join hakukohteet on hakukohteet."hakukohdeOid" = vastaanotot.hakukohde and hakukohteet.kktutkintoonjohtava
                              and hakukohteet.koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}
          where vastaanotot.henkilo = $personOid
          and   active = true
       """.as[Option[Long]]), Duration(1, TimeUnit.SECONDS))
    Ensikertalaisuus(personOid, d.head.map(new Date(_)))
  }

  override def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamisKausi: Kausi): Set[Ensikertalaisuus] = {
    val createTempTable = sqlu"create temporary table person_oids (oid varchar) on commit drop"
    val insertPersonOids = SimpleDBIO[Unit](jdbcActionContext => {
      val statement = jdbcActionContext.connection.prepareStatement("insert into person_oids values (?)")
      try {
        personOids.foreach(oid => {
          statement.setString(1, oid)
          statement.addBatch()
        })
        statement.executeBatch()
      } finally {
        statement.close()
      }
    })
    val findVastaanottos = sql"""select person_oids.oid, min("timestamp") from person_oids
      left join vastaanotot on vastaanotot.henkilo = person_oids.oid and vastaanotot.active = true
      left join hakukohteet on hakukohteet."hakukohdeOid" = vastaanotot.hakukohde and hakukohteet.kktutkintoonjohtava
                               and koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}
      GROUP BY person_oids.oid
    """.as[(String, Option[Long])]

    val operations = createTempTable.andThen(insertPersonOids).andThen(findVastaanottos)
    val result = Await.result(db.run(operations.transactionally), Duration(1, TimeUnit.SECONDS))
    result.map(row => Ensikertalaisuus(row._1, row._2.map(new Date(_)))).toSet
  }
}

package fi.vm.sade.valintatulosservice.valintarekisteri

import java.sql.Timestamp
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

  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus = {
    val r = Await.result(db.run(sql"""select * from vastaanotot""".as[Any]), Duration(1, TimeUnit.SECONDS))
    r.foreach(println(_))
    val d = Await.result(db.run(sql"""select min("timestamp") from vastaanotot
          join hakukohteet on hakukohteet."hakukohdeOid" = vastaanotot.hakukohde
          join koulutushakukohde on koulutushakukohde."hakukohdeOid" = hakukohteet."hakukohdeOid"
          join koulutukset on koulutukset."koulutusOid" = koulutushakukohde."koulutusOid"
          join kaudet on kaudet.kausi = koulutukset.alkamiskausi
          where vastaanotot.henkilo = $personOid
          and   "kkTutkintoonJohtava" = true
          and   active = true
          and   kaudet.ajanjakso &> tsrange(${new Timestamp(koulutuksenAlkamispvm.getTime)}, null)
       """.as[Option[Long]]), Duration(1, TimeUnit.SECONDS))
    Ensikertalaisuus(personOid, d.head.map(new Date(_)))
  }

  override def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamispvm: Date): Set[Ensikertalaisuus] = ???
}

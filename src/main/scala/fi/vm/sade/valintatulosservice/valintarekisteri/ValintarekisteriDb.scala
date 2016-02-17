package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigValueFactory}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.Ensikertalaisuus
import org.flywaydb.core.Flyway
import slick.driver.PostgresDriver.api.{Database, actionBasedSQLInterpolation, _}
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class ValintarekisteriDb(dbConfig: Config) extends ValintarekisteriService with HakijaVastaanottoRepository with HakukohdeRepository with Logging {
  val user = if (dbConfig.hasPath("user")) dbConfig.getString("user") else null
  val password = if (dbConfig.hasPath("password")) dbConfig.getString("password") else null
  logger.info(s"Database configuration: ${dbConfig.withValue("password", ConfigValueFactory.fromAnyRef("***"))}")
  val flyway = new Flyway()
  flyway.setDataSource(dbConfig.getString("url"), user, password)
  flyway.migrate()
  val db = Database.forConfig("", dbConfig)
  private implicit val getVastaanottoResult = GetResult(r => VastaanottoRecord(r.nextString(), r.nextString(),
    r.nextString(), VastaanottoAction(r.nextString()), r.nextString(), new Date(r.nextLong())))

  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamisKausi: Kausi): Ensikertalaisuus = {
    val d = run(
          sql"""select min(all_vastaanotot."timestamp") from
                    (select "timestamp", koulutuksen_alkamiskausi from vastaanotot
                    join hakukohteet on hakukohteet.hakukohde_oid = vastaanotot.hakukohde
                                        and hakukohteet.kk_tutkintoon_johtava
                    where vastaanotot.henkilo = $personOid
                          and vastaanotot.active
                    union
                    select "timestamp", koulutuksen_alkamiskausi from vanhat_vastaanotot
                    where vanhat_vastaanotot.henkilo = $personOid
                          and vanhat_vastaanotot.deleted is null
                          and vanhat_vastaanotot.kk_tutkintoon_johtava) as all_vastaanotot
                where all_vastaanotot.koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}
            """.as[Option[Long]])
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
    val findVastaanottos =
      sql"""select person_oids.oid, min(all_vastaanotot."timestamp") from person_oids
            left join (select henkilo, "timestamp", koulutuksen_alkamiskausi from vastaanotot
                       join hakukohteet on hakukohteet.hakukohde_oid = vastaanotot.hakukohde
                                           and hakukohteet.kk_tutkintoon_johtava
                       where vastaanotot.active and (vastaanotot.action in ('VastaanotaSitovasti', 'VastaanotaEhdollisesti'))
                       union
                       select henkilo, "timestamp", koulutuksen_alkamiskausi from vanhat_vastaanotot
                       where vanhat_vastaanotot.deleted is null
                             and vanhat_vastaanotot.kk_tutkintoon_johtava) as all_vastaanotot
                on all_vastaanotot.henkilo = person_oids.oid
                   and all_vastaanotot.koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}
            group by person_oids.oid
        """.as[(String, Option[Long])]

    val operations = createTempTable.andThen(insertPersonOids).andThen(findVastaanottos)
    val result = run(operations.transactionally, Duration(1, TimeUnit.MINUTES))
    result.map(row => Ensikertalaisuus(row._1, row._2.map(new Date(_)))).toSet
  }

  override def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord] = {
    val vastaanottoRecords = run(sql"""select vo.henkilo as henkiloOid,  hk.haku_oid as hakuOid, hk.hakukohde_oid as hakukohdeOid,
                                  vo.action as action, vo.ilmoittaja as ilmoittaja, vo.timestamp as "timestamp"
                           from vastaanotot vo
                           join hakukohteet hk on hk.hakukohde_oid = vo.hakukohde
                           where vo.henkilo = $henkiloOid
                              and hk.haku_oid = $hakuOid
                              and vo.deleted is null""".as[VastaanottoRecord])
    vastaanottoRecords.toSet
  }

  override def findKkTutkintoonJohtavatVastaanotot(henkiloOid: String, koulutuksenAlkamiskausi: Kausi): Set[VastaanottoRecord] = {
    val vastaanottoRecords = run(sql"""select vo.henkilo as henkiloOid,  hk.haku_oid as hakuOid, hk.hakukohde_oid as hakukohdeOid,
                                  vo.action as action, vo.ilmoittaja as ilmoittaja, vo.timestamp as "timestamp"
                           from vastaanotot vo
                           join hakukohteet hk on hk.hakukohde_oid = vo.hakukohde
                           where vo.henkilo = $henkiloOid
                             and hk.kk_tutkintoon_johtava = true
                             and vo.deleted is null
                             and hk.koulutuksen_alkamiskausi = ${koulutuksenAlkamiskausi.toKausiSpec}""".as[VastaanottoRecord])
    vastaanottoRecords.toSet
  }

  override def store(vastaanottoEvent: VastaanottoEvent): Unit = {
    val VastaanottoEvent(henkiloOid, hakukohdeOid, action) = vastaanottoEvent
    val now = System.currentTimeMillis()
    run(sqlu"""insert into vastaanotot (hakukohde, henkilo, active, action, ilmoittaja, "timestamp")
              values ($hakukohdeOid, $henkiloOid, true, ${action.toString}::vastaanotto_action, $henkiloOid, $now)""")
  }

  def run[R](operations: DBIO[R], timeout: Duration = Duration(1, TimeUnit.SECONDS)) = Await.result(db.run(operations), timeout)

  override def findHakukohde(oid: String): Option[HakukohdeRecord] = {
    implicit val getHakukohdeResult = GetResult(r =>
      HakukohdeRecord(r.nextString(), r.nextString(), r.nextBoolean(), r.nextBoolean(), Kausi(r.nextString())))
    run(sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
           from hakukohteet
           where hakukohde_oid = $oid
         """.as[HakukohdeRecord]).headOption
  }

  override def storeHakukohde(hakukohdeRecord: HakukohdeRecord): Unit =
    run(sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi)
               values (${hakukohdeRecord.oid}, ${hakukohdeRecord.hakuOid}, ${hakukohdeRecord.yhdenPaikanSaantoVoimassa},
                       ${hakukohdeRecord.kktutkintoonJohtava}, ${hakukohdeRecord.koulutuksenAlkamiskausi.toKausiSpec})
        """)
}

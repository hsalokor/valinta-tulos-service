package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigValueFactory}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.Ensikertalaisuus
import org.flywaydb.core.Flyway
import org.postgresql.util.PSQLException
import slick.driver.PostgresDriver.api.{Database, actionBasedSQLInterpolation, _}
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


class ValintarekisteriDb(dbConfig: Config) extends ValintarekisteriService with HakijaVastaanottoRepository
  with HakukohdeRepository with VirkailijaVastaanottoRepository with Logging {
  val user = if (dbConfig.hasPath("user")) dbConfig.getString("user") else null
  val password = if (dbConfig.hasPath("password")) dbConfig.getString("password") else null
  logger.info(s"Database configuration: ${dbConfig.withValue("password", ConfigValueFactory.fromAnyRef("***"))}")
  val flyway = new Flyway()
  flyway.setDataSource(dbConfig.getString("url"), user, password)
  flyway.migrate()
  override val db = Database.forConfig("", dbConfig)
  private implicit val getVastaanottoResult = GetResult(r => VastaanottoRecord(r.nextString(), r.nextString(),
    r.nextString(), VastaanottoAction(r.nextString()), r.nextString(), r.nextTimestamp()))

  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamisKausi: Kausi): Ensikertalaisuus = {
    val d = runBlocking(
          sql"""with newest_vastaanotto_events as (
                select distinct on (vastaanotot.hakukohde) "timestamp", koulutuksen_alkamiskausi, action from vastaanotot
                join hakukohteet on hakukohteet.hakukohde_oid = vastaanotot.hakukohde
                                and hakukohteet.kk_tutkintoon_johtava
                where vastaanotot.henkilo = $personOid
                    and vastaanotot.deleted is null
                order by vastaanotot.hakukohde, vastaanotot.id desc
                ), new_vastaanotot as (
                select "timestamp", koulutuksen_alkamiskausi from newest_vastaanotto_events
                where action in ('VastaanotaSitovasti', 'VastaanotaEhdollisesti')
                ), old_vastaanotot as (
                select "timestamp", koulutuksen_alkamiskausi from vanhat_vastaanotot
                where henkilo = $personOid and kk_tutkintoon_johtava
                )
                select min(all_vastaanotot."timestamp")
                from (select "timestamp", koulutuksen_alkamiskausi from new_vastaanotot
                      union
                      select "timestamp", koulutuksen_alkamiskausi from old_vastaanotot) as all_vastaanotot
                where all_vastaanotot.koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}""".as[Option[java.sql.Timestamp]])
    Ensikertalaisuus(personOid, d.head)
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
            left join ((select vastaanotto_events.henkilo, vastaanotto_events."timestamp", vastaanotto_events.koulutuksen_alkamiskausi
                        from (select distinct on (vastaanotot.henkilo, vastaanotot.hakukohde)
                                  henkilo, "timestamp", koulutuksen_alkamiskausi, action from vastaanotot
                              join hakukohteet on hakukohteet.hakukohde_oid = vastaanotot.hakukohde
                                              and hakukohteet.kk_tutkintoon_johtava
                              where vastaanotot.deleted is null
                              order by vastaanotot.henkilo, vastaanotot.hakukohde, vastaanotot.id desc) as vastaanotto_events
                        where vastaanotto_events.action in ('VastaanotaSitovasti', 'VastaanotaEhdollisesti'))
                       union
                       (select henkilo, "timestamp", koulutuksen_alkamiskausi from vanhat_vastaanotot
                       where vanhat_vastaanotot.kk_tutkintoon_johtava)) as all_vastaanotot
                on all_vastaanotot.henkilo = person_oids.oid
                   and all_vastaanotot.koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}
            group by person_oids.oid
        """.as[(String, Option[java.sql.Timestamp])]

    val operations = createTempTable.andThen(insertPersonOids).andThen(findVastaanottos)
    val result = runBlocking(operations.transactionally, Duration(1, TimeUnit.MINUTES))
    result.map(row => Ensikertalaisuus(row._1, row._2)).toSet
  }

  override def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord] = {
    val vastaanottoRecords = runBlocking(
      sql"""select distinct on (vo.henkilo, vo.hakukohde)
            vo.henkilo as henkiloOid, hk.haku_oid as hakuOid, hk.hakukohde_oid as hakukohdeOid,
            vo.action as action, vo.ilmoittaja as ilmoittaja, vo.timestamp as "timestamp"
            from vastaanotot vo
            join hakukohteet hk on hk.hakukohde_oid = vo.hakukohde
            where vo.henkilo = $henkiloOid
                and hk.haku_oid = $hakuOid
                and vo.deleted is null
            order by vo.henkilo, vo.hakukohde, vo.id desc""".as[VastaanottoRecord])
    vastaanottoRecords.toSet
  }

  override def findHenkilonVastaanottoHakukohteeseen(henkiloOid: String, hakukohdeOid: String): DBIOAction[Option[VastaanottoRecord], NoStream, Effect] = {
    sql"""SELECT DISTINCT ON (vo.henkilo) vo.henkilo AS henkiloOid,  hk.haku_oid AS hakuOid, hk.hakukohde_oid AS hakukohdeOid,
                                                vo.action AS action, vo.ilmoittaja AS ilmoittaja, vo.timestamp AS "timestamp"
                FROM vastaanotot vo
                JOIN hakukohteet hk ON hk.hakukohde_oid = vo.hakukohde
                WHERE vo.henkilo = $henkiloOid
                    AND hk.hakukohde_oid = $hakukohdeOid
                    AND vo.deleted IS NULL
                ORDER BY vo.henkilo, vo.id DESC""".as[VastaanottoRecord].map(_.filter(vastaanottoRecord => {
      Set[VastaanottoAction](VastaanotaSitovasti, VastaanotaEhdollisesti).contains(vastaanottoRecord.action)
    })).map(records => {
      if (records.size > 1) {
        throw new RuntimeException(s"Hakijalla $henkiloOid useita vastaanottoja samaan hakukohteeseen: $records")
      } else {
        records.headOption
      }
    })
  }

  override def findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid: String, koulutuksenAlkamiskausi: Kausi): Option[VastaanottoRecord] = {
    val vastaanottoRecords = runBlocking(
      sql"""select distinct on (vo.henkilo, vo.hakukohde) vo.henkilo as henkiloOid,  hk.haku_oid as hakuOid, hk.hakukohde_oid as hakukohdeOid,
                                            vo.action as action, vo.ilmoittaja as ilmoittaja, vo.timestamp as "timestamp"
            from vastaanotot vo
            join hakukohteet hk on hk.hakukohde_oid = vo.hakukohde
            where vo.henkilo = $henkiloOid
                and hk.yhden_paikan_saanto_voimassa
                and vo.deleted is null
                and hk.koulutuksen_alkamiskausi = ${koulutuksenAlkamiskausi.toKausiSpec}
            order by vo.henkilo, vo.hakukohde, vo.id desc""".as[VastaanottoRecord]).filter(vastaanottoRecord => {
      Set[VastaanottoAction](VastaanotaSitovasti, VastaanotaEhdollisesti).contains(vastaanottoRecord.action)
    })
    if (vastaanottoRecords.size > 1) {
      throw new RuntimeException(s"Hakijalla ${henkiloOid} useita vastaanottoja yhden paikan säännön piirissä: $vastaanottoRecords")
    }
    vastaanottoRecords.headOption
  }

  override def store(vastaanottoEvents: List[VastaanottoEvent]): Unit = {
    runBlocking(DBIO.sequence(
      vastaanottoEvents.map(storeAction)
    ).transactionally)
  }

  override def store(vastaanottoEvent: VastaanottoEvent): Unit = {
    runBlocking(storeAction(vastaanottoEvent))
  }

  private def storeAction(vastaanottoEvent: VastaanottoEvent) = vastaanottoEvent.action match {
    case Poista => kumoaVastaanottotapahtumatAction(vastaanottoEvent)
    case _ => tallennaVastaanottoTapahtumaAction(vastaanottoEvent)
  }

  private def tallennaVastaanottoTapahtumaAction(vastaanottoEvent: VastaanottoEvent) = {
    val VastaanottoEvent(henkiloOid, _, hakukohdeOid, action, ilmoittaja, selite) = vastaanottoEvent
    sqlu"""insert into vastaanotot (hakukohde, henkilo, action, ilmoittaja, selite)
              values ($hakukohdeOid, $henkiloOid, ${action.toString}::vastaanotto_action, $ilmoittaja, $selite)"""
  }

  private def kumoaVastaanottotapahtumatAction(vastaanottoEvent: VastaanottoEvent) = {
    val VastaanottoEvent(henkiloOid, _, hakukohdeOid, _, ilmoittaja, selite) = vastaanottoEvent
    DBIO.seq(
      sqlu"""insert into deleted_vastaanotot (poistaja, selite) values ($ilmoittaja, $selite)""",
      sqlu"""update vastaanotot set deleted = currval('deleted_vastaanotot_id')
             where vastaanotot.henkilo = $henkiloOid
                 and vastaanotot.hakukohde = $hakukohdeOid""").transactionally
  }

  override def findHakukohde(oid: String): Option[HakukohdeRecord] = {
    implicit val getHakukohdeResult = GetResult(r =>
      HakukohdeRecord(r.nextString(), r.nextString(), r.nextBoolean(), r.nextBoolean(), Kausi(r.nextString())))
    runBlocking(sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
           from hakukohteet
           where hakukohde_oid = $oid
         """.as[HakukohdeRecord]).headOption
  }

  override def storeHakukohde(hakukohdeRecord: HakukohdeRecord): Unit = {
    val UNIQUE_VIOLATION = "23505"
    try {
      runBlocking(
        sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi)
                 values (${hakukohdeRecord.oid}, ${hakukohdeRecord.hakuOid}, ${hakukohdeRecord.yhdenPaikanSaantoVoimassa},
                         ${hakukohdeRecord.kktutkintoonJohtava}, ${hakukohdeRecord.koulutuksenAlkamiskausi.toKausiSpec})""")
    } catch {
      case e: PSQLException if e.getSQLState == UNIQUE_VIOLATION =>
        logger.debug(s"Ignored unique violation when inserting hakukohde record $hakukohdeRecord")
    }
  }

  override def findHakukohteenVastaanotot(hakukohdeOid: String): Set[VastaanottoRecord] = {
    val vastaanottoRecords = runBlocking(
      sql"""select distinct on (vo.henkilo, vo.hakukohde) vo.henkilo as henkiloOid,  hk.haku_oid as hakuOid, hk.hakukohde_oid as hakukohdeOid,
                                            vo.action as action, vo.ilmoittaja as ilmoittaja, vo.timestamp as "timestamp"
            from vastaanotot vo
            join hakukohteet hk on hk.hakukohde_oid = vo.hakukohde
            where vo.hakukohde = $hakukohdeOid
                and vo.deleted is null
            order by vo.henkilo, vo.hakukohde, vo.id desc""".as[VastaanottoRecord])
    vastaanottoRecords.toSet
  }
}

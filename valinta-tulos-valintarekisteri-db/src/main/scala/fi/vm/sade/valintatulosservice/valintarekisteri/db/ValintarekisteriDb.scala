package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.{PreparedStatement, Timestamp, Types}
import java.util.Date
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigValueFactory}
import fi.vm.sade.sijoittelu.domain.{Hakukohde, SijoitteluAjo, Valintatapajono, Hakemus => SijoitteluHakemus, _}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.flywaydb.core.Flyway
import org.postgresql.util.PSQLException
import slick.dbio.{DBIO => _, _}
import slick.driver.PostgresDriver.api.{Database, _}
import slick.jdbc.TransactionIsolation

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

class ValintarekisteriDb(dbConfig: Config, isItProfile:Boolean = false) extends ValintarekisteriResultExtractors
  with HakijaVastaanottoRepository with SijoitteluRepository with HakukohdeRepository
  with VirkailijaVastaanottoRepository with ValintarekisteriService with Logging {

  val user = if (dbConfig.hasPath("user")) dbConfig.getString("user") else null
  val password = if (dbConfig.hasPath("password")) dbConfig.getString("password") else null
  logger.info(s"Database configuration: ${dbConfig.withValue("password", ConfigValueFactory.fromAnyRef("***"))}")
  val flyway = new Flyway()
  flyway.setDataSource(dbConfig.getString("url"), user, password)
  flyway.migrate()
  override val db = Database.forConfig("", dbConfig)
  if(isItProfile) {
    logger.warn("alter table public.schema_version owner to oph")
    runBlocking(sqlu"""alter table public.schema_version owner to oph""")
  }

  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamisKausi: Kausi): Ensikertalaisuus = {
    val d = runBlocking(
      sql"""with old_vastaanotot as (
                select "timestamp", koulutuksen_alkamiskausi from vanhat_vastaanotot
                where kk_tutkintoon_johtava
                    and (henkilo in (select linked_oid from henkiloviitteet where person_oid = ${personOid})
                    or vanhat_vastaanotot.henkilo = ${personOid})
            )
            select min(all_vastaanotot."timestamp")
            from (select "timestamp", koulutuksen_alkamiskausi from newest_vastaanotot
                  where newest_vastaanotot.henkilo = ${personOid}
                      and newest_vastaanotot.kk_tutkintoon_johtava
                  union
                  select "timestamp", koulutuksen_alkamiskausi from old_vastaanotot) as all_vastaanotot
            where all_vastaanotot.koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}""".as[Option[java.sql.Timestamp]])
    Ensikertalaisuus(personOid, d.head)
  }

  override def findVastaanottoHistoryHaussa(henkiloOid: String, hakuOid: String): Set[VastaanottoRecord] = {
    runBlocking(
      sql"""select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp"
            from (
                select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp", id
                from vastaanotot
                    join hakukohteet on hakukohde_oid = vastaanotot.hakukohde and haku_oid = ${hakuOid}
                where henkilo = ${henkiloOid}
                union
                select henkiloviitteet.linked_oid as henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp", id
                from vastaanotot
                    join hakukohteet on hakukohde_oid = vastaanotot.hakukohde and haku_oid = ${hakuOid}
                    join henkiloviitteet on vastaanotot.henkilo = henkiloviitteet.person_oid and henkiloviitteet.linked_oid = ${henkiloOid}) as t
            order by id""".as[VastaanottoRecord]).toSet
  }

  override def findVastaanottoHistory(personOid: String): VastaanottoHistoria = {
    val newList = runBlocking(
      sql"""select haku_oid, hakukohde, "action", "timestamp"
            from newest_vastaanotot
            where kk_tutkintoon_johtava
                and henkilo = ${personOid}
            order by "timestamp" desc
      """.as[(String, String, String, java.sql.Timestamp)]
    ).map(vastaanotto => OpintopolunVastaanottotieto(personOid, vastaanotto._1, vastaanotto._2, vastaanotto._3, vastaanotto._4)).toList
    val oldList = runBlocking(
      sql"""select hakukohde, "timestamp" from vanhat_vastaanotot
            where kk_tutkintoon_johtava
                and (henkilo in (select linked_oid from henkiloviitteet where person_oid = ${personOid})
                     or henkilo = ${personOid})
            order by "timestamp" desc
      """.as[(String, java.sql.Timestamp)]
    ).map(vastaanotto => VanhaVastaanottotieto(personOid, vastaanotto._1, vastaanotto._2)).toList
    VastaanottoHistoria(newList, oldList)
  }

  override def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamisKausi: Kausi): Set[Ensikertalaisuus] = {
    val createTempTable = sqlu"create temporary table person_oids (oid varchar) on commit drop"
    val insertPersonOids = SimpleDBIO[Unit](jdbcActionContext => {
      val statement = jdbcActionContext.connection.prepareStatement("""insert into person_oids values (?)""")
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
      sql"""with query_oids as (
                select oid as query_oid, oid as alias_oid
                from person_oids
                union
                select person_oid as query_oid, linked_oid as alias_oid
                from henkiloviitteet hv
                join person_oids on person_oids.oid = hv.person_oid),
            new_vastaanotot as (
                select distinct on (query_oids.query_oid, hakukohde) query_oids.query_oid as henkilo, "timestamp", koulutuksen_alkamiskausi, action
                from vastaanotot
                    join query_oids on query_oids.alias_oid = vastaanotot.henkilo
                    join hakukohteet hk on hk.hakukohde_oid = vastaanotot.hakukohde
                where hk.kk_tutkintoon_johtava and deleted is null
                order by query_oids.query_oid, hakukohde, id desc
            ),
            old_vastaanotot as (
                select query_oids.query_oid as henkilo, "timestamp", koulutuksen_alkamiskausi
                from vanhat_vastaanotot
                    join query_oids on query_oids.alias_oid = vanhat_vastaanotot.henkilo
                where vanhat_vastaanotot.kk_tutkintoon_johtava
            )
            select person_oids.oid, min(all_vastaanotot."timestamp") from person_oids
            left join ((select henkilo, "timestamp", koulutuksen_alkamiskausi from new_vastaanotot
                            where action in ('VastaanotaSitovasti', 'VastaanotaEhdollisesti'))
                       union
                       (select henkilo, "timestamp", koulutuksen_alkamiskausi from old_vastaanotot)) as all_vastaanotot
                on all_vastaanotot.henkilo = person_oids.oid
                   and all_vastaanotot.koulutuksen_alkamiskausi >= ${koulutuksenAlkamisKausi.toKausiSpec}
            group by person_oids.oid
        """.as[(String, Option[java.sql.Timestamp])]

    val operations = createTempTable.andThen(insertPersonOids).andThen(findVastaanottos)
    val result = runBlocking(operations.transactionally, Duration(1, TimeUnit.MINUTES))
    result.map(row => Ensikertalaisuus(row._1, row._2)).toSet
  }

  override def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): DBIO[Set[VastaanottoRecord]] = {
    sql"""select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp"
          from (
              select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp", id
              from vastaanotot
                  join hakukohteet on hakukohde_oid = vastaanotot.hakukohde and haku_oid = ${hakuOid}
              where henkilo = ${henkiloOid} and deleted is null
              union
              select henkiloviitteet.linked_oid as henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp", id
              from vastaanotot
                  join hakukohteet on hakukohde_oid = vastaanotot.hakukohde and haku_oid = ${hakuOid}
                  join henkiloviitteet on vastaanotot.henkilo = henkiloviitteet.person_oid and henkiloviitteet.linked_oid = ${henkiloOid}
              where deleted is null) as t
          order by id""".as[VastaanottoRecord].map(_.toSet)
  }

  override def findHaunVastaanotot(hakuOid: String): Set[VastaanottoRecord] = {
    runBlocking(sql"""select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp"
                      from newest_vastaanotto_events
                      where haku_oid = ${hakuOid}""".as[VastaanottoRecord]).toSet
  }

  override def findHenkilonVastaanottoHakukohteeseen(personOid: String, hakukohdeOid: String): DBIO[Option[VastaanottoRecord]] = {
    sql"""select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp"
          from newest_vastaanotot
          where henkilo = $personOid
              and hakukohde = $hakukohdeOid""".as[VastaanottoRecord].map(vastaanottoRecords => {
      if (vastaanottoRecords.size > 1) {
        throw ConflictingAcceptancesException(personOid, vastaanottoRecords, "samaan hakukohteeseen")
      } else {
        vastaanottoRecords.headOption
      }
    })
  }

  override def findYhdenPaikanSaannonPiirissaOlevatVastaanotot(personOid: String, koulutuksenAlkamiskausi: Kausi): DBIO[Option[VastaanottoRecord]] = {
    sql"""select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp"
          from newest_vastaanotot
          where henkilo = $personOid
              and yhden_paikan_saanto_voimassa
              and koulutuksen_alkamiskausi = ${koulutuksenAlkamiskausi.toKausiSpec}""".as[VastaanottoRecord]
      .map(vastaanottoRecords => {
        if (vastaanottoRecords.size > 1) {
          throw ConflictingAcceptancesException(personOid, vastaanottoRecords, "yhden paikan säännön piirissä")
        } else {
          vastaanottoRecords.headOption
        }
      })
  }

  override def findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(kausi: Kausi): Set[VastaanottoRecord] = {
    runBlocking(
      sql"""select henkilo, haku_oid, hakukohde, action, ilmoittaja, "timestamp"
            from newest_vastaanotot
            where koulutuksen_alkamiskausi = ${kausi.toKausiSpec}
                and yhden_paikan_saanto_voimassa""".as[VastaanottoRecord]).toSet
  }

  override def store(vastaanottoEvent: VastaanottoEvent, vastaanottoDate: Date) = {
    val VastaanottoEvent(henkiloOid, _, hakukohdeOid, action, ilmoittaja, selite) = vastaanottoEvent
    runBlocking(
      sqlu"""insert into vastaanotot (hakukohde, henkilo, action, ilmoittaja, selite, timestamp)
              values ($hakukohdeOid, $henkiloOid, ${action.toString}::vastaanotto_action, $ilmoittaja, $selite, ${new java.sql.Timestamp(vastaanottoDate.getTime)})""")
  }

  def runAsSerialized[T](retries: Int, wait: Duration, description: String, action: DBIO[T]): Either[Throwable, T] = {
    val SERIALIZATION_VIOLATION = "40001"
    try {
      Right(runBlocking(action.transactionally.withTransactionIsolation(TransactionIsolation.Serializable)))
    } catch {
      case e: PSQLException if e.getSQLState == SERIALIZATION_VIOLATION =>
        if (retries > 0) {
          logger.warn(s"$description failed because of an concurrent action, retrying after $wait ms")
          Thread.sleep(wait.toMillis)
          runAsSerialized(retries - 1, wait + wait, description, action)
        } else {
          Left(new RuntimeException(s"$description failed because of an concurrent action.", e))
        }
      case NonFatal(e) => Left(e)
    }
  }

  override def store[T](vastaanottoEvents: List[VastaanottoEvent], postCondition: DBIO[T]): T = {
    runAsSerialized(10, Duration(5, TimeUnit.MILLISECONDS), s"Storing $vastaanottoEvents",
      DBIO.sequence(vastaanottoEvents.map(storeAction)).andThen(postCondition)) match {
      case Right(x) => x
      case Left(e) => throw e
    }
  }

  override def store(vastaanottoEvent: VastaanottoEvent): Unit = {
    runAsSerialized(10, Duration(5, TimeUnit.MILLISECONDS), s"Storing $vastaanottoEvent",
      storeAction(vastaanottoEvent)) match {
      case Right(_) => ()
      case Left(e) => throw e
    }
  }

  def storeAction(vastaanottoEvent: VastaanottoEvent): DBIO[Unit] = vastaanottoEvent.action match {
    case Poista => kumoaVastaanottotapahtumatAction(vastaanottoEvent)
    case _ => tallennaVastaanottoTapahtumaAction(vastaanottoEvent)
  }

  private def tallennaVastaanottoTapahtumaAction(vastaanottoEvent: VastaanottoEvent): DBIO[Unit] = {
    val VastaanottoEvent(henkiloOid, _, hakukohdeOid, action, ilmoittaja, selite) = vastaanottoEvent
    DBIO.seq(
      sqlu"""update vastaanotot set deleted = overriden_vastaanotto_deleted_id()
                 where (henkilo = ${henkiloOid}
                        or henkilo in (select linked_oid from henkiloviitteet where person_oid = ${henkiloOid}))
                     and hakukohde = ${hakukohdeOid}
                     and deleted is null""",
      sqlu"""insert into vastaanotot (hakukohde, henkilo, action, ilmoittaja, selite)
             values ($hakukohdeOid, $henkiloOid, ${action.toString}::vastaanotto_action, $ilmoittaja, $selite)""")
  }

  private def kumoaVastaanottotapahtumatAction(vastaanottoEvent: VastaanottoEvent): DBIO[Unit] = {
    val VastaanottoEvent(henkiloOid, _, hakukohdeOid, _, ilmoittaja, selite) = vastaanottoEvent
    val insertDelete = sqlu"""insert into deleted_vastaanotot (poistaja, selite) values ($ilmoittaja, $selite)"""
    val updateVastaanotto =
      sqlu"""update vastaanotot set deleted = currval('deleted_vastaanotot_id')
                                       where (vastaanotot.henkilo = $henkiloOid
                                              or vastaanotot.henkilo in (select linked_oid from henkiloviitteet where person_oid = $henkiloOid))
                                           and vastaanotot.hakukohde = $hakukohdeOid
                                           and vastaanotot.deleted is null"""
    insertDelete.andThen(updateVastaanotto).flatMap {
      case 0 =>
        DBIO.failed(new IllegalStateException(s"No vastaanotto events found for $henkiloOid to hakukohde $hakukohdeOid"))
      case n =>
        DBIO.successful(())
    }
  }

  override def findHakukohde(oid: String): Option[HakukohdeRecord] = {
    runBlocking(sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
           from hakukohteet
           where hakukohde_oid = $oid
         """.as[HakukohdeRecord]).headOption
  }

  override def findHaunArbitraryHakukohde(oid: String): Option[HakukohdeRecord] = {
    runBlocking(sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
           from hakukohteet
           where haku_oid = $oid
           limit 1
         """.as[HakukohdeRecord]).headOption
  }

  override def findHaunHakukohteet(oid: String): Set[HakukohdeRecord] = {
    runBlocking(sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
           from hakukohteet
           where haku_oid = $oid
         """.as[HakukohdeRecord]).toSet
  }

  override def all: Set[HakukohdeRecord] = {
    runBlocking(
      sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
            from hakukohteet""".as[HakukohdeRecord]).toSet
  }

  override def findHakukohteet(hakukohdeOids: Set[String]): Set[HakukohdeRecord] = hakukohdeOids match {
    case x if 0 == x.size => Set()
    case _ => {
      val invalidOids = hakukohdeOids.filterNot(OidValidator.isOid)
      if (invalidOids.nonEmpty) {
        throw new IllegalArgumentException(s"${invalidOids.size} huonoa oidia syötteessä: $invalidOids")
      }
      val inParameter = hakukohdeOids.map(oid => s"'$oid'").mkString(",")
      runBlocking(
        sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
            from hakukohteet where hakukohde_oid in (#$inParameter)""".as[HakukohdeRecord]).toSet
    }
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

  override def updateHakukohde(hakukohdeRecord: HakukohdeRecord): Boolean = {
    runBlocking(
      sqlu"""update hakukohteet set (yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi)
             = (${hakukohdeRecord.yhdenPaikanSaantoVoimassa},
                ${hakukohdeRecord.kktutkintoonJohtava},
                ${hakukohdeRecord.koulutuksenAlkamiskausi.toKausiSpec})
             where hakukohde_oid = ${hakukohdeRecord.oid}
                 and (yhden_paikan_saanto_voimassa <> ${hakukohdeRecord.yhdenPaikanSaantoVoimassa}
                   or kk_tutkintoon_johtava <> ${hakukohdeRecord.kktutkintoonJohtava}
                   or koulutuksen_alkamiskausi <> ${hakukohdeRecord.koulutuksenAlkamiskausi.toKausiSpec})"""
    ) == 1
  }

  override def hakukohteessaVastaanottoja(oid: String): Boolean = {
    runBlocking(sql"""select count(*) from newest_vastaanotot where hakukohde = ${oid}""".as[Int]).head > 0
  }

  override def aliases(henkiloOid: String): DBIO[Set[String]] = {
    sql"""select linked_oid from henkiloviitteet where person_oid = ${henkiloOid}""".as[String].map(_.toSet)
  }

  override def storeSijoittelu(sijoittelu: SijoitteluWrapper) = {
    runBlocking(handleSijoitteluHistory(sijoittelu).andThen(insertSijoitteluajo(sijoittelu.sijoitteluajo).andThen(
      DBIO.sequence(sijoittelu.hakukohteet.map(hakukohde =>
        storeSijoittelunHakukohde(sijoittelu.sijoitteluajo.getSijoitteluajoId, sijoittelu.sijoitteluajo.getHakuOid, hakukohde,
          sijoittelu.valintatulokset.filter(vt => vt.getHakukohdeOid == hakukohde.getOid))
      ))).transactionally),
      Duration(600, TimeUnit.SECONDS) /* Longer timeout for saving entire sijoittelu in a transaction. */)
      analyzeLargeTables()
  }

  private def analyzeLargeTables() = {
    runBlocking(DBIO.seq(
      sqlu"""analyze jonosijat""",
      sqlu"""analyze valinnantulokset""",
      sqlu"""analyze pistetiedot"""),
      Duration(60, TimeUnit.SECONDS))
  }

  import scala.collection.JavaConverters._

  private def handleSijoitteluHistory(sijoittelu: SijoitteluWrapper) = {
    pistetietoHistoryUpdate(sijoittelu.sijoitteluajo.getHakuOid)
  }

  private def pistetietoHistoryUpdate(hakuOid:String) = {
    sqlu"""
          update pistetiedot p set deleted = true
          from sijoitteluajot s
          where p.sijoitteluajo_id = s.id and p.deleted = false and s.haku_oid = ${hakuOid}"""
  }

  private def storeSijoittelunHakukohde(sijoitteluajoId:Long, hakuOid: String, hakukohde: Hakukohde, valintatulokset: List[Valintatulos]) = {
    insertHakukohde(hakuOid, hakukohde).andThen(
      DBIO.sequence(
        hakukohde.getValintatapajonot.asScala.map(valintatapajono =>
          storeSijoittelunValintatapajono(sijoitteluajoId, hakukohde.getOid, valintatapajono,
            valintatulokset.filter(_.getValintatapajonoOid == valintatapajono.getOid))).toList ++
          hakukohde.getHakijaryhmat.asScala.map(hakijaryhma => storeSijoittelunHakijaryhma(sijoitteluajoId, hakijaryhma,
            hakukohde.getValintatapajonot.asScala.flatMap(_.getHakemukset.asScala).toList)).toList)
    )
  }

  private def storeSijoittelunHakijaryhma(sijoitteluajoId:Long, hakijaryhma: Hakijaryhma, hakemukset: List[SijoitteluHakemus]) = {
    insertHakijaryhma(sijoitteluajoId, hakijaryhma).andThen(
      DBIO.sequence(hakijaryhma.getHakemusOid.asScala.map(hakemusOid => {
        val hakemusExists = hakemukset.exists(h => h.getHakemusOid == hakemusOid && h.getHyvaksyttyHakijaryhmista.contains(hakijaryhma.getOid))
        insertHakijaryhmanHakemus(hakijaryhma.getOid, sijoitteluajoId, hakemusOid, hakemusExists)
      }).toList)
    )
  }

  private def storeSijoittelunValintatapajono(sijoitteluajoId:Long, hakukohdeOid:String, valintatapajono:Valintatapajono, valintatulokset: List[Valintatulos]) = {
    insertValintatapajono(sijoitteluajoId, hakukohdeOid, valintatapajono).andThen(
      storeValintatapajononHakemukset(valintatapajono.getHakemukset.asScala.toList, sijoitteluajoId, hakukohdeOid, valintatapajono.getOid, valintatulokset))
  }

  private def storeValintatapajononHakemukset(hakemukset:List[SijoitteluHakemus], sijoitteluajoId:Long, hakukohdeOid:String, valintatapajonoOid:String, valintatulokset: List[Valintatulos]) = {
    SimpleDBIO { session =>
      val jonosijaStatement = createJonosijaStatement(session.connection)
      val pistetietoStatement = createPistetietoStatement(session.connection)
      val valinnantulosStatement = createValinnantulosStatement(session.connection)
      val valinnantilaStatement = createValinnantilaStatement(session.connection)
      val viestinnanOhjausStatement = createViestinnanOhjaus(session.connection)
      val tilankuvausStatement = createTilankuvausStatement(session.connection)
      hakemukset.foreach( hakemus => {
        val hakemusWrapper = SijoitteluajonHakemusWrapper(hakemus)
        val hakemusOid = hakemusWrapper.hakemusOid
        val valintatulos = valintatulokset.find(_.getHakemusOid == hakemusOid)
        createJonosijaInsertRow(sijoitteluajoId, hakukohdeOid, valintatapajonoOid, hakemusWrapper, jonosijaStatement)
        hakemus.getPistetiedot.asScala.foreach(createPistetietoInsertRow(sijoitteluajoId, valintatapajonoOid, hakemusOid, _, pistetietoStatement))
        createValinnantilanKuvausInsertRow(hakemusWrapper.tilankuvauksenHash, hakemusWrapper.tilankuvauksetWithTarkenne, tilankuvausStatement)
        createValinnantulosInsertRow(hakemusWrapper, valintatulos, sijoitteluajoId, hakukohdeOid, valintatapajonoOid, valinnantulosStatement)
        createValinnantilaInsertRow(hakukohdeOid, valintatapajonoOid, sijoitteluajoId, hakemusWrapper, valinnantilaStatement)
        createViestinnanOhjausInsertRow(hakukohdeOid, valintatapajonoOid, hakemusOid, valintatulos, viestinnanOhjausStatement)
      })
      tilankuvausStatement.executeBatch
      jonosijaStatement.executeBatch
      pistetietoStatement.executeBatch
      valinnantulosStatement.executeBatch
      valinnantilaStatement.executeBatch
      viestinnanOhjausStatement.executeBatch
      insertIlmoittautumiset(session.connection, valintatulokset, hakemukset)
    }
  }

  private def createStatement(sql:String) = (connection:java.sql.Connection) => connection.prepareStatement(sql)

  private def createJonosijaStatement = createStatement("""insert into jonosijat (valintatapajono_oid, sijoitteluajo_id, hakukohde_oid, hakemus_oid, hakija_oid, etunimi, sukunimi, prioriteetti,
          jonosija, varasijan_numero, onko_muuttunut_viime_sijoittelussa, pisteet, tasasijajonosija, hyvaksytty_harkinnanvaraisesti,
          siirtynyt_toisesta_valintatapajonosta, tila, tarkenteen_lisatieto, tilankuvaus_hash) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::valinnantila, ?, ?)""")

  private def createJonosijaInsertRow(sijoitteluajoId: Long, hakukohdeOid: String, valintatapajonoOid: String, hakemus: SijoitteluajonHakemusWrapper, statement: PreparedStatement) = {
    val SijoitteluajonHakemusWrapper(hakemusOid, hakijaOid, etunimi, sukunimi, prioriteetti, jonosija, varasijanNumero,
    onkoMuuttunutViimeSijoittelussa, pisteet, tasasijaJonosija, hyvaksyttyHarkinnanvaraisesti, siirtynytToisestaValintatapajonosta,
    valinnantila, tilanKuvaukset, tilankuvauksenTarkenne, tarkenteenLisatieto, hyvaksyttyHakijaryhmista, _) = hakemus

    statement.setString(1, valintatapajonoOid)
    statement.setLong(2, sijoitteluajoId)
    statement.setString(3, hakukohdeOid)
    statement.setString(4, hakemusOid)
    statement.setString(5, hakijaOid.orNull)
    statement.setString(6, etunimi.orNull)
    statement.setString(7, sukunimi.orNull)
    statement.setInt(8, prioriteetti)
    statement.setInt(9, jonosija)
    varasijanNumero match {
      case Some(x) => statement.setInt(10, x)
      case _ => statement.setNull(10, Types.INTEGER)
    }
    statement.setBoolean(11, onkoMuuttunutViimeSijoittelussa)
    statement.setBigDecimal(12, pisteet.map(_.bigDecimal).getOrElse(null))
    statement.setInt(13, tasasijaJonosija)
    statement.setBoolean(14, hyvaksyttyHarkinnanvaraisesti)
    statement.setBoolean(15, siirtynytToisestaValintatapajonosta)
    statement.setString(16, valinnantila.toString)
    statement.setString(17, tarkenteenLisatieto.orNull)
    statement.setInt(18, hakemus.tilankuvauksenHash)
    statement.addBatch
  }

  private def createPistetietoStatement = createStatement("""insert into pistetiedot (sijoitteluajo_id, hakemus_oid, valintatapajono_oid,
    tunniste, arvo, laskennallinen_arvo, osallistuminen) values (?, ?, ?, ?, ?, ?, ?)""")

  private def createPistetietoInsertRow(sijoitteluajoId: Long, valintatapajonoOid: String, hakemusOid:String, pistetieto: Pistetieto, statement: PreparedStatement) = {
    val SijoitteluajonPistetietoWrapper(tunniste, arvo, laskennallinenArvo, osallistuminen)
    = SijoitteluajonPistetietoWrapper(pistetieto)

    statement.setLong(1, sijoitteluajoId)
    statement.setString(2, hakemusOid)
    statement.setString(3, valintatapajonoOid)
    statement.setString(4, tunniste)
    statement.setString(5, arvo.orNull)
    statement.setString(6, laskennallinenArvo.orNull)
    statement.setString(7, osallistuminen.orNull)
    statement.addBatch
  }

  private def createValinnantulosStatement = createStatement(
    """insert into valinnantulokset (
           valintatapajono_oid,
           hakemus_oid,
           hakukohde_oid,
           henkilo_oid,
           tilankuvaus_hash,
           tarkenteen_lisatieto,
           julkaistavissa,
           ehdollisesti_hyvaksyttavissa,
           hyvaksytty_varasijalta,
           hyvaksy_peruuntunut,
           ilmoittaja,
           selite
       ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::text, 'Sijoittelun tallennus')
       on conflict on constraint valinnantulokset_pkey do update set
           tilankuvaus_hash = excluded.tilankuvaus_hash,
           tarkenteen_lisatieto = excluded.tarkenteen_lisatieto,
           julkaistavissa = excluded.julkaistavissa,
           ehdollisesti_hyvaksyttavissa = excluded.ehdollisesti_hyvaksyttavissa,
           hyvaksytty_varasijalta = excluded.hyvaksytty_varasijalta,
           hyvaksy_peruuntunut = excluded.hyvaksy_peruuntunut,
           ilmoittaja = excluded.ilmoittaja,
           selite = excluded.selite
       where valinnantulokset.tilankuvaus_hash <> excluded.tilankuvaus_hash
           or valinnantulokset.tarkenteen_lisatieto <> excluded.tarkenteen_lisatieto
           or valinnantulokset.julkaistavissa <> excluded.julkaistavissa
           or valinnantulokset.ehdollisesti_hyvaksyttavissa <> excluded.ehdollisesti_hyvaksyttavissa
           or valinnantulokset.hyvaksytty_varasijalta <> excluded.hyvaksytty_varasijalta
           or valinnantulokset.hyvaksy_peruuntunut <> excluded.hyvaksy_peruuntunut""")

  private def createValinnantulosInsertRow(hakemus:SijoitteluajonHakemusWrapper,
                                           valintatulos:Option[Valintatulos],
                                           sijoitteluajoId:Long,
                                           hakukohdeOid:String,
                                           valintatapajonoOid:String,
                                           valinnantulosStatement:PreparedStatement) = {
    valinnantulosStatement.setString(1, valintatapajonoOid)
    valinnantulosStatement.setString(2, hakemus.hakemusOid)
    valinnantulosStatement.setString(3, hakukohdeOid)
    valinnantulosStatement.setString(4, hakemus.hakijaOid.orNull)
    valinnantulosStatement.setInt(5, hakemus.tilankuvauksenHash)
    valinnantulosStatement.setString(6, hakemus.tarkenteenLisatieto.orNull)
    valinnantulosStatement.setBoolean(7, valintatulos.exists(_.getJulkaistavissa))
    valinnantulosStatement.setBoolean(8, valintatulos.exists(_.getEhdollisestiHyvaksyttavissa))
    valinnantulosStatement.setBoolean(9, valintatulos.exists(_.getHyvaksyttyVarasijalta))
    valinnantulosStatement.setBoolean(10, valintatulos.exists(_.getHyvaksyPeruuntunut))
    valinnantulosStatement.setLong(11, sijoitteluajoId)

    valinnantulosStatement.addBatch()
  }

  private def createValinnantilaStatement = createStatement(
    """insert into valinnantilat (
           hakukohde_oid,
           valintatapajono_oid,
           hakemus_oid,
           tila,
           tilan_viimeisin_muutos,
           ilmoittaja
       ) values (?, ?, ?, ?::valinnantila, ?, ?::text)
       on conflict on constraint valinnantilat_pkey do update set
           tila = excluded.tila,
           tilan_viimeisin_muutos = excluded.tilan_viimeisin_muutos,
           ilmoittaja = excluded.ilmoittaja
       where valinnantilat.tila <> excluded.tila
    """)

  private def createValinnantilaInsertRow(hakukohdeOid: String,
                                                  valintatapajonoOid: String,
                                                  sijoitteluajoId: Long,
                                                  hakemus: SijoitteluajonHakemusWrapper,
                                                  statement: PreparedStatement) = {
    val tilanViimeisinMuutos = hakemus.tilaHistoria
      .filter(_.tila.equals(hakemus.tila))
      .map(_.luotu)
      .sortWith(_.after(_))
      .headOption.getOrElse(new Date())

    statement.setString(1, hakukohdeOid)
    statement.setString(2, valintatapajonoOid)
    statement.setString(3, hakemus.hakemusOid)
    statement.setString(4, hakemus.tila.toString)
    statement.setTimestamp(5, new Timestamp(tilanViimeisinMuutos.getTime))
    statement.setLong(6, sijoitteluajoId)

    statement.addBatch()
  }

  private def createViestinnanOhjaus = createStatement(
    """insert into viestinnan_ohjaus (
           hakukohde_oid,
           valintatapajono_oid,
           hakemus_oid,
           previous_check,
           sent,
           done,
           message
       ) values (?, ?, ?, ?, ?, ?, ?)
       on conflict on constraint viestinnan_ohjaus_pkey do update set
           previous_check = excluded.previous_check,
           sent = excluded.sent,
           done = excluded.done,
           message = excluded.message"""
  )

  private def createViestinnanOhjausInsertRow(hakukohdeOid: String,
                                              valintatapajonoOid:String,
                                              hakemusOid: String,
                                              valintatulos: Option[Valintatulos],
                                              statement: PreparedStatement) = {
    statement.setString(1, hakukohdeOid)
    statement.setString(2, valintatapajonoOid)
    statement.setString(3, hakemusOid)
    statement.setTimestamp(4, valintatulos.flatMap(v => Option(v.getMailStatus.previousCheck)).map(x => new Timestamp(x.getTime)).orNull)
    statement.setTimestamp(5, valintatulos.flatMap(v => Option(v.getMailStatus.sent)).map(x => new Timestamp(x.getTime)).orNull)
    statement.setTimestamp(6, valintatulos.flatMap(v => Option(v.getMailStatus.done)).map(x => new Timestamp(x.getTime)).orNull)
    statement.setString(7, valintatulos.map(_.getMailStatus.message).orNull)
    statement.addBatch()
  }

  private def createTilankuvausStatement = createStatement("""insert into valinnantilan_kuvaukset (hash, tilan_tarkenne, text_fi, text_sv, text_en)
      values (?, ?::valinnantilanTarkenne, ?, ?, ?) on conflict do nothing""")

  private def createValinnantilanKuvausInsertRow(hash:Int, tilankuvauksetWithTarkenne: Map[String, String], tilankuvausStatement:PreparedStatement) = {
        tilankuvausStatement.setInt(1, hash)
        tilankuvausStatement.setString(2, tilankuvauksetWithTarkenne("tilankuvauksenTarkenne"))
        tilankuvausStatement.setString(3, tilankuvauksetWithTarkenne.getOrElse("FI", null))
        tilankuvausStatement.setString(4, tilankuvauksetWithTarkenne.getOrElse("SV", null))
        tilankuvausStatement.setString(5, tilankuvauksetWithTarkenne.getOrElse("EN", null))
        tilankuvausStatement.addBatch
  }

  private def newJavaSqlDateOrNull(date:Option[Date]) = date match {
    case Some(x) => new java.sql.Timestamp(x.getTime)
    case _ => null
  }

  private def insertIlmoittautumiset(connection:java.sql.Connection, valintatulokset:List[Valintatulos], hakemukset:List[SijoitteluHakemus]) = {
    val sql =  """insert into ilmoittautumiset (henkilo, hakukohde, tila, ilmoittaja, selite)
           values (?, ?, ?::ilmoittautumistila, ?, ?)"""

      val statement = createStatement(sql)(connection)
      valintatulokset.foreach(v => {
        val SijoitteluajonValinnantulosWrapper(_, _, hakukohdeOid, _, _, _, _, ilmoittautumistila, logEntries, mailStatus)
        = SijoitteluajonValinnantulosWrapper(v)

        val (ilmoittaja, selite) = v.getOriginalLogEntries.asScala.filter(e => e.getLuotu != null).sortBy(_.getLuotu).reverse.headOption match {
          case Some(entry) => (entry.getMuokkaaja, entry.getSelite)
          case None => ("System", "")
        }

        val hakijaOid = hakemukset.find(_.getHakemusOid == v.getHakemusOid).head.getHakijaOid

        statement.setString(1, hakijaOid)
        statement.setString(2, hakukohdeOid)
        statement.setString(3, ilmoittautumistila.get.toString)
        statement.setString(4, ilmoittaja)
        statement.setString(5, selite)
        statement.addBatch
      })
      statement.executeBatch
  }

  private def insertSijoitteluajo(sijoitteluajo:SijoitteluAjo) = {
    val SijoitteluajoWrapper(sijoitteluajoId, hakuOid, startMils, endMils) = SijoitteluajoWrapper(sijoitteluajo)
    sqlu"""insert into sijoitteluajot (id, haku_oid, "start", "end")
             values (${sijoitteluajoId}, ${hakuOid},${new Timestamp(startMils)},${new Timestamp(endMils)})"""
  }

  private def insertHakukohde(hakuOid: String, hakukohde:Hakukohde) = {
    val SijoitteluajonHakukohdeWrapper(sijoitteluajoId, oid, tarjoajaOid, kaikkiJonotSijoiteltu) = SijoitteluajonHakukohdeWrapper(hakukohde)
    sqlu"""insert into sijoitteluajon_hakukohteet (sijoitteluajo_id, haku_oid, hakukohde_oid, tarjoaja_oid, kaikki_jonot_sijoiteltu)
             values (${sijoitteluajoId}, ${hakuOid}, ${oid}, ${tarjoajaOid}, ${kaikkiJonotSijoiteltu})"""
  }

  private def insertValintatapajono(sijoitteluajoId:Long, hakukohdeOid:String, valintatapajono:Valintatapajono) = {
    val SijoitteluajonValintatapajonoWrapper(oid, nimi, prioriteetti, tasasijasaanto, aloituspaikat, alkuperaisetAloituspaikat,
    eiVarasijatayttoa, kaikkiEhdonTayttavatHyvaksytaan, poissaOlevaTaytto, varasijat, varasijaTayttoPaivat,
    varasijojaKaytetaanAlkaen, varasijojaTaytetaanAsti, tayttojono, hyvaksytty, varalla, alinHyvaksyttyPistemaara, valintaesitysHyvaksytty)
    = SijoitteluajonValintatapajonoWrapper(valintatapajono)

    val varasijojaKaytetaanAlkaenTs:Option[Timestamp] = varasijojaKaytetaanAlkaen.flatMap(d => Option(new Timestamp(d.getTime)))
    val varasijojaTaytetaanAstiTs:Option[Timestamp] = varasijojaTaytetaanAsti.flatMap(d => Option(new Timestamp(d.getTime)))

    sqlu"""insert into valintatapajonot (oid, sijoitteluajo_id, hakukohde_oid, nimi, prioriteetti, tasasijasaanto, aloituspaikat,
           alkuperaiset_aloituspaikat, kaikki_ehdon_tayttavat_hyvaksytaan, poissaoleva_taytto, ei_varasijatayttoa,
           varasijat, varasijatayttopaivat, varasijoja_kaytetaan_alkaen, varasijoja_taytetaan_asti, tayttojono, hyvaksytty, varalla,
           alin_hyvaksytty_pistemaara, valintaesitys_hyvaksytty)
           values (${oid}, ${sijoitteluajoId}, ${hakukohdeOid}, ${nimi}, ${prioriteetti}, ${tasasijasaanto.toString}::tasasijasaanto, ${aloituspaikat},
           ${alkuperaisetAloituspaikat}, ${kaikkiEhdonTayttavatHyvaksytaan},
           ${poissaOlevaTaytto}, ${eiVarasijatayttoa}, ${varasijat}, ${varasijaTayttoPaivat},
           ${varasijojaKaytetaanAlkaenTs}, ${varasijojaTaytetaanAstiTs}, ${tayttojono},
           ${hyvaksytty}, ${varalla}, ${alinHyvaksyttyPistemaara}, ${valintaesitysHyvaksytty})"""
  }

  private def dateToTimestamp(date:Option[Date]): Timestamp = date match {
    case Some(d) => new java.sql.Timestamp(d.getTime)
    case None => null
  }

  private def insertHakijaryhma(sijoitteluajoId:Long, hakijaryhma:Hakijaryhma) = {
    val SijoitteluajonHakijaryhmaWrapper(oid, nimi, prioriteetti, kiintio, kaytaKaikki, tarkkaKiintio,
    kaytetaanRyhmaanKuuluvia, _, valintatapajonoOid, hakukohdeOid, hakijaryhmatyyppikoodiUri)
    = SijoitteluajonHakijaryhmaWrapper(hakijaryhma)

    sqlu"""insert into hakijaryhmat (oid, sijoitteluajo_id, hakukohde_oid, nimi, prioriteetti,
           kiintio, kayta_kaikki, tarkka_kiintio, kaytetaan_ryhmaan_kuuluvia,
           valintatapajono_oid, hakijaryhmatyyppikoodi_uri)
           values (${oid}, ${sijoitteluajoId}, ${hakukohdeOid}, ${nimi}, ${prioriteetti}, ${kiintio}, ${kaytaKaikki},
      ${tarkkaKiintio}, ${kaytetaanRyhmaanKuuluvia}, ${valintatapajonoOid}, ${hakijaryhmatyyppikoodiUri})"""
  }

  private def insertHakijaryhmanHakemus(hakijaryhmaOid:String, sijoitteluajoId:Long, hakemusOid:String, hyvaksyttyHakijaryhmasta:Boolean) = {
    sqlu"""insert into hakijaryhman_hakemukset (hakijaryhma_oid, sijoitteluajo_id, hakemus_oid, hyvaksytty_hakijaryhmasta)
           values (${hakijaryhmaOid}, ${sijoitteluajoId}, ${hakemusOid}, ${hyvaksyttyHakijaryhmasta})"""
  }

  override def getLatestSijoitteluajoId(hakuOid:String): Option[Long] = {
    runBlocking(
      sql"""select id
            from sijoitteluajot
            where haku_oid = ${hakuOid}
            order by id desc
            limit 1""".as[Long]).headOption
  }

  override def getSijoitteluajo(sijoitteluajoId: Long): Option[SijoitteluajoRecord] = {
    runBlocking(
      sql"""select id, haku_oid, start, sijoitteluajot.end
            from sijoitteluajot
            where id = ${sijoitteluajoId}""".as[SijoitteluajoRecord]).headOption
  }

  override def getSijoitteluajonHakukohteet(sijoitteluajoId: Long): List[SijoittelunHakukohdeRecord] = {
    runBlocking(
      sql"""select sh.sijoitteluajo_id, sh.hakukohde_oid, sh.tarjoaja_oid, sh.kaikki_jonot_sijoiteltu
            from sijoitteluajon_hakukohteet sh
            where sh.sijoitteluajo_id = ${sijoitteluajoId}
            group by sh.sijoitteluajo_id, sh.hakukohde_oid, sh.tarjoaja_oid, sh.kaikki_jonot_sijoiteltu""".as[SijoittelunHakukohdeRecord]).toList
  }

  override def getSijoitteluajonValintatapajonot(sijoitteluajoId: Long): List[ValintatapajonoRecord] = {
    runBlocking(
      sql"""select tasasijasaanto, oid, nimi, prioriteetti, aloituspaikat, alkuperaiset_aloituspaikat,
            alin_hyvaksytty_pistemaara, ei_varasijatayttoa, kaikki_ehdon_tayttavat_hyvaksytaan, poissaoleva_taytto,
            valintaesitys_hyvaksytty, hyvaksytty, varalla, varasijat,
            varasijatayttopaivat, varasijoja_kaytetaan_alkaen, varasijoja_taytetaan_asti, tayttojono, hakukohde_oid
            from valintatapajonot
            where sijoitteluajo_id = ${sijoitteluajoId}""".as[ValintatapajonoRecord]).toList
  }

  override def getSijoitteluajonHakemukset(sijoitteluajoId:Long): List[HakemusRecord] = {
    runBlocking(
      sql"""select j.hakija_oid, j.hakemus_oid, j.pisteet, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija,
            j.tasasijajonosija, vt.tila, v.tilankuvaus_hash, v.tarkenteen_lisatieto, j.hyvaksytty_harkinnanvaraisesti, j.varasijan_numero,
            j.onko_muuttunut_viime_sijoittelussa,
            j.siirtynyt_toisesta_valintatapajonosta, j.valintatapajono_oid
            from jonosijat as j
            join valinnantulokset as v
            on v.valintatapajono_oid = j.valintatapajono_oid
              and v.hakemus_oid = j.hakemus_oid
              and v.hakukohde_oid = j.hakukohde_oid
            join valinnantilat as vt
            on vt.valintatapajono_oid = v.valintatapajono_oid
              and vt.hakemus_oid = v.hakemus_oid
              and vt.hakukohde_oid = v.hakukohde_oid
            where j.sijoitteluajo_id = ${sijoitteluajoId}""".as[HakemusRecord], Duration(30, TimeUnit.SECONDS)).toList
  }

  override def getSijoitteluajonHakemuksetInChunks(sijoitteluajoId:Long, chunkSize:Int = 300): List[HakemusRecord] = {
    def readHakemukset(offset:Int = 0): List[HakemusRecord] = {
      runBlocking(sql"""
                     with vj as (
                       select oid from valintatapajonot where sijoitteluajo_id = ${sijoitteluajoId}
                       order by oid desc limit ${chunkSize} offset ${offset} )
                       select j.hakija_oid, j.hakemus_oid, j.pisteet, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija,
            j.tasasijajonosija, vt.tila, v.tilankuvaus_hash, v.tarkenteen_lisatieto, j.hyvaksytty_harkinnanvaraisesti, j.varasijan_numero,
            j.onko_muuttunut_viime_sijoittelussa,
            j.siirtynyt_toisesta_valintatapajonosta, j.valintatapajono_oid
            from jonosijat as j
            join valinnantulokset as v
            on v.valintatapajono_oid = j.valintatapajono_oid
              and v.hakemus_oid = j.hakemus_oid
              and v.hakukohde_oid = j.hakukohde_oid
            join valinnantilat as vt
            on vt.valintatapajono_oid = v.valintatapajono_oid
              and vt.hakemus_oid = v.hakemus_oid
              and vt.hakukohde_oid = v.hakukohde_oid
            inner join vj on vj.oid = j.valintatapajono_oid
            where j.sijoitteluajo_id = ${sijoitteluajoId}""".as[HakemusRecord]).toList match {
        case result if result.size == 0 => result
        case result => result ++ readHakemukset(offset + chunkSize)
      }
    }
    readHakemukset()
  }

  def getSijoitteluajonHakemustenHakijaryhmat(sijoitteluajoId:Long): Map[String,Set[String]] = {
    runBlocking(
      sql"""select hh.hakemus_oid, hr.oid as hakijaryhma
            from hakijaryhmat hr
            inner join hakijaryhman_hakemukset hh on hr.oid = hh.hakijaryhma_oid and hr.sijoitteluajo_id = hh.sijoitteluajo_id
            where hr.sijoitteluajo_id = ${sijoitteluajoId};""".as[(String,String)]).groupBy(_._1).map { case (k,v) => (k,v.map(_._2).toSet) }
  }

  override def getSijoitteluajonTilahistoriat(sijoitteluajoId:Long): List[TilaHistoriaRecord] = {
    runBlocking(
      sql"""select vt.valintatapajono_oid, vt.hakemus_oid, vt.tila, vt.tilan_viimeisin_muutos as luotu
            from valinnantilat as vt
            join jonosijat as j on j.hakukohde_oid = vt.hakukohde_oid
                and j.valintatapajono_oid = vt.valintatapajono_oid
                and j.hakemus_oid = vt.hakemus_oid
            join sijoitteluajot as s on s.id = j.sijoitteluajo_id
            where s.id = ${sijoitteluajoId}
                and s.system_time <@ vt.system_time
            union all
            select th.valintatapajono_oid, th.hakemus_oid, th.tila, th.tilan_viimeisin_muutos as luotu
            from valinnantilat_history as th
            join jonosijat as j on j.hakukohde_oid = th.hakukohde_oid
                and j.valintatapajono_oid = th.valintatapajono_oid
                and j.hakemus_oid = th.hakemus_oid
            join sijoitteluajot as s on s.id = j.sijoitteluajo_id
            where s.id = ${sijoitteluajoId}
                and s.system_time &> th.system_time
        """.as[TilaHistoriaRecord], Duration(1, TimeUnit.MINUTES)).toList
  }

  override def getValinnantilanKuvaukset(tilankuvausHashes:List[Int]): Map[Int,TilankuvausRecord] = tilankuvausHashes match {
    case x if 0 == tilankuvausHashes.size => Map()
    case _ => {
      val inParameter = tilankuvausHashes.map(id => s"'$id'").mkString(",")
      runBlocking(
        sql"""select hash, tilan_tarkenne, text_fi, text_sv, text_en
              from valinnantilan_kuvaukset
              where hash in (#${inParameter})""".as[TilankuvausRecord]).map(v => (v.hash, v)).toMap
    }
  }

  override def getSijoitteluajonHakijaryhmat(sijoitteluajoId: Long): List[HakijaryhmaRecord] = {
    runBlocking(
      sql"""select prioriteetti, oid, nimi, hakukohde_oid, kiintio, kayta_kaikki, sijoitteluajo_id,
            tarkka_kiintio, kaytetaan_ryhmaan_kuuluvia, valintatapajono_oid, hakijaryhmatyyppikoodi_uri
            from hakijaryhmat
            where sijoitteluajo_id = ${sijoitteluajoId}""".as[HakijaryhmaRecord]).toList
  }

  override def getSijoitteluajonHakijaryhmanHakemukset(hakijaryhmaOid: String, sijoitteluajoId:Long): List[String] = {
    runBlocking(
      sql"""select hakemus_oid
            from hakijaryhman_hakemukset
            where hakijaryhma_oid = ${hakijaryhmaOid} and sijoitteluajo_id = ${sijoitteluajoId}""".as[String]).toList
  }

  override def getHakemuksenHakija(hakemusOid: String, sijoitteluajoId: Long): Option[HakijaRecord] = {
    runBlocking(
      sql"""select etunimi, sukunimi, hakemus_oid, hakija_oid
            from jonosijat
            where hakemus_oid = ${hakemusOid} and sijoitteluajo_id = ${sijoitteluajoId}""".as[HakijaRecord]).headOption
  }

  override def getHakemuksenHakutoiveet(hakemusOid: String, sijoitteluajoId: Long): List[HakutoiveRecord] = {
    runBlocking(
      sql"""with j as (select * from jonosijat where hakemus_oid = ${hakemusOid} and sijoitteluajo_id = ${sijoitteluajoId})
            select j.hakemus_oid, j.prioriteetti, v.hakukohde_oid, sh.tarjoaja_oid, vt.tila, sh.kaikki_jonot_sijoiteltu
            from j
            left join valinnantulokset as v on v.hakemus_oid = j.hakemus_oid
                and v.valintatapajono_oid = j.valintatapajono_oid
                and v.hakukohde_oid = j.hakukohde_oid
            left join valinnantilat as vt on vt.hakemus_oid = v.hakemus_oid
                and vt.valintatapajono_oid = v.valintatapajono_oid
                and vt.hakukohde_oid = v.hakukohde_oid
            left join sijoitteluajon_hakukohteet as sh on sh.hakukohde_oid = v.hakukohde_oid
        """.as[HakutoiveRecord]).toList
  }

  override def getHakemuksenPistetiedot(hakemusOid:String, sijoitteluajoId:Long): List[PistetietoRecord] = {
    runBlocking(
      sql"""
         select valintatapajono_oid, hakemus_oid, tunniste, arvo, laskennallinen_arvo, osallistuminen
         from pistetiedot
         where sijoitteluajo_id = ${sijoitteluajoId} and hakemus_oid = ${hakemusOid} and deleted = false""".as[PistetietoRecord]).toList
  }

  override def getSijoitteluajonPistetiedot(sijoitteluajoId:Long): List[PistetietoRecord] = {
    runBlocking(sql"""
       select valintatapajono_oid, hakemus_oid, tunniste, arvo, laskennallinen_arvo, osallistuminen
       from  pistetiedot
       where sijoitteluajo_id = ${sijoitteluajoId} and deleted = false""".as[PistetietoRecord]).toList
  }

  override def getSijoitteluajonPistetiedotInChunks(sijoitteluajoId:Long, chunkSize:Int = 200): List[PistetietoRecord] = {
    def readPistetiedot(offset:Int = 0): List[PistetietoRecord] = {
      runBlocking(sql"""
                     with v as (
                       select oid from valintatapajonot where sijoitteluajo_id = ${sijoitteluajoId}
                       order by oid desc limit ${chunkSize} offset ${offset} )
                       select p.valintatapajono_oid, p.hakemus_oid, p.tunniste, p.arvo, p.laskennallinen_arvo, p.osallistuminen
                                from  pistetiedot p
                                inner join v on p.valintatapajono_oid = v.oid
                                where p.sijoitteluajo_id = ${sijoitteluajoId} and p.deleted = false""".as[PistetietoRecord]).toList match {
        case result if result.size == 0 => result
        case result => result ++ readPistetiedot(offset + chunkSize)
      }
    }
    readPistetiedot()
  }
}

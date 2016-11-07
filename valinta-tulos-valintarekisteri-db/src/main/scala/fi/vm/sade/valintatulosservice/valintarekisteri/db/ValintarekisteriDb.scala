package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigValueFactory}
import fi.vm.sade.sijoittelu.domain.{Hakukohde, SijoitteluAjo, Valintatapajono, Hakemus => SijoitteluHakemus, _}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.flywaydb.core.Flyway
import org.postgresql.util.PSQLException
import slick.driver.PostgresDriver.api.{Database, _}
import slick.jdbc.{GetResult, TransactionIsolation}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

class ValintarekisteriDb(dbConfig: Config, isItProfile:Boolean = false) extends ValintarekisteriService with HakijaVastaanottoRepository with SijoitteluRepository
  with HakukohdeRepository with VirkailijaVastaanottoRepository with Logging {
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
  private implicit val getVastaanottoResult = GetResult(r => VastaanottoRecord(r.nextString(), r.nextString(),
    r.nextString(), VastaanottoAction(r.nextString()), r.nextString(), r.nextTimestamp()))
  private implicit val getHakukohdeResult = GetResult(r =>
    HakukohdeRecord(r.nextString(), r.nextString(), r.nextBoolean(), r.nextBoolean(), Kausi(r.nextString())))
  private implicit val getHakijaResult = GetResult(r => HakijaRecord(r.<<, r.<<, r.<<, r.<<))
  private implicit val getHakutoiveResult = GetResult(r => HakutoiveRecord(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))
  private implicit val getPistetiedotResult = GetResult(r => PistetietoRecord(r.<<, r.<<, r.<<, r.<<, r.<<))
  private implicit val getSijoitteluajoResult = GetResult(r => SijoitteluajoRecord(r.<<, r.<<,
    r.nextTimestamp().getTime, r.nextTimestamp().getTime))
  private implicit val getSijoitteluajoHakukohteetResult = GetResult(r => SijoittelunHakukohdeRecord(r.<<, r.<<, r.<<, r.<<, r.<<))
  private implicit val getValintatapajonotResult = GetResult(r => ValintatapajonoRecord(r.<<, r.<<, r.<<, r.<<,
    r.<<, r.<<, r.<<, r.<<, r.<<, r.<< , r.<<, r.<< , r.<<, r.<< , r.<<, r.<< , r.<<, r.<<, r.<<))
  private implicit val getHakemuksetForValintatapajonosResult = GetResult(r => HakemusRecord(r.<<, r.<<, r.<<, r.<<,
    r.<<, r.<<, r.<<, r.<<, Valinnantila(r.<<), r.<< , r.<<, r.<< , r.<<, r.<< , r.<<, r.<< , r.<<, r.<<))
  private implicit val getHakemuksenTilahistoriaResult = GetResult(r => TilaHistoriaRecord(r.<<, r.<<, r.<<, r.<<))
  private implicit val getHakijaryhmatResult = GetResult(r => HakijaryhmaRecord(r.<<, r.<<, r.<<, r.<<, r.<<,
    r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

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

  override def findHakukohteet(hakukohdeOids: Set[String]): Set[HakukohdeRecord] = {
    val invalidOids = hakukohdeOids.filterNot(OidValidator.isOid)
    if (invalidOids.nonEmpty) {
      throw new IllegalArgumentException(s"${invalidOids.size} huonoa oidia syötteessä: $invalidOids")
    }
    val inParameter = hakukohdeOids.map(oid => s"'$oid'").mkString(",")
    runBlocking(
      sql"""select hakukohde_oid, haku_oid, yhden_paikan_saanto_voimassa, kk_tutkintoon_johtava, koulutuksen_alkamiskausi
            from hakukohteet where hakukohde_oid in (#$inParameter)""".as[HakukohdeRecord]).toSet
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
  
  override def storeSijoitteluajo(sijoitteluajo:SijoitteluAjo): Unit = {
    runBlocking(insertSijoitteluajo(sijoitteluajo))
  }

  override def storeSijoittelu(sijoittelu: SijoitteluWrapper) = {
    runBlocking(insertSijoitteluajo(sijoittelu.sijoitteluajo).andThen(
      DBIO.sequence(sijoittelu.hakukohteet.map(hakukohde =>
        storeSijoittelunHakukohde(sijoittelu.sijoitteluajo.getSijoitteluajoId, hakukohde,
          sijoittelu.valintatulokset.filter(vt => vt.getHakukohdeOid == hakukohde.getOid))
      ))
    )
    )
  }

  import scala.collection.JavaConverters._

  def storeSijoittelunHakukohde(sijoitteluajoId:Long, hakukohde: Hakukohde, valintatulokset: List[Valintatulos]) = {
    insertHakukohde(hakukohde).flatMap(id =>
      DBIO.sequence(
        hakukohde.getValintatapajonot.asScala.map(valintatapajono =>
          storeSijoittelunValintatapajono(id.get, hakukohde.getOid, sijoitteluajoId, valintatapajono,
            valintatulokset.filter(_.getValintatapajonoOid == valintatapajono.getOid))).toList ++
          hakukohde.getHakijaryhmat.asScala.map(hakijaryhma => storeSijoittelunHakijaryhma(id.get, hakijaryhma)).toList)
    )
  }

  def storeSijoittelunHakijaryhma(valintatapajonoId:Long, hakijaryhma: Hakijaryhma) = {
    insertHakijaryhma(valintatapajonoId, hakijaryhma).flatMap(hakijaryhmaId =>
      DBIO.sequence(hakijaryhma.getHakemusOid.asScala.map(hakemusOid => insertHakijaryhmanHakemus(hakijaryhmaId.get, hakemusOid)).toList)
    )
  }

  def storeSijoittelunValintatapajono(hakukohdeId:Long, hakukohdeOid:String, sijoitteluajoId:Long, valintatapajono:Valintatapajono, valintatulokset: List[Valintatulos]) = {
    insertValintatapajono(hakukohdeId, valintatapajono).andThen(
      DBIO.sequence(valintatapajono.getHakemukset.asScala.map(hakemus =>
        storeSijoittelunJonosija(hakukohdeId, hakukohdeOid, sijoitteluajoId, valintatapajono.getOid, hakemus,
          valintatulokset.find(_.getHakemusOid == hakemus.getHakemusOid)
        )).toList
      ))
  }

  def storeSijoittelunJonosija(hakukohdeId:Long, hakukohdeOid:String, sijoitteluajoId:Long, valintatapajonoOid:String, hakemus:SijoitteluHakemus, valintatulos:Option[Valintatulos]) = {
    if(valintatulos.isDefined) {
      insertJonosija(valintatapajonoOid, hakukohdeId, hakemus).flatMap(jonosijaId => {
        DBIO.sequence(
          List(insertValinnantulos(sijoitteluajoId, jonosijaId.get, valintatulos.get, hakemus),
            insertIlmoittautuminen(valintatulos.get, hakemus.getHakijaOid)) ++
            hakemus.getPistetiedot.asScala.map(pistetieto => insertPistetieto(jonosijaId.get, pistetieto)).toList
        )
      })
    } else {
      insertJonosija(valintatapajonoOid, hakukohdeId, hakemus).flatMap(jonosijaId => {
        DBIO.sequence(
          List(insertValinnantulos(sijoitteluajoId, jonosijaId.get, SijoitteluajonValinnantulosWrapper(
            valintatapajonoOid, hakemus.getHakemusOid, hakukohdeOid, false, false, false, false, None, Option(List())).valintatulos, hakemus)) ++
            hakemus.getPistetiedot.asScala.map(pistetieto => insertPistetieto(jonosijaId.get, pistetieto)).toList
        )
      })
    }
  }

  private def insertSijoitteluajo(sijoitteluajo:SijoitteluAjo) = {
    val SijoitteluajoWrapper(sijoitteluajoId, hakuOid, startMils, endMils) = SijoitteluajoWrapper(sijoitteluajo)
    sqlu"""insert into sijoitteluajot (id, haku_oid, "start", "end")
             values (${sijoitteluajoId}, ${hakuOid},${new Timestamp(startMils)},${new Timestamp(endMils)})"""
  }

  private def insertHakukohde(hakukohde:Hakukohde) = {
    val SijoitteluajonHakukohdeWrapper(sijoitteluajoId, oid, tarjoajaOid, kaikkiJonotSijoiteltu) = SijoitteluajonHakukohdeWrapper(hakukohde)
    sql"""insert into sijoitteluajon_hakukohteet (sijoitteluajo_id, hakukohde_oid, tarjoaja_oid, kaikki_jonot_sijoiteltu)
             values (${sijoitteluajoId}, ${oid}, ${tarjoajaOid}, ${kaikkiJonotSijoiteltu}) RETURNING id""".as[Long].headOption
  }

  private def insertValintatapajono(hakukohdeId:Long, valintatapajono:Valintatapajono) = {
    val SijoitteluajonValintatapajonoWrapper(oid, nimi, prioriteetti, tasasijasaanto, aloituspaikat, alkuperaisetAloituspaikat,
    eiVarasijatayttoa, kaikkiEhdonTayttavatHyvaksytaan, poissaOlevaTaytto, varasijat, varasijaTayttoPaivat,
    varasijojaKaytetaanAlkaen, varasijojaTaytetaanAsti, tayttojono, hyvaksytty, varalla, alinHyvaksyttyPistemaara, valintaesitysHyvaksytty)
    = SijoitteluajonValintatapajonoWrapper(valintatapajono)

    val varasijojaKaytetaanAlkaenTs:Option[Timestamp] = varasijojaKaytetaanAlkaen.flatMap(d => Option(new Timestamp(d.getTime)))
    val varasijojaTaytetaanAstiTs:Option[Timestamp] = varasijojaTaytetaanAsti.flatMap(d => Option(new Timestamp(d.getTime)))

    sqlu"""insert into valintatapajonot (oid, sijoitteluajon_hakukohde_id, nimi, prioriteetti, tasasijasaanto, aloituspaikat,
           alkuperaiset_aloituspaikat, kaikki_ehdon_tayttavat_hyvaksytaan, poissaoleva_taytto, ei_varasijatayttoa,
           varasijat, varasijatayttopaivat, varasijoja_kaytetaan_alkaen, varasijoja_taytetaan_asti, tayttojono, hyvaksytty, varalla, alin_hyvaksytty_pistemaara)
           values (${oid}, ${hakukohdeId}, ${nimi}, ${prioriteetti}, ${tasasijasaanto.toString}::tasasijasaanto, ${aloituspaikat},
           ${alkuperaisetAloituspaikat}, ${kaikkiEhdonTayttavatHyvaksytaan},
           ${poissaOlevaTaytto}, ${eiVarasijatayttoa}, ${varasijat}, ${varasijaTayttoPaivat},
           ${varasijojaKaytetaanAlkaenTs}, ${varasijojaTaytetaanAstiTs}, ${tayttojono},
           ${hyvaksytty}, ${varalla}, ${alinHyvaksyttyPistemaara})"""
  }

  private def insertJonosija(valintatapajonoOid:String, hakukohdeId:Long, hakemus:SijoitteluHakemus) = {
    val SijoitteluajonHakemusWrapper(hakemusOid, hakijaOid, etunimi, sukunimi, prioriteetti, jonosija, varasijanNumero,
    onkoMuuttunutViimeSijoittelussa, pisteet, tasasijaJonosija, hyvaksyttyHarkinnanvaraisesti,
    hyvaksyttyHakijaryhmasta, siirtynytToisestaValintatapajonosta, _, _)
    = SijoitteluajonHakemusWrapper(hakemus)

    sql"""insert into jonosijat (valintatapajono_oid, sijoitteluajon_hakukohde_id, hakemus_oid, hakija_oid, etunimi, sukunimi, prioriteetti,
           jonosija, varasijan_numero, onko_muuttunut_viime_sijoittelussa, pisteet, tasasijajonosija, hyvaksytty_harkinnanvaraisesti,
           hyvaksytty_hakijaryhmasta, siirtynyt_toisesta_valintatapajonosta)
           values (${valintatapajonoOid}, ${hakukohdeId}, ${hakemusOid}, ${hakijaOid}, ${etunimi}, ${sukunimi}, ${prioriteetti},
           ${jonosija}, ${varasijanNumero}, ${onkoMuuttunutViimeSijoittelussa}, ${pisteet}, ${tasasijaJonosija},
           ${hyvaksyttyHarkinnanvaraisesti},
           ${hyvaksyttyHakijaryhmasta}, ${siirtynytToisestaValintatapajonosta}) RETURNING id""".as[Long].headOption
  }

  private def insertValinnantulos(sijoitteluajoId:Long, jonosijaId:Long, valintatulos:Valintatulos, hakemus:SijoitteluHakemus) = {
    val SijoitteluajonHakemusWrapper(hakemusOid, hakijaOid, etunimi, sukunimi, prioriteetti, jonosija, varasijanNumero,
    onkoMuuttunutViimeSijoittelussa, pisteet, tasasijaJonosija, hyvaksyttyHarkinnanvaraisesti,
    hyvaksyttyHakijaryhmasta, siirtynytToisestaValintatapajonosta, valinnanTila, valinnanTilanTarkenne)
    = SijoitteluajonHakemusWrapper(hakemus)
    val SijoitteluajonValinnantulosWrapper(valintatapajonoOid, _, hakukohdeOid, ehdollisestiHyvaksyttavissa,
    julkaistavissa, hyvaksyttyVarasijalta, hyvaksyPeruuntunut, ilmoittautumistila, originalLogEntries)
    = SijoitteluajonValinnantulosWrapper(valintatulos)

    val(ilmoittaja, selite, tilanViimeisinMuutos) = valintatulos.getOriginalLogEntries.asScala.filter(e => e.getLuotu != null).sortBy(_.getLuotu).reverse.headOption match {
      case Some(entry) => (entry.getMuokkaaja, entry.getSelite, entry.getLuotu)
      case None => ("System", "", new Date())
    }

    val valinnantilanTarkenneString = valinnanTilanTarkenne.flatMap(x => Some(x.tarkenneString))
    val valinnantilanTarkenteenLisatieto = valinnanTilanTarkenne match {
      case Some(HyvaksyttyTayttojonoSaannolla) => valintatulos.getValintatapajonoOid
      case Some(HylattyHakijaryhmaanKuulumattomana) => hakemus.getHakijaryhmaOid
      case _ => null
    }

    sqlu"""insert into valinnantulokset (hakukohde_oid, valintatapajono_oid, hakemus_oid, sijoitteluajo_id, jonosija_id,
           tila, tarkenne, tarkenteen_lisatieto, julkaistavissa, ehdollisesti_hyvaksyttavissa, hyvaksytty_varasijalta,
           hyvaksy_peruuntunut, ilmoittaja, selite, tilan_viimeisin_muutos)
           values (${hakukohdeOid}, ${valintatapajonoOid}, ${hakemusOid}, ${sijoitteluajoId}, ${jonosijaId},
           ${valinnanTila.toString}::valinnantila, ${valinnantilanTarkenneString}::valinnantilanTarkenne,
           ${valinnantilanTarkenteenLisatieto}, ${julkaistavissa}, ${ehdollisestiHyvaksyttavissa}, ${hyvaksyttyVarasijalta},
           ${hyvaksyPeruuntunut}, ${ilmoittaja}, ${selite}, ${new java.sql.Timestamp(tilanViimeisinMuutos.getTime)})"""
  }

  private def insertIlmoittautuminen(valintatulos:Valintatulos, hakijaOid:String) = {
    val SijoitteluajonValinnantulosWrapper(_, _, hakukohdeOid, _, _, _, _, ilmoittautumistila,logEntries)
    = SijoitteluajonValinnantulosWrapper(valintatulos)

    val(ilmoittaja, selite) = valintatulos.getOriginalLogEntries.asScala.filter(e => e.getLuotu != null).sortBy(_.getLuotu).reverse.headOption match {
      case Some(entry) => (entry.getMuokkaaja, entry.getSelite)
      case None => ("System", "")
    }

    sqlu"""insert into ilmoittautumiset (henkilo, hakukohde, tila, ilmoittaja, selite)
           values (${hakijaOid}, ${hakukohdeOid}, ${ilmoittautumistila.get.toString}::ilmoittautumistila, ${ilmoittaja}, ${selite})"""
  }

  private def insertPistetieto(jonosijaId:Long, pistetieto: Pistetieto) = {
    val SijoitteluajonPistetietoWrapper(tunniste, arvo, laskennallinenArvo, osallistuminen)
    = SijoitteluajonPistetietoWrapper(pistetieto)

    sqlu"""insert into pistetiedot (jonosija_id, tunniste, arvo, laskennallinen_arvo, osallistuminen)
           values (${jonosijaId}, ${tunniste}, ${arvo}, ${laskennallinenArvo}, ${osallistuminen})"""
  }

  private def insertHakijaryhma(sijoitteluajonHakukohdeId:Long, hakijaryhma:Hakijaryhma) = {
    val SijoitteluajonHakijaryhmaWrapper(oid, nimi, prioriteetti, paikat, kiintio,
    kaytaKaikki, tarkkaKiintio, kaytetaanRyhmaanKuuluvia, alinHyvaksyttyPistemaara, _)
    = SijoitteluajonHakijaryhmaWrapper(hakijaryhma)

    sql"""insert into hakijaryhmat (oid, sijoitteluajon_hakukohde_id, nimi, prioriteetti, paikat,
           kiintio, kayta_kaikki, tarkka_kiintio, kaytetaan_ryhmaan_kuuluvia, alin_hyvaksytty_pistemaara)
           values (${oid}, ${sijoitteluajonHakukohdeId}, ${nimi}, ${prioriteetti}, ${paikat},
           ${kiintio}, ${kaytaKaikki}, ${tarkkaKiintio}, ${kaytetaanRyhmaanKuuluvia},
           ${alinHyvaksyttyPistemaara})
           RETURNING id""".as[Long].headOption
  }

  private def insertHakijaryhmanHakemus(hakijaryhmaId:Long, hakemusOid:String) = {
    sqlu"""insert into hakijaryhman_hakemukset (hakijaryhma_id, hakemus_oid) values (${hakijaryhmaId}, ${hakemusOid})"""
  }

  override def getLatestSijoitteluajoId(hakuOid:String): Option[Long] = {
    runBlocking(
      sql"""select id
            from sijoitteluajot
            where haku_oid = ${hakuOid}
            order by id desc
            limit 1""".as[Long]).headOption
  }

  override def getSijoitteluajo(hakuOid: String, sijoitteluajoId: Long): Option[SijoitteluajoRecord] = {
    runBlocking(
      sql"""select id, haku_oid, start, sijoitteluajot.end
            from sijoitteluajot
            where id = ${sijoitteluajoId} and haku_oid = ${hakuOid}""".as[SijoitteluajoRecord]).headOption
  }

  override def getSijoitteluajoHakukohteet(sijoitteluajoId: Long): Option[List[SijoittelunHakukohdeRecord]] = {
    // If there are perf problems it's in this subquery!
    Option(runBlocking(
      sql"""select sh.sijoitteluajo_id, h.hakukohde_oid, sh.tarjoaja_oid, sh.kaikki_jonot_sijoiteltu,
              (select min(hr.alin_hyvaksytty_pistemaara)
               from hakijaryhmat as hr
               left join sijoitteluajon_hakukohteet as sh on sh.id = hr.sijoitteluajon_hakukohde_id
               where sh.sijoitteluajo_id = ${sijoitteluajoId}) as ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet
            from hakukohteet as h
            left join sijoitteluajon_hakukohteet as sh on h.hakukohde_oid = sh.hakukohde_oid
            where sh.sijoitteluajo_id = ${sijoitteluajoId}""".as[SijoittelunHakukohdeRecord]).toList)
  }

  override def getValintatapajonot(sijoitteluajoId: Long): Option[List[ValintatapajonoRecord]] = {
    Option(runBlocking(
      sql"""select v.tasasijasaanto, v.oid, v.nimi, v.prioriteetti, v.aloituspaikat, v.alkuperaiset_aloituspaikat,
            v.alin_hyvaksytty_pistemaara, v.ei_varasijatayttoa, v.kaikki_ehdon_tayttavat_hyvaksytaan, v.poissaoleva_taytto,
            v.valintaesitys_hyvaksytty, (v.hyvaksytty + v.varalla) as hakeneet, v.hyvaksytty, v.varalla, v.varasijat,
            v.varasijatayttopaivat, v.varasijoja_kaytetaan_alkaen, v.varasijoja_taytetaan_asti, v.tayttojono
            from valintatapajonot as v
            left join sijoitteluajon_hakukohteet as sh on sh.id = v.sijoitteluajon_hakukohde_id
            where sh.sijoitteluajo_id = ${sijoitteluajoId}""".as[ValintatapajonoRecord]).toList)
  }

  override def getHakemuksetForValintatapajonos(valintatapajonoOids: List[String]): Option[List[HakemusRecord]] = {
    val inParameter = valintatapajonoOids.map(oid => s"'$oid'").mkString(",")
    Option(runBlocking(
      sql"""select distinct on (j.hakija_oid, j.hakemus_oid) j.hakija_oid, j.hakemus_oid, j.pisteet, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija,
            j.tasasijajonosija, v.tila, v.tarkenne, v.tarkenteen_lisatieto, j.hyvaksytty_harkinnanvaraisesti, j.varasijan_numero,
            j.onko_muuttunut_viime_sijoittelussa, j.hyvaksytty_hakijaryhmasta, hh.hakijaryhma_id,
            j.siirtynyt_toisesta_valintatapajonosta, j.valintatapajono_oid
            from jonosijat as j
            inner join valinnantulokset as v on v.jonosija_id = j.id and v.hakemus_oid = j.hakemus_oid
            inner join hakijaryhman_hakemukset as hh on j.hakemus_oid = hh.hakemus_oid
            where j.valintatapajono_oid in (#${inParameter}) and v.deleted is null""".as[HakemusRecord]).toList)
  }

  override def getHakemuksenTilahistoria(valintatapajonoOid:String, hakemusOid: String): List[TilaHistoriaRecord] = {
    runBlocking(
      sql"""select v.tila, dv.poistaja, dv.selite, dv.timestamp
            from valinnantulokset as v
            left join deleted_valinnantulokset as dv on v.deleted = dv.id
            where v.valintatapajono_oid = ${valintatapajonoOid} and v.hakemus_oid = ${hakemusOid} and v.deleted is not null
            order by dv.timestamp desc""".as[TilaHistoriaRecord]).toList
  }

  override def getHakijaryhmat(sijoitteluajoId: Long): List[HakijaryhmaRecord] = {
    runBlocking(
      sql"""select hr.id, hr.prioriteetti, hr.paikat, hr.oid, hr.nimi, sh.hakukohde_oid, hr.kiintio, hr.kayta_kaikki,
            hr.tarkka_kiintio, hr.kaytetaan_ryhmaan_kuuluvia, v.oid
            from hakijaryhmat as hr
            inner join sijoitteluajon_hakukohteet as sh on sh.id = hr.sijoitteluajon_hakukohde_id
            inner join valintatapajonot as v on v.sijoitteluajon_hakukohde_id = sh.id
            where sh.sijoitteluajo_id = ${sijoitteluajoId}""".as[HakijaryhmaRecord]).toList
  }

  override def getHakijaryhmanHakemukset(hakijaryhmaId: Long): List[String] = {
    runBlocking(
      sql"""select hakemus_oid
            from hakijaryhman_hakemukset
            where hakijaryhma_id = ${hakijaryhmaId}""".as[String]).toList
  }

  override def getHakija(hakemusOid: String, sijoitteluajoId: Long): Option[HakijaRecord] = {
    runBlocking(
      sql"""select j.etunimi, j.sukunimi, j.hakemus_oid, j.hakija_oid
            from jonosijat as j
            left join valintatapajonot as v on v.oid = j.valintatapajono_oid
            left join sijoitteluajon_hakukohteet as sh on sh.id = v.sijoitteluajon_hakukohde_id
            where j.hakemus_oid = ${hakemusOid} and sh.sijoitteluajo_id = ${sijoitteluajoId}""".as[HakijaRecord]).headOption
  }

  override def getHakutoiveet(hakemusOid: String, sijoitteluajoId: Long): List[HakutoiveRecord] = {
    runBlocking(
      sql"""select j.id, j.prioriteetti, vt.hakukohde_oid, sh.tarjoaja_oid, vt.tila, sh.kaikki_jonot_sijoiteltu
            from jonosijat as j
            left join valinnantulokset as vt on vt.jonosija_id = j.id and vt.hakemus_oid = j.hakemus_oid
            left join valintatapajonot as v on v.oid = j.valintatapajono_oid
            left join sijoitteluajon_hakukohteet as sh on sh.id = v.sijoitteluajon_hakukohde_id
            where j.hakemus_oid = ${hakemusOid} and sh.sijoitteluajo_id = ${sijoitteluajoId}""".as[HakutoiveRecord]).toList
  }

  override def getPistetiedot(jonosijaIds: List[Int]): List[PistetietoRecord] = {
    val inParameter = jonosijaIds.map(id => s"'$id'").mkString(",")
    runBlocking(
      sql"""select *
            from pistetiedot
            where jonosija_id in (#${inParameter})""".as[PistetietoRecord]).toList
  }

}



/*import java.sql.Timestamp
import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigValueFactory, Config}
import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.SijoitteluajoWrapper
//import org.flywaydb.core.Flyway
import slick.dbio._
import slick.driver.PostgresDriver.api.{Database, _}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ValintarekisteriDb(dbConfig: Config) extends SijoitteluRepository with Logging {
  val user = if (dbConfig.hasPath("user")) dbConfig.getString("user") else null
  val password = if (dbConfig.hasPath("password")) dbConfig.getString("password") else null
  logger.info(s"Database configuration: ${dbConfig.withValue("password", ConfigValueFactory.fromAnyRef("***"))}")
  //val flyway = new Flyway()
  //flyway.setDataSource(dbConfig.getString("url"), user, password)
  //flyway.migrate()
  override val db = Database.forConfig("", dbConfig)

  def runBlocking[R](operations: slick.dbio.DBIO[R], timeout: Duration = Duration(20, TimeUnit.SECONDS)) = Await.result(db.run(operations), timeout)

  override def storeSijoitteluajo(sijoitteluajo:SijoitteluAjo): Unit = {
    runBlocking(insertSijoitteluajo(sijoitteluajo))
  }

  private def insertSijoitteluajo(sijoitteluajo:SijoitteluAjo) = {
    val SijoitteluajoWrapper(sijoitteluajoId, hakuOid, startMils, endMils) = SijoitteluajoWrapper(sijoitteluajo)
    sqlu"""insert into sijoitteluajot (id, haku_oid, "start", "end")
             values (${sijoitteluajoId}, ${hakuOid},${new Timestamp(startMils)},${new Timestamp(endMils)})"""
  }
}*/

package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain.{Kausi, VastaanotaSitovasti, VastaanottoEvent, VastaanottoRecord}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSpec extends Specification with ITSetup with BeforeAfterExample {
  sequential
  private lazy val db = new ValintarekisteriDb(appConfig.settings.valintaRekisteriDbConfig)
  private val henkiloOid = "1.2.246.562.24.00000000001"
  private val hakukohdeOid = "1.2.246.561.20.00000000001"
  private val otherHakukohdeOid = "1.2.246.561.20.00000000002"
  private val hakuOid = "1.2.246.561.29.00000000001"
  private val otherHakuOid = "1.2.246.561.29.00000000002"

  step(appConfig.start)
  step(db.runBlocking(DBIOAction.seq(
    sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
           values ($hakukohdeOid, $hakuOid, true, true, '2015K')""",
    sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
               values ($otherHakukohdeOid, $otherHakuOid, true, true, '2015S')""")))

  "ValintarekisteriDb" should {
    "store vastaanotto actions" in {
      db.store(VastaanottoEvent(henkiloOid, hakukohdeOid, VastaanotaSitovasti))
      val henkiloOidsAndActionsFromDb = db.runBlocking(
        sql"""select henkilo, action from vastaanotot
              where henkilo = $henkiloOid and hakukohde = $hakukohdeOid""".as[(String, String)])
      henkiloOidsAndActionsFromDb must have size 1
      henkiloOidsAndActionsFromDb.head mustEqual (henkiloOid, VastaanotaSitovasti.toString)
    }

    "find vastaanotot rows of person for given haku" in {
      db.store(VastaanottoEvent(henkiloOid, hakukohdeOid, VastaanotaSitovasti))
      db.store(VastaanottoEvent(henkiloOid, otherHakukohdeOid, VastaanotaSitovasti))
      db.store(VastaanottoEvent(henkiloOid + "2", hakukohdeOid, VastaanotaSitovasti))
      val vastaanottoRowsFromDb = db.findHenkilonVastaanototHaussa(henkiloOid, hakuOid)
      vastaanottoRowsFromDb must have size 1
      val VastaanottoRecord(henkiloOidFromDb, hakuOidFromDb, hakukohdeOidFromDb, actionFromDb,
        ilmoittajaFromDb, timestampFromDb) = vastaanottoRowsFromDb.head
      henkiloOidFromDb mustEqual henkiloOid
      hakuOidFromDb mustEqual hakuOid
      hakukohdeOidFromDb mustEqual hakukohdeOid
      actionFromDb mustEqual VastaanotaSitovasti
      ilmoittajaFromDb mustEqual henkiloOid
      timestampFromDb.before(new Date()) mustEqual true
    }

    "find vastaanotot rows leading to higher education degrees of person" in {
      db.store(VastaanottoEvent(henkiloOid, hakukohdeOid, VastaanotaSitovasti))
      db.store(VastaanottoEvent(henkiloOid + "2", hakukohdeOid, VastaanotaSitovasti))
      db.runBlocking(sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
                       values (${hakukohdeOid + "1"}, ${hakuOid + "1"}, false, false, '2015K')""")
      db.store(VastaanottoEvent(henkiloOid, hakukohdeOid + "1", VastaanotaSitovasti))
      val recordsFromDb = db.findKkTutkintoonJohtavatVastaanotot(henkiloOid, Kausi("2015K"))
      recordsFromDb must have size 1
      recordsFromDb.head.hakukohdeOid mustEqual hakukohdeOid
    }
  }

  private def cleanVastaanottosFromDb(): Unit = {
    db.db.run(sqlu"delete from vastaanotot")
  }

  override protected def before: Unit = {
    cleanVastaanottosFromDb()
  }

  override protected def after: Unit = {
    cleanVastaanottosFromDb()
  }
}

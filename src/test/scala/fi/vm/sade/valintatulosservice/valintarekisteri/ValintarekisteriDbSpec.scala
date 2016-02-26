package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSpec extends Specification with ITSetup with BeforeAfterExample {
  sequential
  private val henkiloOid = "1.2.246.562.24.00000000001"
  private val hakemusOid = "1.2.246.562.99.00000000001"
  private val hakukohdeOid = "1.2.246.561.20.00000000001"
  private val otherHakukohdeOid = "1.2.246.561.20.00000000002"
  private val otherHakukohdeOidForHakuOid = "1.2.246.561.20.00000000003"
  private val hakuOid = "1.2.246.561.29.00000000001"
  private val otherHakuOid = "1.2.246.561.29.00000000002"

  step(appConfig.start)
  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
  step(singleConnectionValintarekisteriDb.runBlocking(DBIOAction.seq(
    sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
           values ($hakukohdeOid, $hakuOid, true, true, '2015K')""",
    sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
               values ($otherHakukohdeOid, $otherHakuOid, true, true, '2015S')""",
    sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
               values ($otherHakukohdeOidForHakuOid, $hakuOid, true, true, '2015K')""")))

  "ValintarekisteriDb" should {
    "store vastaanotto actions" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid))
      val henkiloOidsAndActionsFromDb = singleConnectionValintarekisteriDb.runBlocking(
        sql"""select henkilo, action from vastaanotot
              where henkilo = $henkiloOid and hakukohde = $hakukohdeOid""".as[(String, String)])
      henkiloOidsAndActionsFromDb must have size 1
      henkiloOidsAndActionsFromDb.head mustEqual (henkiloOid, VastaanotaSitovasti.toString)
    }

    "find vastaanotot rows of person for given haku" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid))
      val vastaanottoRowsFromDb = singleConnectionValintarekisteriDb.findHenkilonVastaanototHaussa(henkiloOid, hakuOid)
      vastaanottoRowsFromDb must have size 2
      val a = vastaanottoRowsFromDb.find(_.hakukohdeOid == hakukohdeOid).get
      a.henkiloOid mustEqual henkiloOid
      a.hakuOid mustEqual hakuOid
      a.hakukohdeOid mustEqual hakukohdeOid
      a.action mustEqual VastaanotaSitovasti
      a.ilmoittaja mustEqual henkiloOid
      a.timestamp.before(new Date()) must beTrue
      val b = vastaanottoRowsFromDb.find(_.hakukohdeOid == otherHakukohdeOidForHakuOid).get
      b.henkiloOid mustEqual henkiloOid
      b.hakuOid mustEqual hakuOid
      b.hakukohdeOid mustEqual otherHakukohdeOidForHakuOid
      b.action mustEqual VastaanotaSitovasti
      b.ilmoittaja mustEqual henkiloOid
      b.timestamp.before(new Date()) must beTrue
    }

    "find vastaanotot rows of person for given hakukohde" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid))
      val vastaanottoRowsFromDb = singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohdeOid)
      vastaanottoRowsFromDb must beSome
      val VastaanottoRecord(henkiloOidFromDb, hakuOidFromDb, hakukohdeOidFromDb, actionFromDb,
        ilmoittajaFromDb, timestampFromDb) = vastaanottoRowsFromDb.get
      henkiloOidFromDb mustEqual henkiloOid
      hakuOidFromDb mustEqual hakuOid
      hakukohdeOidFromDb mustEqual hakukohdeOid
      actionFromDb mustEqual VastaanotaSitovasti
      ilmoittajaFromDb mustEqual henkiloOid
      timestampFromDb.before(new Date()) mustEqual true
    }

    "find vastaanotot rows leading to higher education degrees of person" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOidForHakuOid, VastaanotaEhdollisesti, henkiloOid))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid))
      singleConnectionValintarekisteriDb.runBlocking(sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
                       values (${hakukohdeOid + "1"}, ${hakuOid + "1"}, false, false, '2015K')""")
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid + "1", VastaanotaSitovasti, henkiloOid))
      val recordsFromDb = singleConnectionValintarekisteriDb.findKkTutkintoonJohtavatVastaanotot(henkiloOid, Kausi("2015K"))
      recordsFromDb must have size 2
      recordsFromDb.find(_.hakukohdeOid == hakukohdeOid).map(_.action) must beSome(VastaanotaSitovasti)
      recordsFromDb.find(_.hakukohdeOid == otherHakukohdeOidForHakuOid).map(_.action) must beSome(VastaanotaEhdollisesti)
    }
  }

  override protected def before: Unit = ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb)
  override protected def after: Unit = ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb)

  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
}

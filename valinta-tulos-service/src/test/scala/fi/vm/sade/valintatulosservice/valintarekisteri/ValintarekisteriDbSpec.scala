package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{EiEnsikertalainen, Ensikertalainen}
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
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      val henkiloOidsAndActionsFromDb = singleConnectionValintarekisteriDb.runBlocking(
        sql"""select henkilo, action from vastaanotot
              where henkilo = $henkiloOid and hakukohde = $hakukohdeOid""".as[(String, String)])
      henkiloOidsAndActionsFromDb must have size 1
      henkiloOidsAndActionsFromDb.head mustEqual (henkiloOid, VastaanotaSitovasti.toString)
    }

    "find vastaanotot rows of person for given haku" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
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
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid + "2", "testiselite"))
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

    "find vastaanotot rows of person for given hakukohde, consider sibling henkilot" in {
      val henkiloOidA = "1.2.246.562.24.0000000000a"
      val henkiloOidB = "1.2.246.562.24.0000000000b"
      val henkiloOidC = "1.2.246.562.24.0000000000c"
      singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidB)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidC)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidA)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, '1.2.246.562.24.0000000000d')"""
      ))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidA, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOidA, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidA, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOidA, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidB, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOidB, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid + "2", "testiselite"))
      val vastaanottoRowsFromDb = singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOidC, hakukohdeOid)
      vastaanottoRowsFromDb must beSome
      val VastaanottoRecord(henkiloOidFromDb, hakuOidFromDb, hakukohdeOidFromDb, actionFromDb,
      ilmoittajaFromDb, timestampFromDb) = vastaanottoRowsFromDb.get
      henkiloOidFromDb mustEqual henkiloOidA
      hakuOidFromDb mustEqual hakuOid
      hakukohdeOidFromDb mustEqual hakukohdeOid
      actionFromDb mustEqual VastaanotaSitovasti
      ilmoittajaFromDb mustEqual henkiloOidA
      timestampFromDb.before(new Date()) mustEqual true
    }

    "find vastaanotot rows of person for given hakukohde, consider linked henkilot" in {
      val henkiloOidA = "1.2.246.562.24.0000000000a"
      val henkiloOidB = "1.2.246.562.24.0000000000b"
      val henkiloOidC = "1.2.246.562.24.0000000000c"
      singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, $henkiloOidC)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, $henkiloOidB)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, $henkiloOidA)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, '1.2.246.562.24.0000000000d')"""
      ))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidA, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOidA, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidA, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOidA, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidB, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOidB, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid + "2", "testiselite"))
      val vastaanottoRowsFromDb = singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOidC, hakukohdeOid)
      vastaanottoRowsFromDb must beSome
      val VastaanottoRecord(henkiloOidFromDb, hakuOidFromDb, hakukohdeOidFromDb, actionFromDb,
      ilmoittajaFromDb, timestampFromDb) = vastaanottoRowsFromDb.get
      henkiloOidFromDb mustEqual henkiloOidA
      hakuOidFromDb mustEqual hakuOid
      hakukohdeOidFromDb mustEqual hakukohdeOid
      actionFromDb mustEqual VastaanotaSitovasti
      ilmoittajaFromDb mustEqual henkiloOidA
      timestampFromDb.before(new Date()) mustEqual true
    }

    "find vastaanotot rows of person affecting yhden paikan saanto" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.runBlocking(sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
                       values (${hakukohdeOid + "1"}, ${hakuOid + "1"}, false, false, '2015K')""")
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid + "1", VastaanotaSitovasti, henkiloOid, "testiselite"))
      val recordsFromDb = singleConnectionValintarekisteriDb.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, Kausi("2015K"))
      recordsFromDb must beSome[VastaanottoRecord]
      recordsFromDb.get.hakukohdeOid must beEqualTo(hakukohdeOid)
      recordsFromDb.get.action must beEqualTo(VastaanotaSitovasti)
    }

    "find vastaanotot rows of person affecting yhden paikan saanto throws if multiple vastaanottos in db" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, Kausi("2015K")) must throwA[RuntimeException]
    }

    "find vastaanotot rows of person affecting yhden paikan saanto, consider sibling henkilot" in {
      val henkiloOidA = "1.2.246.562.24.0000000000a"
      val henkiloOidB = "1.2.246.562.24.0000000000b"
      val henkiloOidC = "1.2.246.562.24.0000000000c"
      singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidB)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidA)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidC)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, '1.2.246.562.24.0000000000d')"""
      ))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidB, hakemusOid, hakukohdeOid, Peru, henkiloOidB, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidA, hakemusOid + "1", otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOidA, "testiselite"))
      val r = singleConnectionValintarekisteriDb.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOidC, Kausi("2015K"))
      r must beSome[VastaanottoRecord]
      r.get.henkiloOid must beEqualTo(henkiloOidA)
      r.get.hakukohdeOid must beEqualTo(otherHakukohdeOidForHakuOid)
      r.get.action must beEqualTo(VastaanotaSitovasti)
    }

    "find vastaanotot rows of person affecting yhden paikan saanto, consider linked henkilot" in {
      val henkiloOidA = "1.2.246.562.24.0000000000a"
      val henkiloOidB = "1.2.246.562.24.0000000000b"
      val henkiloOidC = "1.2.246.562.24.0000000000c"
      singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, $henkiloOidC)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, $henkiloOidA)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, $henkiloOidB)""",
        sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidC, '1.2.246.562.24.0000000000d')"""
      ))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidA, hakemusOid, hakukohdeOid, Peru, henkiloOidA, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidB, hakemusOid + "1", otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOidB, "testiselite"))
      val r = singleConnectionValintarekisteriDb.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOidC, Kausi("2015K"))
      r must beSome[VastaanottoRecord]
      r.get.henkiloOid must beEqualTo(henkiloOidB)
      r.get.hakukohdeOid must beEqualTo(otherHakukohdeOidForHakuOid)
      r.get.action must beEqualTo(VastaanotaSitovasti)
    }

    "mark vastaanotot as deleted" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, Poista, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohdeOid) must beNone
    }

    "rollback failing transaction" in {
      singleConnectionValintarekisteriDb.store( List(
        VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"),
        VirkailijanVastaanotto("123!", "2222323", "134134134.123", VastaanotaSitovasti, "123!", "testiselite"))) must throwA[Exception]
      singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohdeOid) must beNone
    }

    "store vastaanotot in transaction" in {
      singleConnectionValintarekisteriDb.store( List(
        VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"),
        VirkailijanVastaanotto(henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))) must beEqualTo(())
      val henkiloOidsAndActionsFromDb = singleConnectionValintarekisteriDb.runBlocking(
        sql"""select henkilo, action, hakukohde from vastaanotot
              where henkilo = $henkiloOid order by hakukohde""".as[(String, String, String)])
      henkiloOidsAndActionsFromDb must have size 2
      henkiloOidsAndActionsFromDb(0) mustEqual (henkiloOid, VastaanotaSitovasti.toString, hakukohdeOid)
      henkiloOidsAndActionsFromDb(1) mustEqual (henkiloOid, VastaanotaSitovasti.toString, otherHakukohdeOid)
    }

    "findEnsikertalaisuus" in {
      "tarkastelee vastaanottotiedon viimeisintä tilaa" in {
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beAnInstanceOf[EiEnsikertalainen]
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, Peruuta, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beEqualTo(Ensikertalainen(henkiloOid))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(Set(henkiloOid), Kevat(2015)) must beEqualTo(Set(Ensikertalainen(henkiloOid)))
      }
      "ei ota huomioon kumottuja vastaanottotietoja" in {
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beAnInstanceOf[EiEnsikertalainen]
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOid, hakemusOid, hakukohdeOid, Poista, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beEqualTo(Ensikertalainen(henkiloOid))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(Set(henkiloOid), Kevat(2015)) must beEqualTo(Set(Ensikertalainen(henkiloOid)))
      }
      "tarkastelee myös henkilöön linkitettyjen henkilöiden vastaanottoja" in {
        val henkiloOidA = "1.2.246.562.24.0000000000a"
        val henkiloOidB = "1.2.246.562.24.0000000000b"
        val henkiloOidC = "1.2.246.562.24.0000000000c"
        singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
          sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidB)""",
          sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidA)""",
          sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, $henkiloOidC)""",
          sqlu"""insert into henkiloviitteet (master_oid, henkilo_oid) values ($henkiloOidB, '1.2.246.562.24.0000000000d')"""
        ))
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidA, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOidA, "testiselite"))
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(henkiloOidB, hakemusOid + "1", otherHakukohdeOid, Peru, henkiloOidB, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOidC, Kevat(2015)) must beAnInstanceOf[EiEnsikertalainen]
        val r = singleConnectionValintarekisteriDb.findEnsikertalaisuus(Set(henkiloOidA, henkiloOidC), Kevat(2015))
        r must have size 2
        r.head must beAnInstanceOf[EiEnsikertalainen]
        r.tail.head must beAnInstanceOf[EiEnsikertalainen]
      }
    }
  }

  override protected def before: Unit = ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb)
  override protected def after: Unit = ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb)

  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
}

package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date
import java.util.concurrent.TimeUnit

import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{EiEnsikertalainen, Ensikertalainen}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSpec extends Specification with ITSetup with BeforeAfterExample {
  sequential
  private val henkiloOid = "1.2.246.562.24.00000000001"
  private val hakemusOid = "1.2.246.562.99.00000000001"
  private val hakukohdeOid = "1.2.246.561.20.00000000001"
  private val valintatapajonoOid = "1.2.246.561.20.00000000001"
  private val otherHakukohdeOid = "1.2.246.561.20.00000000002"
  private val otherHakukohdeOidForHakuOid = "1.2.246.561.20.00000000003"
  private val refreshedHakukohdeOid = "1.2.246.561.20.00000000004"
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
    "update hakukohde record" in {
      val old = HakukohdeRecord(refreshedHakukohdeOid, hakuOid, true, true, Kevat(2015))
      val fresh = HakukohdeRecord(refreshedHakukohdeOid, hakuOid, false, false, Syksy(2014))
      singleConnectionValintarekisteriDb.storeHakukohde(old)
      singleConnectionValintarekisteriDb.updateHakukohde(fresh) must beTrue
      val stored = singleConnectionValintarekisteriDb.findHakukohde(refreshedHakukohdeOid)
      stored must beSome[HakukohdeRecord](fresh)
    }

    "don't update hakukohde record if no changes" in {
      val old = HakukohdeRecord(refreshedHakukohdeOid, hakuOid, true, true, Kevat(2015))
      val fresh = HakukohdeRecord(refreshedHakukohdeOid, hakuOid, true, true, Kevat(2015))
      singleConnectionValintarekisteriDb.storeHakukohde(old)
      singleConnectionValintarekisteriDb.updateHakukohde(fresh) must beFalse
      val stored = singleConnectionValintarekisteriDb.findHakukohde(refreshedHakukohdeOid)
      stored must beSome[HakukohdeRecord](old)
    }

    "store vastaanotto actions" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      val henkiloOidsAndActionsFromDb = singleConnectionValintarekisteriDb.runBlocking(
        sql"""select henkilo, action from vastaanotot
              where henkilo = $henkiloOid and hakukohde = $hakukohdeOid""".as[(String, String)])
      henkiloOidsAndActionsFromDb must have size 1
      henkiloOidsAndActionsFromDb.head mustEqual (henkiloOid, VastaanotaSitovasti.toString)
    }

    "find vastaanotot rows of person for given haku" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
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
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      val vastaanottoRowsFromDb = singleConnectionValintarekisteriDb.runBlocking(singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohdeOid))
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

    "find vastaanotot rows of person affecting yhden paikan saanto" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.runBlocking(sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
                       values (${hakukohdeOid + "1"}, ${hakuOid + "1"}, false, false, '2015K')""")
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid + "1", VastaanotaSitovasti, henkiloOid, "testiselite"))
      val recordsFromDb = singleConnectionValintarekisteriDb.runBlocking(singleConnectionValintarekisteriDb.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, Kausi("2015K")))
      recordsFromDb must beSome[VastaanottoRecord]
      recordsFromDb.get.hakukohdeOid must beEqualTo(hakukohdeOid)
      recordsFromDb.get.action must beEqualTo(VastaanotaSitovasti)
    }

    "find vastaanotot rows of person affecting yhden paikan saanto throws if multiple vastaanottos in db" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.runBlocking(singleConnectionValintarekisteriDb.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, Kausi("2015K"))) must throwA[RuntimeException]
    }

    "mark vastaanotot as deleted" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, Poista, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.runBlocking(singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohdeOid)) must beNone
    }

    "mark vastaanotot as deleted fails if no vastaanottos found" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, Poista, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.runBlocking(singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohdeOid)) must beNone
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, Poista, henkiloOid, "testiselite")) must throwAn[IllegalStateException]
    }

    "rollback failing transaction" in {
      singleConnectionValintarekisteriDb.store( List(
        VirkailijanVastaanotto(hakuOid, valintatapajonoOid, "123!", "2222323", "134134134.123", VastaanotaSitovasti, "123!", "testiselite")), DBIOAction.successful()) must throwA[Exception]
      singleConnectionValintarekisteriDb.runBlocking(singleConnectionValintarekisteriDb.findHenkilonVastaanottoHakukohteeseen(henkiloOid, hakukohdeOid)) must beNone
    }

    "store vastaanotot in transaction" in {
      singleConnectionValintarekisteriDb.store( List(
        VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"),
        VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite")), DBIOAction.successful()) must beEqualTo(())
      val henkiloOidsAndActionsFromDb = singleConnectionValintarekisteriDb.runBlocking(
        sql"""select henkilo, action, hakukohde from vastaanotot
              where henkilo = $henkiloOid order by hakukohde""".as[(String, String, String)])
      henkiloOidsAndActionsFromDb must have size 2
      henkiloOidsAndActionsFromDb(0) mustEqual (henkiloOid, VastaanotaSitovasti.toString, hakukohdeOid)
      henkiloOidsAndActionsFromDb(1) mustEqual (henkiloOid, VastaanotaSitovasti.toString, otherHakukohdeOid)
    }

    "findEnsikertalaisuus" in {
      "tarkastelee vastaanottotiedon viimeisintä tilaa" in {
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beAnInstanceOf[EiEnsikertalainen]
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, Peruuta, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beEqualTo(Ensikertalainen(henkiloOid))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(Set(henkiloOid), Kevat(2015)) must beEqualTo(Set(Ensikertalainen(henkiloOid)))
      }
      "ei ota huomioon kumottuja vastaanottotietoja" in {
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beAnInstanceOf[EiEnsikertalainen]
        singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, Poista, henkiloOid, "testiselite"))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(henkiloOid, Kevat(2015)) must beEqualTo(Ensikertalainen(henkiloOid))
        singleConnectionValintarekisteriDb.findEnsikertalaisuus(Set(henkiloOid), Kevat(2015)) must beEqualTo(Set(Ensikertalainen(henkiloOid)))
      }
    }

    "store hakukohde multiple times" in {
      Await.result(Future.sequence((1 to 10).map(i => Future {
        singleConnectionValintarekisteriDb.storeHakukohde(HakukohdeRecord("1.2.3", "2.3.4", true, true, Syksy(2016)))
      })), Duration(60, TimeUnit.SECONDS)) must not(throwAn[Exception])
    }

    "find haun vastaanotot" in {
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, otherHakukohdeOidForHakuOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid, hakemusOid, otherHakukohdeOid, VastaanotaSitovasti, henkiloOid, "testiselite"))
      singleConnectionValintarekisteriDb.store(VirkailijanVastaanotto(hakuOid, valintatapajonoOid, henkiloOid + "2", hakemusOid, hakukohdeOid, VastaanotaEhdollisesti, henkiloOid, "testiselite"))
      val vastaanotot = singleConnectionValintarekisteriDb.findHaunVastaanotot(hakuOid)
      vastaanotot must have size 3
      val a = vastaanotot.find(v => v.henkiloOid == henkiloOid && v.hakukohdeOid == hakukohdeOid).get
      a.henkiloOid mustEqual henkiloOid
      a.hakuOid mustEqual hakuOid
      a.hakukohdeOid mustEqual hakukohdeOid
      a.action mustEqual VastaanotaSitovasti
      a.ilmoittaja mustEqual henkiloOid
      a.timestamp.before(new Date()) must beTrue
      val b = vastaanotot.find(_.henkiloOid == henkiloOid + "2").get
      b.henkiloOid mustEqual henkiloOid + "2"
      b.hakuOid mustEqual hakuOid
      b.hakukohdeOid mustEqual hakukohdeOid
      b.action mustEqual VastaanotaEhdollisesti
      b.ilmoittaja mustEqual henkiloOid
      b.timestamp.before(new Date()) must beTrue
      val c = vastaanotot.find(v => v.henkiloOid == henkiloOid && v.hakukohdeOid == otherHakukohdeOidForHakuOid).get
      c.henkiloOid mustEqual henkiloOid
      c.hakuOid mustEqual hakuOid
      c.hakukohdeOid mustEqual otherHakukohdeOidForHakuOid
      c.action mustEqual VastaanotaSitovasti
      c.ilmoittaja mustEqual henkiloOid
      c.timestamp.before(new Date()) must beTrue
    }
  }

  override protected def before: Unit = {
    ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb)
    singleConnectionValintarekisteriDb.runBlocking(sqlu"""delete from hakukohteet where hakukohde_oid = $refreshedHakukohdeOid""")
  }
  override protected def after: Unit = {
    ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb)
    singleConnectionValintarekisteriDb.runBlocking(sqlu"""delete from hakukohteet where hakukohde_oid = $refreshedHakukohdeOid""")
  }

  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
}

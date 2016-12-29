package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.Timestamp

import fi.vm.sade.sijoittelu.domain.{Hakemus => SijoitteluHakemus}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import fi.vm.sade.valintatulosservice.valintarekisteri.{ITSetup, ValintarekisteriDbTools}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult


@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbReadSijoitteluSpec extends Specification with ITSetup with ValintarekisteriDbTools {
  sequential

  step(appConfig.start)
  step(deleteAll())
  step(singleConnectionValintarekisteriDb.storeSijoittelu(loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")))

  "ValintarekisteriDb" should {
    "get hakija" in {
      val res = singleConnectionValintarekisteriDb.getHakemuksenHakija("1.2.246.562.11.00006926939", 1476936450191L).get
      res.etunimi mustEqual "Semi Testi"
    }

    "get hakijan hakutoiveet" in {
      val res = singleConnectionValintarekisteriDb.getHakemuksenHakutoiveet("1.2.246.562.11.00006926939", 1476936450191L)
      res.size mustEqual 1
      res.head.hakutoive mustEqual 6
      res.head.valintatuloksenTila mustEqual "Hyvaksytty"
      res.head.tarjoajaOid mustEqual "1.2.246.562.10.83122281013"
    }

    "get hakijan pistetiedot" in {
      val res = singleConnectionValintarekisteriDb.getHakemuksenPistetiedot("1.2.246.562.11.00006926939", 1476936450191L)
      res.size mustEqual 1
      res.head.tunniste mustEqual "85e2d263-d57d-46e3-3069-651c733c64d8"
    }

    "get latest sijoitteluajoid for haku" in {
      singleConnectionValintarekisteriDb.getLatestSijoitteluajoId("1.2.246.562.29.75203638285").get mustEqual 1476936450191L
    }

    "get sijoitteluajo" in {
      singleConnectionValintarekisteriDb.getSijoitteluajo(1476936450191L).get.sijoitteluajoId mustEqual 1476936450191L
    }

    "get sijoitteluajon hakukohteet" in {
      val res = singleConnectionValintarekisteriDb.getSijoitteluajonHakukohteet(1476936450191L)
      res.map(_.oid).diff(List("1.2.246.562.20.26643418986", "1.2.246.562.20.56217166919", "1.2.246.562.20.69766139963")) mustEqual List()
    }

    "get valintatapajonot for sijoitteluajo" in {
      val res = singleConnectionValintarekisteriDb.getSijoitteluajonValintatapajonot(1476936450191L)
      res.map(r => r.oid).diff(List("14538080612623056182813241345174", "14539780970882907815262745035155", "14525090029451152170747426429137")) mustEqual List()
    }

    "get hakijaryhmat" in {
      singleConnectionValintarekisteriDb.getSijoitteluajonHakijaryhmat(1476936450191L).size mustEqual 5
      singleConnectionValintarekisteriDb.getSijoitteluajonHakijaryhmat(1476936450191L).last.oid mustEqual "14761056762354411505847130564606"
    }

    "get hakijaryhman hakemukset" in {
      val hakijaryhmaOid = singleConnectionValintarekisteriDb.getSijoitteluajonHakijaryhmat(1476936450191L).last.oid
      singleConnectionValintarekisteriDb.getSijoitteluajonHakijaryhmanHakemukset(hakijaryhmaOid, 1476936450191L).size mustEqual 14
    }

    "get hakemuksen ilmoittaja, selite and viimeksiMuokattu" in {
      val hakemus = getHakemusInfo("1.2.246.562.11.00004663595").get
      hakemus.selite mustEqual "Sijoittelun tallennus"
      hakemus.tilanViimeisinMuutos mustEqual dateStringToTimestamp("2016-10-12T04:11:20.527+0000")
    }

    "get ilmoittautumisen ilmoittaja and selite" in {
      val hakemus = getIlmoittautumistiedot("1.2.246.562.24.33442275509").get
      hakemus.selite mustEqual "muokkaus testi"
      hakemus.ilmoittaja mustEqual "testi ilmoittaja"
    }

    "get hakemuksen mailStatus" in {
      val hakemus = getHakemusInfo("1.2.246.562.11.00005820159").get
      hakemus.previousCheck mustEqual dateStringToTimestamp("2016-10-30T06:39:44.246+0000")
      hakemus.sent mustEqual dateStringToTimestamp("2016-10-30T06:44:22.402+0000")
      hakemus.done mustEqual null
      hakemus.message mustEqual "Lähetetty [\"email\"]"
    }
  }

  case class HakemusInfoRecord(selite:String, ilmoittaja:String, tilanViimeisinMuutos:Timestamp,
                               previousCheck:Timestamp, sent:Timestamp, done:Timestamp, message:String)

  private implicit val getHakemusInfoResult = GetResult(r => HakemusInfoRecord(r.nextString, r.nextString,
    r.nextTimestamp, r.nextTimestamp, r.nextTimestamp, r.nextTimestamp, r.nextString))

  def getHakemusInfo(hakemusOid: String): Option[HakemusInfoRecord] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select selite, ilmoittaja, tilan_viimeisin_muutos, previous_check, sent, done, message
            from valinnantulokset
            where hakemus_oid = ${hakemusOid} and deleted is null""".as[HakemusInfoRecord]).headOption
  }

  case class IlmoittautumisRecord(ilmoittaja:String, selite:String)

  private implicit val getIlmoittautumistiedotResult = GetResult(r => IlmoittautumisRecord(r.nextString, r.nextString))

  def getIlmoittautumistiedot(hakijaOid: String): Option[IlmoittautumisRecord] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select ilmoittaja, selite
            from ilmoittautumiset
            where henkilo = ${hakijaOid} and deleted is null""".as[IlmoittautumisRecord]).headOption
  }

  step(deleteAll())
}

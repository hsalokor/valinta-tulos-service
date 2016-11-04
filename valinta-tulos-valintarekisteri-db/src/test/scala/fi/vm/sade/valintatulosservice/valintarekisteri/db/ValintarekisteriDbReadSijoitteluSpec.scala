package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.Timestamp

import fi.vm.sade.sijoittelu.domain.{Hakemus => SijoitteluHakemus}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import fi.vm.sade.valintatulosservice.valintarekisteri.{ITSetup, ValintarekisteriTools}
import org.flywaydb.core.internal.util.scanner.classpath.ClassPathResource
import org.json4s.jackson.JsonMethods._
import org.json4s.native.JsonMethods._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult


@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbReadSijoitteluSpec extends Specification with ITSetup {
  sequential

  step(appConfig.start)
  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
  step(singleConnectionValintarekisteriDb.storeSijoittelu(loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")))

  "ValintarekisteriDb" should {
    "get hakija" in {
      val res = singleConnectionValintarekisteriDb.getHakija("1.2.246.562.11.00006926939", 1476936450191L).get
      res.etunimi mustEqual "Semi Testi"
    }

    "get hakijan hakutoiveet" in {
      val res = singleConnectionValintarekisteriDb.getHakutoiveet("1.2.246.562.11.00006926939", 1476936450191L)
      res.size mustEqual 1
      res.head.hakutoive mustEqual 6
      res.head.valintatuloksenTila mustEqual "Hyvaksytty"
      res.head.tarjoajaOid mustEqual "1.2.246.562.10.83122281013"
    }

    "get hakijan pistetiedot" in {
      val jonosijaIds = singleConnectionValintarekisteriDb.getHakutoiveet("1.2.246.562.11.00006926939", 1476936450191L).map(h => h.jonosijaId)
      val res = singleConnectionValintarekisteriDb.getPistetiedot(jonosijaIds)
      res.size mustEqual 1
      res.head.tunniste mustEqual "85e2d263-d57d-46e3-3069-651c733c64d8"
    }

    "get latest sijoitteluajoid for haku" in {
      singleConnectionValintarekisteriDb.getLatestSijoitteluajoId("1.2.246.562.29.75203638285").get mustEqual 1476936450191L
    }

    "get sijoitteluajo" in {
      singleConnectionValintarekisteriDb.getSijoitteluajo("1.2.246.562.29.75203638285", 1476936450191L).get.sijoitteluajoId mustEqual 1476936450191L
    }

    "get sijoitteluajon hakukohteet" in {
      val res = singleConnectionValintarekisteriDb.getSijoitteluajoHakukohteet(1476936450191L).get
      res.map(r => r.oid) mustEqual List("1.2.246.562.20.26643418986", "1.2.246.562.20.56217166919", "1.2.246.562.20.69766139963")
    }

    "get valintatapajonot for sijoitteluajo" in {
      val res = singleConnectionValintarekisteriDb.getValintatapajonot(1476936450191L).get
      res.map(r => r.oid) mustEqual List("14538080612623056182813241345174", "14539780970882907815262745035155", "14525090029451152170747426429137")
    }

    "get hakemukset for valintatapajono" in {
      val res = singleConnectionValintarekisteriDb.getHakemuksetForValintatapajonos(List("14538080612623056182813241345174", "14539780970882907815262745035155", "14525090029451152170747426429137")).get
      res.size mustEqual 163
    }

    "get tilahistoria for hakemus" in {
      // TODO V
      skipped("Hakemuksen tilahistorialle ei tehdä vielä mitään -> pitää toteuttaa")
      val res = singleConnectionValintarekisteriDb.getHakemuksenTilahistoria("14538080612623056182813241345174", "1.2.246.562.11.00006926939")
      res.size mustEqual 3
    }

    "get hakijaryhmat" in {
      singleConnectionValintarekisteriDb.getHakijaryhmat(1476936450191L).size mustEqual 5
      singleConnectionValintarekisteriDb.getHakijaryhmat(1476936450191L).last.oid mustEqual "14761056762354411505847130564606"
    }

    "get hakijaryhman hakemukset" in {
      val hakijaryhmaId = singleConnectionValintarekisteriDb.getHakijaryhmat(1476936450191L).last.id
      singleConnectionValintarekisteriDb.getHakijaryhmanHakemukset(hakijaryhmaId).size mustEqual 14
    }

    "get hakemuksen tilankuvauksen lisatieto" in {
      val hakemus = getHakemus("1.2.246.562.11.00005808388").get
      hakemus.tarkenne mustEqual "peruuntunutHyvaksyttyYlemmalleHakutoiveelle"
      hakemus.tarkenteenLisatieto mustEqual null
    }

    "get hakemuksen tilankuvauksen tarkenteen lisatiedon tarkenne" in {
      val hakemus = getHakemus("1.2.246.562.11.00006560353").get
      hakemus.tarkenne mustEqual "hyvaksyttyTayttojonoSaannolla"
      hakemus.tarkenteenLisatieto mustEqual "14538080612623056182813241345174"
    }

    "get hakemuksen muokkaaja, muutos and viimeksiMuokattu" in {
      val hakemus = getHakemusInfo("1.2.246.562.11.00004663595").get
      hakemus.selite mustEqual "testimuutos"
      hakemus.tilanViimeisinMuutos mustEqual ValintarekisteriTools.dateStringToTimestamp("2016-10-14T12:44:40.151+0000")
    }
  }

  private implicit val getHakemusResult = GetResult(r => HakemusRecord(r.<<, r.<<, r.<<, r.<<,
    r.<<, r.<<, r.<<, r.<<, Valinnantila(r.<<), r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  def getHakemus(hakemusOid: String): Option[HakemusRecord] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select j.hakija_oid, j.hakemus_oid, j.pisteet, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija,
            j.tasasijajonosija, v.tila, v.tarkenne, v.tarkenteen_lisatieto, j.hyvaksytty_harkinnanvaraisesti, j.varasijan_numero,
            j.onko_muuttunut_viime_sijoittelussa, j.hyvaksytty_hakijaryhmasta, hh.hakijaryhma_id,
            j.siirtynyt_toisesta_valintatapajonosta, j.valintatapajono_oid
            from jonosijat as j
            inner join valinnantulokset as v on v.jonosija_id = j.id and v.hakemus_oid = j.hakemus_oid
            inner join hakijaryhman_hakemukset as hh on j.hakemus_oid = hh.hakemus_oid
            where v.hakemus_oid = ${hakemusOid} and deleted is null""".as[HakemusRecord]).headOption
  }

  case class HakemusInfoRecord(selite:String, ilmoittaja:String, tilanViimeisinMuutos:Timestamp)

  private implicit val getHakemusInfoResult = GetResult(r => HakemusInfoRecord(r.<<, r.<<, r.<<))

  def getHakemusInfo(hakemusOid: String): Option[HakemusInfoRecord] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select selite, ilmoittaja, tilan_viimeisin_muutos
            from valinnantulokset
            where hakemus_oid = ${hakemusOid} and deleted is null""".as[HakemusInfoRecord]).headOption
  }

  def loadSijoitteluFromFixture(fixture: String, path: String = "sijoittelu/"):SijoitteluWrapper = {
    val json = parse(scala.io.Source.fromInputStream(
      new ClassPathResource("fixtures/" + path + fixture + ".json").getInputStream).mkString)
    ValintarekisteriTools.sijoitteluWrapperFromJson(json, singleConnectionValintarekisteriDb)
  }

  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
}

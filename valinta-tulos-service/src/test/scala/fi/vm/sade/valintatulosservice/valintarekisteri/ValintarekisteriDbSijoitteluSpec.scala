package fi.vm.sade.valintatulosservice.valintarekisteri

import java.sql.Timestamp

import fi.vm.sade.sijoittelu.domain.{Hakijaryhma, Hakukohde, Pistetieto, SijoitteluAjo, Valintatapajono, Valintatulos, Hakemus => SijoitteluHakemus}
import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain._
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample
import org.springframework.core.io.ClassPathResource
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSijoitteluSpec extends Specification with ITSetup with BeforeAfterExample {
  sequential
  private val hakuOid = "1.2.246.561.29.00000000001"
  private val henkiloOid = "1.2.246.562.24.00000000001"
  private val hakemusOid = "1.2.246.562.99.00000000001"
  private val hakukohdeOid = "1.2.246.561.20.00000000001"
  private val valintatapajonoOid = "1.2.246.561.20.00000000001"
  private val otherHakukohdeOid = "1.2.246.561.20.00000000002"
  private val otherHakukohdeOidForHakuOid = "1.2.246.561.20.00000000003"
  private val refreshedHakukohdeOid = "1.2.246.561.20.00000000004"
  private val otherHakuOid = "1.2.246.561.29.00000000002"

  private val henkiloOidA = "1.2.246.562.24.0000000000a"
  private val henkiloOidB = "1.2.246.562.24.0000000000b"
  private val henkiloOidC = "1.2.246.562.24.0000000000c"

  val now = System.currentTimeMillis
  val nowDatetime = new Timestamp(1)

  class NumberLongSerializer extends CustomSerializer[Long](format => ( {
    case JObject(List(JField("$numberLong",JString(longValue)))) => longValue.toLong
  }, {
    case x:Long => JObject(List(JField("$numberLong",JString("" + x))))
  }))
  class TasasijasaantoSerializer extends CustomSerializer[Tasasijasaanto](format => ({
    case JString(tasasijaValue) => Tasasijasaanto.getTasasijasaanto(fi.vm.sade.sijoittelu.domain.Tasasijasaanto.valueOf(tasasijaValue))
  }, {
    case x:Tasasijasaanto => JString(x.tasasijasaanto.toString)
  }))
  class ValinnantilaSerializer extends CustomSerializer[Valinnantila](format => ({
    case JString(tilaValue) => Valinnantila.getValinnantila(fi.vm.sade.sijoittelu.domain.HakemuksenTila.valueOf(tilaValue))
  },{
    case x:Valinnantila => JString(x.valinnantila.toString)
  }))

  implicit val formats = DefaultFormats ++ List(new NumberLongSerializer, new TasasijasaantoSerializer, new ValinnantilaSerializer)

  step(appConfig.start)
  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))

  "ValintarekisteriDb" should {
    "store sijoitteluajo" in {
      val sijoitteluajo = createSijoitteluajo()
      singleConnectionValintarekisteriDb.storeSijoitteluajo(sijoitteluajo)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(sijoitteluajo)
    }
    "store sijoitteluajo fixture" in {
      val wrapper = loadSijoitteluFromFixture("hyvaksytty-korkeakoulu-erillishaku")
      singleConnectionValintarekisteriDb.storeSijoitteluajo(wrapper.sijoitteluajo)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
    }
    "store sijoitteluajoWrapper fixture" in {
      val wrapper = loadSijoitteluFromFixture("hyvaksytty-korkeakoulu-erillishaku")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
      val storedHakukohteet: Seq[Hakukohde] = findSijoitteluajonHakukohteet(stored.get.getSijoitteluajoId)
      wrapper.hakukohteet.foreach(hakukohde => {
        val storedHakukohde = storedHakukohteet.find(_.getOid.equals(hakukohde.getOid))
        storedHakukohde.isDefined must beTrue
        SijoitteluajonHakukohdeWrapper(hakukohde) mustEqual SijoitteluajonHakukohdeWrapper(storedHakukohde.get)
        val storedValintatapajonot = findHakukohteenValintatapajonot(hakukohde.getOid)
        import scala.collection.JavaConverters._
        hakukohde.getValintatapajonot.asScala.toList.foreach(valintatapajono => {
          val storedValintatapajono = storedValintatapajonot.find(_.getOid.equals(valintatapajono.getOid))
          storedValintatapajono.isDefined must beTrue
          SijoitteluajonValintatapajonoWrapper(valintatapajono) mustEqual SijoitteluajonValintatapajonoWrapper(storedValintatapajono.get)
          val storedJonosijat = findValintatapajononJonosijat(valintatapajono.getOid)
          valintatapajono.getHakemukset.asScala.toList.foreach(hakemus => {
            val storedJonosija = storedJonosijat.find(_.getHakemusOid.equals(hakemus.getHakemusOid))
            storedJonosija.isDefined must beTrue
            SijoitteluajonHakemusWrapper(hakemus) mustEqual SijoitteluajonHakemusWrapper(storedJonosija.get)
          })
        })
        storedValintatapajonot.length mustEqual hakukohde.getValintatapajonot.size
      })
      storedHakukohteet.length mustEqual wrapper.hakukohteet.length
    }
    "store sijoitteluajoWrapper fixture with hakijaryhmä and pistetiedot" in {
      val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
      val storedHakukohteet: Seq[Hakukohde] = findSijoitteluajonHakukohteet(stored.get.getSijoitteluajoId)
      wrapper.hakukohteet.foreach(hakukohde => {
        val storedHakukohde = storedHakukohteet.find(_.getOid.equals(hakukohde.getOid))
        storedHakukohde.isDefined must beTrue
        SijoitteluajonHakukohdeWrapper(hakukohde) mustEqual SijoitteluajonHakukohdeWrapper(storedHakukohde.get)
        val storedValintatapajonot = findHakukohteenValintatapajonot(hakukohde.getOid)
        import scala.collection.JavaConverters._
        hakukohde.getValintatapajonot.asScala.toList.foreach(valintatapajono => {
          val storedValintatapajono = storedValintatapajonot.find(_.getOid.equals(valintatapajono.getOid))
          storedValintatapajono.isDefined must beTrue
          SijoitteluajonValintatapajonoWrapper(valintatapajono) mustEqual SijoitteluajonValintatapajonoWrapper(storedValintatapajono.get)
          val storedJonosijat = findValintatapajononJonosijat(valintatapajono.getOid)
          valintatapajono.getHakemukset.asScala.toList.foreach(hakemus => {
            val storedJonosija = storedJonosijat.find(_.getHakemusOid.equals(hakemus.getHakemusOid))
            storedJonosija.isDefined must beTrue
            SijoitteluajonHakemusWrapper(hakemus) mustEqual SijoitteluajonHakemusWrapper(storedJonosija.get)

            val storedPistetiedot = findHakemuksenPistetiedot(hakemus.getHakemusOid)
            hakemus.getPistetiedot.size mustEqual storedPistetiedot.size
            hakemus.getPistetiedot.asScala.foreach(pistetieto => {
              val storedPistetieto = storedPistetiedot.find(_.getTunniste.equals(pistetieto.getTunniste))
              storedPistetieto.isDefined must beTrue
              SijoitteluajonPistetietoWrapper(pistetieto) mustEqual SijoitteluajonPistetietoWrapper(storedPistetieto.get)
            })
          })
        })
        val storedHakijaryhmat = findHakukohteenHakijaryhmat(hakukohde.getOid)
        storedHakijaryhmat.length mustEqual hakukohde.getHakijaryhmat.size
        hakukohde.getHakijaryhmat.asScala.toList.foreach(hakijaryhma => {
          val storedHakijaryhma = storedHakijaryhmat.find(_.getOid.equals(hakijaryhma.getOid))
          storedHakijaryhma.isDefined must beTrue
          storedHakijaryhma.get.getHakemusOid.addAll(findHakijaryhmanHakemukset(hakijaryhma.getOid).asJava)
          SijoitteluajonHakijaryhmaWrapper(hakijaryhma) mustEqual SijoitteluajonHakijaryhmaWrapper(storedHakijaryhma.get)
        })
        storedValintatapajonot.length mustEqual hakukohde.getValintatapajonot.size
      })
      storedHakukohteet.length mustEqual wrapper.hakukohteet.length
    }
    "get hakija" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val res = singleConnectionValintarekisteriDb.getHakija("1.2.246.562.11.00006926939", 1476936450191L).get
      res.etunimi mustEqual "Semi Testi"
    }

    "get hakijan hakutoiveet" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val res = singleConnectionValintarekisteriDb.getHakutoiveet("1.2.246.562.11.00006926939", 1476936450191L)
      res.size mustEqual 1
      res.head.hakutoive mustEqual 6
      res.head.valintatuloksenTila mustEqual "Hyvaksytty"
      res.head.tarjoajaOid mustEqual "1.2.246.562.10.83122281013"
    }

    "get hakijan pistetiedot" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val jonosijaIds = singleConnectionValintarekisteriDb.getHakutoiveet("1.2.246.562.11.00006926939", 1476936450191L).map(h => h.jonosijaId)
      val res = singleConnectionValintarekisteriDb.getPistetiedot(jonosijaIds)
      res.size mustEqual 1
      res.head.tunniste mustEqual "85e2d263-d57d-46e3-3069-651c733c64d8"
    }

    "get latest sijoitteluajoid for haku" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      singleConnectionValintarekisteriDb.getLatestSijoitteluajoId("1.2.246.562.29.75203638285").get mustEqual 1476936450191L
    }

    "get sijoitteluajo" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      singleConnectionValintarekisteriDb.getSijoitteluajo("1.2.246.562.29.75203638285", 1476936450191L).get.sijoitteluajoId mustEqual 1476936450191L
    }

    "get sijoitteluajon hakukohteet" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val res = singleConnectionValintarekisteriDb.getSijoitteluajoHakukohteet(1476936450191L).get
      res.map(r => r.oid) mustEqual List("1.2.246.562.20.26643418986", "1.2.246.562.20.56217166919", "1.2.246.562.20.69766139963")
    }

    "get valintatapajonot for sijoitteluajo" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val res = singleConnectionValintarekisteriDb.getValintatapajonot(1476936450191L).get
      res.map(r => r.oid) mustEqual List("14538080612623056182813241345174", "14539780970882907815262745035155", "14525090029451152170747426429137")
    }

    "get hakemukset for valintatapajono" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val res = singleConnectionValintarekisteriDb.getHakemuksetForValintatapajonos(List("14538080612623056182813241345174", "14539780970882907815262745035155", "14525090029451152170747426429137")).get
      res.size mustEqual 163
    }

    "get tilahistoria for hakemus" in {
      // TODO V
      skipped("Hakemuksen tilahistorialle ei tehdä vielä mitään -> pitää toteuttaa")
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val res = singleConnectionValintarekisteriDb.getHakemuksenTilahistoria("14538080612623056182813241345174", "1.2.246.562.11.00006926939")
      res.size mustEqual 3
    }

    "get hakijaryhmat" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      singleConnectionValintarekisteriDb.getHakijaryhmat(1476936450191L).size mustEqual 5
      singleConnectionValintarekisteriDb.getHakijaryhmat(1476936450191L).last.oid mustEqual "14761056762354411505847130564606"
    }

    "get hakijaryhman hakemukset" in {
      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val hakijaryhmaId = singleConnectionValintarekisteriDb.getHakijaryhmat(1476936450191L).last.id
      singleConnectionValintarekisteriDb.getHakijaryhmanHakemukset(hakijaryhmaId).size mustEqual 14
    }
  }

  def loadSijoitteluFromFixture(fixture: String, path: String = "sijoittelu/"):SijoitteluWrapper = {
    val json = parse(scala.io.Source.fromInputStream(
      new ClassPathResource("fixtures/" + path + fixture + ".json").getInputStream).mkString)

    val JArray(sijoittelut) = ( json \ "Sijoittelu" )
    val JArray(sijoitteluajot) = ( sijoittelut(0) \ "sijoitteluajot" )
    val sijoitteluajo:SijoitteluAjo = sijoitteluajot(0).extract[SijoitteluajoWrapper].sijoitteluajo

    val JArray(jsonHakukohteet) = ( json \ "Hakukohde" )
    val hakukohteet:List[Hakukohde] = jsonHakukohteet.map(hakukohdeJson => {
      val hakukohde = hakukohdeJson.extract[SijoitteluajonHakukohdeWrapper].hakukohde
      hakukohde.setValintatapajonot({
        val JArray(valintatapajonot) = (hakukohdeJson \ "valintatapajonot")
        valintatapajonot.map(valintatapajono => {
          val valintatapajonoExt = valintatapajono.extract[SijoitteluajonValintatapajonoWrapper].valintatapajono
          val JArray(hakemukset) = (valintatapajono \ "hakemukset")
          valintatapajonoExt.setHakemukset(hakemukset.map(hakemus => {
            val hakemusExt = hakemus.extract[SijoitteluajonHakemusWrapper].hakemus
            (hakemus \ "pistetiedot") match {
              case JArray(pistetiedot) => hakemusExt.setPistetiedot(pistetiedot.map(pistetieto => pistetieto.extract[SijoitteluajonPistetietoWrapper].pistetieto).asJava)
              case _ =>
            }
            hakemusExt
          }).asJava)
          valintatapajonoExt
        }).asJava
      })
      (hakukohdeJson \ "hakijaryhmat") match {
        case JArray(hakijaryhmat) => hakukohde.setHakijaryhmat(hakijaryhmat.map(hakijaryhma => hakijaryhma.extract[SijoitteluajonHakijaryhmaWrapper].hakijaryhma).asJava)
        case _ =>
      }
      hakukohde
    })

    val JArray(jsonValintatulokset) = (json \ "Valintatulos")
    val valintatulokset:List[Valintatulos] = jsonValintatulokset.map(_.extract[SijoitteluajonValinnantulosWrapper].valintatulos)

    val wrapper:SijoitteluWrapper = SijoitteluWrapper(sijoitteluajo, hakukohteet.filter(h => {
      h.getSijoitteluajoId.equals(sijoitteluajo.getSijoitteluajoId)
    }), valintatulokset)
    hakukohteet.foreach(h => insertHakukohde(h.getOid, sijoitteluajo.getHakuOid))
    wrapper
  }

  def insertHakukohde(hakukohdeOid:String, hakuOid:String) = {
    singleConnectionValintarekisteriDb.runBlocking(DBIOAction.seq(
      sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
           values ($hakukohdeOid, $hakuOid, true, true, '2015K')"""))
  }

  def createSijoitteluajo(): SijoitteluAjo = {
    SijoitteluajoWrapper(now, hakuOid, now-1000, now).sijoitteluajo
  }

  private implicit val getSijoitteluajoResult = GetResult(r => {
    SijoitteluajoWrapper(r.nextLong, r.nextString, r.nextTimestamp.getTime, r.nextTimestamp.getTime).sijoitteluajo
  })

  def findSijoitteluajo(sijoitteluajoId:Long): Option[SijoitteluAjo] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select id, haku_oid, "start", "end"
            from sijoitteluajot
            where id = ${sijoitteluajoId}""".as[SijoitteluAjo]).headOption
  }

  private implicit val getSijoitteluajonHakukohdeResult = GetResult(r => {
    SijoitteluajonHakukohdeWrapper(r.nextLong, r.nextString, r.nextString, r.nextBoolean).hakukohde
  })

  def findSijoitteluajonHakukohteet(sijoitteluajoId:Long): Seq[Hakukohde] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select sijoitteluajo_id, hakukohde_oid as oid, tarjoaja_oid, kaikki_jonot_sijoiteltu
            from sijoitteluajon_hakukohteet
            where sijoitteluajo_id = ${sijoitteluajoId}""".as[Hakukohde])
  }

  private implicit val getSijoitteluajonValintatapajonoResult = GetResult(r => {
    SijoitteluajonValintatapajonoWrapper(r.nextString, r.nextString, r.nextInt, Tasasijasaanto(r.nextString()), r.nextInt, r.nextIntOption, r.nextBoolean,
      r.nextBoolean, r.nextBoolean, r.nextInt, r.nextInt, r.nextDateOption, r.nextDateOption, r.nextStringOption(),
      r.nextIntOption, r.nextIntOption, r.nextBigDecimalOption, None).valintatapajono
  })

  def findHakukohteenValintatapajonot(hakukohdeOid:String): Seq[Valintatapajono] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select oid, nimi, prioriteetti, tasasijasaanto, aloituspaikat, alkuperaiset_aloituspaikat, ei_varasijatayttoa,
            kaikki_ehdon_tayttavat_hyvaksytaan, poissaoleva_taytto,
            varasijat, varasijatayttopaivat, varasijoja_kaytetaan_alkaen, varasijoja_taytetaan_asti, tayttojono,
            hyvaksytty, varalla, alin_hyvaksytty_pistemaara
            from valintatapajonot
            inner join sijoitteluajon_hakukohteet sh on sh.id = valintatapajonot.sijoitteluajon_hakukohde_id
            and sh.hakukohde_oid = ${hakukohdeOid}""".as[Valintatapajono])
  }

  private implicit val getSijoitteluajonHakijaryhmaResult = GetResult(r => {
    SijoitteluajonHakijaryhmaWrapper(r.nextString, r.nextString,
      r.nextIntOption(), r.nextIntOption, r.nextIntOption, r.nextBooleanOption,
      r.nextBooleanOption, r.nextBooleanOption, r.nextBigDecimalOption, List()).hakijaryhma
  })

  def findHakukohteenHakijaryhmat(hakukohdeOid:String): Seq[Hakijaryhma] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select h.oid, h.nimi, h.prioriteetti, h.paikat, h.kiintio, h.kayta_kaikki,
            h.tarkka_kiintio, h.kaytetaan_ryhmaan_kuuluvia, h.alin_hyvaksytty_pistemaara
            from hakijaryhmat h
            inner join sijoitteluajon_hakukohteet sh on sh.id = h.sijoitteluajon_hakukohde_id
            where sh.hakukohde_oid = ${hakukohdeOid}""".as[Hakijaryhma]
    )
  }

  def findHakijaryhmanHakemukset(hakijaryhmaOid:String): Seq[String] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select hh.hakemus_oid from hakijaryhman_hakemukset hh
            inner join hakijaryhmat h ON hh.hakijaryhma_id = h.id
            where h.oid = ${hakijaryhmaOid}""".as[String]
    )
  }

  private implicit val getSijoitteluajonJonosijaResult = GetResult(r => {
    SijoitteluajonHakemusWrapper(r.nextString, r.nextString, r.nextString, r.nextString, r.nextInt, r.nextInt,
      r.nextIntOption, r.nextBooleanOption, r.nextBigDecimalOption, r.nextIntOption, r.nextBooleanOption,
      r.nextBooleanOption, r.nextBooleanOption, Valinnantila(r.nextString), r.nextStringOption().map(ValinnantilanTarkenne(_))).hakemus
  })

  def findValintatapajononJonosijat(valintatapajonoOid:String): Seq[SijoitteluHakemus] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select j.hakemus_oid, j.hakija_oid, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija, j.varasijan_numero, j.onko_muuttunut_viime_sijoittelussa,
            j.pisteet, j.tasasijajonosija, j.hyvaksytty_harkinnanvaraisesti, j.hyvaksytty_hakijaryhmasta, j.siirtynyt_toisesta_valintatapajonosta,
            v.tila, v.tarkenne
            from jonosijat j
            left join valinnantulokset v on j.valintatapajono_oid = v.valintatapajono_oid and j.hakemus_oid = v.hakemus_oid
            where j.valintatapajono_oid = ${valintatapajonoOid}
         """.as[SijoitteluHakemus])
  }

  private implicit val getSijoitteluajonPistetietoResult = GetResult(r => {
    SijoitteluajonPistetietoWrapper(r.nextString, r.nextStringOption, r.nextStringOption, r.nextStringOption).pistetieto
  })

  def findHakemuksenPistetiedot(hakemusOid:String): Seq[Pistetieto] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select p.tunniste, p.arvo, p.laskennallinen_arvo, p.osallistuminen
            from pistetiedot p
            inner join jonosijat j on j.id = p.jonosija_id
            where j.hakemus_oid = ${hakemusOid}""".as[Pistetieto])
  }

  override protected def before: Unit = {
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }
  override protected def after: Unit = {
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }

  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
}

package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.domain.{Valintatapajono, Hakukohde, SijoitteluAjo, Hakemus => SijoitteluHakemus}
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ITSetup
import org.json4s.{CustomSerializer, DefaultFormats}
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample
import org.springframework.core.io.ClassPathResource
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult

import scala.collection.generic.SeqFactory

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

  implicit val formats = DefaultFormats ++ List(new NumberLongSerializer, new TasasijasaantoSerializer)

  step(appConfig.start)
  step(ValintarekisteriTools.deleteSijoitteluajot(singleConnectionValintarekisteriDb))

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
            SijoitteluajonJonosijaWrapper(hakemus) mustEqual SijoitteluajonJonosijaWrapper(storedJonosija.get)
          })
        })
        storedValintatapajonot.length mustEqual hakukohde.getValintatapajonot.size
      })
      storedHakukohteet.length mustEqual wrapper.hakukohteet.length
    }

    "get hakiija" in {
      storeSijoitteluAjot()
      storeSijoitteluajonHakukohteet()
      storeValintatapajonot()
      storeJonosijat()
      singleConnectionValintarekisteriDb.getHakija("12345", 111).etunimi mustEqual "Teppo"
    }

    "get hakijan hakutoiveet" in {
      storeValinnantulokset()
      val res = singleConnectionValintarekisteriDb.getHakutoiveet("12345", 111)
      res.size mustEqual 1
      res.head.hakutoive mustEqual 9999
      res.head.jonosijaId mustEqual 333
    }

    "get hakijan pistetiedot" in {
      storePistetiedot()
      val res = singleConnectionValintarekisteriDb.getPistetiedot(List(333))
      res.size mustEqual 3
      res.head.arvo mustEqual "Arvo1"
    }

    "get latest sijoitteluajoid for haku" in {
      val res = singleConnectionValintarekisteriDb.getLatestSijoitteluajoId(hakuOid)
      res mustEqual 222
    }
  }

  def loadSijoitteluFromFixture(fixture:String):SijoitteluWrapper = {
    val json = parse(scala.io.Source.fromInputStream(
      new ClassPathResource("fixtures/sijoittelu/" + fixture + ".json").getInputStream).mkString)

    val JArray(sijoittelut) = ( json \ "Sijoittelu" )
    val JArray(sijoitteluajot) = ( sijoittelut(0) \ "sijoitteluajot" )
    val sijoitteluajo:SijoitteluAjo = sijoitteluajot(0).extract[SijoitteluajoWrapper].sijoitteluajo

    import scala.collection.JavaConverters._

    val JArray(jsonHakukohteet) = ( json \ "Hakukohde" )
    val hakukohteet:List[Hakukohde] = jsonHakukohteet.map(hakukohdeJson => {
      val hakukohde = hakukohdeJson.extract[SijoitteluajonHakukohdeWrapper].hakukohde
      hakukohde.setValintatapajonot({
        val JArray(valintatapajonot) = (hakukohdeJson \ "valintatapajonot")
        valintatapajonot.map(valintatapajono => {
          val valintatapajonoExt = valintatapajono.extract[SijoitteluajonValintatapajonoWrapper].valintatapajono
          val JArray(hakemukset) = (valintatapajono \ "hakemukset")
          valintatapajonoExt.setHakemukset(hakemukset.map(h => h.extract[SijoitteluajonJonosijaWrapper].hakemus).asJava)
          valintatapajonoExt
        }).asJava
      })
      hakukohde
    })

    val wrapper:SijoitteluWrapper = SijoitteluWrapper(sijoitteluajo, hakukohteet, List())
    hakukohteet.foreach(h => insertHakukohde(h.getOid))
    wrapper
  }

  def insertHakukohde(hakukohdeOid:String) = {
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
      sql"""select id, hakuOid, "start", "end"
            from sijoitteluajot
            where id = ${sijoitteluajoId}""".as[SijoitteluAjo]).headOption
  }

  private implicit val getSijoitteluajonHakukohdeResult = GetResult(r => {
    SijoitteluajonHakukohdeWrapper(r.nextLong, r.nextString, r.nextString, r.nextBoolean).hakukohde
  })

  def findSijoitteluajonHakukohteet(sijoitteluajoId:Long): Seq[Hakukohde] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select sijoitteluajoId, hakukohdeOid as oid, tarjoajaOid, kaikkiJonotSijoiteltu
            from sijoitteluajonhakukohteet
            where sijoitteluajoId = ${sijoitteluajoId}""".as[Hakukohde])
  }

  private implicit val getSijoitteluajonValintatapajonoResult = GetResult(r => {
    SijoitteluajonValintatapajonoWrapper(r.nextString, r.nextString, r.nextInt, Tasasijasaanto(r.nextString()), r.nextInt, r.nextIntOption, r.nextBoolean,
      r.nextBoolean, r.nextBoolean, r.nextInt, r.nextInt, r.nextDateOption, r.nextDateOption, r.nextStringOption(),
      r.nextIntOption, r.nextIntOption, r.nextBigDecimalOption, None).valintatapajono
  })

  def findHakukohteenValintatapajonot(hakukohdeOid:String): Seq[Valintatapajono] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select oid, nimi, prioriteetti, tasasijasaanto, aloituspaikat, alkuperaisetaloituspaikat, eivarasijatayttoa,
            kaikkiehdontayttavathyvaksytaan, poissaolevataytto,
            varasijat, varasijatayttopaivat, varasijojakaytetaanalkaen, varasijojataytetaanasti, tayttojono,
            hyvaksytty, varalla, alinhyvaksyttypistemaara
            from valintatapajonot
            inner join sijoitteluajonhakukohteet on sijoitteluajonhakukohteet.id = valintatapajonot.sijoitteluajonhakukohdeid
            and sijoitteluajonhakukohteet.hakukohdeOid = ${hakukohdeOid}""".as[Valintatapajono])
  }

  private implicit val getSijoitteluajonJonosijaResult = GetResult(r => {
    SijoitteluajonJonosijaWrapper(r.nextString, r.nextString, r.nextString, r.nextString, r.nextInt, r.nextInt,
      r.nextIntOption, r.nextBooleanOption, r.nextBigDecimalOption, r.nextIntOption, r.nextBooleanOption,
      r.nextBooleanOption, r.nextBooleanOption).hakemus
  })

  def findValintatapajononJonosijat(valintatapajonoOid:String): Seq[SijoitteluHakemus] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select hakemusoid, hakijaoid, etunimi, sukunimi, prioriteetti, jonosija, varasijannumero, onkomuuttunutviimesijoittelussa,
            pisteet, tasasijajonosija, hyvaksyttyharkinnanvaraisesti, hyvaksyttyhakijaryhmasta, siirtynyttoisestavalintatapajonosta
            from jonosijat where valintatapajonooid = ${valintatapajonoOid}
         """.as[SijoitteluHakemus])
  }

  private def storeSijoitteluAjot() = {
    singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
      sqlu"""insert into sijoitteluajot (id, hakuOid, start, "end", erillissijoittelu, valisijoittelu) values (111, ${hakuOid}, ${now}, ${now}, FALSE, FALSE)""",
      sqlu"""insert into sijoitteluajot (id, hakuOid, start, "end", erillissijoittelu, valisijoittelu) values (222, ${hakuOid}, ${now}, ${now}, FALSE, FALSE)"""))
  }

  private def storeSijoitteluajonHakukohteet() = {
    singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
      sqlu"""insert into sijoitteluajonhakukohteet values (51, 111, ${hakukohdeOid}, '123123', FALSE)""",
      sqlu"""insert into sijoitteluajonhakukohteet values (52, 222, ${hakukohdeOid}, '123123', FALSE)"""))
  }

  private def storeValintatapajonot() = {
    singleConnectionValintarekisteriDb.runBlocking(
      sqlu"""insert into valintatapajonot (oid, sijoitteluajonHakukohdeId, nimi) values ('5.5.555.555', 51, 'asd')""")
  }

  private def storeJonosijat() = {
    singleConnectionValintarekisteriDb.runBlocking(
      sqlu"""insert into jonosijat (id, valintatapajonoOid, sijoitteluajonHakukohdeId, hakemusOid, hakijaOid, etunimi, sukunimi, prioriteetti, jonosija) values (333, '5.5.555.555', 51, '12345', '54321', 'Teppo', 'The Great', 9999, 1)""")
  }

  private def storeValinnantulokset() = {
    singleConnectionValintarekisteriDb.runBlocking(
      sqlu"""insert into valinnantulokset values (1, ${hakukohdeOid}, '5.5.555.555', '12345', 111, 333, 'Varalla', null, null, TRUE, false, false, false, 'Ilmoittaja', 'Selite', ${now}, ${now}, NULL)"""
    )
  }

  private def storePistetiedot() = {
    singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
      sqlu"""insert into pistetiedot values (333, 'Tunniste', 'Arvo1', 'Laskennallinen Arvo 1', 'Osallistuminen 1')""",
      sqlu"""insert into pistetiedot values (333, 'Tunniste', 'Arvo2', 'Laskennallinen Arvo 2', 'Osallistuminen 2')""",
      sqlu"""insert into pistetiedot values (333, 'Tunniste', 'Arvo3', 'Laskennallinen Arvo 3', 'Osallistuminen 3')"""))
  }

  override protected def before: Unit = {
    ValintarekisteriTools.deleteSijoitteluajot(singleConnectionValintarekisteriDb)
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }
  override protected def after: Unit = {
    ValintarekisteriTools.deleteSijoitteluajot(singleConnectionValintarekisteriDb)
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }
}
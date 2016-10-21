package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.domain.{Valintatapajono, Hakukohde, SijoitteluAjo}
import fi.vm.sade.valintatulosservice.domain.Tasasijasaanto
import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain.{SijoitteluajonValintatapajonoWrapper, SijoitteluWrapper, SijoitteluajonHakukohdeWrapper, SijoitteluajoWrapper}
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

import scala.collection.generic.SeqFactory

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSijoitteluSpec extends Specification with ITSetup with BeforeAfterExample {
  sequential
  private val hakuOid = "1.2.246.561.29.00000000001"
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
      val stored: Option[SijoitteluAjo] = singleConnectionValintarekisteriDb.findSijoitteluajo(sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(sijoitteluajo)
    }
    "store sijoitteluajo fixture" in {
      val wrapper = loadSijoitteluFromFixture("hyvaksytty-korkeakoulu-erillishaku")
      singleConnectionValintarekisteriDb.storeSijoitteluajo(wrapper.sijoitteluajo)
      val stored: Option[SijoitteluAjo] = singleConnectionValintarekisteriDb.findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
    }
    "store sijoitteluajoWrapper fixture" in {
      val wrapper = loadSijoitteluFromFixture("hyvaksytty-korkeakoulu-erillishaku")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val stored: Option[SijoitteluAjo] = singleConnectionValintarekisteriDb.findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
      val storedHakukohteet: Seq[Hakukohde] = singleConnectionValintarekisteriDb.findSijoitteluajonHakukohteet(stored.get.getSijoitteluajoId)
      wrapper.hakukohteet.foreach(hakukohde => {
        val storedHakukohde = storedHakukohteet.find(_.getOid.equals(hakukohde.getOid))
        storedHakukohde.isDefined must beTrue
        SijoitteluajonHakukohdeWrapper(hakukohde) mustEqual SijoitteluajonHakukohdeWrapper(storedHakukohde.get)
        val storedValintatapajonot = singleConnectionValintarekisteriDb.findHakukohteenValintatapajonot(hakukohde.getOid)
        import scala.collection.JavaConverters._
        hakukohde.getValintatapajonot.asScala.toList.foreach(valintatapajono => {
          val storedValintatapajono = storedValintatapajonot.find(_.getOid.equals(valintatapajono.getOid))
          storedValintatapajono.isDefined must beTrue
          SijoitteluajonValintatapajonoWrapper(valintatapajono) mustEqual SijoitteluajonValintatapajonoWrapper(storedValintatapajono.get)
        })
        storedValintatapajonot.length mustEqual hakukohde.getValintatapajonot.size
      })
      storedHakukohteet.length mustEqual wrapper.hakukohteet.length
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
        valintatapajonot.map(_.extract[SijoitteluajonValintatapajonoWrapper].valintatapajono).asJava
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

  override protected def before: Unit = {
    ValintarekisteriTools.deleteSijoitteluajot(singleConnectionValintarekisteriDb)
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }
  override protected def after: Unit = {
    ValintarekisteriTools.deleteSijoitteluajot(singleConnectionValintarekisteriDb)
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }
}

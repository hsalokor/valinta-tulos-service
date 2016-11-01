package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.domain.{Hakukohde, SijoitteluAjo, Valintatulos, Hakemus => SijoitteluHakemus}
import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.springframework.core.io.ClassPathResource
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbReadSijoitteluSpec extends Specification with ITSetup {
  sequential

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

  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
}

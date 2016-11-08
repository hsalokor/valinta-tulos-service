package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.sijoittelu.domain.{Hakukohde, SijoitteluAjo, Valintatulos}
import fi.vm.sade.valintatulosservice.valintarekisteri.db.ValintarekisteriDb
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats}
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

import scala.collection.JavaConverters._

trait ValintarekisteriDbTools {

  val singleConnectionValintarekisteriDb:ValintarekisteriDb

  class NumberLongSerializer extends CustomSerializer[Long](format => ( {
    case JObject(List(JField("$numberLong", JString(longValue)))) => longValue.toLong
  }, {
    case x: Long => JObject(List(JField("$numberLong", JString("" + x))))
  }))

  class DateSerializer extends  CustomSerializer[Date](format => ({
    case JObject(List(JField("$date", JString(dateValue)))) if (dateValue.endsWith("Z")) =>
      new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(dateValue)
    case JObject(List(JField("$date", JString(dateValue)))) =>
      new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(dateValue)
  }, {
    case x: Date => JObject(List(JField("$date", JString("" + x))))
  }))

  class TasasijasaantoSerializer extends CustomSerializer[Tasasijasaanto](format => ( {
    case JString(tasasijaValue) => Tasasijasaanto.getTasasijasaanto(fi.vm.sade.sijoittelu.domain.Tasasijasaanto.valueOf(tasasijaValue))
  }, {
    case x: Tasasijasaanto => JString(x.tasasijasaanto.toString)
  }))

  class ValinnantilaSerializer extends CustomSerializer[Valinnantila](format => ( {
    case JString(tilaValue) => Valinnantila.getValinnantila(fi.vm.sade.sijoittelu.domain.HakemuksenTila.valueOf(tilaValue))
  }, {
    case x: Valinnantila => JString(x.valinnantila.toString)
  }))

  implicit val formats = DefaultFormats ++ List(new NumberLongSerializer, new TasasijasaantoSerializer, new ValinnantilaSerializer, new DateSerializer)

  private val deleteFromVastaanotot = DBIO.seq(
    sqlu"delete from vastaanotot",
    sqlu"delete from deleted_vastaanotot where id <> overriden_vastaanotto_deleted_id()",
    sqlu"delete from henkiloviitteet")

  def deleteAll(): Unit = {
    singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
      deleteFromVastaanotot,
      sqlu"delete from hakijaryhman_hakemukset",
      sqlu"delete from hakijaryhmat",
      sqlu"delete from ilmoittautumiset",
      sqlu"delete from pistetiedot",
      sqlu"delete from valinnantulokset",
      sqlu"delete from jonosijat",
      sqlu"delete from valintatapajonot",
      sqlu"delete from sijoitteluajon_hakukohteet",
      sqlu"delete from hakukohteet",
      sqlu"delete from sijoitteluajot",
      sqlu"delete from vanhat_vastaanotot"
      ).transactionally)
  }

  def dateStringToTimestamp(str:String): Date = {
    new java.sql.Timestamp(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(str).getTime)
  }

  def deleteVastaanotot(): Unit = {
    singleConnectionValintarekisteriDb.runBlocking(deleteFromVastaanotot)
  }

  def sijoitteluWrapperFromJson(json: JValue): SijoitteluWrapper = {
    val JArray(sijoittelut) = (json \ "Sijoittelu")
    val JArray(sijoitteluajot) = (sijoittelut(0) \ "sijoitteluajot")
    val sijoitteluajo: SijoitteluAjo = sijoitteluajot(0).extract[SijoitteluajoWrapper].sijoitteluajo

    val JArray(jsonHakukohteet) = (json \ "Hakukohde")
    val hakukohteet: List[Hakukohde] = jsonHakukohteet.map(hakukohdeJson => {
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
            (hakemus \ "tilanKuvaukset") match {
              case JObject(tilanKuvaukset) => hakemusExt.setTilanKuvaukset(tilanKuvaukset.map(x => Map(x._1 -> x._2.extract[String])).flatten.toMap.asJava)
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
    val valintatulokset: List[Valintatulos] = jsonValintatulokset.map(valintaTulos => {
      val tulos = valintaTulos.extract[SijoitteluajonValinnantulosWrapper].valintatulos
      (valintaTulos \ "logEntries") match {
        case JArray(entries) => tulos.setOriginalLogEntries(entries.map(e => e.extract[LogEntryWrapper].entry).asJava)
        case _ =>
      }
      tulos.setMailStatus((valintaTulos \ "mailStatus").extract[MailStatusWrapper].status)
      tulos
    })

    val wrapper: SijoitteluWrapper = SijoitteluWrapper(sijoitteluajo, hakukohteet.filter(h => {
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

  def loadSijoitteluFromFixture(fixture: String, path: String = "sijoittelu/"):SijoitteluWrapper = {
    val json = parse(getClass.getClassLoader.getResourceAsStream("fixtures/" + path + fixture + ".json"))
    sijoitteluWrapperFromJson(json)
}
}

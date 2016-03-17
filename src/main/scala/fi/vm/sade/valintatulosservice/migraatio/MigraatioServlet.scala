package fi.vm.sade.valintatulosservice.migraatio

import java.util.Date

import com.mongodb.casbah.Imports._
import fi.vm.sade.sijoittelu.domain.{ValintatuloksenTila, Valintatulos}
import fi.vm.sade.valintatulosservice.VtsServletBase
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakukohdeRecordService, ValintarekisteriDb}
import org.mongodb.morphia.Datastore
import org.scalatra.Ok
import org.scalatra.swagger.Swagger
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

import scala.collection.JavaConverters._

class MigraatioServlet(hakukohdeRecordService: HakukohdeRecordService, valintarekisteriDb: ValintarekisteriDb, hakemusRepository: HakemusRepository)
                      (implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {
  override val applicationName = Some("migraatio")

  override protected def applicationDescription: String = "Vanhojen vastaanottojen migraatio REST API"

  private val mongoConfig = appConfig.settings.valintatulosMongoConfig
  private val morphia: Datastore = appConfig.sijoitteluContext.morphiaDs
  private val valintatulosMongoCollection = MongoFactory.createDB(mongoConfig)("Valintatulos")

  private type HakuOid = String
  private type HakijaOid = String
  private type HakukohdeOid = String
  private val hakuOidToExistingVastaanotot: scala.collection.mutable.Map[HakuOid, scala.collection.mutable.Set[(HakijaOid, HakukohdeOid)]] = scala.collection.mutable.Map()

  val getMigraatioHakukohteetSwagger: OperationBuilder = (apiOperation[List[String]]("tuoHakukohteet")
    summary "Migraatio hakukohteille")
  get("/hakukohteet", operation(getMigraatioHakukohteetSwagger)) {
    val hakukohdeOids = haeHakukohdeOidit
    hakukohdeOids.foreach(hakukohdeRecordService.getHakukohdeRecord)
    Ok(hakukohdeOids)
  }

  private def haeHakukohdeOidit: Iterable[String] = {
    morphia.getCollection(classOf[Valintatulos]).distinct("hakukohdeOid").asScala collect { case s:String => s }
  }

  val getMigraatioVastaanototSwagger: OperationBuilder = (apiOperation[List[String]]("tuoVastaanotot")
    summary "Migraatio valintatulosten vastaanotoille")
  get("/vastaanotot", operation(getMigraatioVastaanototSwagger)) {
    //1) hae valintatulos-objektit, joissa tila != kesken
    //2) jos samalle hakijalle ja hakukohteelle on monta valintatulosta
    //   -> hae sijoittelun tulos hakukohteelle, jos sitä ei ole jo haettu
    //   -> valitse merkitsevä jono valinnan tilan perusteella
    //3) jos hakijaoid puuttuu valintatuloksesta -> hae hakemus haku-appista
    //   -> jos hakijaoid puuttuu myös hakemukselta, skippaa valintatulos
    //4) tallenna vastaanottoAction

    val valintatuloksetByHakemusJaHakukohde = findValintatulokset.groupBy(vt => (vt.hakemusOid, vt.hakukohdeOid))

    val (yksiValintatulosPerHakukohde, montaValintatulostaPerHakukohde) = valintatuloksetByHakemusJaHakukohde.partition(_._2.size == 1)

    if(0 < montaValintatulostaPerHakukohde.size) {
      println(s"Löytyi ${montaValintatulostaPerHakukohde.size} kpl moniselitteisiä valintatuloksia")
    }

    Ok(yksiValintatulosPerHakukohde.flatMap { case ((hakemusOid, hakukohdeOid), valintatulokset) =>
      tallenna(valintatulokset.head)
    }.map(_.hakemusOid))
  }

  private def tallenna(valintatulos: MigraatioValintatulos): Option[VirkailijanVastaanotto] = {
   if (!vastaanottoForHakijaAndHakukohdeExists(valintatulos.hakuOid, valintatulos.hakijaOid, valintatulos.hakukohdeOid)) {
     val (vastaanotto, luotu) = createVirkailijanVastaanotto(valintatulos)
     println(s"Saving $vastaanotto")
     valintarekisteriDb.store(vastaanotto, luotu)
     addToExistingVastaanottosCache(valintatulos, vastaanotto)
     Some(vastaanotto)
   } else {
     println(s"Skipped existing ${valintatulos.hakemusOid}")
     None
   }
  }

  private def createVirkailijanVastaanotto(valintatulos: MigraatioValintatulos): (VirkailijanVastaanotto, Date) = {
    val (muokkaaja, selite, luotu) = resolveIlmoittajaJaSeliteJaLuontipvm(valintatulos)

    ( VirkailijanVastaanotto(
      resolveHakijaOid(valintatulos),
      valintatulos.hakemusOid,
      valintatulos.hakukohdeOid,
      convertLegacyTilaToAction(valintatulos.tila),
      muokkaaja,
      selite), luotu)
  }

  private def resolveHakijaOid(valintatulos: MigraatioValintatulos): String = valintatulos.hakijaOid match {
    case x if null == x || "" == x => throw new UnsupportedOperationException(s"""Hakemuksen ${valintatulos.hakemusOid} valintatuloksella ei ollut hakijaoidia""")
    case x => x
  }

  private def resolveIlmoittajaJaSeliteJaLuontipvm(valintatulos: MigraatioValintatulos): (String, String, Date) = {
    val descLogEntries = valintatulos.logEntries.sortWith((a, b) => a.luotu.after(b.luotu))

    descLogEntries.find(e => e.muutos.startsWith("tila:") && e.muutos.contains(valintatulos.tila))
      .orElse(descLogEntries.find(_.muutos.contains(valintatulos.tila))) match {
        case None => ("järjestelmä", "migraatio", new Date())
        case Some(e) => (e.muokkaaja, e.selite, e.luotu)
    }
  }

  private def findValintatulokset: List[MigraatioValintatulos] = {
    val query = Map("$and" -> List(
      Map("tila" -> Map("$ne" -> ValintatuloksenTila.KESKEN.toString)),
      Map("tila" -> Map("$ne" -> "ILMOITETTU"))
    ))

    valintatulosMongoCollection.find(query).toList.map(o => {
      MigraatioValintatulos(
        o.get("hakuOid").asInstanceOf[String],
        o.get("hakijaOid").asInstanceOf[String],
        o.get("hakemusOid").asInstanceOf[String],
        o.get("hakukohdeOid").asInstanceOf[String],
        o.get("tila").asInstanceOf[String],
        o.get("logEntries").asInstanceOf[BasicDBList].toList.map(e => MigraatioLogEntry(
          e.asInstanceOf[DBObject].get("muutos").asInstanceOf[String],
          e.asInstanceOf[DBObject].get("muokkaaja").asInstanceOf[String],
          e.asInstanceOf[DBObject].get("selite").asInstanceOf[String],
          e.asInstanceOf[DBObject].get("luotu").asInstanceOf[Date])
        )
      )
    })
  }

  case class MigraatioValintatulos(hakuOid: String, hakijaOid: String, hakemusOid: String, hakukohdeOid: String, tila: String, logEntries: List[MigraatioLogEntry])
  case class MigraatioLogEntry(muutos:String, muokkaaja:String, selite:String, luotu:Date)

  def convertLegacyTilaToAction(legacyTila: String):VirkailijanVastaanottoAction = legacyTila match {
    case "VASTAANOTTANUT_SITOVASTI" => VastaanotaSitovasti
    case "VASTAANOTTANUT" => VastaanotaSitovasti
    case "VASTAANOTTANUT_LASNA" =>  VastaanotaSitovasti
    case "VASTAANOTTANUT_POISSAOLEVA" => VastaanotaSitovasti
    case "EHDOLLISESTI_VASTAANOTTANUT" => VastaanotaEhdollisesti
    case "EI_VASTAANOTETTU_MAARA_AIKANA" => MerkitseMyohastyneeksi
    case "PERUNUT" => Peru
    case "PERUUTETTU" => Peruuta
    case x => throw new UnsupportedOperationException(s"Tuntematon tila valintatulos-objektissa: ${x}")
  }

  private def vastaanottoForHakijaAndHakukohdeExists(hakuOid: HakuOid, hakijaOid: HakijaOid, hakukohdeOid: HakukohdeOid): Boolean = {
    if (hakijaOid == null || hakijaOid == "") {
      throw new IllegalArgumentException("hakijaOid puuttuu")
    }
    if (!hakuOidToExistingVastaanotot.contains(hakuOid)) {
      populateExistingVastaanottosCacheFromDbForHaku(hakuOid)
    }
    hakuOidToExistingVastaanotot(hakuOid).contains((hakijaOid, hakukohdeOid))
  }

  private def populateExistingVastaanottosCacheFromDbForHaku(hakuOid: HakuOid): hakuOidToExistingVastaanotot.type = {
    val existingVastaanottosFromDb = scala.collection.mutable.Set[(HakijaOid, HakukohdeOid)]() ++=
      valintarekisteriDb.findHaunVastaanotot(hakuOid).map(v => (v.henkiloOid, v.hakukohdeOid))
    hakuOidToExistingVastaanotot += hakuOid -> existingVastaanottosFromDb
  }

  private def addToExistingVastaanottosCache(valintatulos: MigraatioValintatulos, vastaanotto: VirkailijanVastaanotto): Boolean = {
    hakuOidToExistingVastaanotot(valintatulos.hakuOid).add((vastaanotto.henkiloOid, vastaanotto.hakukohdeOid))
  }
}


package fi.vm.sade.valintatulosservice.migraatio

import java.util.Date

import com.mongodb.casbah.Imports._
import fi.vm.sade.sijoittelu.domain.{ValintatuloksenTila, Valintatulos}
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.VtsServletBase
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakukohdeDetailsRetrievalException, HakukohdeRecordService, ValintarekisteriDb}
import org.apache.commons.lang3.StringUtils
import org.mongodb.morphia.Datastore
import org.scalatra.Ok
import org.scalatra.swagger.Swagger
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.springframework.util.StopWatch

import scala.collection.JavaConverters._

class MigraatioServlet(hakukohdeRecordService: HakukohdeRecordService, valintarekisteriDb: ValintarekisteriDb,
                       hakemusRepository: HakemusRepository, raportointiService: RaportointiService)
                      (implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {
  override val applicationName = Some("migraatio")

  override protected def applicationDescription: String = "Vanhojen vastaanottojen migraatio REST API"

  private val migraatioSijoittelutulosService = new MerkitsevaValintatapaJonoResolver(raportointiService)
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
    val stopWatch = new StopWatch("Hakukohteiden migraatio")
    stopWatch.start("uniikkien hakukohdeOidien haku")
    logger.info("Aloitetaan hakukohteiden migraatio, haetaan uniikit hakukohdeoidit valintatuloksista:")
    val hakukohdeOids = haeHakukohdeOidit
    logger.info(s"Löytyi ${hakukohdeOids.size} hakukohdeOidia")
    stopWatch.stop()
    stopWatch.start("hakukohteiden tarkistaminen kannasta ja hakeminen tarjonnasta tarvittaessa")
    hakukohdeOids.foreach(oid => {
      try {
        hakukohdeRecordService.getHakukohdeRecord(oid)
      } catch {
        case e: HakukohdeDetailsRetrievalException => logger.warn(s"Ongelma haettaessa hakukohdetta $oid: ${e.getMessage}")
      }
    })
    stopWatch.stop()
    logger.info(s"${hakukohdeOids.size} hakukohdeOidia käsitelty, hakukohteiden migraatio on valmis.")
    logger.info(s"Hakukohdemigraation vaiheiden kestot:\n${stopWatch.prettyPrint()}")
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

    val merkitsevatValintatuloksetUseammanJononTapauksista = montaValintatulostaPerHakukohde.map(poimiMerkitseva)

    val tallennettavatValintatulokset = yksiValintatulosPerHakukohde ++ merkitsevatValintatuloksetUseammanJononTapauksista

    Ok(tallennettavatValintatulokset.flatMap { case ((hakemusOid, hakukohdeOid), valintatulokset) =>
      tallenna(valintatulokset.head)
    }.map(_.hakemusOid))
  }

  def poimiMerkitseva(tulokset: ((String, String), List[MigraatioValintatulos])): ((String, String), List[MigraatioValintatulos]) = {
    val ((hakemusOid, hakukohdeOid), valintatulokset) = tulokset
    ((hakemusOid, hakukohdeOid), List(findMerkitsevaValintatulos(hakemusOid, hakukohdeOid, valintatulokset)))
  }

  def findMerkitsevaValintatulos(hakemusOid: String, hakukohdeOid: HakukohdeOid,
                                 kaikkiHakemuksenEiKeskenValintatulokset: List[MigraatioValintatulos]): MigraatioValintatulos = {
    val tuloksetHakijaOidienKanssa: List[MigraatioValintatulos] = kaikkiHakemuksenEiKeskenValintatulokset.map(m => m.copy(hakijaOid = resolveHakijaOid(m)))
    val hakuOid = tuloksetHakijaOidienKanssa.head.hakuOid
    val hakijaOid = tuloksetHakijaOidienKanssa.head.hakijaOid
    migraatioSijoittelutulosService.hakemuksenKohteidenMerkitsevatJonot(hakuOid, hakemusOid, hakijaOid).flatMap(_.find(_._1 == hakukohdeOid)).map(_._2) match {
      case Some(jonoOid) => tuloksetHakijaOidienKanssa.find(_.valintatapajonoOid == jonoOid).get
      case None => throw new RuntimeException(s"Ei löydy merkitsevää valintatapajonoOidia hakemuksen $hakemusOid kohteelle $hakukohdeOid")
    }
  }

  private def tallenna(valintatulosFromMongo: MigraatioValintatulos): Option[VirkailijanVastaanotto] = {
    try {
      val valintatulos = resolveHakijaOidIfMissing(valintatulosFromMongo)

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

    } catch {
      case sve:SkipValintatulosException => logger.warn(sve.getMessage, sve)
        None
    }
  }

  private def createVirkailijanVastaanotto(valintatulos: MigraatioValintatulos): (VirkailijanVastaanotto, Date) = {
    val (muokkaaja, selite, luotu) = resolveIlmoittajaJaSeliteJaLuontipvm(valintatulos)

    ( VirkailijanVastaanotto(
      resolveHakijaOid(valintatulos),
      valintatulos.hakemusOid,
      valintatulos.hakukohdeOid,
      convertLegacyTilaToAction(valintatulos),
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
      .orElse(descLogEntries.find(e => StringUtils.contains(e.muutos, valintatulos.tila))) match {
        case None => ("järjestelmä", "migraatio", new Date())
        case Some(e) => (e.muokkaaja, e.selite, e.luotu)
    }
  }

  private def findValintatulokset: List[MigraatioValintatulos] = {
    val query = Map("$and" -> List(
      Map("tila" -> Map("$exists" -> true)),
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
        o.get("valintatapajonoOid").asInstanceOf[String],
        o.get("logEntries").asInstanceOf[BasicDBList].toList.map(e => MigraatioLogEntry(
          e.asInstanceOf[DBObject].get("muutos").asInstanceOf[String],
          e.asInstanceOf[DBObject].get("muokkaaja").asInstanceOf[String],
          e.asInstanceOf[DBObject].get("selite").asInstanceOf[String],
          e.asInstanceOf[DBObject].get("luotu").asInstanceOf[Date])
        )
      )
    })
  }

  case class MigraatioValintatulos(hakuOid: String, hakijaOid: String, hakemusOid: String, hakukohdeOid: String, tila: String,
                                   valintatapajonoOid: String, logEntries: List[MigraatioLogEntry])
  case class MigraatioLogEntry(muutos:String, muokkaaja:String, selite:String, luotu:Date)

  def convertLegacyTilaToAction(valintatulos: MigraatioValintatulos): VirkailijanVastaanottoAction = valintatulos.tila match {
    case "VASTAANOTTANUT_SITOVASTI" => VastaanotaSitovasti
    case "VASTAANOTTANUT" => VastaanotaSitovasti
    case "VASTAANOTTANUT_LASNA" =>  VastaanotaSitovasti
    case "VASTAANOTTANUT_POISSAOLEVA" => VastaanotaSitovasti
    case "EHDOLLISESTI_VASTAANOTTANUT" => VastaanotaEhdollisesti
    case "EI_VASTAANOTETTU_MAARA_AIKANA" => MerkitseMyohastyneeksi
    case "PERUNUT" => Peru
    case "PERUUTETTU" => Peruuta
    case x => throw new UnsupportedOperationException(s"Tuntematon tila valintatulos-objektissa: $x , valintatulos: $valintatulos")
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

  private def resolveHakijaOidIfMissing(valintatulos: MigraatioValintatulos): MigraatioValintatulos = valintatulos.hakijaOid match {
    case hakijaOid if StringUtils.isBlank(hakijaOid) => hakemusRepository.findHakemus(valintatulos.hakemusOid) match {
      case None => throw new IllegalArgumentException(s"Hakemusta ${valintatulos.hakemusOid} ei löydy!")
      case Some(hakemus) if StringUtils.isBlank(hakemus.henkiloOid) => throw new SkipValintatulosException(s"Valintatulokselle ei löydy hakijaOidia hakemukselta ${valintatulos.hakemusOid}")
      case Some(hakemus) => valintatulos.copy(hakijaOid = hakemus.henkiloOid)
    }
    case _ => valintatulos
  }

  class SkipValintatulosException(message:String) extends Exception(message)

}


package fi.vm.sade.valintatulosservice.migraatio

import java.util.Date

import com.mongodb.casbah.Imports._
import fi.vm.sade.sijoittelu.domain.{Valintatulos, ValintatuloksenTila}
import fi.vm.sade.valintatulosservice.VtsServletBase
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Vastaanottotila, VirkailijanVastaanotto, VirkailijanVastaanottoAction}
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}
import org.mongodb.morphia.Datastore
import org.scalatra.Ok
import org.scalatra.swagger.Swagger
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

import scala.collection.JavaConverters._

class MigraatioServlet(hakukohdeRecordService: HakukohdeRecordService, hakijaVastaanottoRepository: HakijaVastaanottoRepository)
                      (implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {
  override val applicationName = Some("migraatio")

  override protected def applicationDescription: String = "Vanhojen vastaanottojen migraatio REST API"

  private val mongoConfig = appConfig.settings.valintatulosMongoConfig


  val getMigraatioHakukohteetSwagger: OperationBuilder = (apiOperation[List[String]]("tuoHakukohteet")
    summary "Migraatio hakukohteille")
  get("/hakukohteet", operation(getMigraatioHakukohteetSwagger)) {
    val hakukohdeOids = haeHakukohdeOidit
    hakukohdeOids.foreach(hakukohdeRecordService.getHakukohdeRecord)
    Ok(hakukohdeOids)
  }

  private def haeHakukohdeOidit: Iterable[String] = {
    val morphia: Datastore = appConfig.sijoitteluContext.morphiaDs
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

    Ok(for{
      ( (hakemusOid, hakukohdeOid), valintatulokset ) <- yksiValintatulosPerHakukohde
    } yield {
      tallenna(valintatulokset.head).hakemusOid
    })
  }

  private def tallenna(valintatulos: MigraatioValintatulos):VirkailijanVastaanotto = {
   val (vastaanotto, luotu) = createVirkailijanVastaanotto(valintatulos)
   hakijaVastaanottoRepository.store(vastaanotto, luotu)
   vastaanotto
  }

  private def createVirkailijanVastaanotto(valintatulos: MigraatioValintatulos): (VirkailijanVastaanotto, Date) = {
    val (muokkaaja, selite, luotu) = resolveIlmoittajaJaSeliteJaLuontipvm(valintatulos)

    ( VirkailijanVastaanotto(
      resolveHakijaOid(valintatulos),
      valintatulos.hakemusOid,
      valintatulos.hakukohdeOid,
      VirkailijanVastaanottoAction.getVirkailijanVastaanottoAction(Vastaanottotila.withName(valintatulos.tila)),
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
    val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

    val query = Map("tila" -> Map("$ne" -> ValintatuloksenTila.KESKEN.toString))

    valintatulos.find(query).toList.map(o => {
      MigraatioValintatulos(
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

  case class MigraatioValintatulos(hakijaOid:String, hakemusOid:String, hakukohdeOid:String, tila:String, logEntries:List[MigraatioLogEntry])
  case class MigraatioLogEntry(muutos:String, muokkaaja:String, selite:String, luotu:Date)

}
/*
public enum MigraatioValintatuloksenTila {
  ILMOITETTU,                    // Hakijalle on ilmoitettu, sijoittelun tulos ei voi muuttaa paikkaa peruuntuneeksi
  VASTAANOTTANUT,
  VASTAANOTTANUT_LASNA,          // Hakija ottanut paikan vastaan ja on lasna
  VASTAANOTTANUT_POISSAOLEVA,    // Hakija ottanut paikan vastaan ja ilmoittautunut poissaolevaksi
  EI_VASTAANOTETTU_MAARA_AIKANA, // Hakija ei ole ilmoittanut paikkaa vastaanotetuksi maaraaikana ja on nain ollen hylatty
  PERUNUT,                       // Hakija ei ota paikkaa vastaan
  PERUUTETTU,                    // Hakijan tila on peruutettu
  EHDOLLISESTI_VASTAANOTTANUT,    // Ehdollisesti vastaanottanut
  VASTAANOTTANUT_SITOVASTI,       // Sitovasti vastaanottanut, kk-tila
  KESKEN
}*/

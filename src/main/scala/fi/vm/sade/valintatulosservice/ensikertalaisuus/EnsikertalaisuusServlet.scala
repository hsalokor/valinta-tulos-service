package fi.vm.sade.valintatulosservice.ensikertalaisuus

import java.text.SimpleDateFormat
import java.util.TimeZone

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Kausi
import fi.vm.sade.valintatulosservice.ensikertalaisuus.EnsikertalaisuusServlet._
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.valintarekisteri.ValintarekisteriService
import fi.vm.sade.valintatulosservice.{VtsServletBase, VtsSwaggerBase}
import org.json4s.jackson.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._

class EnsikertalaisuusServlet(valintarekisteriService: ValintarekisteriService, maxHenkiloOids: Int)(implicit val swagger: Swagger, appConfig: AppConfig)
  extends VtsServletBase with EnsikertalaisuusSwagger {
  override implicit val jsonFormats: Formats = EnsikertalaisuusServlet.ensikertalaisuusJsonFormats

  def henkiloOid(oid: String): String = {
    require(oid.startsWith("1.2.246.562.24."), "Illegal henkilo oid")
    oid
  }

  get("/:henkilo", operation(getEnsikertalaisuusSwagger)) {
    valintarekisteriService.findEnsikertalaisuus(henkiloOid(params("henkilo")), parseKausi(params("koulutuksenAlkamiskausi")))
  }

  get("/:henkilo/historia", operation(getVastaanottoHistoriaSwagger)) {
    valintarekisteriService.findVastaanottoHistory(henkiloOid(params("henkilo")))
  }

  post("/", operation(postEnsikertalaisuusSwagger)) {
    val henkilot = read[Set[String]](request.body).map(henkiloOid)
    if (henkilot.size > maxHenkiloOids) throw new IllegalArgumentException(s"Too many henkilo oids (limit is $maxHenkiloOids, got ${henkilot.size})")

    valintarekisteriService.findEnsikertalaisuus(henkilot, parseKausi(params("koulutuksenAlkamiskausi")))
  }
}

object EnsikertalaisuusServlet {
  private val finnishDateFormats: Formats =  new DefaultFormats {
    override def dateFormatter = {
      val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
      format.setTimeZone(TimeZone.getTimeZone("Europe/Helsinki"))
      format
    }
  }
  val ensikertalaisuusJsonFormats: Formats = finnishDateFormats ++ JsonFormats.customSerializers

  def parseKausi(d: String): Kausi = Kausi(d)
}

trait EnsikertalaisuusSwagger extends VtsSwaggerBase { this: SwaggerSupport =>
  override val applicationName = Some("ensikertalaisuus")
  override protected def applicationDescription: String = "Ensikertalaisuus-tiedon vastaanotto-osuus"

  registerModel(Model("Ensikertalaisuus", "Ensikertalaisuus", None, None,
    List("personOid" -> ModelProperty(DataType.String, required = true), "paattyi" -> ModelProperty(DataType.DateTime)),
    Some("Ensikertalaisuus"), None
  ))

  val getEnsikertalaisuusSwagger: OperationBuilder = apiOperation[Ensikertalaisuus]("getEnsikertalaisuus")
    .summary("Hae ensikertalaisuus-tiedon vastaanotto-osuus")
    .notes("Ei pidä käyttää sellaisenaan, vain ainoastaan suoritusrekisterin kautta. Rajapinta palauttaa joko pelkän " +
      "henkilo oidin tai henkilo oidin ja päivämäärän, jolloin ensikertalaisuus päättyi.")
    .parameter(pathParam[String]("henkiloOid").description("Henkilön oid").required)
    .parameter(queryParam[String]("koulutuksenAlkamiskausi")
      .description("Koulutuksen alkamiskausi, josta lähtien alkavat koulutukset kyselyssä otetaan huomioon (esim. 2016S").required)
    .responseMessage(ModelResponseMessage(400, "Kuvaus virheellisestä pyynnöstä"))
    .responseMessage(ModelResponseMessage(500, "Virhe palvelussa"))

  val getVastaanottoHistoriaSwagger: OperationBuilder = apiOperation[VastaanottoHistoria]("getVastaanottoHistoria")
    .summary("Hae henkilön kk vastaanotto historia")
    .notes("Palauttaa vain korkeakoulupaikkojen vastaanotot.")
    .parameter(pathParam[String]("henkiloOid").description("Henkilön oid").required)
    .responseMessage(ModelResponseMessage(400, "Kuvaus virheellisestä pyynnöstä"))
    .responseMessage(ModelResponseMessage(500, "Virhe palvelussa"))

  val postEnsikertalaisuusSwagger: OperationBuilder = apiOperation[Seq[Ensikertalaisuus]]("getEnsikertalaisuus")
    .summary("Hae ensikertalaisuus-tiedon vastaanotto-osuus")
    .notes("Ei pidä käyttää sellaisenaan, vain ainoastaan suoritusrekisterin kautta. Rajapinta palauttaa setin ensikertalaisuus-objekteja.")
    .parameter(bodyParam[Seq[String]]("henkiloOids")
      .description("Henkilöiden oidit json-sekvenssinä, enintään 10000 oidia yhdessä pyynnössä").required)
    .parameter(queryParam[String]("koulutuksenAlkamiskausi")
      .description("Koulutuksen alkamiskausi, josta lähtien alkavat koulutukset kyselyssä otetaan huomioon (esim. 2016S").required)
    .responseMessage(ModelResponseMessage(400, "Kuvaus virheellisestä pyynnöstä"))
    .responseMessage(ModelResponseMessage(500, "Virhe palvelussa"))
}


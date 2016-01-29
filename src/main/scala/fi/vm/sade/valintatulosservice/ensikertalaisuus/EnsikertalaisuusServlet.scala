package fi.vm.sade.valintatulosservice.ensikertalaisuus

import java.text.SimpleDateFormat
import java.util.Date

import fi.vm.sade.valintatulosservice.{VtsSwaggerBase, VtsServletBase}
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.valintarekisteri.ValintarekisteriService
import org.json4s.Formats
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._
import org.json4s.jackson.Serialization.read
import EnsikertalaisuusServlet._

class EnsikertalaisuusServlet(valintarekisteriService: ValintarekisteriService)(implicit val swagger: Swagger, appConfig: AppConfig)
  extends VtsServletBase with EnsikertalaisuusSwagger {

  def henkiloOid(oid: String): String = {
    require(oid.startsWith("1.2.246.562.24."), "Illegal henkilo oid")
    oid
  }

  get("/:henkilo", operation(getEnsikertalaisuusSwagger)) {
    valintarekisteriService.findEnsikertalaisuus(henkiloOid(params("henkilo")), parseKoulutuksenAlkamispvm(params("koulutuksenAlkamispvm")))
  }

  post("/", operation(postEnsikertalaisuusSwagger)) {
    val henkilot = read[Set[String]](request.body).map(henkiloOid)
    if (henkilot.size > maxHenkiloOids) throw new IllegalArgumentException("Too many henkilo oids")

    valintarekisteriService.findEnsikertalaisuus(henkilot, parseKoulutuksenAlkamispvm(params("koulutuksenAlkamispvm")))
  }
}

object EnsikertalaisuusServlet {
  private val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSX"

  def parseKoulutuksenAlkamispvm(d: String)(implicit formats: Formats): Date = {
    formats.dateFormat.parse(d).getOrElse(new SimpleDateFormat(dateFormat).parse(d))
  }

  val maxHenkiloOids = 10000
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
    .parameter(queryParam[String]("koulutuksenAlkamispvm")
      .description("Aikaleima, jonka jälkeen alkavat koulutukset kyselyssä otetaan huomioon (esim. 2014-08-01T00:00:00.000+03:00)").required)
    .responseMessage(ModelResponseMessage(400, "Kuvaus virheellisestä pyynnöstä"))
    .responseMessage(ModelResponseMessage(500, "Virhe palvelussa"))

  val postEnsikertalaisuusSwagger: OperationBuilder = apiOperation[Seq[Ensikertalaisuus]]("getEnsikertalaisuus")
    .summary("Hae ensikertalaisuus-tiedon vastaanotto-osuus")
    .notes("Ei pidä käyttää sellaisenaan, vain ainoastaan suoritusrekisterin kautta. Rajapinta palauttaa setin ensikertalaisuus-objekteja.")
    .parameter(bodyParam[Seq[String]]("henkiloOids")
      .description("Henkilöiden oidit json-sekvenssinä, enintään 10000 oidia yhdessä pyynnössä").required)
    .parameter(queryParam[String]("koulutuksenAlkamispvm")
      .description("Aikaleima, jonka jälkeen alkavat koulutukset kyselyssä otetaan huomioon (esim. 2014-08-01T00:00:00.000+03:00)").required)
    .responseMessage(ModelResponseMessage(400, "Kuvaus virheellisestä pyynnöstä"))
    .responseMessage(ModelResponseMessage(500, "Virhe palvelussa"))
}


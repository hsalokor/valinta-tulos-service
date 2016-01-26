package fi.vm.sade.valintatulosservice.ensikertalaisuus

import java.text.SimpleDateFormat
import java.util.Date

import fi.vm.sade.valintatulosservice.VtsServletBase
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.valintarekisteri.ValintarekisteriService
import org.json4s.Formats
import org.json4s.jackson.Serialization._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger.Swagger
import org.json4s.jackson.Serialization.read

class EnsikertalaisuusServlet(valintarekisteriService: ValintarekisteriService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {
  import EnsikertalaisuusServlet._
  override val applicationName = Some("ensikertalaisuus")
  override protected def applicationDescription: String = "Ensikertalaisuus-tiedon vastaanotto-osuus"

  private val getEnsikertalaisuusSwagger: OperationBuilder = apiOperation[Unit]("getEnsikertalaisuus")
    .summary("Hae ensikertalaisuus-tiedon vastaanotto-osuus")
    .notes("Ei pidä käyttää sellaisenaan, vain ainoastaan suoritusrekisterin kautta. Rajapinta palauttaa joko pelkän henkilo oidin tai henkilo oidin ja päivämäärän, jolloin ensikertalaisuus päättyi. Esim: " +
      write(Ensikertalainen("1.2.246.561.24.00000000001")) + " tai " +
      write(EiEnsikertalainen("1.2.246.561.24.00000000001", new Date())))
    .parameter(pathParam[String]("henkiloOid").description("Henkilön oid").required)
    .parameter(queryParam[String]("koulutuksenAlkamispvm").description("Ensimmäinen koulutuksen alkamishetki, joka kyselyssä otetaan huomioon").required)

  private val postEnsikertalaisuusSwagger: OperationBuilder = apiOperation[Unit]("getEnsikertalaisuus")
    .summary("Hae ensikertalaisuus-tiedon vastaanotto-osuus")
    .notes("Ei pidä käyttää sellaisenaan, vain ainoastaan suoritusrekisterin kautta. Rajapinta palauttaa setin ensikertalaisuus-objekteja.")
    .parameter(bodyParam[Seq[String]]("henkiloOids").description("Henkilöiden oidit json-sekvenssinä, enintään 10000 oidia yhdessä pyynnössä").required)
    .parameter(queryParam[String]("koulutuksenAlkamispvm").description("Ensimmäinen koulutuksen alkamishetki, joka kyselyssä otetaan huomioon").required)

  private case class HenkiloOid(oid: String) {
    require(oid.startsWith("1.2.246.561.24."), "Illegal henkilo oid")
  }

  get("/:henkilo", operation(getEnsikertalaisuusSwagger)) {
    valintarekisteriService.findEnsikertalaisuus(
      HenkiloOid(params("henkilo")).oid,
      parseKoulutuksenAlkamispvm(params("koulutuksenAlkamispvm"))
    )
  }

  post("/", operation(postEnsikertalaisuusSwagger)) {
    val henkilot = read[Set[String]](request.body).map(HenkiloOid)
    if (henkilot.size > maxHenkiloOids) throw new IllegalArgumentException("Too many henkilo oids")
    val pvm = parseKoulutuksenAlkamispvm(params("koulutuksenAlkamispvm"))

    for (
      henkilo <- henkilot
    ) yield valintarekisteriService.findEnsikertalaisuus(henkilo.oid, pvm) // TODO optimize me
  }
}

object EnsikertalaisuusServlet {
  private val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSX"
  def parseKoulutuksenAlkamispvm(d: String)(implicit formats: Formats): Date = {
    formats.dateFormat.parse(d).getOrElse(new SimpleDateFormat(dateFormat).parse(d))
  }
  val maxHenkiloOids = 10000
}


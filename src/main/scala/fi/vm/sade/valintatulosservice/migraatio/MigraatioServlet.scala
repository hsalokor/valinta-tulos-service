package fi.vm.sade.valintatulosservice.migraatio

import fi.vm.sade.sijoittelu.domain.Valintatulos
import fi.vm.sade.valintatulosservice.VtsServletBase
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.valintarekisteri.HakukohdeRecordService
import org.mongodb.morphia.Datastore
import org.scalatra.Ok
import org.scalatra.swagger.Swagger
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder


class MigraatioServlet(hakukohdeRecordService: HakukohdeRecordService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {
  override val applicationName = Some("migraatio")

  override protected def applicationDescription: String = "Vanhojen vastaanottojen migraatio REST API"

  val getMigraatioHakukohteetSwagger: OperationBuilder = (apiOperation[List[String]]("tuoHakukohteet")
    summary "Migraatio hakukohteille")
  get("/hakukohteet", operation(getMigraatioHakukohteetSwagger)) {
    val hakukohdeOids = haeHakukohdeOidit
    hakukohdeOids.foreach(hakukohdeRecordService.getHakukohdeRecord)
    Ok(hakukohdeOids)
  }

  private def haeHakukohdeOidit: Iterable[String] = {
    val morphia: Datastore = appConfig.sijoitteluContext.morphiaDs

    import scala.collection.JavaConverters._
    morphia.getCollection(classOf[Valintatulos]).distinct("hakukohdeOid").asScala collect { case s:String => s }
  }

}

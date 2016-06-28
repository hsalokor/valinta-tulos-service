package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.sijoittelu.domain.{Hakukohde, HakukohdeItem, SijoitteluAjo}
import fi.vm.sade.sijoittelu.tulos.dto.{HakukohdeDTO, SijoitteluajoDTO}
import fi.vm.sade.valintatulosservice.config.AppConfig.{AppConfig, StubbedExternalDeps}
import fi.vm.sade.valintatulosservice.json.StreamingJsonArrayRetriever

import scala.collection.JavaConverters._

class LatestSijoitteluAjoClient(appConfig: AppConfig) {
  private val retriever = new StreamingJsonArrayRetriever(appConfig)

  def fetchLatestSijoitteluAjoFromSijoitteluService(hakuOid: String, hakukohdeOid: Option[String]): Option[SijoitteluAjo] = {
    val ajo = new SijoitteluAjo
    val processor: SijoitteluajoDTO => SijoitteluAjo = dto => {
      ajo.setSijoitteluajoId(dto.getSijoitteluajoId)
      ajo.setHakuOid(dto.getHakuOid)
      ajo.setStartMils(dto.getStartMils)
      ajo.setEndMils(dto.getEndMils)
      ajo.setHakukohteet(dto.getHakukohteet.asScala.map(hakukohdeDtoToHakukohde).asJava)
      ajo
    }

    retriever.processStreaming[SijoitteluajoDTO,SijoitteluAjo]("/sijoittelu-service", url(hakuOid, hakukohdeOid), classOf[SijoitteluajoDTO], processor)

    if (ajo.getSijoitteluajoId == null) { // empty object was created in SijoitteluResourceImpl
      None
    } else {
      Some(ajo)
    }
  }

  private def hakukohdeDtoToHakukohde(hakukohdeDTO: HakukohdeDTO): HakukohdeItem = {
    val item = new HakukohdeItem
    item.setOid(hakukohdeDTO.getOid)
    item
  }

  private def url(hakuOid: String, hakukohdeOidOption: Option[String]): String = {
    val latestUrlForHaku = s"${appConfig.settings.sijoitteluServiceRestUrl}/resources/sijoittelu/$hakuOid/sijoitteluajo/latest"
    hakukohdeOidOption match {
      case Some(hakukohdeOid) => latestUrlForHaku + "?hakukohdeOid=" + hakukohdeOid
      case None => latestUrlForHaku
    }
  }
}

object LatestSijoitteluAjoClient {
  def apply(appConfig: AppConfig) = appConfig match {
    case _: StubbedExternalDeps => new DirectMongoLatestSijoitteluAjoClient(appConfig)
    case _ => new LatestSijoitteluAjoClient(appConfig)
  }
}

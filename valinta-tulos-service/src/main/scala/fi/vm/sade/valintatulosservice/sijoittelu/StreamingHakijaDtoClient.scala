package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import fi.vm.sade.valintatulosservice.OphUrlProperties
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.json.StreamingJsonArrayRetriever

class StreamingHakijaDtoClient(appConfig: AppConfig) {
  private val retriever = new StreamingJsonArrayRetriever(appConfig)

  def processSijoittelunTulokset[T](hakuOid: String, sijoitteluajoId: String, processor: HakijaDTO => T) = {
    retriever.processStreaming[HakijaDTO,T]("/sijoittelu-service", url(hakuOid, sijoitteluajoId), classOf[HakijaDTO], processor)
  }

  private def url(hakuOid: String, sijoitteluajoId: String): String = {
    OphUrlProperties.ophProperties.url("sijoittelu-service.all.hakemus.for.sijoittelu", hakuOid, sijoitteluajoId)
  }
}

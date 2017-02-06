package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import fi.vm.sade.valintatulosservice.config.VtsAppConfig.VtsAppConfig
import fi.vm.sade.valintatulosservice.json.StreamingJsonArrayRetriever

class StreamingHakijaDtoClient(appConfig: VtsAppConfig) {
  private val retriever = new StreamingJsonArrayRetriever(appConfig)

  def processSijoittelunTulokset[T](hakuOid: String, sijoitteluajoId: String, processor: HakijaDTO => T) = {
    retriever.processStreaming[HakijaDTO,T]("/sijoittelu-service", url(hakuOid, sijoitteluajoId), classOf[HakijaDTO], processor)
  }

  private def url(hakuOid: String, sijoitteluajoId: String): String = {
    s"${appConfig.settings.sijoitteluServiceRestUrl}/resources/sijoittelu/$hakuOid/sijoitteluajo/$sijoitteluajoId/hakemukset"
  }
}

package fi.vm.sade.valintatulosservice.json

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import fi.vm.sade.valintatulosservice.ServletSpecification
import org.apache.commons.lang3.builder.ToStringBuilder
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StreamingJsonArrayRetrieverSpec extends ServletSpecification {
  step(appConfig.start)

  "StreamingJsonArrayRetriever" should {
    "Return contents of JSON array via an iterator" in {
      val retriever = new StreamingJsonArrayRetriever(appConfig)

      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      val hakuOid = "1.2.246.562.5.2013080813081926341928"

      val localUrl = s"$baseUrl/haku/$hakuOid/sijoitteluajo/latest/hakemukset"
      val results = retriever.requestStreaming("/any-service", localUrl, classOf[HakijaDTO]).toList
      val firstResult = results.head
      println(ToStringBuilder.reflectionToString(firstResult))
      firstResult.getEtunimi must_== "Teppo"
      firstResult.getHakijaOid must_== "1.2.246.562.24.14229104472"
      firstResult.getHakemusOid must_== "1.2.246.562.11.00000441369"
      firstResult.getHakutoiveet.size must_== 1
      results.size must_== 1
    }
  }
}

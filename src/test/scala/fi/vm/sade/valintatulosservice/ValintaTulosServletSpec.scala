package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.scalatra.test.specs2.MutableScalatraSpec
import org.specs2.specification.{Step, Fragments}

class ValintaTulosServletSpec extends MutableScalatraSpec {
  implicit val appConfig: AppConfig = new AppConfig.IT

  "GET /haku/:hakuId/hakemus/:hakemusId" should {
    "palauttaa valintatulokset" in {
      get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
        body must_== """{"hakemusOid":"1.2.246.562.11.00000441369","hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","tarjoajaOid":"1.2.246.562.10.591352080610","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":"EI_TEHTY","vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T19:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T19:05:23Z"}]}"""
      }
    }
  }
  addServlet(new ValintatulosServlet(), "/*")
  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}
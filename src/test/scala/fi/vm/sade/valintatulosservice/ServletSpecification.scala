package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.json.JsonFormats
import org.scalatra.test.HttpComponentsClient
import org.specs2.mutable.Specification
import org.specs2.specification.{Fragments, Step}

trait ServletSpecification extends Specification with ITSetup with TimeWarp with HttpComponentsClient {
  sequential

  def baseUrl = "http://localhost:" + SharedJetty.port + "/valinta-tulos-service"
  implicit val formats = JsonFormats.jsonFormats
  val hakemusFixtureImporter = HakemusFixtures()

  override def map(fs: => Fragments) = {
    Step(SharedJetty.start) ^ super.map(fs)
  }
}


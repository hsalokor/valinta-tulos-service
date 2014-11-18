package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.tcp.PortChecker
import fi.vm.sade.valintatulosservice.{JettyLauncher, TimeWarp}
import org.scalatra.test.HttpComponentsClient
import org.specs2.mutable.Specification
import org.specs2.specification.{Fragments, Step}

trait ServletSpecification extends Specification with TimeWarp with HttpComponentsClient {
  def baseUrl = "http://localhost:" + SharedJetty.port + "/valinta-tulos-service"
  implicit val appConfig: AppConfig = new AppConfig.IT
  implicit val formats = JsonFormats.jsonFormats
  val hakemusFixtureImporter = HakemusFixtures()

  override def map(fs: => Fragments) = {
    Step(SharedJetty.start) ^ super.map(fs)
  }
  sequential
}

object SharedJetty {
  private lazy val jettyLauncher = new JettyLauncher(PortChecker.findFreeLocalPort, Some("it"))

  def port = jettyLauncher.port

  def start {
    jettyLauncher.start
  }
}
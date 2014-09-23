package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}
import fi.vm.sade.sijoittelu.tulos.testfixtures.{FixtureImporter => SijoitteluFixtureImporter}

trait ITSetup extends Specification {
  implicit val appConfig = new AppConfig.IT

  def useFixture(fixtureName: String) {
    SijoitteluFixtureImporter.importFixture(appConfig.sijoitteluContext.database, fixtureName, true)
  }

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

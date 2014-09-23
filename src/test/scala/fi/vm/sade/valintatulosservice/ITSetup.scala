package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}

trait ITSetup extends Specification {
  implicit val appConfig = new AppConfig.IT

  def useFixture(fixtureName: String) {
    SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, fixtureName, true)
  }

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

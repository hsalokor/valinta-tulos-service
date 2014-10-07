package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}
import fi.vm.sade.valintatulosservice.ohjausparametrit.StubbedOhjausparametritService
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritFixtures

trait ITSetup extends Specification {
  implicit val appConfig = new AppConfig.IT

  def useFixture(fixtureName: String, ohjausparametritFixture: String = OhjausparametritFixtures.DEFAULT_FIXTURE) {
    SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, fixtureName, true)
    OhjausparametritFixtures.activeFixture = ohjausparametritFixture
  }

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

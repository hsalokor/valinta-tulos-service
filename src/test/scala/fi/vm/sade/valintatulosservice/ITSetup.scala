package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}
import fi.vm.sade.valintatulosservice.ohjausparametrit.StubbedOhjausparametritService
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritFixtures

trait ITSetup extends Specification {
  implicit val appConfig = new AppConfig.IT

  def useFixture(
                  fixtureName: String,
                  ohjausparametritFixture: String = OhjausparametritFixtures.vastaanottoLoppuu2100,
                  hakuFixture: String) {
    SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, fixtureName, true)
    OhjausparametritFixtures.activeFixture = ohjausparametritFixture
    HakuFixtures.activeFixture = hakuFixture
  }

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

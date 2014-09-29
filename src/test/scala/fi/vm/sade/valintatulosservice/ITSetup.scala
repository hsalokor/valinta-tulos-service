package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}
import fi.vm.sade.valintatulosservice.ohjausparametrit.StubbedOhjausparametritService

trait ITSetup extends Specification {
  implicit val appConfig = new AppConfig.IT

  def useFixture(fixtureName: String, ohjausparametritFixture: String = "vastaanotto-loppuu-2100") {
    SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, fixtureName, true)
    appConfig.ohjausparametritService.asInstanceOf[StubbedOhjausparametritService].fixture = ohjausparametritFixture
  }

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

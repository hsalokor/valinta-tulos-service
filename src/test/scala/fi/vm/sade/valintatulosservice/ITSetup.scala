package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}

trait ITSetup extends Specification {
  implicit val appConfig = new AppConfig.IT

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

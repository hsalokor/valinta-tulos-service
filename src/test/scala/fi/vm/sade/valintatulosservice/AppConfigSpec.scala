package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluSpringContext
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.{ExampleTemplatedProps, AppConfig}
import org.specs2.mutable.Specification

class AppConfigSpec extends Specification {
  "Config with default profile" should {
    "Start up" in {
      validateConfig(new AppConfig with ExampleTemplatedProps {
        def springConfiguration = new SijoitteluSpringContext.Default()
      })
    }
  }

  "Config with dev profile" should {
    "Start up" in {
      validateConfig(new AppConfig.Dev())
    }
  }

  def validateConfig(config: AppConfig) = {
    config.springContext.raportointiService
    success
  }
}


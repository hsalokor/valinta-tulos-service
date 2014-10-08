package fi.vm.sade.valintatulosservice.testenvironment

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.TarjontaHakuService
import org.specs2.mutable.Specification

class TarjontaIntegrationTest extends Specification {
  "HakuService" should {
    "Extract response from tarjonta API"in {
      val haku = new TarjontaHakuService(new AppConfig.IT).getHaku("1.2.246.562.5.2013080813081926341927").get
      haku.toinenAste must_== true
    }
  }
}

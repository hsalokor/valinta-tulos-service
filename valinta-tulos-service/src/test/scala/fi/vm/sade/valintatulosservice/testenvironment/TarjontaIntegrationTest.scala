package fi.vm.sade.valintatulosservice.testenvironment

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.TarjontaHakuService
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TarjontaIntegrationTest extends Specification {
  "HakuService" should {
    "Extract response from tarjonta API"in {
      val haku = new TarjontaHakuService(null, new AppConfig.IT).getHaku("1.2.246.562.5.2013080813081926341927").get
      haku.korkeakoulu must_== false
      haku.yhteishaku must_== true
    }
  }

  "HakuService fail case" should {
    "return None for non existing haku ID" in {
      val haku = new TarjontaHakuService(null, new AppConfig.IT).getHaku("987654321")
      haku must_== None
    }
  }
}

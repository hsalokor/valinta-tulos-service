package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.ITSpecification
import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}

class HakuServiceSpec extends ITSpecification {
  val service = HakuService(appConfig)

  "HakuService" should {
    "löytää varsinaisen haun lisähaut" in {
      HakuFixtures.useFixture(HakuFixtures.korkeakouluYhteishaku)
      val haku = service.getHaku(HakuFixtures.korkeakouluYhteishaku)
      val liittyvätHaut = service.findLiittyvatHaut(haku.get)
      liittyvätHaut must_== Set("korkeakoulu-lisahaku1", "korkeakoulu-lisahaku2")
    }

    "löytää lisähaun muut lisähaut ja varsinaisen haun" in {
      HakuFixtures.useFixture(HakuFixtures.korkeakouluLisahaku1)
      val haku = service.getHaku(HakuFixtures.korkeakouluLisahaku1)
      val liittyvätHaut = service.findLiittyvatHaut(haku.get)
      liittyvätHaut must_== Set("korkeakoulu-yhteishaku", "korkeakoulu-lisahaku2")
    }

    "löytää kaikki haut, jotka on JULKAISTU" in {
      HakuFixtures.useFixture(HakuFixtures.korkeakouluLisahaku1)
      service.kaikkiJulkaistutHaut must_== Nil
      HakuFixtures.useFixture(HakuFixtures.korkeakouluYhteishaku)
      service.kaikkiJulkaistutHaut.size must_== 1
    }
  }
}
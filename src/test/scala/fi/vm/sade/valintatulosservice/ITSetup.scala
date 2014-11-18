package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritFixtures
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures

trait ITSetup {
  implicit val appConfig = new AppConfig.IT

  def useFixture(
                  fixtureName: String,
                  ohjausparametritFixture: String = OhjausparametritFixtures.vastaanottoLoppuu2100,
                  hakemusFixtures: List[String] = HakemusFixtures.defaultFixtures,
                  hakuFixture: String = HakuFixtures.korkeakouluYhteishaku) {
    SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, fixtureName, true)
    OhjausparametritFixtures.activeFixture = ohjausparametritFixture
    HakuFixtures.activeFixture = hakuFixture
    hakemusFixtures.foreach(HakemusFixtures().importFixture(_))
  }
}

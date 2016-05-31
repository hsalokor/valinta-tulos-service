package fi.vm.sade.valintatulosservice

import com.typesafe.config.ConfigValueFactory
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritFixtures
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures
import fi.vm.sade.valintatulosservice.valintarekisteri.ValintarekisteriDb

trait ITSetup {
  implicit val appConfig = new AppConfig.IT
  val dbConfig = appConfig.settings.valintaRekisteriDbConfig

  lazy val singleConnectionValintarekisteriDb = new ValintarekisteriDb(
    dbConfig.withValue("connectionPool", ConfigValueFactory.fromAnyRef("disabled")))

  lazy val valintarekisteriDbWithPool = new ValintarekisteriDb(dbConfig)

  lazy val hakemusFixtureImporter = HakemusFixtures()(appConfig)

  lazy val sijoitteluFixtures = SijoitteluFixtures(appConfig.sijoitteluContext.database, singleConnectionValintarekisteriDb)

  def useFixture(fixtureName: String,
                 extraFixtureNames: List[String] = List(),
                 ohjausparametritFixture: String = OhjausparametritFixtures.vastaanottoLoppuu2100,
                 hakemusFixtures: List[String] = HakemusFixtures.defaultFixtures,
                 hakuFixture: String = HakuFixtures.korkeakouluYhteishaku,
                 yhdenPaikanSaantoVoimassa: Boolean = false,
                 kktutkintoonJohtava: Boolean = false
                ) {

    sijoitteluFixtures.importFixture(fixtureName, true, yhdenPaikanSaantoVoimassa, kktutkintoonJohtava)
    extraFixtureNames.map(fixtureName =>
      sijoitteluFixtures.importFixture(fixtureName, false, yhdenPaikanSaantoVoimassa, kktutkintoonJohtava)
    )

    OhjausparametritFixtures.activeFixture = ohjausparametritFixture
    HakuFixtures.useFixture(hakuFixture)
    hakemusFixtureImporter.clear
    hakemusFixtures.foreach(hakemusFixtureImporter.importFixture)
  }
}

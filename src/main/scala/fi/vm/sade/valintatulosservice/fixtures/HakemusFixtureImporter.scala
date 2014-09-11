package fi.vm.sade.valintatulosservice.fixtures

import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

object HakemusFixtureImporter {

  def importData(config: MongoConfig) = {
    val db = MongoFactory.createDB(config)
    MongoMockData.insertData(db.underlying, MongoMockData.readJson("fixtures/hakemus/00000878229.json"))
  }
}

package fi.vm.sade.valintatulosservice.fixtures

import com.mongodb.BasicDBObject
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

class HakemusFixtureImporter(config: MongoConfig) {
  lazy val db = MongoFactory.createDB(config)

  def clear = {
    db.getCollection("application").remove(new BasicDBObject())
    this
  }

  def importData: HakemusFixtureImporter = {
    List("fixtures/hakemus/00000878229.json", "fixtures/hakemus/00000441369.json", "fixtures/hakemus/00000441370.json").foreach(importData(_))
    this
  }

  def importData(filename: String): HakemusFixtureImporter = {
    MongoMockData.insertData(db.underlying, MongoMockData.readJson(filename))
    this
  }
}

package fi.vm.sade.valintatulosservice.hakemus

import com.mongodb.BasicDBObject
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

class HakemusFixtures(config: MongoConfig) {
  lazy val db = MongoFactory.createDB(config)

  if (config.url.indexOf("localhost") < 0)
    throw new IllegalArgumentException("HakemusFixtureImporter can only be used with IT profile")

  def clear = {
    db.getCollection("application").remove(new BasicDBObject())
    this
  }

  def importData: HakemusFixtures = {
    List("00000878229", "00000441369", "00000441370").foreach(importData(_))
    this
  }

  def importData(fixtureName: String): HakemusFixtures = {
    val filename = "fixtures/hakemus/" + fixtureName + ".json"
    MongoMockData.insertData(db.underlying, MongoMockData.readJson(filename))
    this
  }
}

object HakemusFixtures {
  def apply()(implicit appConfig: AppConfig) = {
    new HakemusFixtures(appConfig.settings.hakemusMongoConfig)
  }
}
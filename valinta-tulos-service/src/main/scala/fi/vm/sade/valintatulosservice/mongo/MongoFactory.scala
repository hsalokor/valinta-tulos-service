package fi.vm.sade.valintatulosservice.mongo

import com.mongodb.casbah.{ReadPreference, MongoClient, MongoClientURI}
import fi.vm.sade.utils.config.MongoConfig

object MongoFactory {

  def createDB(config: MongoConfig) = {
    val client = MongoClient(MongoClientURI(config.url))
    client.setReadPreference(ReadPreference.Primary)
    client(config.dbname)
  }

  def createCollection(config: MongoConfig, collection: String) = {
    createDB(config)(collection)
  }
}

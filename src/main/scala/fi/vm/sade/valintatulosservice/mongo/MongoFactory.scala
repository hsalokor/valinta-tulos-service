package fi.vm.sade.valintatulosservice.mongo

import com.mongodb.casbah.{MongoClient, MongoClientURI}
import fi.vm.sade.valintatulosservice.config.MongoConfig

object MongoFactory {

  def createDB(config: MongoConfig) = {
    MongoClient(MongoClientURI(config.url))(config.dbname)
  }

  def createCollection(config: MongoConfig, collection: String) = {
    createDB(config)(collection)
  }
}

package fi.vm.sade.valintatulosservice.experimental

import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

object MailPollerProd extends App {
  val url = System.getProperty("mongo.uri")
  val valintatulos = MongoFactory.createCollection(MongoConfig(url, "sijoitteludb", "Valintatulos"))
  val query = MongoDBObject(
    "hakuOid" -> "1.2.246.562.5.2013080813081926341927",
    "julkaistavissa" -> true,
    "tila" -> "KESKEN",
    "lahetetty" -> MongoDBObject("$ne" -> true)
  )
  val cursor = valintatulos.find(query).sort(MongoDBObject("wat" -> 1)).limit(1000)
  println(cursor.toList.size)
}

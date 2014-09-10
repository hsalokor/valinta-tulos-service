package fi.vm.sade.valintatulosservice.hakemus

import com.mongodb.casbah
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.config.MongoConfig

class HakemusRepository()(implicit appConfig: AppConfig) {

  val applicationCollection: casbah.MongoCollection = {
    val mongoConfig: MongoConfig = appConfig.settings.hakemusMongoConfig
    MongoClient(MongoClientURI(mongoConfig.url))(mongoConfig.dbname)(mongoConfig.collection)
  }

  def findHakutoiveOids(hakemusOid: String): Option[List[String]] = {
    val query = MongoDBObject("oid" -> hakemusOid)
    val fields = MongoDBObject("answers.hakutoiveet" -> 1)
    val res = applicationCollection.findOne(query, fields)
    res.flatMap { result =>
      result.getAs[MongoDBObject]("answers").flatMap { answers =>
        answers.getAs[MongoDBObject]("hakutoiveet").map { hakutoiveet =>
          hakutoiveet.filter { case (k, v) =>
            k.endsWith("Koulutus-id") && !v.asInstanceOf[String].isEmpty
          }.map { case (k, v) => v.asInstanceOf[String]}.toList
        }
      }
    }
  }
}

package fi.vm.sade.valintatulosservice.hakemus

import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Hakemus
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

class HakemusRepository()(implicit appConfig: AppConfig) {

  val application = MongoFactory.createCollection(appConfig.settings.hakemusMongoConfig)

  def findHakutoiveOids(hakemusOid: String): Option[Hakemus] = {
    val query = MongoDBObject("oid" -> hakemusOid)
    val fields = MongoDBObject("answers.hakutoiveet" -> 1)
    val res = application.findOne(query, fields)

    res.flatMap { result =>
      result.getAs[MongoDBObject]("answers").flatMap { answers =>
        answers.getAs[MongoDBObject]("hakutoiveet").map { hakutoiveet =>
          val toiveet = hakutoiveet.filter { case (k, v) =>
            k.endsWith("Koulutus-id") && !v.asInstanceOf[String].isEmpty
          }.toList
            .sortWith { _._1 < _._1}
            .map { case (k, v) => v.asInstanceOf[String]}
          Hakemus(hakemusOid, toiveet)
        }
      }
    }
  }

}

package fi.vm.sade.valintatulosservice.hakemus

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Hakutoive, Hakemus}
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

object DatabaseKeys {
  val oidKey: String = "oid"
  val hakutoiveetPath: String = "answers.hakutoiveet"
  val answersKey: String = "answers"
  val hakutoiveetKey: String = "hakutoiveet"
  val hakutoiveKeyPostfix: String = "Koulutus-id"
  val tarjoajaKeyPostfix: String = "Opetuspiste-id"
}

class HakemusRepository()(implicit appConfig: AppConfig) {

  val application = MongoFactory.createCollection(appConfig.settings.hakemusMongoConfig)


  def findHakutoiveOids(hakemusOid: String): Option[Hakemus] = {
    val query = MongoDBObject(DatabaseKeys.oidKey -> hakemusOid)
    val fields = MongoDBObject(DatabaseKeys.hakutoiveetPath -> 1)
    val res = application.findOne(query, fields)

    res.flatMap { result =>
      result.getAs[MongoDBObject](DatabaseKeys.answersKey).flatMap { answers =>
        answers.getAs[MongoDBObject](DatabaseKeys.hakutoiveetKey).map { hakutoiveet =>
          Hakemus(hakemusOid, parseHakutoiveet(hakutoiveet))
        }
      }
    }
  }

  def parseHakutoiveet(data: Imports.MongoDBObject): List[Hakutoive] = {
    data.filter { case (key, value) =>
      key.endsWith(DatabaseKeys.hakutoiveKeyPostfix) && !value.asInstanceOf[String].isEmpty
    }.toList.sortWith {
      _._1 < _._1
    }.map {
      case (hakukohdeKey, hakuKohdeOid) => {
       Hakutoive(hakuKohdeOid.asInstanceOf[String], parseTarjoajaOid(data, hakukohdeKey))
      }
    }
  }

  def parseTarjoajaOid(data: MongoDBObject, hakukohdeKey: String): String = {
    data.find {
      case (tarjoajaKey, tarjoajaOId) =>  {
        hakukohdeKey.regionMatches(0, tarjoajaKey, 0, 11) && tarjoajaKey.endsWith(DatabaseKeys.tarjoajaKeyPostfix)
      }
    }.map {
      case (_, tarjoajaOid) => tarjoajaOid.asInstanceOf[String]
    }.getOrElse("")
  }
}

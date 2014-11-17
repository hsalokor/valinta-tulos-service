package fi.vm.sade.valintatulosservice.hakemus

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Hakutoive, Hakemus}
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

object DatabaseKeys {
  val oidKey: String = "oid"
  val applicationSystemIdKey: String = "applicationSystemId"
  val hakutoiveetPath: String = "answers.hakutoiveet"
  val answersKey: String = "answers"
  val hakutoiveetKey: String = "hakutoiveet"
  val hakutoiveKeyPostfix: String = "Koulutus-id"
  val tarjoajaKeyPostfix: String = "Opetuspiste-id"
}

class HakemusRepository()(implicit appConfig: AppConfig) {

  val application = MongoFactory.createCollection(appConfig.settings.hakemusMongoConfig, "application")

  def findHakemukset(hakuOid: String): Seq[Hakemus] = {
    val query = MongoDBObject(DatabaseKeys.applicationSystemIdKey -> hakuOid)
    val fields = MongoDBObject(DatabaseKeys.hakutoiveetPath -> 1, DatabaseKeys.oidKey -> 1)
    val cursor = application.find(query, fields)

    (for (
      hakemus <- cursor
    ) yield for (
        hakemusOid <- hakemus.getAs[String](DatabaseKeys.oidKey);
        answers <- hakemus.getAs[MongoDBObject](DatabaseKeys.answersKey);
        hakutoiveet <- answers.getAs[MongoDBObject](DatabaseKeys.hakutoiveetKey)
      ) yield Hakemus(hakemusOid, parseHakutoiveet(hakutoiveet))).flatten.toSeq
  }

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

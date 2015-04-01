package fi.vm.sade.valintatulosservice.hakemus

import fi.vm.sade.utils.slf4j.Logging
import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Henkilotiedot, Hakutoive, Hakemus}
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

object DatabaseKeys {
  val oidKey: String = "oid"
  val personOidKey: String = "personOid"
  val applicationSystemIdKey: String = "applicationSystemId"
  val hakutoiveetPath: String = "answers.hakutoiveet"
  val hakutoiveetSearchPath: String = "authorizationMeta.applicationPreferences.preferenceData.Koulutus-id"
  val henkilotiedotPath: String = "answers.henkilotiedot"
  val answersKey: String = "answers"
  val hakutoiveetKey: String = "hakutoiveet"
  val hakutoiveKeyPostfix: String = "Koulutus-id"
  val tarjoajaKeyPostfix: String = "Opetuspiste-id"
  val lisatiedotPath: String = "answers.lisatiedot"
  val asiointiKieliKey: String = "lisatiedot.asiointikieli"
}

@deprecated("Should be removed ASAP. Has no idea of indexes. Also has no idea of search structures")
class HakemusRepository()(implicit appConfig: AppConfig) extends Logging {
  val application = MongoFactory.createCollection(appConfig.settings.hakemusMongoConfig, "application")
  val fields = MongoDBObject(
    DatabaseKeys.hakutoiveetPath -> 1,
    DatabaseKeys.henkilotiedotPath -> 1,
    DatabaseKeys.applicationSystemIdKey -> 1,
    DatabaseKeys.oidKey -> 1,
    DatabaseKeys.personOidKey -> 1,
    DatabaseKeys.lisatiedotPath -> 1
  )

  val kieliKoodit = Map(("suomi", "FI"), ("ruotsi", "SV"), ("englanti", "EN"))

  def findHakemukset(hakuOid: String): Seq[Hakemus] = {
    val query = MongoDBObject(DatabaseKeys.applicationSystemIdKey -> hakuOid)
    val cursor = application.find(query, fields)
    (for {
      hakemus <- cursor
      h <- parseHakemus(hakemus)
    } yield { h }).toList
  }

  def findHakemus(hakemusOid: String): Option[Hakemus] = {
    val query = MongoDBObject(DatabaseKeys.oidKey -> hakemusOid)
    val res = application.findOne(query, fields)

    res.flatMap(parseHakemus(_))
  }

  def findHakemukset(hakuOid: String, personOid: String): List[Hakemus] = {
    val query = MongoDBObject(DatabaseKeys.personOidKey -> personOid,
    DatabaseKeys.applicationSystemIdKey -> hakuOid)
    val cursor = application.find(query, fields)
    (for {
      hakemus <- cursor
      h <- parseHakemus(hakemus)
    } yield { h }).toList
  }

  def findHakemuksetByHakukohde(hakuOid: String, hakukohdeOid: String): List[Hakemus] = {
    val query = MongoDBObject(DatabaseKeys.applicationSystemIdKey -> hakuOid,
      DatabaseKeys.hakutoiveetSearchPath -> hakukohdeOid)

    val cursor = application.find(query, fields)

    val hkms: List[Hakemus] =
    (for {
      hakemus <- cursor
      h <- parseHakemus(hakemus)
    } yield { h }).toList

    return hkms;
  }

  private def parseHakemus(data: Imports.MongoDBObject): Option[Hakemus] = {
    for {
      hakemusOid <- data.getAs[String](DatabaseKeys.oidKey)
      hakuOid <- data.getAs[String](DatabaseKeys.applicationSystemIdKey)
      henkiloOid <- data.getAs[String](DatabaseKeys.personOidKey)
      answers <- data.getAs[MongoDBObject](DatabaseKeys.answersKey)
      asiointikieli = parseAsiointikieli(answers.expand[String](DatabaseKeys.asiointiKieliKey))
      hakutoiveet <- answers.getAs[MongoDBObject](DatabaseKeys.hakutoiveetKey)
      henkilotiedot <- answers.getAs[MongoDBObject]("henkilotiedot")
    } yield {
      Hakemus(hakemusOid, hakuOid, henkiloOid, asiointikieli, parseHakutoiveet(hakutoiveet), parseHenkilotiedot(henkilotiedot))
    }
  }

  private def parseAsiointikieli(asiointikieli: Option[String]): String = {
    kieliKoodit.getOrElse(asiointikieli.getOrElse(""), "FI")
  }

  private def parseHenkilotiedot(data: Imports.MongoDBObject): Henkilotiedot = {
    Henkilotiedot(emptyStringToNone(data.getAs[String]("Kutsumanimi")), emptyStringToNone(data.getAs[String]("Sähköposti")), data.getAs[String]("Henkilotunnus").isDefined)
  }

  private def parseHakutoiveet(data: Imports.MongoDBObject): List[Hakutoive] = {
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

  private def emptyStringToNone(o: Option[String]): Option[String] = o.flatMap {
    case "" => None
    case s => Some(s)
  }
}

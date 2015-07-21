package fi.vm.sade.valintatulosservice.vastaanottomeili

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.utils.config.MongoConfig
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, Hakutoiveentulos}
import fi.vm.sade.valintatulosservice.json.JsonFormats._
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import org.joda.time.{DateTime, DateTimeUtils}

class ValintatulosMongoCollection(mongoConfig: MongoConfig) {
  private val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

  def pollForCandidates(hakuOids: List[String], limit: Int, recheckIntervalHours: Int = 24,  excludeHakemusOids: Set[String] = Set.empty): Set[HakemusIdentifier] = {
    val query = Map(
      "hakuOid" -> Map("$in" -> hakuOids),
      "tila" -> "KESKEN",
      "julkaistavissa" -> true,
      "mailStatus.sent" -> Map("$exists" -> false),
      "$or" -> List(
        Map("mailStatus.previousCheck" -> Map("$lt" -> new DateTime().minusHours(recheckIntervalHours).toDate)),
        Map("mailStatus.previousCheck" -> Map("$exists" -> false))
      )
    )

    val candidates = valintatulos.find(query)
      .filterNot { tulos =>
      val hakemusOid = tulos.get("hakemusOid").asInstanceOf[String]
      excludeHakemusOids.contains(hakemusOid)
    }
      .take(limit)
      .toList
      .map{ tulos => HakemusIdentifier(tulos.get("hakuOid").asInstanceOf[String], tulos.get("hakemusOid").asInstanceOf[String])}

    updateCheckTimestamps(candidates.map(_.hakemusOid))

    candidates.toSet
  }

  def alreadyMailed(hakemus: Hakemuksentulos, hakutoive: Hakutoiveentulos): Boolean = {
    val query = Map(
      "hakukohdeOid" -> hakutoive.hakukohdeOid,
      "hakemusOid" -> hakemus.hakemusOid,
      "mailStatus.sent" -> Map(
        "$exists" -> true
      )
    )
    valintatulos.count(query) > 0
  }

  def addMessage(hakemus: HakemusMailStatus, hakukohde: HakukohdeMailStatus, message: String): Unit = {
    updateValintatulos(hakemus.hakemusOid, hakukohde.hakukohdeOid, Map("mailStatus.message" -> message))
  }

  def markAsNonMailable(hakemus: HakemusMailStatus, hakukohde: HakukohdeMailStatus, message: String) = {
    markAsSent(hakemus.hakemusOid, hakukohde.hakukohdeOid, Nil, message)
  }

  def markAsSent(mailContents: LahetysKuittaus) {
    mailContents.hakukohteet.foreach { hakukohde =>
      markAsSent(mailContents.hakemusOid, hakukohde, mailContents.mediat, "Lähetetty " + formatJson(mailContents.mediat))
    }
  }

  private def markAsSent(hakemusOid: String, hakuKohdeOid: String, sentViaMedias: List[String], message: String) {
    val timestamp = DateTimeUtils.currentTimeMillis
    val fields: Map[JSFunction, Any] = Map("mailStatus.sent" -> timestamp, "mailStatus.media" -> sentViaMedias, "mailStatus.message" -> message)

    updateValintatulos(hakemusOid, hakuKohdeOid, fields)
  }

  private def updateValintatulos(hakemusOid: String, hakuKohdeOid: String, fields: Map[Imports.JSFunction, Any]): Unit = {
    val query = MongoDBObject("hakemusOid" -> hakemusOid, "hakukohdeOid" -> hakuKohdeOid)
    val update = Map("$set" -> fields)
    valintatulos.update(query, update, multi = true)
  }

  private def updateCheckTimestamps(hakemusOids: List[String]) = {
    val timestamp = new DateTime().toDate

    val update = Map(
      "$set" -> Map(
        "mailStatus" -> Map(
          "previousCheck" -> timestamp
        )
      )
    )

    val query = MongoDBObject(
      "hakemusOid" -> Map(
        "$in" -> hakemusOids
      )
    )

    valintatulos.update(query, update, multi = true)
  }
}

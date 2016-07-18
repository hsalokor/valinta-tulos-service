package fi.vm.sade.valintatulosservice.vastaanottomeili

import java.util.Date

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.utils.config.MongoConfig
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, Hakutoiveentulos}
import fi.vm.sade.valintatulosservice.json.JsonFormats._
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import org.joda.time.{DateTime, DateTimeUtils}

class ValintatulosMongoCollection(mongoConfig: MongoConfig) {
  private val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

  def pollForCandidates(hakuOids: List[String], limit: Int, recheckIntervalHours: Int = (24 * 3),  excludeHakemusOids: Set[String] = Set.empty): Set[HakemusIdentifier] = {
    val query = Map(
      "hakuOid" -> Map("$in" -> hakuOids),
      "julkaistavissa" -> true,
      "mailStatus.done" -> Map("$exists" -> false),
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
      .map{ tulos => HakemusIdentifier(tulos.get("hakuOid").asInstanceOf[String],
        tulos.get("hakemusOid").asInstanceOf[String],
        tulos.expand[java.util.Date]("mailStatus.sent"))}

    updateCheckTimestamps(candidates.map(_.hakemusOid))

    candidates.toSet
  }

  def alreadyMailed(hakemusOid: String, hakukohdeOid: String): Option[java.util.Date] = {
    val result = valintatulos.findOne(
      ("hakukohdeOid" $eq hakukohdeOid) ++
        ("hakemusOid" $eq hakemusOid) ++
        ("mailStatus.sent" $exists(true))
    )
    result.flatMap(_.expand[java.util.Date]("mailStatus.sent"))
  }

  def addMessage(hakemus: HakemusMailStatus, hakukohde: HakukohdeMailStatus, message: String): Unit = {
    updateValintatulos(hakemus.hakemusOid, hakukohde.hakukohdeOid, Map("mailStatus.message" -> message))
  }

  def markAsSent(mailContents: LahetysKuittaus) {
    mailContents.hakukohteet.foreach { hakukohde =>
      markAsSent(mailContents.hakemusOid, hakukohde, mailContents.mediat, "Lähetetty " + formatJson(mailContents.mediat))
    }
  }

  def markAsNonMailable(hakemusOid: String, hakuKohdeOid: String, message: String) {
    val timestamp = new Date(DateTimeUtils.currentTimeMillis)
    val fields: Map[JSFunction, Any] = Map("mailStatus.done" -> timestamp, "mailStatus.message" -> message)

    updateValintatulos(hakemusOid, hakuKohdeOid, fields)
  }

  private def markAsSent(hakemusOid: String, hakuKohdeOid: String, sentViaMedias: List[String], message: String) {
    val timestamp = new Date(DateTimeUtils.currentTimeMillis)
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
        "mailStatus.previousCheck" -> timestamp
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

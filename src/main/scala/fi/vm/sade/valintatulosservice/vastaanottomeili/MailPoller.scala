package fi.vm.sade.valintatulosservice.vastaanottomeili

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.utils.config.MongoConfig
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.{Valintatila, Hakemuksentulos, Hakutoiveentulos, Vastaanotettavuustila}
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.json.JsonFormats._
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.ohjausparametrit.{Ohjausparametrit, OhjausparametritService}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.ValintatulosService
import org.joda.time.{DateTime, DateTimeUtils}

class MailPoller(mongoConfig: MongoConfig, valintatulosService: ValintatulosService, hakuService: HakuService, ohjausparameteritService: OhjausparametritService, val limit: Integer, recheckIntervalHours: Int = 24) extends Logging {
  private val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

  def etsiHaut: List[String] = {
    val found = hakuService.kaikkiJulkaistutHaut
      .filter{haku => haku.korkeakoulu}
      .filter{haku =>
        val include = haku.hakuAjat.isEmpty || haku.hakuAjat.exists(hakuaika => hakuaika.hasStarted)
        if (!include) logger.debug("Pudotetaan haku " + haku.oid + " koska hakuaika ei alkanut")
        include
       }
      .filter{haku =>
        ohjausparameteritService.ohjausparametrit(haku.oid) match {
          case Some(Ohjausparametrit(_, _, _, Some(hakuKierrosPaattyy))) =>
            val include = new DateTime().isBefore(hakuKierrosPaattyy)
            if (!include) logger.debug("Pudotetaan haku " + haku.oid + " koska hakukierros päättyy " + hakuKierrosPaattyy)
            include
          case x =>
            true
        }
      }
      .map(_.oid)


    logger.info("haut {}", formatJson(found))
    found
  }

  def pollForMailables(hakuOids: List[String] = etsiHaut, limit: Int = this.limit, excludeHakemusOids: Set[String] = Set.empty): List[HakemusMailStatus] = {
    val candidates = pollForCandidates(hakuOids, limit, excludeHakemusOids)
    val statii: Set[HakemusMailStatus] = for {
      candidateId <- candidates
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
    } yield {
      mailStatusFor(hakemuksenTulos)
    }
    val mailables = statii.filter(_.anyMailToBeSent).toList
    logger.info("found {} mailables from {} candidates", mailables.size, candidates.size)

    for {
      hakemus <- statii
      hakukohde <- hakemus.hakukohteet
    } {
      hakukohde.status match {
        case MailStatus.MAILED =>
          // already mailed. why here?
        case MailStatus.NEVER_MAIL =>
          markAsNonMailable(hakemus, hakukohde, hakukohde.message)
        case _ =>
          addMessage(hakemus, hakukohde, hakukohde.message)
      }
    }

    if (candidates.size > 0 && mailables.size < limit) {
      logger.debug("fetching more mailables")
      mailables ++ pollForMailables(hakuOids, limit = limit - mailables.size, excludeHakemusOids = excludeHakemusOids ++ mailables.map(_.hakemusOid).toSet)
    } else {
      mailables
    }
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

  def updateValintatulos(hakemusOid: String, hakuKohdeOid: String, fields: Map[Imports.JSFunction, Any]): Unit = {
    val query = MongoDBObject("hakemusOid" -> hakemusOid, "hakukohdeOid" -> hakuKohdeOid)
    val update = Map("$set" -> fields)
    println("update valintatulos: " + fields)
    valintatulos.update(query, update, multi = true)
  }

  private def mailStatusFor(hakemuksenTulos: Hakemuksentulos): HakemusMailStatus = {
    val mailables = hakemuksenTulos.hakutoiveet.map { (hakutoive: Hakutoiveentulos) =>
      val (status, message) = if (mailed(hakemuksenTulos, hakutoive)) {
        (MailStatus.MAILED, "Already mailed")
      } else if (Vastaanotettavuustila.isVastaanotettavissa(hakutoive.vastaanotettavuustila)) {
        (MailStatus.SHOULD_MAIL, "Vastaanotettavissa (" + hakutoive.valintatila + ")")
      } else if (!Valintatila.isHyväksytty(hakutoive.valintatila) && Valintatila.isFinal(hakutoive.valintatila)) {
        (MailStatus.NEVER_MAIL, "Ei hyväksytty (" + hakutoive.valintatila + ")")
      } else {
        (MailStatus.NOT_MAILED, "Ei vastaanotettavissa (" + hakutoive.valintatila + ")")
      }
      HakukohdeMailStatus(hakutoive.hakukohdeOid, hakutoive.valintatapajonoOid, status, hakutoive.vastaanottoDeadline, message)
    }
    HakemusMailStatus(hakemuksenTulos.hakemusOid, mailables)
  }

  private def mailed(hakemus: Hakemuksentulos, hakutoive: Hakutoiveentulos) = {
    val query = Map(
      "hakukohdeOid" -> hakutoive.hakukohdeOid,
      "hakemusOid" -> hakemus.hakemusOid,
      "mailStatus.sent" -> Map(
        "$exists" -> true
      )
    )
    valintatulos.count(query) > 0
  }

  private def fetchHakemuksentulos(id: HakemusIdentifier): Option[Hakemuksentulos] = {
    try {
      valintatulosService.hakemuksentulos(id.hakuOid, id.hakemusOid)
    } catch {
      case e: Exception =>
        logger.error("Error fetching data for email polling. Candidate identifier=" + id, e)
        None
    }
  }

  def pollForCandidates: Set[HakemusIdentifier] = {
    pollForCandidates(etsiHaut)
  }

  def pollForCandidates(hakuOids: List[String], limit: Int = this.limit, excludeHakemusOids: Set[String] = Set.empty): Set[HakemusIdentifier] = {
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

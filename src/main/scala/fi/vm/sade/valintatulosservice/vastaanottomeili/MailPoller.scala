package fi.vm.sade.valintatulosservice.vastaanottomeili

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, Hakutoiveentulos, Vastaanotettavuustila}
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.ohjausparametrit.{Ohjausparametrit, OhjausparametritService}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}
import org.joda.time.DateTime

import scala.collection.SeqLike

class MailPoller(mongoConfig: MongoConfig, valintatulosService: ValintatulosService, hakuService: HakuService, ohjausparameteritService: OhjausparametritService, limit: Integer = 5, recheckIntervalHours: Int = 24) extends Logging {
  private val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

  def etsiHaut: List[String] = {
    val found = hakuService.kaikkiJulkaistutHaut
      .filter{haku => haku.korkeakoulu}
      .filter{haku =>
        val include = haku.hakuAjat.isEmpty || haku.hakuAjat.find { hakuaika => hakuaika.hasStarted}.isDefined
        if (!include) logger.debug("Pudotetaan haku " + haku.oid + " koska hakuaika ei alkanut")
        include
       }
      .filter{haku =>
        ohjausparameteritService.ohjausparametrit(haku.oid) match {
          case Some(Ohjausparametrit(_, _, Some(hakuKierrosPaattyy))) =>
            val include = new DateTime().isBefore(new DateTime(hakuKierrosPaattyy))
            if (!include) logger.debug("Pudotetaan haku " + haku.oid + " koska hakukierros päättyy " + hakuKierrosPaattyy)
            include
          case x =>
            true
        }
      }
      .map(_.oid)

    logger.info("haut=" + found.size)
    found
  }

  def pollForMailables(hakuOids: List[String] = etsiHaut, limit: Int = this.limit, excludeHakemusOids: Set[String] = Set.empty): List[HakemusMailStatus] = {
    val candidates = pollForCandidates(hakuOids, limit, excludeHakemusOids)
    val mailables = (for {
      candidateId <- candidates
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
      mailStatus = mailStatusFor(hakemuksenTulos)
      if (mailStatus.anyMailToBeSent)
    } yield {
      mailStatus
    }).toList
    val result = if (candidates.size > 0 && mailables.size < limit) {
      logger.info("fetching more mailables")
      mailables ++ pollForMailables(hakuOids, limit = limit - mailables.size, excludeHakemusOids = excludeHakemusOids ++ mailables.map(_.hakemusOid).toSet)
    } else {
      mailables
    }
    logger.info("pollForMailables found " + result.size + " results, " + result.filter(_.anyMailToBeSent).size + " actionable")
    result
  }

  def markAsSent(mailContents: LahetysKuittaus) = {
    val timestamp = System.currentTimeMillis()
    mailContents.hakukohteet.foreach { hakukohde =>
      val query = MongoDBObject(
        "hakemusOid" -> mailContents.hakemusOid,
        "hakukohdeOid" -> hakukohde
      )
      val update = Map(
        "$set" -> Map(
          "mailStatus.sent" -> timestamp
        )
      )
      valintatulos.update(query, update, multi = true)
    }
  }

  private def mailStatusFor(hakemuksenTulos: Hakemuksentulos): HakemusMailStatus = {
    val mailables = hakemuksenTulos.hakutoiveet.map { (hakutoive: Hakutoiveentulos) =>
      val shouldMail: Boolean = Vastaanotettavuustila.isVastaanotettavissa(hakutoive.vastaanotettavuustila) && !mailed(hakemuksenTulos, hakutoive)
      HakukohdeMailStatus(hakutoive.hakukohdeOid, hakutoive.valintatapajonoOid, shouldMail)
    }
    HakemusMailStatus(hakemuksenTulos.hakemusOid, mailables)
  }

  private def mailed(hakemus: Hakemuksentulos, hakutoive: Hakutoiveentulos) = {
    // TODO: Valintatulokseen tarvitaan indeksi, tai lähetetty-tieto domain-olioihin

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
    // TODO: Valintatulokseen tarvitaan indeksi

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

    val candidates = valintatulos.find(query).sort(Map("mailStatus.previousCheck" -> 1))
      .toStream
      .filterNot { tulos =>
        val hakemusOid = tulos.get("hakemusOid").asInstanceOf[String]
        excludeHakemusOids.contains(hakemusOid)
      }
      .take(limit)
      .toList
      .map{ tulos => HakemusIdentifier(tulos.get("hakuOid").asInstanceOf[String], tulos.get("hakemusOid").asInstanceOf[String])}

    updateCheckTimestamps(candidates.map(_.hakemusOid))

    logger.info("pollCandidates found " + candidates.size + " candidates for hakuOids= " + hakuOids)

    candidates

      .toSet
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

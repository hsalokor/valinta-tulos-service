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

class MailPoller(mongoConfig: MongoConfig, valintatulosService: ValintatulosService, hakuService: HakuService, ohjausparameteritService: OhjausparametritService, limit: Integer = 5, recheckIntervalHours: Int = 24) extends Logging {
  private val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

  def haut: List[String] = {
    val found = hakuService.kaikkiHaut
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

  def pollForMailables: List[HakemusMailStatus] = {
    pollForMailables(haut)
  }

  def pollForMailables(hakuOids: List[String]): List[HakemusMailStatus] = {
    val mailables = (for {
      candidateId: HakemusIdentifier <- pollForCandidates(hakuOids)
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
    } yield {
      mailStatusFor(hakemuksenTulos)
    }).toList
    logger.info("pollForMailables found " + mailables.size + " results, " + mailables.filter(_.anyMailToBeSent).size + " actionable")
    mailables
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
    pollForCandidates(haut)
  }

  def pollForCandidates(hakuOids: List[String]): Set[HakemusIdentifier] = {
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

    val candidates: List[Imports.DBObject] = valintatulos.find(query).sort(
      Map("mailStatus.previousCheck" -> 1)
    ).limit(limit).toList

    updateCheckTimestamps(candidates)

    logger.info("pollCandidates found " + candidates.size + " candidates for hakuOids= " + hakuOids)

    candidates.map{ tulos => HakemusIdentifier(tulos.get("hakuOid").asInstanceOf[String], tulos.get("hakemusOid").asInstanceOf[String])}
      .toSet
  }

  private def updateCheckTimestamps(candidates: List[Imports.DBObject]) = {
    val timestamp = new DateTime().toDate

    val update = Map(
      "$set" -> Map(
        "mailStatus" -> Map(
          "previousCheck" -> timestamp
        )
      )
    )

    val query = MongoDBObject(
      "_id" -> Map(
        "$in" -> candidates.map(_.get("_id"))
      )
    )

    valintatulos.update(query, update, multi = true)
  }

}

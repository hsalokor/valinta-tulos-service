package fi.vm.sade.valintatulosservice.experimental

import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.domain.{Vastaanotettavuustila, Hakemuksentulos}
import fi.vm.sade.valintatulosservice.{ValintatulosService, Logging}
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.mongo.MongoFactory

class MailPoller(mongoConfig: MongoConfig, valintatulosService: ValintatulosService, limit: Integer = 5) extends Logging {
  val valintatulos = MongoFactory.createCollection(mongoConfig)

  def pollForMailables(hakuOid: String): List[HakemusMailContents] = {
    for {
      candidateId <- pollForCandidates(hakuOid)
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
      // TODO: filter duplicates (sama hakemus voi esiintyä monesti)
      mailContents = mailContentsFor(hakemuksenTulos)
      if (!mailContents.mailables.isEmpty)
    } yield {
      mailContents
    }
  }

  private def mailContentsFor(hakemuksenTulos: Hakemuksentulos): HakemusMailContents = {
    val mailables = for {
      hakutoive <- hakemuksenTulos.hakutoiveet
      if (Vastaanotettavuustila.isVastaanotettavissa(hakutoive.vastaanotettavuustila))
    } yield {
      Mailable(hakutoive.hakukohdeOid)
    }
    HakemusMailContents(hakemuksenTulos.hakemusOid, mailables)
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

  private def pollForCandidates(hakuOid: String) = {
    // TODO: Valintatulokseen tarvitaan indeksi

    val query = MongoDBObject(
      "hakuOid" -> hakuOid,
      "tila" -> "KESKEN",
      "julkaistavissa" -> true,
      "mailStatus.sent" -> MongoDBObject("$exists" -> false)
    )

    val cursor = valintatulos.find(query).sort(
      MongoDBObject("mailStatus.previousCheck" -> 1)
    ).limit(limit)
    cursor.toList.map{ tulos => HakemusIdentifier(tulos.get("hakuOid").asInstanceOf[String], tulos.get("hakemusOid").asInstanceOf[String])}
  }

  case class HakemusIdentifier(hakuOid: String, hakemusOid: String)

  case class HakemusMailContents(hakemusOid: String, mailables: List[Mailable])

  case class Mailable(hakukohdeOid: String)
}

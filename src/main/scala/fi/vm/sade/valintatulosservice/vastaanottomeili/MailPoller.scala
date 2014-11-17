package fi.vm.sade.valintatulosservice.vastaanottomeili

import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, Hakutoiveentulos, Vastaanotettavuustila}
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}

class MailPoller(mongoConfig: MongoConfig, valintatulosService: ValintatulosService, limit: Integer = 5) extends Logging {
  val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

  def pollForMailables(hakuOid: String): List[HakemusMailStatus] = {
    for {
      candidateId <- pollForCandidates(hakuOid)
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
      // TODO: filter duplicates (sama hakemus voi esiintyä monesti)
    } yield {
      mailStatusFor(hakemuksenTulos)
    }
  }

  def markAsHandled(mailContents: HakemusMailStatus) = {
    val timestamp = System.currentTimeMillis()
    mailContents.hakukohteet.foreach { hakukohde =>
      val query = MongoDBObject(
        "hakemusOid" -> mailContents.hakemusOid,
        "hakukohdeOid" -> hakukohde.hakukohdeOid,
        "valintatapajonoOid" -> hakukohde.valintatapajonoOid
      )
      val update = Map(
        "$set" -> Map(
          "mailStatus" -> Map(
            "previousCheck" -> timestamp
          )
        )
      )
      valintatulos.update(query, update)
    }
  }

  private def mailStatusFor(hakemuksenTulos: Hakemuksentulos): HakemusMailStatus = {
    val mailables = hakemuksenTulos.hakutoiveet.map { (hakutoive: Hakutoiveentulos) =>
      // TODO: voi tulla false positiveja?
      HakukohdeMailStatus(hakutoive.hakukohdeOid, hakutoive.valintatapajonoOid, Vastaanotettavuustila.isVastaanotettavissa(hakutoive.vastaanotettavuustila))
    }
    HakemusMailStatus(hakemuksenTulos.hakemusOid, mailables)
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
}

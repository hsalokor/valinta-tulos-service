package fi.vm.sade.valintatulosservice.vastaanottomeili

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, Hakutoiveentulos, Vastaanotettavuustila}
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.{Logging, ValintatulosService}

class MailPoller(mongoConfig: MongoConfig, valintatulosService: ValintatulosService, hakuService: HakuService, limit: Integer = 5) extends Logging {
  private val valintatulos = MongoFactory.createDB(mongoConfig)("Valintatulos")

  def haut = hakuService.kaikkiHaut.filter(_.korkeakoulu).map(_.oid)

  def pollForMailables: List[HakemusMailStatus] = {
    pollForMailables(haut)
  }

  def pollForMailables(hakuOids: List[String]): List[HakemusMailStatus] = {
    for {
      candidateId: HakemusIdentifier <- pollForCandidates(hakuOids).toSet.toList
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
    } yield {
      mailStatusFor(hakemuksenTulos)
    }
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

  def pollForCandidates: List[HakemusIdentifier] = {
    pollForCandidates(haut)
  }

  def pollForCandidates(hakuOids: List[String]): List[HakemusIdentifier] = {
    // TODO: Valintatulokseen tarvitaan indeksi

    val query = Map(
      "hakuOid" -> Map("$in" -> hakuOids),
      "tila" -> "KESKEN",
      "julkaistavissa" -> true,
      "mailStatus.sent" -> Map("$exists" -> false)
    )

    val candidates: List[Imports.DBObject] = valintatulos.find(query).sort(
      Map("mailStatus.previousCheck" -> 1)
    ).limit(limit).toList

    updateCheckTimestamps(candidates)

    candidates.map{ tulos => HakemusIdentifier(tulos.get("hakuOid").asInstanceOf[String], tulos.get("hakemusOid").asInstanceOf[String])}
  }

  private def updateCheckTimestamps(candidates: List[Imports.DBObject]) = {
    val timestamp = System.currentTimeMillis()

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

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
      candidateId: HakemusIdentifier <- pollForCandidates(hakuOid).toSet.toList
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
    } yield {
      mailStatusFor(hakemuksenTulos)
    }
  }

  def markAsHandled(mailContents: HakemusMailStatus) = {
    val timestamp = System.currentTimeMillis()
    mailContents.hakukohteet.foreach { hakukohde =>
      val query = MongoDBObject(
        "hakemusOid" -> mailContents.hakemusOid,
        "hakukohdeOid" -> hakukohde.hakukohdeOid
      )
      var mailStatus = Map(
        "previousCheck" -> timestamp
      )
      if (hakukohde.shouldMail) {
        mailStatus += ("sent" -> timestamp)
      }
      val update = Map(
        "$set" -> Map(
          "mailStatus" -> mailStatus
        )
      )
      valintatulos.update(query, update)
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

  def pollForCandidates(hakuOid: String): List[HakemusIdentifier] = {
    // TODO: Valintatulokseen tarvitaan indeksi

    val query = Map(
      "hakuOid" -> hakuOid,
      "tila" -> "KESKEN",
      "julkaistavissa" -> true,
      "mailStatus.sent" -> Map("$exists" -> false)
    )

    val cursor = valintatulos.find(query).sort(
      Map("mailStatus.previousCheck" -> 1)
    ).limit(limit)
    cursor.toList.map{ tulos => HakemusIdentifier(tulos.get("hakuOid").asInstanceOf[String], tulos.get("hakemusOid").asInstanceOf[String])}
  }
}

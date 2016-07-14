package fi.vm.sade.valintatulosservice.vastaanottomeili

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.{VastaanottoRecord, HakijaVastaanottoRepository}
import fi.vm.sade.valintatulosservice.{VastaanottoService, ValintatulosService}
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.json.JsonFormats._
import fi.vm.sade.valintatulosservice.ohjausparametrit.{Ohjausparametrit, OhjausparametritService}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import org.joda.time.DateTime

class MailPoller(valintatulosCollection: ValintatulosMongoCollection, valintatulosService: ValintatulosService, hakijaVastaanottoRepository: HakijaVastaanottoRepository, hakuService: HakuService, ohjausparameteritService: OhjausparametritService, val limit: Integer) extends Logging {
  def etsiHaut: List[String] = {
    val found = hakuService.kaikkiJulkaistutHaut
      .filter{haku => haku.korkeakoulu}
      .filter{haku =>
        val include = haku.hakuAjat.isEmpty || haku.hakuAjat.exists(hakuaika => hakuaika.hasStarted)
        if (!include) logger.info("Pudotetaan haku " + haku.oid + " koska hakuaika ei alkanut")
        include
       }
      .filter{haku =>
        ohjausparameteritService.ohjausparametrit(haku.oid) match {
          case Some(Ohjausparametrit(_, _, _, Some(hakukierrosPaattyy), _, _)) if hakukierrosPaattyy.isBeforeNow() =>
            logger.info("Pudotetaan haku " + haku.oid + " koska hakukierros päättynyt " + hakukierrosPaattyy)
            false
          case Some(Ohjausparametrit(_, _, _, _, Some(tulostenJulkistusAlkaa), _)) if tulostenJulkistusAlkaa.isAfterNow()=>
            logger.info("Pudotetaan haku " + haku.oid + " koska tulosten julkistus alkaa " + tulostenJulkistusAlkaa)
            false
          case None =>
            logger.warn("Pudotetaan haku " + haku.oid + " koska ei saatu haettua ohjausparametreja")
            false
          case x =>
            true
        }
      }
      .map(_.oid)


    logger.info("haut {}", formatJson(found))
    found
  }

  def searchMailsToSend(limit: Int = this.limit, mailDecorator: MailDecorator): List[Ilmoitus] = {
    val mailCandidates: List[HakemusMailStatus] = pollForMailables(limit = limit)
    val sendableMails: List[Ilmoitus] = mailCandidates.flatMap(mailDecorator.statusToMail)
    logger.info("{} statuses converted to {} mails", mailCandidates.size, sendableMails.size)

    if (sendableMails.size > 0 || mailCandidates.isEmpty) {
      sendableMails
    } else {
      searchMailsToSend(limit, mailDecorator)
    }
  }



  def pollForMailables(hakuOids: List[String] = etsiHaut, limit: Int = this.limit, excludeHakemusOids: Set[String] = Set.empty): List[HakemusMailStatus] = {
    val candidates: Set[HakemusIdentifier] = valintatulosCollection.pollForCandidates(hakuOids, limit, excludeHakemusOids = excludeHakemusOids)
    logger.info("candidates found {}", formatJson(candidates))

    // onlyIfHasKeskenVastaanottotiloja : if hakemuksenTulos.hakutoiveet.exists(hk => hk.vastaanottotila == Vastaanottotila.kesken)
    val statii: Set[HakemusMailStatus] = for {
      candidateId <- candidates
      hakemuksenTulos <- fetchHakemuksentulos(candidateId)
    } yield {
      val (hakijaOid, hakuOid) = (hakemuksenTulos.hakijaOid, hakemuksenTulos.hakuOid)
      val vastaanotot = hakijaVastaanottoRepository.findVastaanottoHistoryHaussa(hakijaOid, hakuOid)
      val uudetVastaanotot: Set[VastaanottoRecord] = candidateId.lastSent match {
        case Some(lastCheck) => vastaanotot.filter(_.timestamp.compareTo(lastCheck) >= 0)
        case None => vastaanotot
      }
      val vanhatVastaanotot: Set[VastaanottoRecord] = candidateId.lastSent match {
        case Some(lastCheck) => vastaanotot.filter(_.timestamp.before(lastCheck))
        case None => Set()
      }

      mailStatusFor(hakemuksenTulos, uudetVastaanotot, vanhatVastaanotot)
    }
    val mailables = statii.filter(_.anyMailToBeSent).toList
    logger.info("found {} mailables from {} candidates", mailables.size, candidates.size)

    saveMessages(statii)

    if (!candidates.isEmpty && mailables.size < limit) {
      logger.debug("fetching more mailables")
      mailables ++ pollForMailables(hakuOids, limit = limit - mailables.size, excludeHakemusOids = excludeHakemusOids ++ mailables.map(_.hakemusOid).toSet)
    } else {
      mailables
    }
  }

  def saveMessages(statii: Set[HakemusMailStatus]): Unit = {
    for {hakemus <- statii
         hakukohde <- hakemus.hakukohteet} {
      hakukohde.status match {
        case MailStatus.MAILED =>
        // already mailed. why here?
        case MailStatus.NEVER_MAIL =>
          valintatulosCollection.markAsNonMailable(hakemus.hakemusOid, hakukohde.hakukohdeOid, hakukohde.message)
        case _ =>
          valintatulosCollection.addMessage(hakemus, hakukohde, hakukohde.message)
      }
    }
  }

  private def hakukohdeMailStatusFor(hakemusOid: String, hakutoive: Hakutoiveentulos, uudetVastaanotot: Set[VastaanottoRecord], vanhatVastaanotot: Set[VastaanottoRecord]) = {
    val alreadySent = valintatulosCollection.alreadyMailed(hakemusOid, hakutoive.hakukohdeOid)
    val (status, reason, message) = if (alreadySent.isDefined) {
      val newestSitovaIsNotVastaanotettuByHakija =
        uudetVastaanotot.toList.sortBy(_.timestamp).headOption
          .filter(_.hakukohdeOid == hakutoive.hakukohdeOid)
          .filter(vastaanotto => vastaanotto.henkiloOid != vastaanotto.ilmoittaja)
          .exists(_.action == VastaanotaSitovasti)
      if(newestSitovaIsNotVastaanotettuByHakija) {
        (MailStatus.SHOULD_MAIL, Some(MailReason.SITOVAN_VASTAANOTON_ILMOITUS), "Sitova vastaanotto")
      } else {
        val newestEhdollinenNotVastaanotettuByHakija =
          uudetVastaanotot.toList.sortBy(_.timestamp).headOption
            .filter(_.hakukohdeOid == hakutoive.hakukohdeOid)
            .filter(vastaanotto => vastaanotto.henkiloOid != vastaanotto.ilmoittaja)
            .exists(_.action == VastaanotaEhdollisesti)

        val atLeastOneOtherEhdollinenVastaanottoCanBeFound =
          (uudetVastaanotot ++ vanhatVastaanotot).filter(_.hakukohdeOid != hakutoive.hakukohdeOid)
            .exists(_.action == VastaanotaEhdollisesti)

        if(newestEhdollinenNotVastaanotettuByHakija && atLeastOneOtherEhdollinenVastaanottoCanBeFound) {
          (MailStatus.SHOULD_MAIL, Some(MailReason.EHDOLLISEN_PERIYTYMISEN_ILMOITUS), "Ehdollinen vastaanotto periytynyt")
        } else {
          (MailStatus.MAILED, None, "Already mailed")
        }
      }
    } else if (Vastaanotettavuustila.isVastaanotettavissa(hakutoive.vastaanotettavuustila)) {
      (MailStatus.SHOULD_MAIL, Some(MailReason.VASTAANOTTOILMOITUS), "Vastaanotettavissa (" + hakutoive.valintatila + ")")
    } else if (!Valintatila.isHyväksytty(hakutoive.valintatila) && Valintatila.isFinal(hakutoive.valintatila)) {
      (MailStatus.NEVER_MAIL, None, "Ei hyväksytty (" + hakutoive.valintatila + ")")
    } else {
      (MailStatus.NOT_MAILED, None, "Ei vastaanotettavissa (" + hakutoive.valintatila + ")")
    }

    HakukohdeMailStatus(hakutoive.hakukohdeOid, hakutoive.valintatapajonoOid, status,
      reason,
      hakutoive.vastaanottoDeadline, message, hakutoive.vastaanottotila,
      hakutoive.ehdollisestiHyvaksyttavissa)
  }

  private def mailStatusFor(hakemuksenTulos: Hakemuksentulos, uudetVastaanotot: Set[VastaanottoRecord], vanhatVastaanotot: Set[VastaanottoRecord]): HakemusMailStatus = {
    val hakutoiveet = hakemuksenTulos.hakutoiveet
    val mailables = hakutoiveet.map { (hakutoive: Hakutoiveentulos) =>
      hakukohdeMailStatusFor(
        hakemuksenTulos.hakemusOid,
        hakutoive,
        uudetVastaanotot,
        vanhatVastaanotot)
    }
    HakemusMailStatus(hakemuksenTulos.hakijaOid, hakemuksenTulos.hakemusOid, mailables, hakemuksenTulos.hakuOid)
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
}




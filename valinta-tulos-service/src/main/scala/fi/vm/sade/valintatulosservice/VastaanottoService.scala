package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat
import java.util.Date

import fi.vm.sade.sijoittelu.domain.{ValintatuloksenTila, Valintatulos}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.valintarekisteri._
import slick.dbio.{DBIO, SuccessAction}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


class VastaanottoService(hakuService: HakuService,
                         hakukohdeRecordService: HakukohdeRecordService,
                         vastaanotettavuusService: VastaanotettavuusService,
                         valintatulosService: ValintatulosService,
                         hakijaVastaanottoRepository: HakijaVastaanottoRepository,
                         virkailijaVastaanottoRepository: VirkailijaVastaanottoRepository,
                         valintatulosRepository: ValintatulosRepository) extends Logging {

  private val statesMatchingInexistentActions = Set(
    Vastaanottotila.kesken,
    Vastaanottotila.ei_vastaanotettu_määräaikana,
    Vastaanottotila.ottanut_vastaan_toisen_paikan
  )

  def vastaanotaVirkailijana(vastaanotot: List[VastaanottoEventDto]): Iterable[VastaanottoResult] = {
    vastaanotot.groupBy(v => (v.hakukohdeOid, v.hakuOid)).flatMap {
      case ((hakukohdeOid, hakuOid), vastaanottoEventDtos) => tallennaVirkailijanHakukohteenVastaanotot(hakukohdeOid, hakuOid, vastaanottoEventDtos)
    }
  }

  def vastaanotaVirkailijanaInTransaction(vastaanotot: List[VastaanottoEventDto]): Try[Unit] = {
    val tallennettavatVastaanotot = generateTallennettavatVastaanototList(vastaanotot)
    logger.info(s"Tallennettavat vastaanotot (${tallennettavatVastaanotot.size} kpl): " + tallennettavatVastaanotot)
    val vastaanottosToCheckInPostCondition = tallennettavatVastaanotot.filter(v => v.action == VastaanotaEhdollisesti || v.action == VastaanotaSitovasti)
    val postCondition = DBIO.sequence(vastaanottosToCheckInPostCondition.
      map(v => vastaanotettavuusService.tarkistaAiemmatVastaanotot(v.henkiloOid, v.hakukohdeOid, aiempiVastaanotto => SuccessAction())))

    tallennettavatVastaanotot.toStream.map(vastaanotto => findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)).find(_.isFailure) match {
      case Some(failure) => failure.map(_ => ())
      case None => Try {
        tallennettavatVastaanotot.foreach(v => hakukohdeRecordService.getHakukohdeRecord(v.hakukohdeOid))
        hakijaVastaanottoRepository.store(tallennettavatVastaanotot, postCondition)
      }
    }
  }

  private def generateTallennettavatVastaanototList(vastaanotot: List[VastaanottoEventDto]): List[VirkailijanVastaanotto] = {
    val hakuOidit = vastaanotot.map(_.hakuOid).toSet
    logger.info(s"Ollaan tallentamassa ${vastaanotot.size} vastaanottoa, joista löytyi ${hakuOidit.size} eri hakuOidia ($hakuOidit).")
    if (hakuOidit.size > 1) {
      logger.warn("Pitäisi olla vain yksi hakuOid")
    } else if (hakuOidit.isEmpty) {
      logger.warn("Ei löytynyt yhtään hakuOidia, lopetetaan.")
      return Nil
    }

    val henkiloidenVastaanototHauissaByHakuOid: Map[String, Map[String, List[Valintatulos]]] =
      hakuOidit.map(hakuOid => (hakuOid, findValintatulokset(hakuOid))).toMap

    (for {
      ((hakukohdeOid, hakuOid), vastaanottoEventDtos) <- vastaanotot.groupBy(v => (v.hakukohdeOid, v.hakuOid))
      haunValintatulokset = henkiloidenVastaanototHauissaByHakuOid(hakuOid)
      hakukohteenValintatulokset: Map[String, Option[Valintatulos]] = haunValintatulokset.mapValues(_.find(_.getHakukohdeOid == hakukohdeOid))
      vastaanottoEventDto <- vastaanottoEventDtos if isPaivitys(vastaanottoEventDto, hakukohteenValintatulokset.get(vastaanottoEventDto.henkiloOid).flatten)
    } yield {
      VirkailijanVastaanotto(vastaanottoEventDto)
    }).toList.sortWith(VirkailijanVastaanotto.tallennusJarjestys)
  }

  private def checkVastaanotettavuusVirkailijana(vastaanotto: VirkailijanVastaanotto): Try[(Hakutoiveentulos, Int)] = {
    for {
      hakutoiveJaPrioriteetti@(hakutoive, _) <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid, vastaanotettavuusVirkailijana = true)
      _ <- vastaanotto.action match {
        case VastaanotaEhdollisesti if hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti =>
          Failure(new IllegalArgumentException("Hakutoivetta ei voi ottaa ehdollisesti vastaan"))
        case VastaanotaSitovasti if !Valintatila.hasBeenHyväksytty(hakutoive.valintatila) =>
          logger.warn(s"Could not save $VastaanotaSitovasti because state was ${hakutoive.valintatila} in $vastaanotto")
          Failure(new IllegalArgumentException(s"""Ei voi tallentaa vastaanottotietoa, koska hakijalle näytettävä tila on "${hakutoive.valintatila}""""))
        case VastaanotaSitovasti | VastaanotaEhdollisesti =>
          Try { hakijaVastaanottoRepository.runBlocking(vastaanotettavuusService.tarkistaAiemmatVastaanotot(vastaanotto.henkiloOid, vastaanotto.hakukohdeOid)) }
        case MerkitseMyohastyneeksi => tarkistaHakijakohtainenDeadline(hakutoive)
        case Peru => Success(())
        case Peruuta => Success(())
        case Poista => Success(())
      }
    } yield hakutoiveJaPrioriteetti
  }

  private def tallennaVirkailijanHakukohteenVastaanotot(hakukohdeOid: String, hakuOid: String, uudetVastaanotot: List[VastaanottoEventDto]): List[VastaanottoResult] = {
    val hakukohteenValintatulokset = findValintatulokset(hakuOid, hakukohdeOid)
    val vastaanottoResultsWithUpdatedInfo: List[(VastaanottoResult, Boolean)] = uudetVastaanotot.map(vastaanottoDto => {
      if (isPaivitys(vastaanottoDto, hakukohteenValintatulokset.get(vastaanottoDto.henkiloOid))) {
        (tallenna(VirkailijanVastaanotto(vastaanottoDto)).get, true)
      } else {
        (VastaanottoResult(vastaanottoDto.henkiloOid, vastaanottoDto.hakemusOid, vastaanottoDto.hakukohdeOid, Result(200, None)), false)
      }
    })
    if (!vastaanottoResultsWithUpdatedInfo.exists(_._2)) {
      logger.debug(s"Determined that no vastaanotto events needed to be saved for haku $hakuOid / hakukohde $hakukohdeOid from the input of ${uudetVastaanotot.size} items.")
      logger.debug(s"Vastaanotto events for haku $hakuOid / hakukohde $hakukohdeOid were: $uudetVastaanotot.")
      val existingValintatulokset = uudetVastaanotot.map(vastaanottoDto => hakukohteenValintatulokset.get(vastaanottoDto.henkiloOid))
      logger.debug(s"Existing valintatulokset for vastaanotto events for haku $hakuOid / hakukohde $hakukohdeOid were: $existingValintatulokset.")
    }
    vastaanottoResultsWithUpdatedInfo.map(_._1)
  }

  private def isPaivitys(virkailijanVastaanotto: VastaanottoEventDto, valintatulos: Option[Valintatulos]): Boolean = valintatulos match {
    case Some(v) => existingTilaMustBeUpdated(v.getTila, virkailijanVastaanotto.tila)
    case None => !statesMatchingInexistentActions.contains(virkailijanVastaanotto.tila)
  }

  private def existingTilaMustBeUpdated(currentState: ValintatuloksenTila, newStateFromVirkailijanVastaanotto: Vastaanottotila): Boolean = {
    if (newStateFromVirkailijanVastaanotto == Vastaanottotila.ottanut_vastaan_toisen_paikan || Vastaanottotila.matches(newStateFromVirkailijanVastaanotto, currentState)) {
      return false
    }
    if (newStateFromVirkailijanVastaanotto == Vastaanottotila.kesken && currentState == ValintatuloksenTila.OTTANUT_VASTAAN_TOISEN_PAIKAN) {
      // Even if the stored state is OTTANUT_VASTAAN_TOISEN_PAIKAN, UI can see it as "KESKEN" in some cases
      return false
    }
    !Vastaanottotila.matches(newStateFromVirkailijanVastaanotto, currentState)
  }

  private def findValintatulokset(hakuOid: String, hakukohdeOid: String): Map[String, Valintatulos] = {
    valintatulosService.findValintaTuloksetForVirkailija(hakuOid, hakukohdeOid).asScala.toList.groupBy(_.getHakijaOid).mapValues(_.head)
  }

  private def findValintatulokset(hakuOid: String): Map[String, List[Valintatulos]] = {
    valintatulosService.findValintaTuloksetForVirkailija(hakuOid).asScala.toList.groupBy(_.getHakijaOid)
  }

  private def tallenna(vastaanotto: VirkailijanVastaanotto): Try[VastaanottoResult] = {
    (for {
      hakutoiveJaJarjestysNumero <- checkVastaanotettavuusVirkailijana(vastaanotto)
      _ <- Try {
        hakukohdeRecordService.getHakukohdeRecord(vastaanotto.hakukohdeOid)
        hakijaVastaanottoRepository.store(vastaanotto)
      }
      _ <- Try {
        val hakutoiveenJarjestysNumero = hakutoiveJaJarjestysNumero._2

        val createMissingValintatulos: Unit => Valintatulos = Unit => new Valintatulos(vastaanotto.valintatapajonoOid,
          vastaanotto.hakemusOid, vastaanotto.hakukohdeOid, vastaanotto.henkiloOid, vastaanotto.hakuOid, hakutoiveenJarjestysNumero)

        valintatulosRepository.modifyValintatulos(vastaanotto.hakukohdeOid, vastaanotto.valintatapajonoOid, vastaanotto.hakemusOid, createMissingValintatulos) { valintatulos =>
          valintatulos.setTila(ValintatuloksenTila.valueOf(hakutoiveJaJarjestysNumero._1.vastaanottotila.toString), vastaanotto.action.valintatuloksenTila, vastaanotto.selite, vastaanotto.ilmoittaja)
        }
      }
    } yield {
      createVastaanottoResult(200, None, vastaanotto)
    }).recover {
      case e: PriorAcceptanceException => createVastaanottoResult(403, Some(e), vastaanotto)
      case e @ (_: IllegalArgumentException | _: IllegalStateException) => createVastaanottoResult(400, Some(e), vastaanotto)
      case e: Exception => createVastaanottoResult(500, Some(e), vastaanotto)
    }
  }

  private def createVastaanottoResult(statusCode: Int, exception: Option[Throwable], vastaanottoEvent: VastaanottoEvent) = {
    VastaanottoResult(vastaanottoEvent.henkiloOid, vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid, Result(statusCode, exception.map(_.getMessage)))
  }

  @Deprecated
  def tarkistaVastaanotettavuus(vastaanotettavaHakemusOid: String, hakukohdeOid: String): Unit = {
    findHakutoive(vastaanotettavaHakemusOid, hakukohdeOid).get
  }

  def vastaanotaHakijana(vastaanotto: VastaanottoEvent): Try[Unit] = {
    for {
      hakutoive <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid).map(_._1)
      _ <- tarkistaHakutoiveenVastaanotettavuus(hakutoive, vastaanotto.action)
    } yield {
      hakukohdeRecordService.getHakukohdeRecord(vastaanotto.hakukohdeOid)
      hakijaVastaanottoRepository.store(vastaanotto)
      valintatulosRepository.modifyValintatulos(vastaanotto.hakukohdeOid,hakutoive.valintatapajonoOid,vastaanotto.hakemusOid,(Unit) => throw new IllegalArgumentException("Valintatulosta ei löydy")) { valintatulos =>
        valintatulos.setTila(ValintatuloksenTila.valueOf(hakutoive.vastaanottotila.toString), vastaanotto.action.valintatuloksenTila, vastaanotto.selite, vastaanotto.ilmoittaja)
      }
    }
  }

  private def findHakutoive(hakemusOid: String, hakukohdeOid: String, vastaanotettavuusVirkailijana: Boolean = false): Try[(Hakutoiveentulos, Int)] = {
    Try {
      val hakuOid = hakuService.getHakukohde(hakukohdeOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon hakukohde $hakukohdeOid")).hakuOid
      val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid, vastaanotettavuusVirkailijana).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
      hakemuksenTulos.findHakutoive(hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))
    }
  }

  private def tarkistaHakijakohtainenDeadline(hakutoive: Hakutoiveentulos): Try[Unit] = {
    val vastaanottoDeadline = hakutoive.vastaanottoDeadline
    Try {
      if(vastaanottoDeadline.isDefined && vastaanottoDeadline.get.after(new Date())) {
        throw new IllegalArgumentException(
          s"""Hakijakohtaista määräaikaa ${new SimpleDateFormat("dd-MM-yyyy").format(vastaanottoDeadline)}
             |kohteella ${hakutoive.hakukohdeOid} : ${hakutoive.vastaanotettavuustila.toString} ei ole vielä ohitettu.""".stripMargin)
      }
    }
  }

  private def tarkistaHakutoiveenVastaanotettavuus(hakutoive: Hakutoiveentulos, haluttuTila: VastaanottoAction): Try[Unit] = {
    val e = new IllegalArgumentException(s"Väärä vastaanotettavuustila kohteella ${hakutoive.hakukohdeOid}: ${hakutoive.vastaanotettavuustila.toString} (yritetty muutos: $haluttuTila)")
    haluttuTila match {
      case Peru | VastaanotaSitovasti if !Vastaanotettavuustila.isVastaanotettavissa(hakutoive.vastaanotettavuustila) => Failure(e)
      case VastaanotaEhdollisesti if hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti => Failure(e)
      case _ => Success(())
    }
  }
}

case class PriorAcceptanceException(aiempiVastaanotto: VastaanottoRecord)
  extends IllegalArgumentException(s"Löytyi aiempi vastaanotto $aiempiVastaanotto")

case class ConflictingAcceptancesException(personOid: String, conflictingVastaanottos: Seq[VastaanottoRecord], conflictDescription: String)
  extends IllegalStateException(s"Hakijalla $personOid useita vastaanottoja $conflictDescription: $conflictingVastaanottos")

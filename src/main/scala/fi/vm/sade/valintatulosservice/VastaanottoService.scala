package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.Valintatulos
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, VastaanottoEvent, VastaanottoRecord, VirkailijaVastaanottoRepository}

import scala.collection.JavaConverters._
import scala.util.{Success, Failure, Try}


class VastaanottoService(hakuService: HakuService,
                         vastaanotettavuusService: VastaanotettavuusService,
                         valintatulosService: ValintatulosService,
                         hakijaVastaanottoRepository: HakijaVastaanottoRepository,
                         virkailijaVastaanottoRepository: VirkailijaVastaanottoRepository,
                         valintatulosRepository: ValintatulosRepository) extends Logging{

  private val statesMatchingInexistentActions = Set(Vastaanottotila.kesken, Vastaanottotila.ei_vastaanotettu_määräaikana)


  def vastaanotaVirkailijana(vastaanotot: List[VastaanottoEventDto]): Iterable[VastaanottoResult] = {
    vastaanotot.groupBy(v => (v.hakukohdeOid, v.hakuOid)).flatMap {
      case ((hakukohdeOid, hakuOid), vastaanottoEventDtos) => tallennaHakukohteenVastaanotot(hakukohdeOid, hakuOid, vastaanottoEventDtos)
    }
  }

  def vastaanotaVirkailijanaInTransaction(vastaanotot: List[VastaanottoEventDto]): Try[Unit] = {

    val tallennettavatVastaanotot = generateTallennettavatVastaanototList(vastaanotot)
    tallennettavatVastaanotot.toStream.map(checkVastaanotettavuus).find(_.isFailure) match {
      case Some(failure) => failure
      case None => Try {hakijaVastaanottoRepository.store(tallennettavatVastaanotot) }
    }
  }

  private def generateTallennettavatVastaanototList(vastaanotot: List[VastaanottoEventDto]): List[VirkailijanVastaanotto] = {
    (for {
      ((hakukohdeOid, hakuOid), vastaanottoEventDtos) <- vastaanotot.groupBy(v => (v.hakukohdeOid, v.hakuOid))
      hakukohteenValintatulokset = findValintatulokset(hakuOid, hakukohdeOid)
      vastaanottoEventDto <- vastaanottoEventDtos if (isPaivitys(vastaanottoEventDto, hakukohteenValintatulokset.get(vastaanottoEventDto.henkiloOid)))
    } yield {
      VirkailijanVastaanotto(vastaanottoEventDto)
    }).toList
  }

  private def checkVastaanotettavuus(vastaanotto: VirkailijanVastaanotto): Try[Unit] = vastaanotto.action match {
    case VastaanotaSitovasti | VastaanotaEhdollisesti => for {
      _ <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      _ <- vastaanotettavuusService.tarkistaAiemmatVastaanotot(vastaanotto.henkiloOid, vastaanotto.hakukohdeOid)
    } yield ()
    case Peru => for {
      hakutoive <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      _ <- tarkistaHakutoiveenVastaanotettavuus(hakutoive, vastaanotto.action)
    } yield ()
    case Peruuta => for {
      _ <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
    } yield ()
    case Poista => Success(())
  }

  private def tallennaHakukohteenVastaanotot(hakukohdeOid: String, hakuOid: String, uudetVastaanotot: List[VastaanottoEventDto]): List[VastaanottoResult] = {
    val hakukohteenValintatulokset = findValintatulokset(hakuOid, hakukohdeOid)
    uudetVastaanotot.map(vastaanottoDto => {
      if (isPaivitys(vastaanottoDto, hakukohteenValintatulokset.get(vastaanottoDto.henkiloOid))) {
        tallenna(VirkailijanVastaanotto(vastaanottoDto))
      } else {
        VastaanottoResult(vastaanottoDto.henkiloOid, vastaanottoDto.hakemusOid, vastaanottoDto.hakukohdeOid, Result(200, None))
      }
    })
  }

  private def isPaivitys(virkailijanVastaanotto: VastaanottoEventDto, valintatulos: Option[Valintatulos]): Boolean = valintatulos match {
    case Some(valintatulos) => !Vastaanottotila.matches(virkailijanVastaanotto.tila, valintatulos.getTila)
    case None => !statesMatchingInexistentActions.contains(virkailijanVastaanotto.tila)
  }

  private def findValintatulokset(hakuOid: String, hakukohdeOid: String): Map[String, Valintatulos] = {
    valintatulosService.findValintaTulokset(hakuOid, hakukohdeOid).asScala.toList.groupBy(_.getHakijaOid).mapValues(_.head)
  }

  private def tallenna(vastaanotto: VirkailijanVastaanotto): VastaanottoResult = vastaanotto.action match {
    case VastaanotaSitovasti => tallennaJosEiAiempiaVastaanottoja(vastaanotto)
    case VastaanotaEhdollisesti => tallennaJosEiAiempiaVastaanottoja(vastaanotto)
    case Peru => vastaanotaHakijana(vastaanotto) match {
      case Success(()) => createVastaanottoResult(200, None, vastaanotto)
      case Failure(e: IllegalArgumentException) => createVastaanottoResult(400, Some(e), vastaanotto)
      case Failure(e: Throwable) => {
        logger.error("Unexpected error when saving vastaanotto", e)
        createVastaanottoResult(500, Some(e), vastaanotto)
      }
    }
    case Peruuta => peruutaAiempiVastaanotto(vastaanotto)
    case Poista => {
      hakijaVastaanottoRepository.store(vastaanotto)
      createVastaanottoResult(200, None, vastaanotto)
    }
  }

  private def tallennaJosEiAiempiaVastaanottoja(vastaanotto: VirkailijanVastaanotto): VastaanottoResult = {
    (for {
      _ <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      _ <- vastaanotettavuusService.tarkistaAiemmatVastaanotot(vastaanotto.henkiloOid, vastaanotto.hakukohdeOid)
      _ <- Try { hakijaVastaanottoRepository.store(vastaanotto) }
    } yield {
      createVastaanottoResult(200, None, vastaanotto)
    }).recover {
      case e: PriorAcceptanceException => createVastaanottoResult(403, Some(e), vastaanotto)
      case e @ (_: IllegalArgumentException | _: IllegalStateException) => createVastaanottoResult(400, Some(e), vastaanotto)
      case e => createVastaanottoResult(500, Some(e), vastaanotto)
    }.get
  }

  private def peruutaAiempiVastaanotto(vastaanotto: VirkailijanVastaanotto): VastaanottoResult = {
    (for {
      _ <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      _ <- Try { hakijaVastaanottoRepository.store(vastaanotto) }
    } yield {
      createVastaanottoResult(200, None, vastaanotto)
    }).recover {
      case e @ (_: IllegalArgumentException | _: IllegalStateException) => createVastaanottoResult(400, Some(e), vastaanotto)
      case e => createVastaanottoResult(500, Some(e), vastaanotto)
    }.get
  }

  private def createVastaanottoResult(statusCode: Int, exception: Option[Throwable], vastaanottoEvent: VastaanottoEvent) = {
    VastaanottoResult(vastaanottoEvent.henkiloOid, vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid, Result(statusCode, exception.map(_.getMessage)))
  }

  @Deprecated
  def tarkistaVastaanotettavuus(vastaanotettavaHakemusOid: String, hakukohdeOid: String): Unit = {
    findHakutoive(vastaanotettavaHakemusOid, hakukohdeOid).get
  }

  @Deprecated
  def vastaanota(hakemusOid: String, vastaanotto: Vastaanotto): Unit = {
    vastaanotaHakijana(HakijanVastaanotto(
      vastaanotto.muokkaaja,
      hakemusOid,
      vastaanotto.hakukohdeOid,
      HakijanVastaanottoAction.getHakijanVastaanottoAction(vastaanotto.tila)
    )).get
  }

  def vastaanotaHakijana(vastaanotto: VastaanottoEvent): Try[Unit] = {
    for {
      hakutoive <- findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      _ <- tarkistaHakutoiveenVastaanotettavuus(hakutoive, vastaanotto.action)
    } yield {
      hakijaVastaanottoRepository.store(vastaanotto)
    }
  }

  private def findHakutoive(hakemusOid: String, hakukohdeOid: String): Try[Hakutoiveentulos] = {
    Try {
      val hakuOid = hakuService.getHakukohde(hakukohdeOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon hakukohde ${hakukohdeOid}")).hakuOid
      val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
      hakemuksenTulos.findHakutoive(hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))
    }
  }

  private def tarkistaHakutoiveenVastaanotettavuus(hakutoive: Hakutoiveentulos, haluttuTila: VastaanottoAction): Try[Unit] = {
    Try {
      if (List(Peru, VastaanotaSitovasti).contains(haluttuTila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
        throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila + ")")
      }
      if (haluttuTila == VastaanotaEhdollisesti && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
        throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila + ")")
      }
    }
  }
}

case class PriorAcceptanceException(aiempiVastaanotto: VastaanottoRecord)
  extends IllegalArgumentException(s"Löytyi aiempi vastaanotto $aiempiVastaanotto")

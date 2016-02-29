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
    val vastaanototByHakukohdeOid: Map[(String, String), List[VastaanottoEventDto]] = vastaanotot.groupBy(v => (v.hakukohdeOid, v.hakuOid))
    vastaanototByHakukohdeOid.keys.flatMap(hakuKohdeAndHakuOids =>
      tallennaHakukohteenVastaanotot(hakuKohdeAndHakuOids._1, hakuKohdeAndHakuOids._2, vastaanototByHakukohdeOid(hakuKohdeAndHakuOids)))
  }

  private def tallennaHakukohteenVastaanotot(hakukohdeOid: String, hakuOid: String, uudetVastaanotot: List[VastaanottoEventDto]): List[VastaanottoResult] = {
    val hakukohteenValintatulokset: List[Valintatulos] = valintatulosService.findValintaTulokset(hakuOid, hakukohdeOid).asScala.toList
    uudetVastaanotot.map(vastaanottoDto => {
      if (isPaivitys(vastaanottoDto, hakukohteenValintatulokset)) {
        tallenna(VirkailijanVastaanotto(vastaanottoDto))
      } else {
        VastaanottoResult(vastaanottoDto.henkiloOid, vastaanottoDto.hakemusOid, vastaanottoDto.hakukohdeOid, Result(200, None))
      }
    })
  }

  private def isPaivitys(virkailijanVastaanotto: VastaanottoEventDto, valintatulokset: Iterable[Valintatulos]): Boolean = {
    valintatulokset.find(v => v.getHakijaOid == virkailijanVastaanotto.henkiloOid) match {
      case Some(valintatulos) => !Vastaanottotila.matches(virkailijanVastaanotto.tila, valintatulos.getTila)
      case None => !statesMatchingInexistentActions.contains(virkailijanVastaanotto.tila)
    }
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
    case Poista => ???
  }

  private def tallennaJosEiAiempiaVastaanottoja(vastaanotto: VirkailijanVastaanotto): VastaanottoResult = {
    try {
      findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      vastaanotettavuusService.tarkistaAiemmatVastaanotot(vastaanotto.henkiloOid, vastaanotto.hakukohdeOid).get
      hakijaVastaanottoRepository.store(vastaanotto)
      createVastaanottoResult(200, None, vastaanotto)
    } catch {
      case e: PriorAcceptanceException => createVastaanottoResult(403, Some(e), vastaanotto)
      case e: Exception => createVastaanottoResult(400, Some(e), vastaanotto)
    }
  }

  private def peruutaAiempiVastaanotto(vastaanotto: VirkailijanVastaanotto): VastaanottoResult = {
    try {
      findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      hakijaVastaanottoRepository.store(vastaanotto)
      createVastaanottoResult(200, None, vastaanotto)
    } catch {
      case e: Exception => createVastaanottoResult(400, Some(e), vastaanotto)
    }
  }

  private def createVastaanottoResult(statusCode: Int, exception: Option[Throwable], vastaanottoEvent: VastaanottoEvent) = {
    VastaanottoResult(vastaanottoEvent.henkiloOid, vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid, Result(statusCode, exception.map(_.getMessage)))
  }

  @Deprecated
  def tarkistaVastaanotettavuus(vastaanotettavaHakemusOid: String, hakukohdeOid: String): Unit = {
    findHakutoive(vastaanotettavaHakemusOid, hakukohdeOid)
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
    try {
      val hakutoive = findHakutoive(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)
      tarkistaHakutoiveenVastaanotettavuus(hakutoive, vastaanotto.action)
      hakijaVastaanottoRepository.store(vastaanotto)
      Success(())
    } catch {
      case e: IllegalArgumentException => Failure(e)
    }
  }

  private def findHakutoive(hakemusOid: String, hakukohdeOid: String): Hakutoiveentulos = {
    val hakuOid = hakuService.getHakukohde(hakukohdeOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon hakukohde ${hakukohdeOid}")).hakuOid
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    hakemuksenTulos.findHakutoive(hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))
  }

  private def tarkistaHakutoiveenVastaanotettavuus(hakutoive: Hakutoiveentulos, haluttuTila: VastaanottoAction): Unit = {
    if (List(Peru, VastaanotaSitovasti).contains(haluttuTila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila + ")")
    }
    if (haluttuTila == VastaanotaEhdollisesti && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila + ")")
    }
  }
}

case class PriorAcceptanceException(aiempiVastaanotto: VastaanottoRecord)
  extends IllegalArgumentException(s"Löytyi aiempi vastaanotto $aiempiVastaanotto")

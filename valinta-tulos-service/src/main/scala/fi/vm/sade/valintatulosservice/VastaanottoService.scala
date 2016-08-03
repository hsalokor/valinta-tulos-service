package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import fi.vm.sade.sijoittelu.domain.{ValintatuloksenTila, Valintatulos}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritService
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoittelunTulosRestClient, SijoittelutulosService, ValintatulosRepository}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.valintarekisteri._
import slick.dbio.{DBIO, SuccessAction}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}


class VastaanottoService(hakuService: HakuService,
                         hakukohdeRecordService: HakukohdeRecordService,
                         vastaanotettavuusService: VastaanotettavuusService,
                         valintatulosService: ValintatulosService,
                         hakijaVastaanottoRepository: HakijaVastaanottoRepository,
                         virkailijaVastaanottoRepository: VirkailijaVastaanottoRepository,
                         ohjausparametritService: OhjausparametritService,
                         sijoittelutulosService: SijoittelutulosService,
                         hakemusRepository: HakemusRepository,
                         sijoittelunTulosClient: SijoittelunTulosRestClient,
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
      vastaanottoEventDto <- vastaanottoEventDtos if isPaivitys(vastaanottoEventDto, hakukohteenValintatulokset.get(vastaanottoEventDto.henkiloOid).flatten.map(_.getTila))
    } yield {
      VirkailijanVastaanotto(vastaanottoEventDto)
    }).toList.sortWith(VirkailijanVastaanotto.tallennusJarjestys)
  }

  private def tallennaVirkailijanHakukohteenVastaanotot(hakukohdeOid: String, hakuOid: String, uudetVastaanotot: List[VastaanottoEventDto]): List[VastaanottoResult] = {
    val HakukohdeRecord(_, _, _, _, koulutuksenAlkamiskausi) = hakukohdeRecordService.getHakukohdeRecord(hakukohdeOid)
    val haku = hakuService.getHaku(hakuOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon haku $hakuOid"))
    val ohjausparametrit = ohjausparametritService.ohjausparametrit(hakuOid)
    uudetVastaanotot.map(vastaanottoDto => {
      val henkiloOid = vastaanottoDto.henkiloOid
      val hakemusOid = vastaanottoDto.hakemusOid
      val vastaanotto = VirkailijanVastaanotto(vastaanottoDto)
      val maybeHakemus = hakemusRepository.findHakemus(hakemusOid)
      val vastaanotettuHakutoive = hakijaVastaanottoRepository.runAsSerialized(10, Duration(5, TimeUnit.MILLISECONDS), s"Storing vastaanotto $vastaanottoDto",
        (for {
          hakemus <- maybeHakemus.map(DBIO.successful).getOrElse(DBIO.failed(new IllegalArgumentException(s"Hakemusta $hakemusOid ei löydy hausta $hakuOid")))
          sijoittelunTulos <- sijoittelutulosService.latestSijoittelunTulosVirkailijana(hakuOid, henkiloOid, hakemusOid, ohjausparametrit.flatMap(_.vastaanottoaikataulu))
          maybeAiempiVastaanottoKaudella <- if (haku.yhdenPaikanSaanto.voimassa) {
            hakijaVastaanottoRepository.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, koulutuksenAlkamiskausi).map(Some(_))
          } else {
            DBIO.successful(None)
          }
          hakemuksenTulos = valintatulosService.julkaistavaTulos(sijoittelunTulos, haku, ohjausparametrit, true, maybeAiempiVastaanottoKaudella.map(_.map(_.henkiloOid).toSet))(hakemus)
          r <- tarkistaHakutoiveenVastaanotettavuusVirkailijana(hakemuksenTulos, hakukohdeOid, vastaanottoDto, maybeAiempiVastaanottoKaudella) match {
            case Success(Some(hakutoive)) => hakijaVastaanottoRepository.storeAction(vastaanotto).andThen(DBIO.successful(Some(hakutoive)))
            case Success(None) => DBIO.successful(None)
            case Failure(e) => DBIO.failed(e)
          }
        } yield r).asTry)
      vastaanotettuHakutoive.foreach(o => o.foreach(t => {
        val hakutoive = t._1
        val hakutoiveenJarjestysnumero = t._2
        val createMissingValintatulos: Unit => Valintatulos = Unit => new Valintatulos(vastaanotto.valintatapajonoOid,
          vastaanotto.hakemusOid, hakukohdeOid, vastaanotto.henkiloOid, vastaanotto.hakuOid, hakutoiveenJarjestysnumero)

        valintatulosRepository.modifyValintatulos(hakukohdeOid, vastaanotto.valintatapajonoOid, vastaanotto.hakemusOid, createMissingValintatulos) { valintatulos =>
          valintatulos.setTila(ValintatuloksenTila.valueOf(hakutoive.vastaanottotila.toString), vastaanotto.action.valintatuloksenTila, vastaanotto.selite, vastaanotto.ilmoittaja)
        }
      }))
      vastaanotettuHakutoive.map(_ => createVastaanottoResult(200, None, vastaanotto)).recover {
        case e: PriorAcceptanceException => createVastaanottoResult(403, Some(e), vastaanotto)
        case e@(_: IllegalArgumentException | _: IllegalStateException) => createVastaanottoResult(400, Some(e), vastaanotto)
        case e: Exception => createVastaanottoResult(500, Some(e), vastaanotto)
      }.get
    })
  }

  private def isPaivitys(virkailijanVastaanotto: VastaanottoEventDto, valintatuloksenTila: Option[ValintatuloksenTila]): Boolean = valintatuloksenTila match {
    case Some(v) => existingTilaMustBeUpdated(v, virkailijanVastaanotto.tila)
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

  private def findValintatulokset(hakuOid: String): Map[String, List[Valintatulos]] = {
    valintatulosService.findValintaTuloksetForVirkailija(hakuOid).asScala.toList.groupBy(_.getHakijaOid)
  }

  private def createVastaanottoResult(statusCode: Int, exception: Option[Throwable], vastaanottoEvent: VastaanottoEvent) = {
    VastaanottoResult(vastaanottoEvent.henkiloOid, vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid, Result(statusCode, exception.map(_.getMessage)))
  }

  @Deprecated
  def tarkistaVastaanotettavuus(vastaanotettavaHakemusOid: String, hakukohdeOid: String): Unit = {
    findHakutoive(vastaanotettavaHakemusOid, hakukohdeOid).get
  }

  def vastaanotaHakijana(vastaanotto: VastaanottoEvent): Unit = {
    val VastaanottoEvent(henkiloOid, hakemusOid, hakukohdeOid, _, _, _) = vastaanotto
    val HakukohdeRecord(_, hakuOid, _, _, koulutuksenAlkamiskausi) = hakukohdeRecordService.getHakukohdeRecord(hakukohdeOid)
    val haku = hakuService.getHaku(hakuOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon haku $hakuOid"))
    val ohjausparametrit = ohjausparametritService.ohjausparametrit(hakuOid)
    val hakemus = hakemusRepository.findHakemus(hakemusOid).getOrElse(throw new IllegalArgumentException(s"Hakemusta $hakemusOid ei löydy hausta $hakuOid"))
    val hakutoive = hakijaVastaanottoRepository.runAsSerialized(10, Duration(5, TimeUnit.MILLISECONDS), s"Storing vastaanotto $vastaanotto",
      for {
        sijoittelunTulos <- sijoittelutulosService.latestSijoittelunTulos(hakuOid, henkiloOid, hakemusOid, ohjausparametrit.flatMap(_.vastaanottoaikataulu))
        hakemuksenTulos <- (if (haku.yhdenPaikanSaanto.voimassa) {
          hakijaVastaanottoRepository.findYhdenPaikanSaannonPiirissaOlevatVastaanotot(henkiloOid, koulutuksenAlkamiskausi).map {
            case Some(v) => Some(Set(v.henkiloOid))
            case None => Some(Set[String]())
          }
        } else {
          DBIO.successful(None)
        }).map(valintatulosService.julkaistavaTulos(sijoittelunTulos, haku, ohjausparametrit, true, _)(hakemus))
        hakutoive <- tarkistaHakutoiveenVastaanotettavuus(hakemuksenTulos, hakukohdeOid, vastaanotto.action) match {
          case Success(h) => hakijaVastaanottoRepository.storeAction(vastaanotto).andThen(DBIO.successful(h))
          case Failure(t) => DBIO.failed(t)
        }
      } yield hakutoive)
    valintatulosRepository.modifyValintatulos(hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid, (Unit) => throw new IllegalArgumentException("Valintatulosta ei löydy")) { valintatulos =>
      valintatulos.setTila(ValintatuloksenTila.valueOf(hakutoive.vastaanottotila.toString), vastaanotto.action.valintatuloksenTila, vastaanotto.selite, vastaanotto.ilmoittaja)
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

  private def tarkistaHakutoiveenVastaanotettavuus(hakemuksenTulos: Hakemuksentulos, hakukohdeOid: String, haluttuTila: VastaanottoAction): Try[Hakutoiveentulos] = {
    val missingHakutoive = s"Hakutoivetta $hakukohdeOid ei löydy hakemukselta ${hakemuksenTulos.hakemusOid}"
    for {
      (hakutoive, _) <- Try(hakemuksenTulos.findHakutoive(hakukohdeOid).getOrElse(throw new IllegalArgumentException(missingHakutoive)))
      _ <- tarkistaHakutoiveenVastaanotettavuus(hakutoive, haluttuTila)
    } yield hakutoive
  }

  private def tarkistaHakutoiveenVastaanotettavuusVirkailijana(vastaanotto: VirkailijanVastaanotto, hakutoive: Hakutoiveentulos, maybeAiempiVastaanottoKaudella: Option[Option[VastaanottoRecord]]): Try[Unit] = vastaanotto.action match {
    case VastaanotaEhdollisesti if hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti =>
      Failure(new IllegalArgumentException("Hakutoivetta ei voi ottaa ehdollisesti vastaan"))
    case VastaanotaSitovasti if !Valintatila.hasBeenHyväksytty(hakutoive.valintatila) =>
      logger.warn(s"Could not save $VastaanotaSitovasti because state was ${hakutoive.valintatila} in $vastaanotto")
      Failure(new IllegalArgumentException(s"""Ei voi tallentaa vastaanottotietoa, koska hakijalle näytettävä tila on "${hakutoive.valintatila}""""))
    case VastaanotaSitovasti | VastaanotaEhdollisesti =>
      maybeAiempiVastaanottoKaudella match {
        case Some(Some(aiempiVastaanotto)) => Failure(PriorAcceptanceException(aiempiVastaanotto))
        case _ => Success(())
      }
    case MerkitseMyohastyneeksi => tarkistaHakijakohtainenDeadline(hakutoive)
    case Peru => Success(())
    case Peruuta => Success(())
    case Poista => Success(())
  }

  private def tarkistaHakutoiveenVastaanotettavuusVirkailijana(hakemuksentulos: Hakemuksentulos, hakukohdeOid: String,
                                                               vastaanottoDto: VastaanottoEventDto,
                                                               maybeAiempiVastaanottoKaudella: Option[Option[VastaanottoRecord]]): Try[Option[(Hakutoiveentulos, Int)]] = {
    val missingHakutoive = s"Hakutoivetta $hakukohdeOid ei löydy hakemukselta ${hakemuksentulos.hakemusOid}"
    for {
      (hakutoive, i) <- Try(hakemuksentulos.findHakutoive(hakukohdeOid).getOrElse(throw new IllegalArgumentException(missingHakutoive)))
      vastaanotto = VirkailijanVastaanotto(vastaanottoDto)
      valintatuloksenTila = ValintatuloksenTila.valueOf(hakutoive.virkailijanTilat.vastaanottotila.toString)
      r <- if (isPaivitys(vastaanottoDto, Some(valintatuloksenTila))) {
        tarkistaHakutoiveenVastaanotettavuusVirkailijana(vastaanotto, hakutoive, maybeAiempiVastaanottoKaudella)
          .map(_ => Some((hakutoive, i)))
      } else {
        logger.info(s"Vastaanotto event $vastaanottoDto is not an update to hakutoive $hakutoive")
        Success(None)
      }
    } yield r
  }

}

case class PriorAcceptanceException(aiempiVastaanotto: VastaanottoRecord)
  extends IllegalArgumentException(s"Löytyi aiempi vastaanotto $aiempiVastaanotto")

case class ConflictingAcceptancesException(personOid: String, conflictingVastaanottos: Seq[VastaanottoRecord], conflictDescription: String)
  extends IllegalStateException(s"Hakijalla $personOid useita vastaanottoja $conflictDescription: $conflictingVastaanottos")

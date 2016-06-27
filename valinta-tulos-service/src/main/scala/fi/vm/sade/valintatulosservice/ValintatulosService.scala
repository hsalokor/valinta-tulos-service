package fi.vm.sade.valintatulosservice

import java.util

import fi.vm.sade.sijoittelu.domain.{ValintatuloksenTila, Valintatulos}
import fi.vm.sade.sijoittelu.tulos.dto
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakijaPaginationObject}
import fi.vm.sade.utils.Timer.timed
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Valintatila.isHyväksytty
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.ohjausparametrit.{Ohjausparametrit, OhjausparametritService}
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoittelutulosService, StreamingHakijaDtoClient}
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakukohdeRecordService, VastaanottoRecord, VirkailijaVastaanottoRepository}
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime

import scala.collection.JavaConverters._

private object HakemustenTulosHakuLock

class ValintatulosService(vastaanotettavuusService: VastaanotettavuusService,
                          sijoittelutulosService: SijoittelutulosService,
                          ohjausparametritService: OhjausparametritService,
                          hakemusRepository: HakemusRepository,
                          virkailijaVastaanottoRepository: VirkailijaVastaanottoRepository,
                          hakuService: HakuService,
                          hakukohdeRecordService: HakukohdeRecordService)(implicit appConfig: AppConfig) extends Logging {
  def this(vastaanotettavuusService: VastaanotettavuusService,
           sijoittelutulosService: SijoittelutulosService,
           virkailijaVastaanottoRepository: VirkailijaVastaanottoRepository,
           hakuService: HakuService,
           hakukohdeRecordService: HakukohdeRecordService)(implicit appConfig: AppConfig) =
    this(vastaanotettavuusService, sijoittelutulosService, appConfig.ohjausparametritService, new HakemusRepository(), virkailijaVastaanottoRepository, hakuService, hakukohdeRecordService)

  val valintatulosDao = appConfig.sijoitteluContext.valintatulosDao
  private val streamingHakijaDtoClient = new StreamingHakijaDtoClient(appConfig)

  def haunKoulutuksenAlkamiskaudenVastaanototYhdenPaikanSaadoksenPiirissa(hakuOid: String) : Set[VastaanottoRecord] = {
    hakuJaSenAlkamiskaudenVastaanototYhdenPaikanSaadoksenPiirissa(hakuOid)._2
  }

  private def hakuJaSenAlkamiskaudenVastaanototYhdenPaikanSaadoksenPiirissa(hakuOid: String): (Haku, Set[VastaanottoRecord]) = {
    val haku = hakuService.getHaku(hakuOid)
      .getOrElse(throw new IllegalArgumentException(s"Haku $hakuOid does not exist"))
    if (haku.yhdenPaikanSaanto.voimassa) {
      hakukohdeRecordService.getHaunKoulutuksenAlkamiskausi(hakuOid)
        .map(kausi => (haku, virkailijaVastaanottoRepository.findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(kausi)))
        .getOrElse(throw new IllegalStateException(s"No koulutuksen alkamiskausi for haku $hakuOid"))
    } else {
      (haku, Set())
    }
  }

  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    val vastaanottoaikataulu = sijoittelutulosService.findAikatauluFromOhjausparametritService(hakuOid)

    for {
      haku <- hakuService.getHaku(hakuOid)
      latestSijoitteluAjo = sijoittelutulosService.findLatestSijoitteluAjoForHaku(haku)
      hakemus <- fetchTulokset(
        haku,
        () => hakemusRepository.findHakemus(hakemusOid).iterator,
        hakijaOidsByHakemusOids => sijoittelutulosService.hakemuksenTulos(haku, hakemusOid, hakijaOidsByHakemusOids.get(hakemusOid), vastaanottoaikataulu, latestSijoitteluAjo).toSeq
      ).toSeq.headOption
    } yield hakemus
  }

  def hakemuksentuloksetByPerson(hakuOid: String, personOid: String): List[Hakemuksentulos] = {
    val hakemukset = hakemusRepository.findHakemukset(hakuOid, personOid).toSeq
    val vastaanottoAikataulu = sijoittelutulosService.findAikatauluFromOhjausparametritService(hakuOid)
    (for {
      haku <- hakuService.getHaku(hakuOid)
      latestSijoitteluAjo <- sijoittelutulosService.findLatestSijoitteluAjoForHaku(haku)
    } yield {
      fetchTulokset(
        haku,
        () => hakemukset.toIterator,
        hakijaOidsByHakemusOids => hakemukset.flatMap(hakemus => sijoittelutulosService.hakemuksenTulos(haku, hakemus.oid, hakijaOidsByHakemusOids.get(hakemus.oid), vastaanottoAikataulu, Some(latestSijoitteluAjo)))
      ).toList
    }).getOrElse(List.empty)
  }

  def hakemustenTulosByHaku(hakuOid: String, checkJulkaisuAikaParametri: Boolean): Option[Iterator[Hakemuksentulos]] = {
    val haunVastaanotot = timed("Fetch haun vastaanotot for haku: " + hakuOid, 1000) {
      virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    }
    hakemustenTulosByHaku(hakuOid, Some(haunVastaanotot), checkJulkaisuAikaParametri)
  }

  def haeTilatHakijoille(hakuOid: String, hakukohdeOid: String, valintatapajonoOid: String, hakemusOids: Set[String]): Set[TilaHakijalle] = {
    hakuService.getHaku(hakuOid) match {
      case Some(haku) =>
        val vastaanottoaikataulu = sijoittelutulosService.findAikatauluFromOhjausparametritService(hakuOid)
        val latestSijoitteluAjo = sijoittelutulosService.findLatestSijoitteluAjo(hakuOid, Some(hakukohdeOid))
        val hakemustenTulokset = fetchTulokset(haku,
          () => hakemusRepository.findHakemuksetByOids(hakemusOids),
          hakijaOidsByHakemusOids => hakemusOids.flatMap(hakemusOid => sijoittelutulosService.hakemuksenTulos(haku, hakemusOid, hakijaOidsByHakemusOids.get(hakemusOid), vastaanottoaikataulu, latestSijoitteluAjo)).toSeq)
        hakemustenTulokset.map { hakemuksenTulos =>
          hakemuksenTulos.hakutoiveet.find(_.valintatapajonoOid == valintatapajonoOid).map { hakutoiveenTulos =>
            val tilaHakijalle = ValintatuloksenTila.valueOf(hakutoiveenTulos.vastaanottotila.toString)
            TilaHakijalle(hakemusOid = hakemuksenTulos.hakemusOid,
              hakukohdeOid = hakutoiveenTulos.hakukohdeOid,
              valintatapajonoOid = hakutoiveenTulos.valintatapajonoOid,
              tilaHakijalle = tilaHakijalle.toString)
          }
        }.flatten.toSet
      case None =>
        logger.warn(s"Could not find haku $hakuOid")
        Set()
    }
  }

  private def hakemustenTulosByHaku(hakuOid: String, haunVastaanotot: Option[Map[String,Set[VastaanottoRecord]]], checkJulkaisuAikaParametri: Boolean = true): Option[Iterator[Hakemuksentulos]] = {
    timed("Fetch hakemusten tulos for haku: " + hakuOid, 1000) (
      for {
        haku <- hakuService.getHaku(hakuOid)
      } yield {
        hakukohdeRecordService.getHaunKoulutuksenAlkamiskausi(hakuOid) match {
          case Some(koulutuksenAlkamiskausi) =>
            fetchTulokset(
              haku,
              () => hakemusRepository.findHakemukset(hakuOid),
              hakijaOidsByHakemusOids => sijoittelutulosService.hakemustenTulos(hakuOid, hakijaOidsByHakemusOids = hakijaOidsByHakemusOids, haunVastaanotot = haunVastaanotot),
              Some(timed("personOids from hakemus", 1000)(hakemusRepository.findPersonOids(hakuOid))),
              checkJulkaisuAikaParametri,
              if (haku.yhdenPaikanSaanto.voimassa) {
                Some(timed("kaudenVastaanotot", 1000)({
                  virkailijaVastaanottoRepository.findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(koulutuksenAlkamiskausi)
                    .map(_.henkiloOid)
                }))
              } else {
                None
              }
            )
          case None => Iterator.empty
        }
      }
    )
  }

  def hakemustenTulosByHakukohde(hakuOid: String, hakukohdeOid: String, hakukohteenVastaanotot: Option[Map[String,Set[VastaanottoRecord]]] = None, checkJulkaisuAikaParametri: Boolean = true): Option[Iterator[Hakemuksentulos]] = {
    timed("Fetch hakemusten tulos for haku: "+ hakuOid + " and hakukohde: " + hakuOid, 1000) (
      for {
        haku <- hakuService.getHaku(hakuOid)
        koulutuksenAlkamiskausi <- hakukohdeRecordService.getHaunKoulutuksenAlkamiskausi(hakuOid)
      } yield {
        fetchTulokset(
          haku,
          () => hakemusRepository.findHakemuksetByHakukohde(hakuOid, hakukohdeOid),
          hakijaOidsByHakemusOids => sijoittelutulosService.hakemustenTulos(hakuOid, Some(hakukohdeOid), hakijaOidsByHakemusOids, hakukohteenVastaanotot),
          Some(timed("personOids from hakemus", 1000)(hakemusRepository.findPersonOids(hakuOid, hakukohdeOid))),
          checkJulkaisuAikaParametri,
          if (haku.yhdenPaikanSaanto.voimassa) {
            Some(timed("kaudenVastaanotot", 1000)({
              virkailijaVastaanottoRepository.findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(koulutuksenAlkamiskausi)
                .map(_.henkiloOid)
            }))
          } else {
            None
          }
        )
      }
    )
  }

  def findValintaTuloksetForVirkailija(hakuOid: String): util.List[Valintatulos] = {
    val haunVastaanotot = virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    val hakemustenTulokset = hakemustenTulosByHaku(hakuOid, Some(haunVastaanotot)).getOrElse(throw new IllegalArgumentException(s"Unknown hakuOid $hakuOid"))
    val valintatulokset = valintatulosDao.loadValintatulokset(hakuOid)

    setValintatuloksetTilat(hakuOid, valintatulokset.asScala, mapHakemustenTuloksetByHakemusOid(hakemustenTulokset), haunVastaanotot)
    valintatulokset
  }

  def findValintaTuloksetForVirkailija(hakuOid: String, hakukohdeOid: String): util.List[Valintatulos] = {
    val haunVastaanotot = virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    val hakemustenTulokset = hakemustenTulosByHakukohde(hakuOid, hakukohdeOid, Some(haunVastaanotot)).getOrElse(throw new IllegalArgumentException(s"Unknown hakuOid $hakuOid"))
    val valintatulokset: util.List[Valintatulos] = valintatulosDao.loadValintatuloksetForHakukohde(hakukohdeOid)

    setValintatuloksetTilat(hakuOid, valintatulokset.asScala, mapHakemustenTuloksetByHakemusOid(hakemustenTulokset), haunVastaanotot)
    valintatulokset
  }

  def findValintaTuloksetForVirkailijaWithoutTilaHakijalle(hakuOid: String, hakukohdeOid: String): util.List[Valintatulos] = {
    val haunVastaanotot: Map[String, Set[VastaanottoRecord]] = timed(s"Fetch vastaanotto records for haku $hakuOid", 1000) {
      virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    }
    lazy val hakemusOidsByHakijaOids = timed(s"Fetch hakija oids by hakemus oids for haku $hakuOid and hakukohde $hakukohdeOid", 1000) {
      hakemusRepository.findPersonOids(hakuOid, hakukohdeOid)
    }

    val valintatulokset: util.List[Valintatulos] = timed(s"Fetch plain valintatulokset for haku $hakuOid", 1000) {
      valintatulosDao.loadValintatuloksetForHakukohde(hakukohdeOid)
    }

    val (haku, kaudenVastaanotot) = timed(s"Fetch YPS related kauden vastaanotot for haku $hakuOid", 1000) {
      hakuJaSenAlkamiskaudenVastaanototYhdenPaikanSaadoksenPiirissa(hakuOid)
    }

    val valintatulosIterator = valintatulokset.iterator()
    while (valintatulosIterator.hasNext) {
      val v = valintatulosIterator.next()
      val hakijaOid = if (StringUtils.isNotBlank(v.getHakijaOid)) v.getHakijaOid else hakemusOidsByHakijaOids(v.getHakemusOid)
      val henkilonVastaanotot = haunVastaanotot.get(hakijaOid)
      val hakijanVastaanototHakukohteeseen: List[VastaanottoRecord] = henkilonVastaanotot.map(_.filter(_.hakukohdeOid == hakukohdeOid)).toList.flatten
      val tilaVirkailijalle: ValintatuloksenTila = paatteleVastaanottotilaVirkailijaaVarten(hakijaOid, hakijanVastaanototHakukohteeseen, haku, kaudenVastaanotot)
      v.setTila(tilaVirkailijalle, tilaVirkailijalle, "", "") // pass same old and new tila to avoid log entries
      v.setHakijaOid(hakijaOid, "")
    }
    valintatulokset
  }

  def haeVastaanotonAikarajaTiedot(hakuOid: String, hakukohdeOid: String, hakemusOids: Set[String]): Set[VastaanottoAikarajaMennyt] = {
    sijoittelutulosService.haeVastaanotonAikarajaTiedot(hakuOid, hakukohdeOid, hakemusOids)
  }


  def findValintaTuloksetForVirkailijaByHakemus(hakuOid: String, hakemusOid: String): util.List[Valintatulos] = {
    val hakemuksenTulos = hakemuksentulos(hakuOid, hakemusOid)
      .getOrElse(throw new IllegalArgumentException(s"Not hakemuksen tulos for hakemus $hakemusOid in haku $hakuOid"))
    val henkiloOid = hakemuksenTulos.hakijaOid
    val vastaanotot = virkailijaVastaanottoRepository.findHenkilonVastaanototHaussa(henkiloOid, hakuOid)
    val valintatulokset: util.List[Valintatulos] = valintatulosDao.loadValintatuloksetForHakemus(hakemusOid)

    setValintatuloksetTilat(hakuOid, valintatulokset.asScala, Map(hakemusOid -> hakemuksenTulos), Map(henkiloOid -> vastaanotot))
    valintatulokset
  }

  def sijoittelunTulokset(hakuOid: String, sijoitteluajoId: String, hyvaksytyt: Option[Boolean], ilmanHyvaksyntaa: Option[Boolean], vastaanottaneet: Option[Boolean],
                          hakukohdeOid: Option[List[String]], count: Option[Int], index: Option[Int]): HakijaPaginationObject = {
    val haunVastaanototByHakijaOid = timed("Fetch haun vastaanotot for haku: " + hakuOid, 1000) {
      virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    }
    val hakemustenTulokset = hakemustenTulosByHaku(hakuOid, Some(haunVastaanototByHakijaOid))
    val hakutoiveidenTuloksetByHakemusOid: Map[String,List[Hakutoiveentulos]] = hakemustenTulokset match {
      case Some(hakemustenTulosIterator) => hakemustenTulosIterator.map(h => (h.hakemusOid, h.hakutoiveet)).toMap
      case None => Map()
    }

    val personOidsByHakemusOids = hakemusRepository.findHakemukset(hakuOid).map(h => (h.oid, h.henkiloOid)).toMap
    try {
      val haku = hakuService.getHaku(hakuOid).getOrElse(throw new IllegalArgumentException(s"No such haku $hakuOid"))
      val hakijaPaginationObject = sijoittelutulosService.sijoittelunTuloksetWithoutVastaanottoTieto(hakuOid, sijoitteluajoId, hyvaksytyt, ilmanHyvaksyntaa, vastaanottaneet,
        hakukohdeOid, count, index, haunVastaanototByHakijaOid)

      hakijaPaginationObject.getResults.asScala.foreach { hakijaDto =>
        val hakijaOidFromHakemus = personOidsByHakemusOids(hakijaDto.getHakemusOid)
        hakijaDto.setHakijaOid(hakijaOidFromHakemus)
        val hakijanVastaanotot = haunVastaanototByHakijaOid.get(hakijaDto.getHakijaOid)
        val hakutoiveidenTulokset = hakutoiveidenTuloksetByHakemusOid.getOrElse(hakijaDto.getHakemusOid, throw new IllegalArgumentException(s"Hakemusta ${hakijaDto.getHakemusOid} ei löydy"))
        val yhdenPaikanSannonHuomioiminen = asetaVastaanotettavuusValintarekisterinPerusteella(hakijaDto.getHakijaOid)(hakutoiveidenTulokset, haku, None)
        hakijaDto.getHakutoiveet.asScala.foreach(palautettavaHakutoiveDto =>
          hakijanVastaanotot match {
            case Some(vastaanottos) =>
              vastaanottos.find(_.hakukohdeOid == palautettavaHakutoiveDto.getHakukohdeOid).foreach(vastaanotto => {
                yhdenPaikanSannonHuomioiminen.find(_.hakukohdeOid == palautettavaHakutoiveDto.getHakukohdeOid).foreach(hakutoiveenOikeaTulos => {
                  palautettavaHakutoiveDto.setVastaanottotieto(fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila.valueOf(hakutoiveenOikeaTulos.vastaanottotila.toString))
                  palautettavaHakutoiveDto.getHakutoiveenValintatapajonot.asScala.foreach(_.setTilanKuvaukset(hakutoiveenOikeaTulos.tilanKuvaukset.asJava))
                })
              })
            case None => palautettavaHakutoiveDto.setVastaanottotieto(dto.ValintatuloksenTila.KESKEN)
          }
        )
      }
      hakijaPaginationObject
    } catch {
      case e: Exception =>
        logger.error(s"Sijoittelun hakemuksia ei saatu haulle $hakuOid", e)
        new HakijaPaginationObject
    }
  }

  def sijoittelunTulosHakemukselle(hakuOid: String, sijoitteluajoId: String, hakemusOid: String): Option[HakijaDTO] = {
    val hakemuksenTulosOption = hakemuksentulos(hakuOid, hakemusOid)
    val hakijaOidFromHakemusOption = hakemusRepository.findHakemus(hakemusOid).map(_.henkiloOid)

    sijoittelutulosService.findSijoitteluAjo(hakuOid, sijoitteluajoId) match {
      case Some(sijoitteluAjo) =>
        val hakijaDto = sijoittelutulosService.sijoittelunTulosForAjoWithoutVastaanottoTieto(sijoitteluAjo, hakemusOid)
        if (hakijaDto != null) {
          hakijaOidFromHakemusOption.foreach(hakijaOidFromHakemus => hakijaDto.setHakijaOid(hakijaOidFromHakemus))
          hakemuksenTulosOption.foreach(hakemuksenTulos => populateVastaanottotieto(hakijaDto, hakemuksenTulos.hakutoiveet))
          Some(hakijaDto)
        } else {
          None
        }
      case None => None
      }
  }

  def streamSijoittelunTulokset(hakuOid: String, sijoitteluajoId: String, writeResult: HakijaDTO => Unit): Unit = {
    val haunVastaanototByHakijaOid = timed("Fetch haun vastaanotot for haku: " + hakuOid, 1000) {
      virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    }
    val hakemustenTulokset = hakemustenTulosByHaku(hakuOid, Some(haunVastaanototByHakijaOid))
    val hakutoiveidenTuloksetByHakemusOid: Map[String, (String, List[Hakutoiveentulos])] = timed(s"Find hakutoiveiden tulokset for haku $hakuOid", 1000) {
      hakemustenTulokset match {
        case Some(hakemustenTulosIterator) => hakemustenTulosIterator.map(h => (h.hakemusOid, (h.hakijaOid, h.hakutoiveet))).toMap
        case None => Map()
      }
    }
    logger.info(s"Found ${hakutoiveidenTuloksetByHakemusOid.keySet.size} hakemus objects for sijoitteluajo $sijoitteluajoId of haku $hakuOid")

    try {
      streamingHakijaDtoClient.processSijoittelunTulokset(hakuOid, sijoitteluajoId, { hakijaDto: HakijaDTO =>
        hakutoiveidenTuloksetByHakemusOid.get(hakijaDto.getHakemusOid) match {
          case Some((hakijaOid, hakutoiveidenTulokset)) =>
            hakijaDto.setHakijaOid(hakijaOid)
            populateVastaanottotieto(hakijaDto, hakutoiveidenTulokset)
            writeResult(hakijaDto)
          case None => crashOrLog(s"Hakemus ${hakijaDto.getHakemusOid} not found in hakemusten tulokset for haku $hakuOid")
        }
      })
    } catch {
      case e: Exception =>
        logger.error(s"Sijoitteluajon $sijoitteluajoId hakemuksia ei saatu palautettua haulle $hakuOid", e)
        throw e
    }
  }

  private def populateVastaanottotieto(hakijaDto: HakijaDTO, hakemuksenHakutoiveidenTuloksetVastaanottotiedonKanssa: List[Hakutoiveentulos]): Unit = {
    hakijaDto.getHakutoiveet.asScala.foreach(palautettavaHakutoiveDto =>
      hakemuksenHakutoiveidenTuloksetVastaanottotiedonKanssa.find(_.hakukohdeOid == palautettavaHakutoiveDto.getHakukohdeOid) match {
        case Some(hakutoiveenOikeaTulos) =>
          palautettavaHakutoiveDto.setVastaanottotieto(fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila.valueOf(hakutoiveenOikeaTulos.vastaanottotila.toString))
          palautettavaHakutoiveDto.getHakutoiveenValintatapajonot.asScala.foreach(_.setTilanKuvaukset(hakutoiveenOikeaTulos.tilanKuvaukset.asJava))
        case None => palautettavaHakutoiveDto.setVastaanottotieto(dto.ValintatuloksenTila.KESKEN)
      }
    )
  }

  private def mapHakemustenTuloksetByHakemusOid(hakemustenTulokset:Iterator[Hakemuksentulos]):Map[String,Hakemuksentulos] = {
    hakemustenTulokset.toList.groupBy(_.hakemusOid).mapValues(_.head)
  }

  private def setValintatuloksetTilat(hakuOid:String,
                                      valintatulokset: Seq[Valintatulos],
                                      hakemustenTulokset: Map[String,Hakemuksentulos],
                                      haunVastaanotot: Map[String,Set[VastaanottoRecord]] ): Unit = {
    valintatulokset.foreach(valintaTulos => {
      val hakemuksenTulosOption: Option[Hakemuksentulos] = hakemustenTulokset.get(valintaTulos.getHakemusOid).orElse(
        crashOrLog(s"No hakemuksen tulos found for hakemus ${valintaTulos.getHakemusOid}"))
      val hakutoiveenTulosOption: Option[Hakutoiveentulos] = hakemuksenTulosOption.flatMap { _.findHakutoive(valintaTulos.getHakukohdeOid).map(_._1) }.orElse(
        crashOrLog(s"No hakutoive found for hakukohde ${valintaTulos.getHakukohdeOid} in hakemus ${valintaTulos.getHakemusOid}"))

      val tulosPari: Option[(Hakemuksentulos, Hakutoiveentulos)] = hakemuksenTulosOption.flatMap { hakemuksenTulos => hakutoiveenTulosOption.map((hakemuksenTulos, _)) }

      tulosPari match {
        case Some((hakemuksenTulos, hakutoiveenTulos)) =>
          assertThatHakijaOidsDoNotConflict(valintaTulos, hakemuksenTulos)
          val tilaHakijalle = ValintatuloksenTila.valueOf(hakutoiveenTulos.vastaanottotila.toString)

          val hakijaOid = hakemuksenTulos.hakijaOid
          val tilaVirkailijalle = ValintatulosService.toVirkailijaTila(tilaHakijalle, haunVastaanotot.get(hakijaOid), hakutoiveenTulos.hakukohdeOid)
          valintaTulos.setTila(tilaVirkailijalle, tilaVirkailijalle, "", "") // pass same old and new tila to avoid log entries
          valintaTulos.setHakijaOid(hakemuksenTulos.hakijaOid, "")
          valintaTulos.setTilaHakijalle(tilaHakijalle)
        case None =>
          crashOrLog(s"Problem when processing valintatulos for hakemus ${valintaTulos.getHakemusOid}")
          valintaTulos.setTila(ValintatuloksenTila.KESKEN, ValintatuloksenTila.KESKEN, "Tilaa ei saatu luettua sijoittelun tuloksista", "")
          valintaTulos.setTilaHakijalle(ValintatuloksenTila.KESKEN)
      }
    })
  }

  private def assertThatHakijaOidsDoNotConflict(valintaTulos: Valintatulos, hakemuksenTulos: Hakemuksentulos): Unit = {
    if (valintaTulos.getHakijaOid != null && !valintaTulos.getHakijaOid.equals(hakemuksenTulos.hakijaOid)) {
      crashOrLog(s"Conflicting hakija oids: valintaTulos: ${valintaTulos.getHakijaOid} vs hakemuksenTulos: ${hakemuksenTulos.hakijaOid} in $valintaTulos , $hakemuksenTulos")
    }
  }

  private def fetchTulokset(haku: Haku,
                            getHakemukset: () => Iterator[Hakemus],
                            getSijoittelunTulos: Map[String, String] => Seq[HakemuksenSijoitteluntulos],
                            hakijaOidsByHakemusOids: Option[Map[String, String]] = None,
                            checkJulkaisuAikaParametri: Boolean = true,
                            kaudenVastaanottaneet: Option[Set[String]] = None): Iterator[Hakemuksentulos] = {
    val ohjausparametrit = ohjausparametritService.ohjausparametrit(haku.oid)
    val hakemukset = getHakemukset()
    val sijoitteluTulokset = timed("Fetch sijoittelun tulos", 1000) {
      getSijoittelunTulos(
        hakijaOidsByHakemusOids.getOrElse(getHakemukset().map(h => (h.oid, h.henkiloOid)).toMap)
      ).map(t => (t.hakemusOid, t)).toMap
    }
    hakemukset.map(hakemus => {
      val sijoitteluTulos = sijoitteluTulokset.getOrElse(hakemus.oid, tyhjäHakemuksenTulos(hakemus.oid, ohjausparametrit.flatMap(_.vastaanottoaikataulu)))
      julkaistavaTulos(sijoitteluTulos, haku, ohjausparametrit, checkJulkaisuAikaParametri, kaudenVastaanottaneet)(hakemus)
    })
  }

  private def julkaistavaTulos(sijoitteluTulos: HakemuksenSijoitteluntulos,
                               haku: Haku,
                               ohjausparametrit: Option[Ohjausparametrit],
                               checkJulkaisuAikaParametri: Boolean,
                               kaudenVastaanottaneet: Option[Set[String]] = None)(h:Hakemus)(implicit appConfig: AppConfig): Hakemuksentulos = {
    val tulokset = h.toiveet.map { toive =>
      val hakutoiveenSijoittelunTulos: HakutoiveenSijoitteluntulos = sijoitteluTulos.hakutoiveet.find { t =>
        t.hakukohdeOid == toive.oid
      }.getOrElse(HakutoiveenSijoitteluntulos.kesken(toive.oid, toive.tarjoajaOid))

      Hakutoiveentulos.julkaistavaVersioSijoittelunTuloksesta(hakutoiveenSijoittelunTulos, toive, haku, ohjausparametrit, checkJulkaisuAikaParametri)
    }

    val lopullisetTulokset = Välitulos(tulokset, haku, ohjausparametrit)
      .map(näytäJulkaisematontaAlemmatPeruutetutKeskeneräisinä)
      .map(peruValmistaAlemmatKeskeneräisetJosKäytetäänSijoittelua)
      .map(näytäVarasijaltaHyväksytytHyväksyttyinäJosVarasijasäännötEiVoimassa)
      .map(sovellaKorkeakoulujenVarsinaisenYhteishaunSääntöjä)
      .map(sovellaKorkeakoulujenLisähaunSääntöjä)
      .map(näytäAlemmatPeruutuneetKeskeneräisinäJosYlemmätKeskeneräisiä)
      .map(piilotaKuvauksetKeskeneräisiltä)
      .map(asetaVastaanotettavuusValintarekisterinPerusteella(h.henkiloOid, kaudenVastaanottaneet))
      .tulokset

    Hakemuksentulos(haku.oid, h.oid, sijoitteluTulos.hakijaOid.getOrElse(h.henkiloOid), ohjausparametrit.flatMap(_.vastaanottoaikataulu), lopullisetTulokset)
  }

  private def tyhjäHakemuksenTulos(hakemusOid: String, aikataulu: Option[Vastaanottoaikataulu]) = HakemuksenSijoitteluntulos(hakemusOid, None, Nil)

  private def asetaVastaanotettavuusValintarekisterinPerusteella(henkiloOid: String, kaudenVastaanottaneet: Option[Set[String]] = None)(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    def ottanutVastaanToisenPaikan(tulos: Hakutoiveentulos): Hakutoiveentulos =
      tulos.copy(
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa,
        vastaanottotila = Vastaanottotila.ottanut_vastaan_toisen_paikan
      )

    def peruuntunutOttanutVastaanToisenPaikan(tulos: Hakutoiveentulos): Hakutoiveentulos =
      ottanutVastaanToisenPaikan(if (tulos.julkaistavissa) {
        tulos.copy(
          valintatila = Valintatila.peruuntunut,
          tilanKuvaukset = Map(
            "FI" -> "Peruuntunut, vastaanottanut toisen korkeakoulupaikan",
            "SV" -> "Annullerad, tagit emot en annan högskoleplats",
            "EN" -> "Cancelled, accepted another higher education study place"
          )
        )
      } else {
        tulos
      })

    def hyvaksyttyTaiVaralla(tulos: Hakutoiveentulos): Boolean = isHyväksytty(tulos.valintatila) || tulos.valintatila == Valintatila.varalla

    def aiempiVastaanotto(hakukohdeOid: String): Boolean = kaudenVastaanottaneet.map(_.contains(henkiloOid))
      .getOrElse(try {
        virkailijaVastaanottoRepository.runBlocking(vastaanotettavuusService.tarkistaAiemmatVastaanotot(henkiloOid, hakukohdeOid))
        false
      } catch {
        case t: PriorAcceptanceException => true
      })
    if (haku.yhdenPaikanSaanto.voimassa) {
      val ehdollinenVastaanottoTallaHakemuksella = tulokset.exists(x => Vastaanottotila.ehdollisesti_vastaanottanut == x.vastaanottotila)
      val sitovaVastaanottoTallaHakemuksella = tulokset.exists(x => Vastaanottotila.vastaanottanut == x.vastaanottotila)
      if (ehdollinenVastaanottoTallaHakemuksella) {
        tulokset
      } else if (sitovaVastaanottoTallaHakemuksella) {
        tulokset.map(tulos => if (Vastaanottotila.kesken == tulos.vastaanottotila && hyvaksyttyTaiVaralla(tulos)) {
          peruuntunutOttanutVastaanToisenPaikan(tulos)
        } else {
          tulos
        })
      } else {
        tulokset.map(tulos => if (Vastaanottotila.kesken == tulos.vastaanottotila && hyvaksyttyTaiVaralla(tulos) && aiempiVastaanotto(tulos.hakukohdeOid)) {
          peruuntunutOttanutVastaanToisenPaikan(tulos)
        } else {
          tulos
        })
      }
    } else {
      tulokset
    }
  }

  private def paatteleVastaanottotilaVirkailijaaVarten(hakijaOid: String, hakijanVastaanototHakukohteeseen: List[VastaanottoRecord],
                                                       haku: Haku, kaudenVastaanototYpsnPiirissa: Set[VastaanottoRecord]): ValintatuloksenTila = {
    def resolveValintatuloksenTilaVirkailijalleFrom(several: Iterable[VastaanottoRecord]): ValintatuloksenTila = {
      if (several.groupBy(_.action).size == 1) {
        several.head.action.valintatuloksenTila
      } else {
        throw new IllegalStateException(s"Don't know how to choose relevant vastaanotto record from $several")
      }
    }

    val tilaSuoraanHakukohteenVastaanottojenPerusteella: ValintatuloksenTila = hakijanVastaanototHakukohteeseen match {
      case Nil => ValintatuloksenTila.KESKEN
      case only :: Nil => only.action.valintatuloksenTila
      case several => resolveValintatuloksenTilaVirkailijalleFrom(several)
    }

    if (!haku.yhdenPaikanSaanto.voimassa) {
      tilaSuoraanHakukohteenVastaanottojenPerusteella
    } else if (tilaSuoraanHakukohteenVastaanottojenPerusteella == ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI ||
      tilaSuoraanHakukohteenVastaanottojenPerusteella == ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT ||
      tilaSuoraanHakukohteenVastaanottojenPerusteella == ValintatuloksenTila.PERUNUT ||
      tilaSuoraanHakukohteenVastaanottojenPerusteella == ValintatuloksenTila.PERUUTETTU ||
      tilaSuoraanHakukohteenVastaanottojenPerusteella == ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA
    ) {
      tilaSuoraanHakukohteenVastaanottojenPerusteella
    } else {
      paatteleVastaanottotilaYhdenPaikanSaadoksenMukaanVirkailijaaVarten(hakijaOid, kaudenVastaanototYpsnPiirissa, haku)
    }
  }

  private def paatteleVastaanottotilaYhdenPaikanSaadoksenMukaanVirkailijaaVarten(henkiloOid: String, kaudenVastaanototYpsnPiirissa: Set[VastaanottoRecord],
                                                                                 haku: Haku): ValintatuloksenTila= {
    val henkilonAiemmatVastaanototSamalleKaudelleYpsnPiirissa = kaudenVastaanototYpsnPiirissa.filter(_.henkiloOid == henkiloOid)
    def vastaanottoEriHaussa: Boolean = henkilonAiemmatVastaanototSamalleKaudelleYpsnPiirissa.exists(_.hakuOid != haku.oid)

    if (henkilonAiemmatVastaanototSamalleKaudelleYpsnPiirissa.isEmpty) {
      ValintatuloksenTila.KESKEN
    } else if (vastaanottoEriHaussa) {
      ValintatuloksenTila.OTTANUT_VASTAAN_TOISEN_PAIKAN
    } else ValintatuloksenTila.KESKEN
  }

  private def sovellaKorkeakoulujenVarsinaisenYhteishaunSääntöjä(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    if (haku.korkeakoulu && haku.yhteishaku && haku.varsinainenhaku) {
      val firstVaralla = tulokset.indexWhere(_.valintatila == Valintatila.varalla)
      val firstVastaanotettu = tulokset.indexWhere(_.vastaanottotila == Vastaanottotila.vastaanottanut)
      val firstKesken = tulokset.indexWhere(_.valintatila == Valintatila.kesken)
      val indexedTulokset = tulokset.zipWithIndex
      val firstHyvaksyttyUnderFirstVaralla = if (firstVaralla >= 0) {
        indexedTulokset.indexWhere { case (tulos, index) => index > firstVaralla && Valintatila.isHyväksytty(tulos.valintatila) }
      } else {
        -1
      }

      tulokset.zipWithIndex.map {
        case (tulos, index) if isHyväksytty(tulos.valintatila) && tulos.vastaanottotila == Vastaanottotila.kesken =>
          if (firstVastaanotettu >= 0 && index != firstVastaanotettu)
            // Peru vastaanotettua paikkaa alemmat hyväksytyt hakutoiveet
            tulos.copy(valintatila = Valintatila.peruuntunut, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
          else if (index == firstHyvaksyttyUnderFirstVaralla) {
           if(ehdollinenVastaanottoMahdollista(ohjausparametrit))
            // Ehdollinen vastaanotto mahdollista
            tulos.copy(vastaanotettavuustila = Vastaanotettavuustila.vastaanotettavissa_ehdollisesti)
           else
            // Ehdollinen vastaanotto ei vielä mahdollista, näytetään keskeneräisenä
            tulos.toKesken
          }
          else if (firstKesken >= 0 && index > firstKesken)
            tulos.toKesken
          else
            tulos
        case (tulos, index) if firstVastaanotettu >= 0 && index != firstVastaanotettu && List(Valintatila.varalla, Valintatila.kesken).contains(tulos.valintatila) =>
          // Peru muut varalla/kesken toiveet, jos jokin muu vastaanotettu
          tulos.copy(valintatila = Valintatila.peruuntunut, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
        case (tulos, _) => tulos
      }
    } else {
      tulokset
    }
  }

  private def sovellaKorkeakoulujenLisähaunSääntöjä(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    if (haku.korkeakoulu && haku.yhteishaku && haku.lisähaku) {
      if (tulokset.count(_.vastaanottotila == Vastaanottotila.vastaanottanut) > 0) {
        // Peru muut kesken toiveet, jokin vastaanotettu
        tulokset.map( tulos => if (List(Vastaanottotila.kesken).contains(tulos.vastaanottotila)) {
          tulos.copy(valintatila = Valintatila.peruuntunut, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
        } else {
          tulos
        })
      } else {
        tulokset
      }
    } else {
      tulokset
    }
  }

  private def peruValmistaAlemmatKeskeneräisetJosKäytetäänSijoittelua(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    if (haku.käyttääSijoittelua) {
      val firstFinished = tulokset.indexWhere { t =>
        isHyväksytty(t.valintatila) || List(Valintatila.perunut, Valintatila.peruutettu, Valintatila.peruuntunut).contains(t.valintatila)
      }

      tulokset.zipWithIndex.map {
        case (tulos, index) if haku.käyttääSijoittelua && firstFinished > -1 && index > firstFinished && tulos.valintatila == Valintatila.kesken =>
          tulos.copy(valintatila = Valintatila.peruuntunut)
        case (tulos, _) => tulos
      }
    } else {
      tulokset
    }
  }

  private def näytäAlemmatPeruutuneetKeskeneräisinäJosYlemmätKeskeneräisiä(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    val firstKeskeneräinen = tulokset.indexWhere (_.valintatila == Valintatila.kesken)
    tulokset.zipWithIndex.map {
      case (tulos, index) if firstKeskeneräinen >= 0 && index > firstKeskeneräinen && tulos.valintatila == Valintatila.peruuntunut => tulos.toKesken
      case (tulos, _) => tulos
    }
  }

  private def näytäJulkaisematontaAlemmatPeruutetutKeskeneräisinä(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    val firstJulkaisematon = tulokset.indexWhere (!_.julkaistavissa)
    tulokset.zipWithIndex.map {
      case (tulos, index) if firstJulkaisematon >= 0 && index > firstJulkaisematon && tulos.valintatila == Valintatila.peruuntunut => tulos.toKesken
      case (tulos, _) => tulos
    }
  }

  private def piilotaKuvauksetKeskeneräisiltä(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    tulokset.map {
      case h if h.valintatila == Valintatila.kesken => h.copy(tilanKuvaukset = Map.empty)
      case h => h
    }
  }

  private def näytäVarasijaltaHyväksytytHyväksyttyinäJosVarasijasäännötEiVoimassa(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    tulokset.map {
      case tulos if tulos.valintatila == Valintatila.varasijalta_hyväksytty && !ehdollinenVastaanottoMahdollista(ohjausparametrit) =>
        tulos.copy(valintatila = Valintatila.hyväksytty, tilanKuvaukset = Map.empty)
      case tulos => tulos
    }
  }

  private def ehdollinenVastaanottoMahdollista(ohjausparametrit: Option[Ohjausparametrit]): Boolean = {
    ohjausparametrit.getOrElse(Ohjausparametrit(None, None, None, None, None)).varasijaSaannotAstuvatVoimaan match {
      case None => true
      case Some(varasijaSaannotAstuvatVoimaan) => varasijaSaannotAstuvatVoimaan.isBefore(new DateTime())
    }
  }

  case class Välitulos(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) {
    def map(f: (List[Hakutoiveentulos], Haku, Option[Ohjausparametrit]) => List[Hakutoiveentulos]) = {
      Välitulos(f(tulokset, haku, ohjausparametrit), haku, ohjausparametrit)
    }
  }

  private def crashOrLog[T](msg: String): Option[T] = {
    if (appConfig.settings.lenientSijoitteluntuloksetParsing) {
      logger.warn(msg)
      None
    } else {
      throw new IllegalStateException(msg)
    }
  }
}

object ValintatulosService {
  def toVirkailijaTila(valintatuloksenTilaForHakija: ValintatuloksenTila,
                       hakijanVastaanototHaussa: Option[Set[VastaanottoRecord]],
                       hakukohdeOid: String): ValintatuloksenTila = {

    def merkittyMyohastyneeksi(v: VastaanottoRecord) = v.hakukohdeOid == hakukohdeOid && v.action == MerkitseMyohastyneeksi
    if(valintatuloksenTilaForHakija == ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA && !hakijanVastaanototHaussa.exists(_.exists(merkittyMyohastyneeksi))) {
      ValintatuloksenTila.KESKEN
    } else {
      valintatuloksenTilaForHakija
    }
  }
}


package fi.vm.sade.valintatulosservice

import java.util

import fi.vm.sade.sijoittelu.domain.{SijoitteluAjo, ValintatuloksenTila, Valintatulos}
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

  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    for {
      haku <- hakuService.getHaku(hakuOid)
      hakemus <- fetchTulokset(
        haku,
        () => hakemusRepository.findHakemus(hakemusOid).iterator,
        hakijaOidsByHakemusOids => sijoittelutulosService.hakemuksenTulos(haku, hakemusOid, hakijaOidsByHakemusOids.get(hakemusOid)).toSeq
      ).toSeq.headOption
    } yield hakemus
  }

  def hakemuksentuloksetByPerson(hakuOid: String, personOid: String): List[Hakemuksentulos] = {
    val hakemukset = hakemusRepository.findHakemukset(hakuOid, personOid).toSeq
    (for {
      haku <- hakuService.getHaku(hakuOid)
    } yield {
      fetchTulokset(
        haku,
        () => hakemukset.toIterator,
        hakijaOidsByHakemusOids => hakemukset.flatMap(hakemus => sijoittelutulosService.hakemuksenTulos(haku, hakemus.oid, hakijaOidsByHakemusOids.get(hakemus.oid)))
      ).toList
    }).getOrElse(List.empty)
  }

  def hakemustenTulosByHaku(hakuOid: String): Option[Iterator[Hakemuksentulos]] = {
    val haunVastaanotot = timed("Fetch haun vastaanotot for haku: " + hakuOid, 1000) {
      virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    }
    hakemustenTulosByHaku(hakuOid, Some(haunVastaanotot))
  }

  private def hakemustenTulosByHaku(hakuOid: String, haunVastaanotot: Option[Map[String,Set[VastaanottoRecord]]] ): Option[Iterator[Hakemuksentulos]] = {
    timed("Fetch hakemusten tulos for haku: " + hakuOid, 1000) (
      for {
        haku <- hakuService.getHaku(hakuOid)
      } yield {
        fetchTulokset(
          haku,
          () => hakemusRepository.findHakemukset(hakuOid),
          hakijaOidsByHakemusOids => sijoittelutulosService.hakemustenTulos(hakuOid, hakijaOidsByHakemusOids = hakijaOidsByHakemusOids, haunVastaanotot = haunVastaanotot),
          Some(timed("personOids from hakemus", 1000)(hakemusRepository.findPersonOids(hakuOid))),
          Some(timed("kaudenVastaanotot", 1000)({
            val kausi = hakukohdeRecordService.getHaunKoulutuksenAlkamiskausi(hakuOid)
            virkailijaVastaanottoRepository.findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(kausi)
          }))
        )
      }
    )
  }

  def hakemustenTulosByHakukohde(hakuOid: String, hakukohdeOid: String, hakukohteenVastaanotot: Option[Map[String,Set[VastaanottoRecord]]] = None): Option[Iterator[Hakemuksentulos]] = {
    timed("Fetch hakemusten tulos for haku: "+ hakuOid + " and hakukohde: " + hakuOid, 1000) (
      for {
        haku <- hakuService.getHaku(hakuOid)
      } yield {
        fetchTulokset(
          haku,
          () => hakemusRepository.findHakemuksetByHakukohde(hakuOid, hakukohdeOid),
          hakijaOidsByHakemusOids => sijoittelutulosService.hakemustenTulos(hakuOid, Some(hakukohdeOid), hakijaOidsByHakemusOids, hakukohteenVastaanotot)
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
      val hakijaPaginationObject = sijoittelutulosService.sijoittelunTuloksetWithoutVastaanottoTieto(hakuOid, sijoitteluajoId, hyvaksytyt, ilmanHyvaksyntaa, vastaanottaneet,
        hakukohdeOid, count, index, haunVastaanototByHakijaOid)

      hakijaPaginationObject.getResults.asScala.foreach { hakijaDto =>
        val hakijaOidFromHakemus = personOidsByHakemusOids(hakijaDto.getHakemusOid)
        hakijaDto.setHakijaOid(hakijaOidFromHakemus)
        val hakijanVastaanotot = haunVastaanototByHakijaOid.get(hakijaDto.getHakijaOid)
        val hakutoiveidenTulokset = hakutoiveidenTuloksetByHakemusOid.getOrElse(hakijaDto.getHakemusOid, throw new IllegalArgumentException(s"Hakemusta ${hakijaDto.getHakemusOid} ei löydy"))
        val yhdenPaikanSannonHuomioiminen = asetaVastaanotettavuusValintarekisterinPerusteella(hakijaDto.getHakijaOid)(hakutoiveidenTulokset, haku = null, None)
        hakijaDto.getHakutoiveet.asScala.foreach(palautettavaHakutoiveDto =>
          hakijanVastaanotot match {
            case Some(vastaanottos) =>
              vastaanottos.find(_.hakukohdeOid == palautettavaHakutoiveDto.getHakukohdeOid).foreach(vastaanotto => {
                val tilaIlmanYhdenPaikanSaantoa = sijoittelutulosService.vastaanottotilaVainViimeisimmanVastaanottoActioninPerusteella(Some(vastaanotto.action))
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

  def streamSijoittelunTulokset(hakuOid: String, sijoitteluajoId: String, writeResult: HakijaDTO => Unit): Unit = {
    val haunVastaanototByHakijaOid = timed("Fetch haun vastaanotot for haku: " + hakuOid, 1000) {
      virkailijaVastaanottoRepository.findHaunVastaanotot(hakuOid).groupBy(_.henkiloOid)
    }
    val hakemustenTulokset = hakemustenTulosByHaku(hakuOid, Some(haunVastaanototByHakijaOid))
    val hakutoiveidenTuloksetByHakemusOid: Map[String, List[Hakutoiveentulos]] = timed(s"Find hakutoiveiden tulokset for haku $hakuOid", 1000) {
      hakemustenTulokset match {
        case Some(hakemustenTulosIterator) => hakemustenTulosIterator.map(h => (h.hakemusOid, h.hakutoiveet)).toMap
        case None => Map()
      }
    }
    val personOidsByHakemusOids = timed(s"Fetch hakemus to person oid mapping for haku $hakuOid", 1000) {
      hakemusRepository.findPersonOids(hakuOid)
    }
    logger.info(s"Found ${personOidsByHakemusOids.keySet.size} hakemus objects for sijoitteluajo $sijoitteluajoId of haku $hakuOid")

    try {
      streamingHakijaDtoClient.processSijoittelunTulokset(hakuOid, sijoitteluajoId, { hakijaDto: HakijaDTO =>
        val hakijaOidFromHakemus = personOidsByHakemusOids(hakijaDto.getHakemusOid)
        val hakutoiveidenTulokset = hakutoiveidenTuloksetByHakemusOid.getOrElse(hakijaDto.getHakemusOid, throw new IllegalArgumentException(s"Hakemusta ${hakijaDto.getHakemusOid} ei löydy"))
        hakijaDto.setHakijaOid(hakijaOidFromHakemus)
        hakijaDto.getHakutoiveet.asScala.foreach(palautettavaHakutoiveDto =>
          hakutoiveidenTulokset.find(_.hakukohdeOid == palautettavaHakutoiveDto.getHakukohdeOid) match {
            case Some(hakutoiveenOikeaTulos) =>
              palautettavaHakutoiveDto.setVastaanottotieto(fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila.valueOf(hakutoiveenOikeaTulos.vastaanottotila.toString))
              palautettavaHakutoiveDto.getHakutoiveenValintatapajonot.asScala.foreach(_.setTilanKuvaukset(hakutoiveenOikeaTulos.tilanKuvaukset.asJava))
            case None => palautettavaHakutoiveDto.setVastaanottotieto(dto.ValintatuloksenTila.KESKEN)
          }
        )
        writeResult(hakijaDto)
      })
    } catch {
      case e: Exception =>
        logger.error(s"Sijoitteluajon $sijoitteluajoId hakemuksia ei saatu palautettua haulle $hakuOid", e)
        throw e
    }
  }

  private def mapHakemustenTuloksetByHakemusOid(hakemustenTulokset:Iterator[Hakemuksentulos]):Map[String,Hakemuksentulos] = {
    hakemustenTulokset.toList.groupBy(_.hakemusOid).mapValues(_.head)
  }

  private def setValintatuloksetTilat(hakuOid:String,
                                      valintatulokset: Seq[Valintatulos],
                                      hakemustenTulokset: Map[String,Hakemuksentulos],
                                      haunVastaanotot: Map[String,Set[VastaanottoRecord]] ): Unit = {
    valintatulokset.foreach(valintaTulos => {
      val hakemuksenTulos = hakemustenTulokset.getOrElse(valintaTulos.getHakemusOid,
        throw new IllegalStateException(s"No hakemuksen tulos found for hakemus ${valintaTulos.getHakemusOid}"))
      assertThatHakijaOidsDoNotConflict(valintaTulos, hakemuksenTulos)
      val hakutoiveenTulos = hakemuksenTulos.findHakutoive(valintaTulos.getHakukohdeOid)
        .getOrElse(throw new IllegalStateException(s"No hakutoive found for hakukohde ${valintaTulos.getHakukohdeOid} in hakemus ${valintaTulos.getHakemusOid}"))
      val tilaHakijalle = ValintatuloksenTila.valueOf(hakutoiveenTulos.vastaanottotila.toString)

      val hakijaOid = hakemuksenTulos.hakijaOid
      val tilaVirkailijalle = ValintatulosService.toVirkailijaTila(tilaHakijalle, haunVastaanotot.get(hakijaOid), hakutoiveenTulos.hakukohdeOid)
      valintaTulos.setTila(tilaVirkailijalle, tilaVirkailijalle, "", "") // pass same old and new tila to avoid log entries
      valintaTulos.setHakijaOid(hakemuksenTulos.hakijaOid, "")
      valintaTulos.setTilaHakijalle(tilaHakijalle)
    })
  }

  private def assertThatHakijaOidsDoNotConflict(valintaTulos: Valintatulos, hakemuksenTulos: Hakemuksentulos): Unit = {
    if (valintaTulos.getHakijaOid != null && !valintaTulos.getHakijaOid.equals(hakemuksenTulos.hakijaOid)) {
      throw new IllegalStateException(s"Conflicting hakija oids: valintaTulos: ${valintaTulos.getHakijaOid} vs hakemuksenTulos: ${hakemuksenTulos.hakijaOid} in $valintaTulos , $hakemuksenTulos")
    }
  }

  private def fetchTulokset(haku: Haku,
                            getHakemukset: () => Iterator[Hakemus],
                            getSijoittelunTulos: Map[String, String] => Seq[HakemuksenSijoitteluntulos],
                            hakijaOidsByHakemusOids: Option[Map[String, String]] = None,
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
      julkaistavaTulos(sijoitteluTulos, haku, ohjausparametrit, kaudenVastaanottaneet)(hakemus)
    })
  }

  private def julkaistavaTulos(sijoitteluTulos: HakemuksenSijoitteluntulos,
                               haku: Haku,
                               ohjausparametrit: Option[Ohjausparametrit],
                               kaudenVastaanottaneet: Option[Set[String]] = None)(h:Hakemus)(implicit appConfig: AppConfig): Hakemuksentulos = {
    val tulokset = h.toiveet.map { toive =>
      val hakutoiveenSijoittelunTulos: HakutoiveenSijoitteluntulos = sijoitteluTulos.hakutoiveet.find { t =>
        t.hakukohdeOid == toive.oid
      }.getOrElse(HakutoiveenSijoitteluntulos.kesken(toive.oid, toive.tarjoajaOid))

      Hakutoiveentulos.julkaistavaVersioSijoittelunTuloksesta(hakutoiveenSijoittelunTulos, toive, haku, ohjausparametrit)
    }

    val lopullisetTulokset = Välitulos(tulokset, haku, ohjausparametrit)
      .map(näytäJulkaisematontaAlemmatPeruutetutKeskeneräisinä)
      .map(peruValmistaAlemmatKeskeneräisetJosKäytetäänSijoittelua)
      .map(näytäAlemmatPeruutuneetKeskeneräisinäJosYlemmätKeskeneräisiä)
      .map(näytäVarasijaltaHyväksytytHyväksyttyinäJosVarasijasäännötEiVoimassa)
      .map(sovellaKorkeakoulujenVarsinaisenYhteishaunSääntöjä)
      .map(sovellaKorkeakoulujenLisähaunSääntöjä)
      .map(piilotaKuvauksetKeskeneräisiltä)
      .map(asetaVastaanotettavuusValintarekisterinPerusteella(h.henkiloOid, kaudenVastaanottaneet))
      .tulokset

    Hakemuksentulos(haku.oid, h.oid, sijoitteluTulos.hakijaOid.getOrElse(h.henkiloOid), ohjausparametrit.flatMap(_.vastaanottoaikataulu), lopullisetTulokset)
  }

  private def tyhjäHakemuksenTulos(hakemusOid: String, aikataulu: Option[Vastaanottoaikataulu]) = HakemuksenSijoitteluntulos(hakemusOid, None, Nil)

  private def asetaVastaanotettavuusValintarekisterinPerusteella(henkiloOid: String, kaudenVastaanottaneet: Option[Set[String]] = None)(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    def ottanutVastaanToisenPaikan(tulos: Hakutoiveentulos): Hakutoiveentulos = {
      val t = tulos.copy(
        vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa,
        vastaanottotila = Vastaanottotila.ottanut_vastaan_toisen_paikan
      )
      if (tulos.julkaistavissa && (isHyväksytty(tulos.valintatila) || tulos.valintatila == Valintatila.varalla)) {
        t.copy(
          valintatila = Valintatila.peruuntunut,
          tilanKuvaukset = Map(
            "FI" -> "Peruuntunut, vastaanottanut toisen korkeakoulupaikan",
            "SV" -> "Annullerad, tagit emot en annan högskoleplats",
            "EN" -> "Cancelled, accepted another higher education study place"
          )
        )
      } else {
        t
      }
    }
    def aiempiVastaanotto(hakukohdeOid: String): Boolean = kaudenVastaanottaneet.map(_.contains(henkiloOid))
      .getOrElse(try {
        virkailijaVastaanottoRepository.runBlocking(vastaanotettavuusService.tarkistaAiemmatVastaanotot(henkiloOid, hakukohdeOid))
        false
      } catch {
        case t: PriorAcceptanceException => true
      })
    val vastaanottoTallaHakemuksella = tulokset.exists(x => Set(Vastaanottotila.vastaanottanut, Vastaanottotila.ehdollisesti_vastaanottanut).contains(x.vastaanottotila))
    if (vastaanottoTallaHakemuksella) {
      tulokset
    } else {
      tulokset.map(tulos => if (Vastaanottotila.kesken == tulos.vastaanottotila && aiempiVastaanotto(tulos.hakukohdeOid)) {
        ottanutVastaanToisenPaikan(tulos)
      } else {
        tulos
      })
    }
  }

  private def sovellaKorkeakoulujenVarsinaisenYhteishaunSääntöjä(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    if (haku.korkeakoulu && haku.yhteishaku && haku.varsinainenhaku) {
      val firstVaralla = tulokset.indexWhere(_.valintatila == Valintatila.varalla)
      val firstVastaanotettu = tulokset.indexWhere(_.vastaanottotila == Vastaanottotila.vastaanottanut)
      val firstKesken = tulokset.indexWhere(_.valintatila == Valintatila.kesken)

      tulokset.zipWithIndex.map {
        case (tulos, index) if isHyväksytty(tulos.valintatila) && tulos.vastaanottotila == Vastaanottotila.kesken =>
          if (firstVastaanotettu >= 0 && index != firstVastaanotettu)
            // Peru vastaanotettua paikkaa alemmat hyväksytyt hakutoiveet
            tulos.copy(valintatila = Valintatila.peruuntunut, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
          else if (firstVaralla >= 0 && index > firstVaralla) {
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
    ohjausparametrit.getOrElse(Ohjausparametrit(None, None, None, None)).varasijaSaannotAstuvatVoimaan match {
      case None => true
      case Some(varasijaSaannotAstuvatVoimaan) => varasijaSaannotAstuvatVoimaan.isBefore(new DateTime())
    }
  }

  case class Välitulos(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) {
    def map(f: (List[Hakutoiveentulos], Haku, Option[Ohjausparametrit]) => List[Hakutoiveentulos]) = {
      Välitulos(f(tulokset, haku, ohjausparametrit), haku, ohjausparametrit)
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


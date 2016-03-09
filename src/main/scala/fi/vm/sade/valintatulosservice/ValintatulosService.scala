package fi.vm.sade.valintatulosservice

import java.util
import java.util.Date

import fi.vm.sade.sijoittelu.domain.{ValintatuloksenTila, Valintatulos}
import fi.vm.sade.utils.Timer.timed
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.ohjausparametrit.{Ohjausparametrit, OhjausparametritService}
import fi.vm.sade.valintatulosservice.sijoittelu.SijoittelutulosService
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService}
import org.joda.time.DateTime

import scala.collection.JavaConverters._

private object HakemustenTulosHakuLock

class ValintatulosService(vastaanotettavuusService: VastaanotettavuusService,
                          sijoittelutulosService: SijoittelutulosService,
                          ohjausparametritService: OhjausparametritService,
                          hakemusRepository: HakemusRepository,
                          hakuService: HakuService)(implicit appConfig: AppConfig) extends Logging {
  def this(vastaanotettavuusService: VastaanotettavuusService, sijoittelutulosService: SijoittelutulosService, hakuService: HakuService)(implicit appConfig: AppConfig) = this(vastaanotettavuusService, sijoittelutulosService, appConfig.ohjausparametritService, new HakemusRepository(), hakuService)

  val valintatulosDao = appConfig.sijoitteluContext.valintatulosDao

  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    fetchTulokset(hakuOid, () => hakemusRepository.findHakemus(hakemusOid).iterator, (haku, hakijaOidsByHakemusOids) => sijoittelutulosService.hakemuksenTulos(haku, hakemusOid, hakijaOidsByHakemusOids(hakemusOid)).toSeq).flatMap(_.toSeq.headOption)
  }

  def hakemuksentuloksetByPerson(hakuOid: String, personOid: String): List[Hakemuksentulos] = {
    val hakemukset = hakemusRepository.findHakemukset(hakuOid, personOid).toSeq
    fetchTulokset(hakuOid, () => hakemukset.toIterator, (haku, hakijaOidsByHakemusOids) => hakemukset.flatMap(hakemus => sijoittelutulosService.hakemuksenTulos(haku, hakemus.oid, hakijaOidsByHakemusOids(hakemus.oid))))
      .map(_.toList).getOrElse(List.empty)
  }

  def hakemustenTulosByHaku(hakuOid: String): Option[Iterator[Hakemuksentulos]] = {
    timed("Fetch hakemusten tulos for haku: " + hakuOid, 1000) (
      fetchTulokset(hakuOid, () => hakemusRepository.findHakemukset(hakuOid), (haku,  hakijaOidsByHakemusOids) => sijoittelutulosService.hakemustenTulos(hakuOid, hakijaOidsByHakemusOids = hakijaOidsByHakemusOids))
    )
  }

  def hakemustenTulosByHakukohde(hakuOid: String, hakukohdeOid: String): Option[Iterator[Hakemuksentulos]] = {
    timed("Fetch hakemusten tulos for haku: "+ hakuOid + " and hakukohde: " + hakuOid, 1000) (
      fetchTulokset(hakuOid, () => hakemusRepository.findHakemuksetByHakukohde(hakuOid, hakukohdeOid), (haku, hakijaOidsByHakemusOids) => sijoittelutulosService.hakemustenTulos(hakuOid, Some(hakukohdeOid), hakijaOidsByHakemusOids))
    )
  }
  def findValintaTulokset(hakuOid: String): util.List[Valintatulos] = {
    val hakemustenTulokset = hakemustenTulosByHaku(hakuOid).getOrElse(throw new IllegalArgumentException(s"Unknown hakuOid ${hakuOid}"))
    val valintatulokset = valintatulosDao.loadValintatulokset(hakuOid)

    setValintatuloksetTilat(hakuOid, valintatulokset.asScala, mapHakemustenTuloksetByHakemusOid(hakemustenTulokset))
    valintatulokset
  }

  def findValintaTulokset(hakuOid: String, hakukohdeOid: String): util.List[Valintatulos] = {
    val hakemustenTulokset = hakemustenTulosByHakukohde(hakuOid, hakukohdeOid).getOrElse(throw new IllegalArgumentException(s"Unknown hakuOid ${hakuOid}"))
    val valintatulokset: util.List[Valintatulos] = valintatulosDao.loadValintatuloksetForHakukohde(hakukohdeOid)

    setValintatuloksetTilat(hakuOid, valintatulokset.asScala, mapHakemustenTuloksetByHakemusOid(hakemustenTulokset))
    valintatulokset
  }

  private def mapHakemustenTuloksetByHakemusOid(hakemustenTulokset:Iterator[Hakemuksentulos]):Map[String,Hakemuksentulos] = {
    hakemustenTulokset.toList.groupBy(_.hakemusOid).mapValues(_.head)
  }

  private def getVirkailijanVastaanottoBuffer(hakuOid:String) = {
    ohjausparametritService.ohjausparametrit(hakuOid).flatMap(_.vastaanottoaikataulu).flatMap(_.virkailijanVastaanottoBufferDays)
  }

  private def virkailijanVastaanottoaikaaJäljellä(hakijanVastaanottoDeadline:Option[Date], virkailijanVastaanottoBuffer:Option[Int]) = {
    hakijanVastaanottoDeadline.isDefined && new DateTime(hakijanVastaanottoDeadline.get).plusDays(virkailijanVastaanottoBuffer.getOrElse(0)).isAfterNow
  }

  private def setValintatuloksetTilat(hakuOid:String, valintatulokset: Seq[Valintatulos], hakemustenTulokset: Map[String,Hakemuksentulos] ): Unit = {
    val virkailijanVastaanottoBuffer = getVirkailijanVastaanottoBuffer(hakuOid)
    valintatulokset.foreach(valintaTulos => {
      hakemustenTulokset.get(valintaTulos.getHakemusOid).foreach(hakemuksenTulos => { // TODO is hakemus person + haku specific?
        hakemuksenTulos.findHakutoive(valintaTulos.getHakukohdeOid).foreach(hakutoiveenTulos => {
          val valintatuloksenTila = ValintatuloksenTila.valueOf(hakutoiveenTulos.vastaanottotila.toString)
          if(ValintatuloksenTila.EI_VASTAANOTETTU_MAARA_AIKANA == valintatuloksenTila &&
            virkailijanVastaanottoaikaaJäljellä(hakutoiveenTulos.vastaanottoDeadline, virkailijanVastaanottoBuffer)){
            valintaTulos.setTila(ValintatuloksenTila.KESKEN, "")
          } else {
            valintaTulos.setTila(valintatuloksenTila, "")
          }
        })
      })
    })
  }

  private def fetchTulokset(hakuOid: String, getHakemukset: () => Iterator[Hakemus], getSijoittelunTulos: (Haku, Map[String, String]) => Seq[HakemuksenSijoitteluntulos]): Option[Iterator[Hakemuksentulos]] = {
    for (
      haku <- hakuService.getHaku(hakuOid)
    ) yield {
      val ohjausparametrit = ohjausparametritService.ohjausparametrit(hakuOid)
      val hakemukset = timed("Fetch hakemukset", 1000) { getHakemukset() }.toSeq
      val hakijaOidsByHakemusOids: Map[String, String] = hakemukset.groupBy(_.oid).map(t => (t._1, t._2.head.henkiloOid))
      val sijoitteluTulokset = timed("Fetch sijoittelun tulos", 1000) {
        getSijoittelunTulos(haku, hakijaOidsByHakemusOids).groupBy(_.hakemusOid).mapValues(_.head)
      }
      for (
        hakemus: Hakemus <- hakemukset.toIterator
      ) yield {
        val sijoitteluTulos = sijoitteluTulokset.getOrElse(hakemus.oid, tyhjäHakemuksenTulos(hakemus.oid, ohjausparametrit.flatMap(_.vastaanottoaikataulu)))
        julkaistavaTulos(sijoitteluTulos, haku, ohjausparametrit)(hakemus)
      }
    }
  }

  private def julkaistavaTulos(sijoitteluTulos: HakemuksenSijoitteluntulos, haku: Haku, ohjausparametrit: Option[Ohjausparametrit])(h:Hakemus)(implicit appConfig: AppConfig): Hakemuksentulos = {
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
      .map(asetaVastaanotettavuusValintarekisterinPerusteella(h.henkiloOid))
      .tulokset

    Hakemuksentulos(haku.oid, h.oid, sijoitteluTulos.hakijaOid.getOrElse(h.henkiloOid), ohjausparametrit.flatMap(_.vastaanottoaikataulu), lopullisetTulokset)
  }

  private def tyhjäHakemuksenTulos(hakemusOid: String, aikataulu: Option[Vastaanottoaikataulu]) = HakemuksenSijoitteluntulos(hakemusOid, None, Nil)

  private def asetaVastaanotettavuusValintarekisterinPerusteella(henkiloOid: String)(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    tulokset.map(tulos => {
      val vastaanotettu = List(Vastaanottotila.vastaanottanut, Vastaanottotila.ehdollisesti_vastaanottanut).contains(tulos.vastaanottotila)
      if (!vastaanotettu && vastaanotettavuusService.tarkistaAiemmatVastaanotot(henkiloOid, tulos.hakukohdeOid).isFailure) {
        tulos.copy(vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa,
          vastaanottotila = Vastaanottotila.perunut)
      } else {
        tulos
      }
    })
  }

  private def sovellaKorkeakoulujenVarsinaisenYhteishaunSääntöjä(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    if (haku.korkeakoulu && haku.yhteishaku && haku.varsinainenhaku) {
      val firstVaralla = tulokset.indexWhere(_.valintatila == Valintatila.varalla)
      val firstVastaanotettu = tulokset.indexWhere(_.vastaanottotila == Vastaanottotila.vastaanottanut)
      val firstKesken = tulokset.indexWhere(_.valintatila == Valintatila.kesken)

      tulokset.zipWithIndex.map {
        case (tulos, index) if Valintatila.isHyväksytty(tulos.valintatila) && tulos.vastaanottotila == Vastaanottotila.kesken =>
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
        Valintatila.isHyväksytty(t.valintatila) || List(Valintatila.perunut, Valintatila.peruutettu, Valintatila.peruuntunut).contains(t.valintatila)
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




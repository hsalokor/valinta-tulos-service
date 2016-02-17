package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila._
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakukohdeRecordService, HakijaVastaanottoRepository}

import scala.util.{Success, Failure, Try}

class VastaanottoService(hakuService: HakuService,
                         valintatulosService: ValintatulosService,
                         hakijaVastaanottoRepository: HakijaVastaanottoRepository,
                         hakukohdeRecordService: HakukohdeRecordService,
                         tulokset: ValintatulosRepository) extends Logging{

  val sallitutVastaanottotilat: Set[ValintatuloksenTila] = Set(VASTAANOTTANUT_SITOVASTI, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT)

  def vastaanota(hakuOid: String, vastaanotettavaHakemusOid: String, vastaanotto: Vastaanotto) {
    vastaanota(hakuOid, vastaanotettavaHakemusOid, vastaanotto, tallennaValintatulosmuutos = true)
  }

  def tarkistaVastaanotettavuus(hakuOid: String, vastaanotettavaHakemusOid: String, hakukohdeOid: String) {
    vastaanota(hakuOid, vastaanotettavaHakemusOid, Vastaanotto(hakukohdeOid, Vastaanottotila.vastaanottanut, "", ""), tallennaValintatulosmuutos = false)
  }

  private def vastaanota(hakuOid: String, vastaanotettavaHakemusOid: String, vastaanotto: Vastaanotto, tallennaValintatulosmuutos: Boolean) {
    val haku = hakuService.getHaku(hakuOid).getOrElse(throw new IllegalArgumentException("Hakua ei löydy"))
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, vastaanotettavaHakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    val hakutoive = hakemuksenTulos.findHakutoive(vastaanotto.hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))
    val haluttuTila = ValintatuloksenTila.valueOf(vastaanotto.tila.toString)
    val vastaanotettavaHakuKohdeOid = vastaanotto.hakukohdeOid

    val tarkistettavatHakemukset = korkeakouluYhteishaunVastaanottoonLiittyvienHakujenHakemukset(haku, hakemuksenTulos.hakijaOid, haluttuTila)

    tarkistaEttaEiVastaanottoja(tarkistettavatHakemukset, haluttuTila, hakutoive, vastaanotettavaHakemusOid, vastaanotettavaHakuKohdeOid)

    if (tallennaValintatulosmuutos) {
      tarkistaHakutoiveenJaValintatuloksenTila(hakutoive, haluttuTila)

      tulokset.modifyValintatulos(
        vastaanotto.hakukohdeOid,
        hakutoive.valintatapajonoOid,
        vastaanotettavaHakemusOid
      ) { valintatulos => valintatulos.setTila(haluttuTila, vastaanotto.selite, vastaanotto.muokkaaja) }

      peruMuutHyvaksytyt(tarkistettavatHakemukset, vastaanotto, haku, vastaanotettavaHakemusOid, vastaanotettavaHakuKohdeOid)
    }
  }

  def paatteleVastaanotettavuus(hakuOid: String, hakemusOid: String, hakukohdeOid: String): Vastaanotettavuus = {
    // TODO pitäisikö tässä kohtaa tarkistaa, että haku <-> hakukohde <-> hakemus liittyvät toisiinsa?

    val haku = hakuService.getHaku(hakuOid).getOrElse(throw new IllegalArgumentException(s"Hakua $hakuOid ei löydy"))
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException(s"Hakemusta $hakemusOid ei löydy"))

    val aiemmatVastaanotot = haeAiemmatVastaanotot(hakukohdeOid, hakemuksenTulos.hakijaOid)
    if (aiemmatVastaanotot.nonEmpty) {
      Vastaanotettavuus(Nil, Some(s"Ei voida ottaa vastaan, koska löytyi aiempi vastaanotto: $aiemmatVastaanotot."))
    } else {
      val hakutoiveenTulos = hakemuksenTulos.findHakutoive(hakukohdeOid).getOrElse(
        throw new IllegalStateException(s"Ei löydy kohteen $hakukohdeOid tulosta hakemuksen tuloksesta $hakemuksenTulos"))
      if (!Valintatila.isHyväksytty(hakutoiveenTulos.valintatila)) {
        Vastaanotettavuus(Nil, Some(s"Ei voida ottaa vastaan, koska hakutoiveen valintatila ei ole hyväksytty: ${hakutoiveenTulos.valintatila}"))
      } else {
        val vastaanotettavissaEhdollisesti = valintatulosService.onkoVastaanotettavissaEhdollisesti(hakutoiveenTulos, haku)
        Vastaanotettavuus(List(Peru, VastaanotaSitovasti) ++ (if (vastaanotettavissaEhdollisesti) List(VastaanotaEhdollisesti) else Nil))
      }
    }
  }

  def vastaanotaHakukohde(vastaanottoEvent: VastaanottoEvent): Try[Unit] = {
    val aiemmatVastaanotot = haeAiemmatVastaanotot(vastaanottoEvent.hakukohdeOid, vastaanottoEvent.henkiloOid)
    if (aiemmatVastaanotot.isEmpty) {
      Success(hakijaVastaanottoRepository.store(vastaanottoEvent))
    } else if (aiemmatVastaanotot.size == 1) {
      val aiempiVastaanotto = aiemmatVastaanotot.head
      Failure(PriorAcceptanceException(aiempiVastaanotto, vastaanottoEvent))
    } else {
      Failure(new IllegalStateException(s"Hakijalla ${vastaanottoEvent.henkiloOid} useita vastaanottoja: $aiemmatVastaanotot"))
    }
  }

  private def haeAiemmatVastaanotot(hakukohdeOid: String, hakijaOid: String): Set[VastaanottoRecord] = {
    val HakukohdeRecord(_, hakuOid, yhdenPaikanSaantoVoimassa, _, koulutuksenAlkamiskausi) =
      hakukohdeRecordService.getHakukohdeRecord(hakukohdeOid)
    val aiemmatVastaanotot = if (yhdenPaikanSaantoVoimassa) {
      hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(hakijaOid, koulutuksenAlkamiskausi)
    } else {
      hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(hakijaOid, hakuOid)
    }
    aiemmatVastaanotot.filter(_.action != Peru)
  }

  private def tarkistaHakutoiveenJaValintatuloksenTila(hakutoive: Hakutoiveentulos, haluttuTila: ValintatuloksenTila) {
    if (!sallitutVastaanottotilat.contains(haluttuTila)) {
      throw new IllegalArgumentException("Ei-hyväksytty vastaanottotila: " + haluttuTila)
    }
    if (List(VASTAANOTTANUT_SITOVASTI, PERUNUT).contains(haluttuTila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila + ")")
    }
    if (haluttuTila == EHDOLLISESTI_VASTAANOTTANUT && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila + ")")
    }
  }

  private def korkeakouluYhteishaunVastaanottoonLiittyvienHakujenHakemukset(haku: Haku, personOid: String, tila: ValintatuloksenTila): Set[Hakemuksentulos] =
    if (haku.korkeakoulu && haku.yhteishaku && (haku.varsinainenhaku || haku.lisähaku) && List(VASTAANOTTANUT_SITOVASTI, EHDOLLISESTI_VASTAANOTTANUT).contains(tila)) {
      (Set(haku.oid) ++ hakuService.findLiittyvatHaut(haku)).flatMap(valintatulosService.hakemuksentuloksetByPerson(_, personOid))
    } else {
      Set()
    }

  private def muutKuinHakutoive(tulos: Hakemuksentulos, vastaanotettavaHakemusOid: String, vastaanotettavaHakuKohdeOid: String): List[Hakutoiveentulos] = {
    tulos.hakutoiveet.filter(toive => !(tulos.hakemusOid == vastaanotettavaHakemusOid && toive.hakukohdeOid == vastaanotettavaHakuKohdeOid))
  }

  private def tarkistaEttaEiVastaanottoja(muutHakemukset: Set[Hakemuksentulos], tila: ValintatuloksenTila, hakutoive: Hakutoiveentulos, vastaanotettavaHakemusOid: String, vastaanotettavaHakuKohdeOid: String) {
    muutHakemukset.foreach(tulos => {
      val hakemuksenMuutHakuToiveet: List[Hakutoiveentulos] = muutKuinHakutoive(tulos, vastaanotettavaHakemusOid, vastaanotettavaHakuKohdeOid)
      val vastaanotettu = hakemuksenMuutHakuToiveet.find(toive => List(Vastaanottotila.vastaanottanut, Vastaanottotila.ehdollisesti_vastaanottanut).contains(toive.vastaanottotila))
      throw new UnsupportedOperationException("Deprecated vastaanotto functionality")
    })
  }

  private def peruMuutHyvaksytyt(muutHakemukset: Set[Hakemuksentulos], vastaanotto: Vastaanotto, vastaanotonHaku: Haku, vastaanotettavaHakemusOid: String, vastaanotettavaHakuKohdeOid: String) {
    muutHakemukset.foreach(hakemus => {
      val peruttavatHakutoiveenTulokset = muutKuinHakutoive(hakemus, vastaanotettavaHakemusOid, vastaanotettavaHakuKohdeOid)
        .filter(toive => Valintatila.isHyväksytty(toive.valintatila) && toive.vastaanottotila == Vastaanottotila.kesken)
      peruttavatHakutoiveenTulokset.foreach(kohde =>
        tulokset.modifyValintatulos(
          kohde.hakukohdeOid,
          kohde.valintatapajonoOid,
          hakemus.hakemusOid
        ) { valintatulos => valintatulos.setTila(
          ValintatuloksenTila.PERUNUT,
          vastaanotto.tila + " paikan " + vastaanotto.hakukohdeOid + " toisesta hausta " + vastaanotonHaku.oid,
          vastaanotto.muokkaaja)
        }
      )
    })
  }
}

case class PriorAcceptanceException(aiempiVastaanotto: VastaanottoRecord, yritettyVastaanotto: VastaanottoEvent)
  extends IllegalArgumentException(s"Löytyi aiempi vastaanotto $aiempiVastaanotto yrittäessä vastaanottoa $yritettyVastaanotto")

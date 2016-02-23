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
                         vastaanotettavuusService: VastaanotettavuusService,
                         valintatulosService: ValintatulosService,
                         hakijaVastaanottoRepository: HakijaVastaanottoRepository,
                         hakukohdeRecordService: HakukohdeRecordService,
                         tulokset: ValintatulosRepository) extends Logging{

  val sallitutVastaanottotilat: Set[ValintatuloksenTila] = Set(VASTAANOTTANUT_SITOVASTI, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT)

  def vastaanota(vastaanotettavaHakemusOid: String, vastaanotto: Vastaanotto) {
    vastaanota(vastaanotettavaHakemusOid, vastaanotto, tallennaValintatulosmuutos = true)
  }

  def tarkistaVastaanotettavuus(vastaanotettavaHakemusOid: String, hakukohdeOid: String) {
    vastaanota(vastaanotettavaHakemusOid, Vastaanotto(hakukohdeOid, Vastaanottotila.vastaanottanut, "", ""), tallennaValintatulosmuutos = false)
  }

  private def vastaanota(vastaanotettavaHakemusOid: String, vastaanotto: Vastaanotto, tallennaValintatulosmuutos: Boolean) {
    val hakuOid = hakuService.getHakukohde(vastaanotto.hakukohdeOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon hakukohde ${vastaanotto.hakukohdeOid}")).hakuOid
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, vastaanotettavaHakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    val hakutoive = hakemuksenTulos.findHakutoive(vastaanotto.hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))
    val haluttuTila = ValintatuloksenTila.valueOf(vastaanotto.tila.toString)
    val vastaanotettavaHakuKohdeOid = vastaanotto.hakukohdeOid

    if (tallennaValintatulosmuutos) {
      tarkistaHakutoiveenJaValintatuloksenTila(hakutoive, haluttuTila)

      hakijaVastaanottoRepository.store(
        VastaanottoEvent(hakemuksenTulos.hakijaOid, vastaanotettavaHakemusOid, vastaanotettavaHakuKohdeOid, haluttuTila match {
          case ValintatuloksenTila.PERUNUT => Peru
          case ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI => VastaanotaSitovasti
          case ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT => VastaanotaEhdollisesti
          case _ => throw new IllegalArgumentException("Ei-hyväksytty vastaanottotila: " + haluttuTila)
      }))
      tulokset.modifyValintatulos(
        vastaanotto.hakukohdeOid,
        hakutoive.valintatapajonoOid,
        vastaanotettavaHakemusOid
      ) { valintatulos => valintatulos.setTila(haluttuTila, vastaanotto.selite, vastaanotto.muokkaaja) }
    }
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
}

case class PriorAcceptanceException(aiempiVastaanotto: VastaanottoRecord)
  extends IllegalArgumentException(s"Löytyi aiempi vastaanotto $aiempiVastaanotto")

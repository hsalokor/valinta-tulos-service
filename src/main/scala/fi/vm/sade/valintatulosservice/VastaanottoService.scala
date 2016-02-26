package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.{ehdollisesti_vastaanottanut, vastaanottanut}
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.valintarekisteri.{VastaanottoEvent, VastaanottoRecord, HakijaVastaanottoRepository}

class VastaanottoService(hakuService: HakuService,
                         vastaanotettavuusService: VastaanotettavuusService,
                         valintatulosService: ValintatulosService,
                         hakijaVastaanottoRepository: HakijaVastaanottoRepository,
                         valintatulosRepository: ValintatulosRepository) extends Logging{

  def virkailijanVastaanota(vastaanotot: List[VirkailijanVastaanotto]): List[VastaanottoResult] = {
    for(vastaanotto <- vastaanotot) yield {
      try {
        val ( hakemuksenTulos, hakutoive ) = checkVastaanotettavuus(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)

        if(vastaanotto.action == VastaanotaEhdollisesti || vastaanotto.action == VastaanotaSitovasti ) {
          vastaanotettavuusService.tarkistaAiemmatVastaanotot(vastaanotto.henkiloOid, vastaanotto.hakukohdeOid).get
        }

        hakijaVastaanottoRepository.store(vastaanotto)
        createVastaanottoResult(200, None, vastaanotto)

      } catch {
        case e:PriorAcceptanceException => createVastaanottoResult( 403, Some(e), vastaanotto)
        case e:Exception => createVastaanottoResult( 400, Some(e), vastaanotto)
      }
    }
  }

  private def createVastaanottoResult(statusCode: Int, exception: Option[Exception], vastaanottoEvent: VastaanottoEvent) = {
    VastaanottoResult(vastaanottoEvent.henkiloOid, vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid, Result(statusCode, (exception.map(_.getMessage))))
  }

  def tarkistaVastaanotettavuus(vastaanotettavaHakemusOid: String, hakukohdeOid: String): Unit = {
    checkVastaanotettavuus(vastaanotettavaHakemusOid, hakukohdeOid)
  }

  @Deprecated
  def vastaanota(hakemusOid: String, vastaanotto: Vastaanotto): Unit = {
    vastaanota(HakijanVastaanotto(
      vastaanotto.muokkaaja,
      hakemusOid,
      vastaanotto.hakukohdeOid,
      HakijanVastaanottoAction.getHakijanVastaanottoAction(vastaanotto.tila)
    ))
  }

  def vastaanota(vastaanotto: HakijanVastaanotto) {
    val ( hakemuksenTulos, hakutoive ) = checkVastaanotettavuus(vastaanotto.hakemusOid, vastaanotto.hakukohdeOid)

    tarkistaHakutoiveenJaValintatuloksenTila(hakutoive, vastaanotto.action)

    hakijaVastaanottoRepository.store(vastaanotto)
    valintatulosRepository.modifyValintatulos(
      vastaanotto.hakukohdeOid,
      hakemuksenTulos.findHakutoive(vastaanotto.hakukohdeOid).get.valintatapajonoOid,
      vastaanotto.hakemusOid){valintatulos => valintatulos.setTila(ValintatuloksenTila.valueOf(vastaanotto.action.vastaanottotila.toString), "Hakijan tekemä vastaanotto", vastaanotto.ilmoittaja)}
  }

  private def checkVastaanotettavuus(hakemusOid: String, hakukohdeOid: String): (Hakemuksentulos, Hakutoiveentulos) = {
    val hakuOid = hakuService.getHakukohde(hakukohdeOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon hakukohde ${hakukohdeOid}")).hakuOid
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    val hakutoive = hakemuksenTulos.findHakutoive(hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))
    ( hakemuksenTulos, hakutoive )
  }

  private def tarkistaHakutoiveenJaValintatuloksenTila(hakutoive: Hakutoiveentulos, haluttuTila: HakijanVastaanottoAction) {
    if (List(Peru, VastaanotaSitovasti).contains(haluttuTila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila.vastaanottotila + ")")
    }
    if (haluttuTila == VastaanotaEhdollisesti && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + haluttuTila.vastaanottotila + ")")
    }
  }
}

case class PriorAcceptanceException(aiempiVastaanotto: VastaanottoRecord)
  extends IllegalArgumentException(s"Löytyi aiempi vastaanotto $aiempiVastaanotto")

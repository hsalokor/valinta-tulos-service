package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila._
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.{ehdollisesti_vastaanottanut, vastaanottanut}
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.valintarekisteri.HakijaVastaanottoRepository

class VastaanottoService(hakuService: HakuService,
                         vastaanotettavuusService: VastaanotettavuusService,
                         valintatulosService: ValintatulosService,
                         hakijaVastaanottoRepository: HakijaVastaanottoRepository,
                         valintatulosRepository: ValintatulosRepository) extends Logging{
  val sallitutVastaanottotilat: Set[ValintatuloksenTila] = Set(VASTAANOTTANUT_SITOVASTI, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT)

  def virkailijanVastaanota(vastaanottoEntries: List[VirkailijanVastaanottoEntry]): List[VastaanottoResult] = {
    for(vastaanottoEntry <- vastaanottoEntries) yield {
      try {
        val ( hakemuksenTulos, hakutoive ) = checkVastaanotettavuus(vastaanottoEntry.hakemusOid, vastaanottoEntry.hakukohdeOid)

        if (List(vastaanottanut, ehdollisesti_vastaanottanut).contains(vastaanottoEntry.newState)) {
          vastaanotettavuusService.tarkistaAiemmatVastaanotot(vastaanottoEntry.hakijaOid, vastaanottoEntry.hakukohdeOid).get
        }

        val vastaanottoEvent = vastaanottoEntry.toEvent
        tallennaVastaanotto(vastaanottoEvent, hakutoive,
          Vastaanotto(
            vastaanottoEntry.hakukohdeOid,
            vastaanottoEntry.newState,
            vastaanottoEntry.ilmoittaja,
            "Virkailijan tekemä vastaanotto"))

        createVastaanottoResult(200, None, vastaanottoEvent)

      } catch {
        case e:PriorAcceptanceException => createVastaanottoResult( 403, Some(e), vastaanottoEntry.toEvent)
        case e:Exception => createVastaanottoResult( 400, Some(e), vastaanottoEntry.toEvent)
      }
    }
  }

  private def createVastaanottoResult(statusCode: Int, exception: Option[Exception], vastaanottoEvent: VastaanottoEvent) = {
    VastaanottoResult(vastaanottoEvent.henkiloOid, vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid, Result(statusCode, (exception.map(_.getMessage))))
  }

  def tarkistaVastaanotettavuus(vastaanotettavaHakemusOid: String, hakukohdeOid: String): Unit = {
    checkVastaanotettavuus(vastaanotettavaHakemusOid, hakukohdeOid)
  }

  def vastaanota(vastaanotettavaHakemusOid: String, vastaanotto: Vastaanotto) {
    val ( hakemuksenTulos, hakutoive ) = checkVastaanotettavuus(vastaanotettavaHakemusOid, vastaanotto.hakukohdeOid)
    val haluttuTila = ValintatuloksenTila.valueOf(vastaanotto.tila.toString)

    tarkistaHakutoiveenJaValintatuloksenTila(hakutoive, haluttuTila)

    tallennaVastaanotto(
      VastaanottoEvent(hakemuksenTulos.hakijaOid, vastaanotettavaHakemusOid, vastaanotto.hakukohdeOid, getHaluttuTila(haluttuTila), hakemuksenTulos.hakijaOid),
      hakutoive,
      vastaanotto
    )
  }

  def getHaluttuTila(haluttuTila: ValintatuloksenTila) = haluttuTila match {
    case ValintatuloksenTila.PERUNUT => Peru
    case VASTAANOTTANUT_SITOVASTI => VastaanotaSitovasti
    case EHDOLLISESTI_VASTAANOTTANUT => VastaanotaEhdollisesti
    case _ => throw new IllegalArgumentException("Ei-hyväksytty vastaanottotila: " + haluttuTila)
  }

  private def checkVastaanotettavuus(hakemusOid: String, hakukohdeOid: String): (Hakemuksentulos, Hakutoiveentulos) = {
    val hakuOid = hakuService.getHakukohde(hakukohdeOid).getOrElse(throw new IllegalArgumentException(s"Tuntematon hakukohde ${hakukohdeOid}")).hakuOid
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    val hakutoive = hakemuksenTulos.findHakutoive(hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))
    ( hakemuksenTulos, hakutoive )
  }

  private def tallennaVastaanotto(vastaanottoEvent: VastaanottoEvent, hakutoive: Hakutoiveentulos, vastaanotto: Vastaanotto) = {

    hakijaVastaanottoRepository.store(vastaanottoEvent)

    valintatulosRepository.modifyValintatulos(
      vastaanottoEvent.hakukohdeOid,
      hakutoive.valintatapajonoOid,
      vastaanottoEvent.hakemusOid
    ) {
      valintatulos => valintatulos.setTila(ValintatuloksenTila.valueOf(vastaanotto.tila.toString), vastaanotto.selite, vastaanotto.muokkaaja)
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

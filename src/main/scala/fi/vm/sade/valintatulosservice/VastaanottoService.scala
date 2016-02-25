package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila._
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.utils.slf4j.Logging
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

  def virkailijanVastaanota(vastaanottoEvents: List[VastaanottoEvent]): List[VastaanottoResult] = {
    for(vastaanottoEvent <- vastaanottoEvents) yield {
      try {
        val ( hakemuksenTulos, hakutoive ) = checkVastaanotettavuus(vastaanottoEvent.hakemusOid, vastaanottoEvent.hakukohdeOid)

        if(vastaanottoEvent.action == VastaanotaEhdollisesti || vastaanottoEvent.action == VastaanotaSitovasti ) {
          vastaanotettavuusService.tarkistaAiemmatVastaanotot(vastaanottoEvent.henkiloOid, vastaanottoEvent.hakukohdeOid).get
        }

        tallennaVastaanotto(vastaanottoEvent, hakutoive,
          Vastaanotto(
            vastaanottoEvent.hakukohdeOid,
            vastaanottoEvent.action.vastaanottotila,
            vastaanottoEvent.ilmoittaja,
            "Virkailijan tekemä vastaanotto"))

        createVastaanottoResult(200, None, vastaanottoEvent)

      } catch {
        case e:PriorAcceptanceException => createVastaanottoResult( 403, Some(e), vastaanottoEvent)
        case e:Exception => createVastaanottoResult( 400, Some(e), vastaanottoEvent)
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
    case ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI => VastaanotaSitovasti
    case ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT => VastaanotaEhdollisesti
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

package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila._
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.valintatulosservice.domain.{Vastaanottotila, Hakutoiveentulos, Vastaanotettavuustila, Vastaanotto}
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService}

class VastaanottoService(hakuService: HakuService, valintatulosService: ValintatulosService, tulokset: ValintatulosRepository) {

  def vastaanota(hakuOid: String, hakemusOid: String, vastaanotto: Vastaanotto) {
    val haku = hakuService.getHaku(hakuOid).getOrElse(throw new IllegalArgumentException("Hakua ei löydy"))
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    val hakutoive = hakemuksenTulos.findHakutoive(vastaanotto.hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))

    val tila: ValintatuloksenTila = ValintatuloksenTila.valueOf(vastaanotto.tila.toString)
    tarkistaVastaanotettavuus(hakutoive, tila)
    tarkistaLiittyvatHaut(haku, hakemuksenTulos.hakijaOid, tila, hakutoive)
    tulokset.modifyValintatulos(vastaanotto.hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid, tila.name, vastaanotto.muokkaaja, vastaanotto.selite) { valintatulos => {
        valintatulos.setTila(vastaanotaSitovastiJosKorkeakouluYhteishaku(haku, tila))
      }
    }
  }

  private def tarkistaVastaanotettavuus(hakutoive: Hakutoiveentulos, tila: ValintatuloksenTila) {
    if (!List(VASTAANOTTANUT, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT).contains(tila)) {
      throw new IllegalArgumentException("Ei-hyväksytty vastaanottotila: " + tila)
    }
    if (List(VASTAANOTTANUT, PERUNUT).contains(tila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + tila + ")")
    }
    if (tila == EHDOLLISESTI_VASTAANOTTANUT && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila kohteella " + hakutoive.hakukohdeOid + ": " + hakutoive.vastaanotettavuustila.toString + " (yritetty muutos: " + tila + ")")
    }
  }

  private def tarkistaLiittyvatHaut(haku: Haku, personOid: String, tila: ValintatuloksenTila, hakutoive: Hakutoiveentulos) {
    if(haku.korkeakoulu && haku.yhteishaku && List(VASTAANOTTANUT, EHDOLLISESTI_VASTAANOTTANUT).contains(tila)) {
      val liittyvatHaut = hakuService.findLiittyvatHaut(haku)
      liittyvatHaut.flatMap(valintatulosService.hakemuksentuloksetByPerson(_, personOid)).map { tulos =>
        val vastaanotettu = tulos.hakutoiveet.find(toive => List(Vastaanottotila.vastaanottanut, Vastaanottotila.ehdollisesti_vastaanottanut).contains(toive.vastaanottotila))
        if(vastaanotettu.isDefined) {
          throw new IllegalArgumentException("Väärä vastaanottotila toisen haun " + tulos.hakuOid + " kohteella " + vastaanotettu.get.hakukohdeOid + ": " + vastaanotettu.get.vastaanottotila + " (yritetty muutos: " + tila + " " + hakutoive.hakukohdeOid + ")")
        }
      }
    }
  }

  private def vastaanotaSitovastiJosKorkeakouluYhteishaku(haku: Haku, tila: ValintatuloksenTila): ValintatuloksenTila = {
    if(tila == VASTAANOTTANUT && haku.korkeakoulu && haku.yhteishaku) {
      ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI
    }
    else {
      tila
    }
  }
}
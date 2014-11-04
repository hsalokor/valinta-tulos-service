package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import scala.collection.JavaConversions._

class SijoittelutulosService(yhteenvetoService: YhteenvetoService) {
  def hakemuksenTulos(haku: Haku, hakemusOid: String): Option[HakemuksenSijoitteluntulos] = {
    yhteenvetoService.hakemuksenYhteenveto(haku, hakemusOid).map(yhteenveto2Tulos)
  }

  def hakemustenTulos(haku: Haku): List[HakemuksenSijoitteluntulos] = {
    (for (
      tulokset <- yhteenvetoService.hakemustenYhteenveto(haku)
    ) yield for (
        yhteenveto <- tulokset
      ) yield yhteenveto2Tulos(yhteenveto)).getOrElse(List())
  }

  private def yhteenveto2Tulos(hakemuksenYhteenveto: HakemuksenYhteenveto) = {
    val hakija = hakemuksenYhteenveto.hakija
    val aikataulu = hakemuksenYhteenveto.aikataulu
    HakemuksenSijoitteluntulos(hakija.getHakemusOid, hakija.getHakijaOid, hakemuksenYhteenveto.hakutoiveet.map { hakutoiveenYhteenveto =>
      HakutoiveenSijoitteluntulos(
        hakutoiveenYhteenveto.hakutoive.getHakukohdeOid,
        hakutoiveenYhteenveto.hakutoive.getTarjoajaOid,
        hakutoiveenYhteenveto.valintatapajono.getValintatapajonoOid,
        hakutoiveenYhteenveto.valintatila,
        hakutoiveenYhteenveto.vastaanottotila,
        Ilmoittautumistila.withName(Option(hakutoiveenYhteenveto.valintatapajono.getIlmoittautumisTila).getOrElse(IlmoittautumisTila.EI_TEHTY).name()),
        hakutoiveenYhteenveto.vastaanotettavuustila,
        Option(hakutoiveenYhteenveto.viimeisinValintatuloksenMuutos.orNull),
        Option(hakutoiveenYhteenveto.valintatapajono.getJonosija).map(_.toInt),
        Option(hakutoiveenYhteenveto.valintatapajono.getVarasijojaKaytetaanAlkaen),
        Option(hakutoiveenYhteenveto.valintatapajono.getVarasijojaTaytetaanAsti),
        Option(hakutoiveenYhteenveto.valintatapajono.getVarasijanNumero).map(_.toInt),
        hakutoiveenYhteenveto.julkaistavissa,
        hakutoiveenYhteenveto.valintatapajono.getTilanKuvaukset.toMap,
        hakutoiveenYhteenveto.pisteet
      )
    })
  }
}

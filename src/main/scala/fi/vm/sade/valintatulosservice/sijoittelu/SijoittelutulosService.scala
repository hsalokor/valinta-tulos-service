package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import scala.collection.JavaConversions._

class SijoittelutulosService(yhteenvetoService: YhteenvetoService) {
  def hakemuksenTulos(haku: Haku, hakemusOid: String): Option[Hakemuksentulos] = {
    yhteenvetoService.hakemuksenYhteenveto(haku, hakemusOid).map { hakemuksenYhteenveto =>
      val hakija = hakemuksenYhteenveto.hakija
      val aikataulu = hakemuksenYhteenveto.aikataulu
      new Hakemuksentulos(hakija.getHakemusOid, hakija.getHakijaOid(), aikataulu, hakemuksenYhteenveto.hakutoiveet.map { hakutoiveenYhteenveto =>
        new Hakutoiveentulos(
          hakutoiveenYhteenveto.hakutoive.getHakukohdeOid(),
          hakutoiveenYhteenveto.hakutoive.getTarjoajaOid(),
          hakutoiveenYhteenveto.valintatapajono.getValintatapajonoOid(),
          hakutoiveenYhteenveto.valintatila,
          hakutoiveenYhteenveto.vastaanottotila,
          Ilmoittautumistila.withName(Option(hakutoiveenYhteenveto.valintatapajono.getIlmoittautumisTila()).getOrElse(IlmoittautumisTila.EI_TEHTY).name()),
          hakutoiveenYhteenveto.vastaanotettavuustila,
          Option(hakutoiveenYhteenveto.viimeisinValintatuloksenMuutos.getOrElse(null)),
          Option(hakutoiveenYhteenveto.valintatapajono.getJonosija()).map(_.toInt),
          Option(hakutoiveenYhteenveto.valintatapajono.getVarasijojaKaytetaanAlkaen()),
          Option(hakutoiveenYhteenveto.valintatapajono.getVarasijojaTaytetaanAsti()),
          Option(hakutoiveenYhteenveto.valintatapajono.getVarasijanNumero()).map(_.toInt),
          hakutoiveenYhteenveto.julkaistavissa,
          hakutoiveenYhteenveto.valintatapajono.getTilanKuvaukset.toMap
        )
      })
    }
  }
}

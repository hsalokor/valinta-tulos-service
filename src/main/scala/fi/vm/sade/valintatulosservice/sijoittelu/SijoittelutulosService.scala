package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.sijoittelu.tulos.dto.IlmoittautumisTila
import fi.vm.sade.valintatulosservice.domain._
import scala.collection.JavaConversions._

class SijoittelutulosService(yhteenvetoService: YhteenvetoService) {
  def hakemuksenTulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    yhteenvetoService.hakemuksenYhteenveto(hakuOid, hakemusOid).map { hakemuksenYhteenveto =>
      val hakija = hakemuksenYhteenveto.hakija
      val aikataulu = hakemuksenYhteenveto.aikataulu
      new Hakemuksentulos(hakija.getHakemusOid, aikataulu, hakemuksenYhteenveto.hakutoiveet.map { hakutoiveenYhteenveto =>
        new Hakutoiveentulos(
          hakutoiveenYhteenveto.hakutoive.getHakukohdeOid(),
          hakutoiveenYhteenveto.hakutoive.getTarjoajaOid(),
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

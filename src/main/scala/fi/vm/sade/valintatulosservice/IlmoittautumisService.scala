package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.{VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI}
import fi.vm.sade.valintatulosservice.domain.Ilmoittautuminen
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import org.json4s.jackson.Serialization

class IlmoittautumisService(valintatulosService: ValintatulosService, tulokset: ValintatulosRepository) extends JsonFormats {
  def ilmoittaudu(hakuOid: String, hakemusOid: String, ilmoittautuminen: Ilmoittautuminen) {
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    val hakutoive = hakemuksenTulos.findHakutoive(ilmoittautuminen.hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))

    if(!hakutoive.ilmoittautumistila.ilmoittauduttavissa)  {
      throw new IllegalArgumentException(s"""Hakutoive ${ilmoittautuminen.hakukohdeOid} ei ole ilmoittauduttavissa: ilmoittautumisaika: ${Serialization.write(hakutoive.ilmoittautumistila.ilmoittautumisaika)}, ilmoittautumistila: ${hakutoive.ilmoittautumistila.ilmoittautumistila}, valintatila: ${hakutoive.valintatila}, vastaanottotila: ${hakutoive.vastaanottotila}""")
    }
    val sopivatTilat = Array(VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI)

    tulokset.modifyValintatulos(ilmoittautuminen.hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid, ilmoittautuminen.tila.toString, ilmoittautuminen.muokkaaja, ilmoittautuminen.selite) { valintatulos =>
      if(!sopivatTilat.contains(valintatulos.getTila)) {
        throw new IllegalArgumentException(s"""Valintatulokselle (kohde: ${hakutoive.hakukohdeOid}, jono: ${hakutoive.valintatapajonoOid}, hakemus: ${hakemusOid}), jonka vastaanottotila on ${valintatulos.getTila} ei voi tallentaa ilmoittautumistietoa""")
      }
      valintatulos.setIlmoittautumisTila(IlmoittautumisTila.valueOf(ilmoittautuminen.tila.toString))
    }
  }
}
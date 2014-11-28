package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.{VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI}
import fi.vm.sade.valintatulosservice.domain.Ilmoittautuminen
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import org.json4s.jackson.Serialization

class IlmoittautumisService(valintatulosService: ValintatulosService, tulokset: ValintatulosRepository) extends JsonFormats {
  def ilmoittaudu(hakuOid: String, hakemusOid: String, ilmoittautuminen: Ilmoittautuminen) {
    val hakutoive = valintatulosService.hakutoive(hakuOid, hakemusOid, ilmoittautuminen.hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakemusta tai hakutoivetta ei löydy"))

    if(!hakutoive.ilmoittautumistila.ilmoittauduttavissa)  {
      throw new IllegalStateException(s"""Hakutoive ei ole ilmottauduttavissa: ${Serialization.write(hakutoive.ilmoittautumistila)}""")
    }
    val sopivatTilat = Array(VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI)

    tulokset.modifyValintatulos(ilmoittautuminen.hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid, ilmoittautuminen.tila.toString, ilmoittautuminen.muokkaaja, ilmoittautuminen.selite) { valintatulos =>
      if(!sopivatTilat.contains(valintatulos.getTila)) {
        throw new IllegalArgumentException(s"""Valintatulokselle, jonka tila on ${valintatulos.getTila} ei voi tallentaa ilmoittautumistietoa""")
      }
      valintatulos.setIlmoittautumisTila(IlmoittautumisTila.valueOf(ilmoittautuminen.tila.toString))
    }
  }
}
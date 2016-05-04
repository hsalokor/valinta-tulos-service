package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila
import fi.vm.sade.valintatulosservice.domain.{Ilmoittautuminen, VastaanotaSitovasti}
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.sijoittelu.ValintatulosRepository
import fi.vm.sade.valintatulosservice.valintarekisteri.HakijaVastaanottoRepository
import org.json4s.jackson.Serialization

class IlmoittautumisService(valintatulosService: ValintatulosService,
                            tulokset: ValintatulosRepository,
                            hakijaVastaanottoRepository: HakijaVastaanottoRepository) extends JsonFormats {
  def ilmoittaudu(hakuOid: String, hakemusOid: String, ilmoittautuminen: Ilmoittautuminen) {
    val hakemuksenTulos = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).getOrElse(throw new IllegalArgumentException("Hakemusta ei löydy"))
    val hakutoive = hakemuksenTulos.findHakutoive(ilmoittautuminen.hakukohdeOid).getOrElse(throw new IllegalArgumentException("Hakutoivetta ei löydy"))

    if (!hakutoive.ilmoittautumistila.ilmoittauduttavissa)  {
      throw new IllegalStateException(s"""Hakutoive ${ilmoittautuminen.hakukohdeOid} ei ole ilmoittauduttavissa: ilmoittautumisaika: ${Serialization.write(hakutoive.ilmoittautumistila.ilmoittautumisaika)}, ilmoittautumistila: ${hakutoive.ilmoittautumistila.ilmoittautumistila}, valintatila: ${hakutoive.valintatila}, vastaanottotila: ${hakutoive.vastaanottotila}""")
    }

    if (!hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(hakemuksenTulos.hakijaOid, hakuOid).exists(vastaanotto => {
      vastaanotto.action == VastaanotaSitovasti && vastaanotto.hakukohdeOid == ilmoittautuminen.hakukohdeOid
    })) {
      throw new IllegalStateException(s"Hakija ${hakemuksenTulos.hakijaOid} ei voi ilmoittautua hakukohteeseen ${hakutoive.hakukohdeOid} koska sitovaa vastaanottoa ei löydy.")
    }

    tulokset.modifyValintatulos(ilmoittautuminen.hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid, (Unit) => throw new IllegalArgumentException("Valintatulosta ei löydy")) { valintatulos =>
      valintatulos.setIlmoittautumisTila(IlmoittautumisTila.valueOf(ilmoittautuminen.tila.toString), ilmoittautuminen.selite, ilmoittautuminen.muokkaaja)
    }
  }
}

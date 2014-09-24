package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService

case class SijoitteluClient(raportointiService: RaportointiService, valintatulosDao: ValintatulosDao) {
  def yhteenveto(hakuOid: String, hakemusOid: String): Option[HakemusYhteenvetoDTO] = {
    optionalToOption(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).flatMap { sijoitteluAjo =>
      Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid)).map { hakijaDto => YhteenvetoService.yhteenveto(hakijaDto)}
    }
  }

  def sijoittelunTulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    yhteenveto(hakuOid, hakemusOid).map { yhteenveto =>
      Hakemuksentulos(hakemusOid, yhteenveto.hakutoiveet.toList.map { hakutoiveDto =>
        if (hakutoiveDto.julkaistavissa) {
          Hakutoiveentulos(hakutoiveDto.hakukohdeOid,
            hakutoiveDto.tarjoajaOid,
            hakutoiveDto.valintatila,
            hakutoiveDto.vastaanottotila,
            hakutoiveDto.ilmoittautumistila,
            hakutoiveDto.vastaanotettavuustila,
            hakutoiveDto.viimeisinVastaanottotilanMuutos,
            hakutoiveDto.jonosija,
            hakutoiveDto.varasijojaKaytetaanAlkaen,
            hakutoiveDto.varasijojaTaytetaanAsti,
            hakutoiveDto.varasijanumero
          )
        } else {
          Hakutoiveentulos(hakutoiveDto.hakukohdeOid,
            hakutoiveDto.tarjoajaOid,
            Valintatila.kesken,
            hakutoiveDto.vastaanottotila,
            hakutoiveDto.ilmoittautumistila,
            Vastaanotettavuustila.ei_vastaanotettavissa,
            None,
            None,
            hakutoiveDto.varasijojaKaytetaanAlkaen,
            hakutoiveDto.varasijojaTaytetaanAsti,
            None
          )
        }
      })
    }
  }

  def optionalToOption[T](option: Optional[T]) = if (option.isPresent) {
    Some(option.get)
  } else {
    None
  }
}

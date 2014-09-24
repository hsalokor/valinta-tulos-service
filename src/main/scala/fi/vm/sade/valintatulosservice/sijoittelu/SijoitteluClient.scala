package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService

case class SijoitteluClient(raportointiService: RaportointiService, valintatulosDao: ValintatulosDao) {
  def yhteenveto(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    optionalToOption(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).flatMap { sijoitteluAjo =>
      Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid)).map { hakijaDto => YhteenvetoService.yhteenveto(hakijaDto)}
    }
  }

  def sijoittelunTulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    yhteenveto(hakuOid, hakemusOid).map { yhteenveto =>
      Hakemuksentulos(hakemusOid, yhteenveto.hakutoiveet.toList.map(_.julkaistavaVersio))
    }
  }

  def optionalToOption[T](option: Optional[T]) = if (option.isPresent) {
    Some(option.get)
  } else {
    None
  }
}

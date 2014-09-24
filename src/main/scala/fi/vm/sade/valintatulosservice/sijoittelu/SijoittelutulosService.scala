package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService

class SijoittelutulosService(raportointiService: RaportointiService, valintatulosDao: ValintatulosDao) {
  import Java8Conversions._
  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    fromOptional(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).flatMap { sijoitteluAjo =>
      Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid)).map { hakijaDto => YhteenvetoService.yhteenveto(hakijaDto)}
    }
  }
}

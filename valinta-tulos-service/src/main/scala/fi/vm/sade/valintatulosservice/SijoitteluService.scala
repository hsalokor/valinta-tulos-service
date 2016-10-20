package fi.vm.sade.valintatulosservice


import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.SijoitteluRepository

class SijoitteluService(sijoitteluRepository:SijoitteluRepository) extends Logging {

  def luoSijoitteluajo(sijoitteluajo:SijoitteluAjo) = sijoitteluRepository.storeSijoitteluajo(sijoitteluajo)

}

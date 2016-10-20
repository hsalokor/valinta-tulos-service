package fi.vm.sade.valintatulosservice


import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.SijoitteluRepository
import fi.vm.sade.valintatulosservice.domain.Sijoitteluajo

class SijoitteluService(sijoitteluRepository:SijoitteluRepository) extends Logging {

  def luoSijoitteluajo(sijoitteluajo:Sijoitteluajo) = sijoitteluRepository.storeSijoitteluAjo(sijoitteluajo)

//  def getHakemusBySijoitteluajo(hakuOid:String, sijoitteluajoOid:String, hakemusOid:String) = sijoitteluRepository.getHakemusBySijoitteluajo(hakuOid, sijoitteluajoOid, hakemusOid)
}

package fi.vm.sade.valintatulosservice

import java.util

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.valintarekisteri.SijoitteluRepository

class SijoitteluService(sijoitteluRepository:SijoitteluRepository) extends Logging {

  def luoSijoitteluajo(sijoitteluajo:SijoitteluAjo) = sijoitteluRepository.storeSijoitteluajo(sijoitteluajo)

  def getHakemusBySijoitteluajo(hakuOid:String, sijoitteluajoId:Int, hakemusOid:String): HakijaDTO = {
    val hakija = sijoitteluRepository.getHakija(hakemusOid, sijoitteluajoId)
    val hakutoiveet = sijoitteluRepository.getHakutoiveet(hakemusOid, sijoitteluajoId)
    val jonosijaIds = hakutoiveet.map(h => h.jonosijaId)
    val pistetiedot = sijoitteluRepository.getPistetiedot(jonosijaIds)

    val hakijaDTO = Sijoittelu.hakijaRecordToDTO(hakija)

    val hakutoiveetDTOs = hakutoiveet.map(h => {
      val hakutoiveenPistetiedot = pistetiedot.filter(p => {
        p.jonosijaId == h.jonosijaId
      })
      Sijoittelu.hakutoiveRecordToDTO(h, hakutoiveenPistetiedot)
    })
    hakutoiveetDTOs.sortBy(h => h.getHakutoive)
    hakijaDTO.setHakutoiveet(hakutoiveetDTOs.asInstanceOf[util.SortedSet[HakutoiveDTO]])
    return hakijaDTO
  }
}

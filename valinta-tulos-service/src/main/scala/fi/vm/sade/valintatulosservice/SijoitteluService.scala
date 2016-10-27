package fi.vm.sade.valintatulosservice

import java.util

import collection.JavaConverters._
import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.sijoittelu.tulos.dto.{SijoitteluajoDTO, ValintatapajonoDTO}
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.SijoitteluUtil
import fi.vm.sade.valintatulosservice.valintarekisteri.SijoitteluRepository

class SijoitteluService(sijoitteluRepository:SijoitteluRepository, sijoitteluUtil: SijoitteluUtil) extends Logging {

  def luoSijoitteluajo(sijoitteluajo:SijoitteluAjo) = sijoitteluRepository.storeSijoitteluajo(sijoitteluajo)

  def getSijoitteluajo(hakuOid:String, sijoitteluajoId:String): SijoitteluajoDTO = {
    val latestId = sijoitteluUtil.getLatestSijoitteluajoId(sijoitteluajoId, hakuOid)
    val sijoitteluAjoDTO = sijoitteluRepository.getSijoitteluajo(hakuOid, latestId) match {
      case Some(s) => sijoitteluUtil.sijoitteluajoRecordToDto(s)
      case None => new SijoitteluajoDTO
    }
    val hakukohdeDTOs = sijoitteluRepository.getSijoitteluajoHakukohteet(latestId) match {
      case Some(hakukohteet) => hakukohteet.map(h => sijoitteluUtil.sijoittelunHakukohdeRecordToDTO(h))
      case None => List()
    }
    val valintatapajonoDTOs = sijoitteluRepository.getValintatapajonot(latestId) match {
      case Some(valintatapajonot) => valintatapajonot.map(v => sijoitteluUtil.valintatapajonoRecordToDTO(v))
      case None => List()
    }
    val valintatapajonoOids = valintatapajonoDTOs.map(v => v.getOid)
    val valintatapajononHakemukset = sijoitteluRepository.getHakemuksetForValintatapajonos(valintatapajonoOids) match {
      case Some(hakemukset) => hakemukset.map(h => sijoitteluUtil.hakemusRecordToDTO(h))
      case None => List()
    }

    valintatapajononHakemukset.map(h => {
      val tilahistoria = sijoitteluRepository.getHakemuksenTilahistoria(h.getValintatapajonoOid,h.getHakemusOid)
      h.setTilaHistoria(tilahistoria.map(h => sijoitteluUtil.tilaHistoriaRecordToDTO(h)).asJava)
    })
    sijoitteluAjoDTO
  }

  def getHakemusBySijoitteluajo(hakuOid:String, sijoitteluajoId:String, hakemusOid:String): HakijaDTO = {
    val latestId = sijoitteluUtil.getLatestSijoitteluajoId(sijoitteluajoId, hakuOid)
    val hakija = sijoitteluRepository.getHakija(hakemusOid, latestId) match {
      case Some(h) => h
      case None => throw new IllegalArgumentException(s"Hakijaa ei löytynyt hakemukselle $hakemusOid, sijoitteluajoid: $latestId")
    }

    val hakutoiveet = sijoitteluRepository.getHakutoiveet(hakemusOid, latestId)
    val jonosijaIds = hakutoiveet.map(h => h.jonosijaId)
    val pistetiedot = sijoitteluRepository.getPistetiedot(jonosijaIds)

    val hakijaDTO = sijoitteluUtil.hakijaRecordToDTO(hakija)

    val hakutoiveetDTOs = hakutoiveet.map(h => {
      val hakutoiveenPistetiedot = pistetiedot.filter(p => {
        p.jonosijaId == h.jonosijaId
      })
      sijoitteluUtil.hakutoiveRecordToDTO(h, hakutoiveenPistetiedot)
    })
    hakutoiveetDTOs.sortBy(h => h.getHakutoive)
    hakijaDTO.setHakutoiveet(hakutoiveetDTOs.asInstanceOf[util.SortedSet[HakutoiveDTO]])
    return hakijaDTO
  }
}

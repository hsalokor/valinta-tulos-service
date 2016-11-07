package fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu

import java.util

import com.typesafe.config.ConfigFactory
import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.sijoittelu.tulos.dto.SijoitteluajoDTO
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakutoiveDTO, HakijaDTO}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.db.SijoitteluRepository
import fi.vm.sade.valintatulosservice.valintarekisteri.db.ValintarekisteriDb
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.SijoitteluUtil
import scala.collection.JavaConverters._

class ValintarekisteriForSijoittelu(sijoitteluRepository:SijoitteluRepository) extends Logging {

  def this() = this({
    val config: ApplicationSettings = ApplicationSettingsParser.parse(ConfigFactory.load())
    new ValintarekisteriDb(config.valintaRekisteriDbConfig)
  })

  lazy val sijoitteluUtil = new SijoitteluUtil(sijoitteluRepository)

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
    val valintatapajononHakemuksDTOs = sijoitteluRepository.getHakemuksetForValintatapajonos(valintatapajonoOids) match {
      case Some(hakemukset) => hakemukset.map(h => sijoitteluUtil.hakemusRecordToDTO(h))
      case None => List()
    }

    valintatapajononHakemuksDTOs.map(h => {
      val tilahistoria = sijoitteluRepository.getHakemuksenTilahistoria(h.getValintatapajonoOid,h.getHakemusOid)
      h.setTilaHistoria(tilahistoria.map(h => sijoitteluUtil.tilaHistoriaRecordToDTO(h)).asJava)
    })

    valintatapajonoDTOs.map(v => {
      v.setHakemukset(valintatapajononHakemuksDTOs.filter(vh => vh.getValintatapajonoOid == v.getOid).asJava)
    })

    val hakijaRyhmat = sijoitteluRepository.getHakijaryhmat(latestId)
    val hakijaryhmaDTOs = hakijaRyhmat.map(hr => {
      val hakijaryhmanHakemukset = sijoitteluRepository.getHakijaryhmanHakemukset(hr.id)
      val hakijaryhmaDTO = sijoitteluUtil.hakijaryhmaRecordToDTO(hr)
      hakijaryhmaDTO.setHakemusOid(hakijaryhmanHakemukset.asJava)
      hakijaryhmaDTO
    })

    hakukohdeDTOs.map(h => {
      // TODO miten mäpätään valintatapajonot hakukohteille?
      h.setHakijaryhmat(hakijaryhmaDTOs.filter(hr => hr.getHakukohdeOid == h.getOid).asJava)
    })

    sijoitteluAjoDTO.setHakukohteet(hakukohdeDTOs.asJava)
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
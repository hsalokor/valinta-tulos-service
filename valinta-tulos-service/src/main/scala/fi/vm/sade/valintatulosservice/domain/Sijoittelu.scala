package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}

import scala.collection.JavaConverters._

case class Sijoitteluajo(sijoitteluajoId:Long, hakuOid:String, startMils:Long, endMils:Long)

case class HakijaRecord(etunimi:String, sukunimi:String, hakemusOid:String, hakijaOid:String)

case class HakutoiveRecord(jonosijaId:Int, hakutoive:Int, hakukohdeOid:String, tarjoajaOid:String, valintatuloksenTila:String, kaikkiJonotsijoiteltu:Boolean)

case class PistetietoRecord(jonosijaId:Int, tunniste:String, arvo:String, laskennallinenArvo:String, osallistuminen:String)

object Sijoittelu {
  def hakijaRecordToDTO(hakija:HakijaRecord): HakijaDTO = {
    val hakijaDTO = new HakijaDTO
    hakijaDTO.setHakijaOid(hakija.hakijaOid)
    hakijaDTO.setHakemusOid(hakija.hakemusOid)
    hakijaDTO.setEtunimi(hakija.etunimi)
    hakijaDTO.setSukunimi(hakija.sukunimi)
    return hakijaDTO
  }

  def hakutoiveRecordToDTO(hakutoive:HakutoiveRecord, pistetiedot:List[PistetietoRecord]): HakutoiveDTO = {
    val hakutoiveDTO = new HakutoiveDTO
    hakutoiveDTO.setHakutoive(hakutoive.hakutoive)
    hakutoiveDTO.setHakukohdeOid(hakutoive.hakukohdeOid)
    hakutoiveDTO.setTarjoajaOid(hakutoive.tarjoajaOid)
//  TODO mites tämä? hakutoiveDTO.setVastaanottotieto(hakutoive.valintatuloksenTila)

    val pistetietoDTOs = pistetiedot.map(p => pistetietoRecordToTDO(p))
    hakutoiveDTO.setPistetiedot(pistetietoDTOs.asJava)
    return hakutoiveDTO
  }

  def pistetietoRecordToTDO(pistetieto:PistetietoRecord): PistetietoDTO = {
    val pistetietoDTO = new PistetietoDTO
    pistetietoDTO.setArvo(pistetieto.arvo)
    pistetietoDTO.setLaskennallinenArvo(pistetieto.laskennallinenArvo)
    pistetietoDTO.setOsallistuminen(pistetieto.osallistuminen)
    pistetietoDTO.setTunniste(pistetieto.tunniste)
    return pistetietoDTO
  }

  def parseSijoitteluajoId(sijoitteluajoId:String): Long = {
    try {
      sijoitteluajoId.toLong
    } catch {
      case e: NumberFormatException => throw new NumberFormatException(s"Väärän tyyppinen sijoitteuajon ID: $sijoitteluajoId")
    }
  }
}
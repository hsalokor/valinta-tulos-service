package fi.vm.sade.valintatulosservice.valintarekisteri.domain

import fi.vm.sade.sijoittelu.tulos.dto._
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}
import fi.vm.sade.valintatulosservice.valintarekisteri.db.SijoitteluRepository

import scala.collection.JavaConverters._

case class SijoitteluajoRecord(sijoitteluajoId:Long, hakuOid:String, startMils:Long, endMils:Long)

case class HakijaRecord(etunimi:String, sukunimi:String, hakemusOid:String, hakijaOid:String)

case class HakutoiveRecord(jonosijaId:Int, hakutoive:Int, hakukohdeOid:String, tarjoajaOid:String, valintatuloksenTila:String, kaikkiJonotsijoiteltu:Boolean)

case class PistetietoRecord(jonosijaId:Int, tunniste:String, arvo:String, laskennallinenArvo:String, osallistuminen:String)

case class SijoittelunHakukohdeRecord(sijoitteluajoId:Long, oid:String, tarjoajaOid:String, kaikkiJonotsijoiteltu:Boolean, ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet:BigDecimal)

case class ValintatapajonoRecord(tasasijasaanto:String, oid:String, nimi:String, prioriteetti:Int, aloituspaikat:Int,
                                 alkuperaisetAloituspaikat:Int, alinHyvaksyttyPistemaara:BigDecimal, eiVarasijatayttoa:Boolean,
                                 kaikkiEhdonTayttavatHyvaksytaan:Boolean, poissaOlevaTaytto:Boolean, valintaesitysHyvaksytty:Boolean,
                                 hakeneet:Int, hyvaksytty:Int, varalla:Int, varasijat:Int, varasijanTayttoPaivat:Int,
                                 varasijojaKaytetaanAlkaen:java.sql.Date, varasijojaKaytetaanAsti:java.sql.Date, tayttoJono:String)

case class HakemusRecord(hakijaOid:String, hakemusOid:String, pisteet:BigDecimal, etunimi:String, sukunimi:String,
                         prioriteetti:Int, jonosija:Int, tasasijaJonosija:Int, tila:Valinnantila,
                         tarkenne:String, tarkenteenLisatieto:String, hyvaksyttyHarkinnanvaraisesti:Boolean,
                         varasijaNumero:Int, onkoMuuttunutviimesijoittelusta:Boolean, hyvaksyttyHakijaRyhmasta:Boolean,
                         hakijaryhmaOid:String, siirtynytToisestaValintatapaJonosta:Boolean, valintatapajonoOid:String)

case class TilaHistoriaRecord(tila:String, poistaja:String, selite:String, luotu:java.sql.Date)

case class HakijaryhmaRecord(id:Long, prioriteetti:Int, paikat:Int, oid:String, nimi:String, hakukohdeOid:String, kiintio:Int,
                       kaytaKaikki:Boolean, tarkkaKiintio:Boolean, kaytetaanRyhmaanKuuluvia:Boolean,
                       valintatapajonoOid:String)

class SijoitteluUtil(sijoitteluRepository: SijoitteluRepository) {
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

  def sijoitteluajoRecordToDto(sijoitteluajo:SijoitteluajoRecord): SijoitteluajoDTO = {
    val sijoitteluajoDTO = new SijoitteluajoDTO
    sijoitteluajoDTO.setSijoitteluajoId(sijoitteluajo.sijoitteluajoId)
    sijoitteluajoDTO.setHakuOid(sijoitteluajo.hakuOid)
    sijoitteluajoDTO.setStartMils(sijoitteluajo.startMils)
    sijoitteluajoDTO.setEndMils(sijoitteluajo.endMils)
    sijoitteluajoDTO
  }

  import scala.collection.JavaConverters._

  def sijoittelunHakukohdeRecordToDTO(hakukohde:SijoittelunHakukohdeRecord): HakukohdeDTO = {
    val hakukohdeDTO = new HakukohdeDTO
    hakukohdeDTO.setSijoitteluajoId(hakukohde.sijoitteluajoId)
    hakukohdeDTO.setOid(hakukohde.oid)
    hakukohdeDTO.setTarjoajaOid(hakukohde.tarjoajaOid)
    hakukohdeDTO.setKaikkiJonotSijoiteltu(hakukohde.kaikkiJonotsijoiteltu)
    hakukohdeDTO.setEnsikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet(hakukohde.ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet match {
      case i:BigDecimal => i.bigDecimal
      case _ => null
     })
    hakukohdeDTO
  }

  def valintatapajonoRecordToDTO(jono:ValintatapajonoRecord): ValintatapajonoDTO = {
    val jonoDTO = new ValintatapajonoDTO
    jonoDTO.setTasasijasaanto(fi.vm.sade.sijoittelu.tulos.dto.Tasasijasaanto.valueOf(jono.tasasijasaanto.toUpperCase()))
    jonoDTO.setOid(jono.oid)
    jonoDTO.setNimi(jono.nimi)
    jonoDTO.setPrioriteetti(jono.prioriteetti)
    jonoDTO.setAloituspaikat(jono.aloituspaikat)
    jonoDTO.setAlkuperaisetAloituspaikat(jono.alkuperaisetAloituspaikat)
    jonoDTO.setAlinHyvaksyttyPistemaara(jono.alinHyvaksyttyPistemaara match {
      case i:BigDecimal => i.bigDecimal
      case _ => null
    })
    jonoDTO.setEiVarasijatayttoa(jono.eiVarasijatayttoa)
    jonoDTO.setKaikkiEhdonTayttavatHyvaksytaan(jono.kaikkiEhdonTayttavatHyvaksytaan)
    jonoDTO.setPoissaOlevaTaytto(jono.poissaOlevaTaytto)
    jonoDTO.setValintaesitysHyvaksytty(jono.valintaesitysHyvaksytty)
    jonoDTO.setHakeneet(jono.hakeneet)
    jonoDTO.setHyvaksytty(jono.hyvaksytty)
    jonoDTO.setVaralla(jono.varalla)
    jonoDTO.setVarasijat(jono.varasijat)
    jonoDTO.setVarasijaTayttoPaivat(jono.varasijanTayttoPaivat)
    jonoDTO.setVarasijojaKaytetaanAlkaen(jono.varasijojaKaytetaanAlkaen)
    jonoDTO.setVarasijojaTaytetaanAsti(jono.varasijojaKaytetaanAsti)
    jonoDTO.setTayttojono(jono.tayttoJono)
    jonoDTO
  }

  def hakemusRecordToDTO(hakemus:HakemusRecord): HakemusDTO = {
    val hakemusDTO = new HakemusDTO
    hakemusDTO.setHakijaOid(hakemus.hakijaOid)
    hakemusDTO.setHakemusOid(hakemus.hakemusOid)
    hakemusDTO.setPisteet(hakemus.pisteet match {
      case i:BigDecimal => i.bigDecimal
      case _ => null
    })
    hakemusDTO.setEtunimi(hakemus.etunimi)
    hakemusDTO.setSukunimi(hakemus.sukunimi)
    hakemusDTO.setPrioriteetti(hakemus.prioriteetti)
    hakemusDTO.setJonosija(hakemus.jonosija)
    hakemusDTO.setTasasijaJonosija(hakemus.tasasijaJonosija)
    hakemusDTO.setTila(HakemuksenTila.valueOf(hakemus.tila.valinnantila.name()))
    hakemusDTO.setTilanKuvaukset(ValinnantilanTarkenne.getValinnantilanTarkenne(Map[String, String](hakemus.tarkenne -> hakemus.tarkenteenLisatieto)) match {
      case Some(t) => t.valinnantilanTarkenne.asJava
      case None => null
    })
    hakemusDTO.setHyvaksyttyHarkinnanvaraisesti(hakemus.hyvaksyttyHarkinnanvaraisesti)
    hakemusDTO.setVarasijanNumero(hakemus.varasijaNumero)
    hakemusDTO.setOnkoMuuttunutViimeSijoittelussa(hakemus.onkoMuuttunutviimesijoittelusta)
    hakemusDTO.setHyvaksyttyHakijaryhmasta(hakemus.hyvaksyttyHakijaRyhmasta)
    hakemusDTO.setHakijaryhmaOid(hakemus.hakijaryhmaOid)
    hakemusDTO.setSiirtynytToisestaValintatapajonosta(hakemus.siirtynytToisestaValintatapaJonosta)
    hakemusDTO.setValintatapajonoOid(hakemus.valintatapajonoOid)
    hakemusDTO
  }

  def tilaHistoriaRecordToDTO(tila:TilaHistoriaRecord): TilaHistoriaDTO = {
    val tilaDTO = new TilaHistoriaDTO
    tilaDTO.setLuotu(tila.luotu)
    tilaDTO.setTila(tila.tila)
    tilaDTO
  }

  def hakijaryhmaRecordToDTO(hakijaRyhma:HakijaryhmaRecord): HakijaryhmaDTO = {
    val ryhmaDTO = new HakijaryhmaDTO
    ryhmaDTO.setPrioriteetti(hakijaRyhma.prioriteetti)
    ryhmaDTO.setPaikat(hakijaRyhma.paikat)
    ryhmaDTO.setOid(hakijaRyhma.oid)
    ryhmaDTO.setNimi(hakijaRyhma.nimi)
    ryhmaDTO.setHakukohdeOid(hakijaRyhma.hakukohdeOid)
    ryhmaDTO.setKiintio(hakijaRyhma.kiintio)
    ryhmaDTO.setKaytaKaikki(hakijaRyhma.kaytaKaikki)
    ryhmaDTO.setTarkkaKiintio(hakijaRyhma.tarkkaKiintio)
    ryhmaDTO.setKaytetaanRyhmaanKuuluvia(hakijaRyhma.kaytetaanRyhmaanKuuluvia)
    ryhmaDTO.setValintatapajonoOid(hakijaRyhma.valintatapajonoOid)
    ryhmaDTO
  }

  def getLatestSijoitteluajoId(sijoitteluajoId:String, hakuOid:String): Long = {
    if (sijoitteluajoId.equalsIgnoreCase("latest")) {
      sijoitteluRepository.getLatestSijoitteluajoId(hakuOid) match {
        case Some(i) => i
        case None => throw new IllegalArgumentException(s"Yhtään sijoitteluajoa ei löytynyt haulle $hakuOid")
      }
    } else {
      try {
        sijoitteluajoId.toLong
      } catch {
        case e: NumberFormatException => throw new NumberFormatException(s"Väärän tyyppinen sijoitteuajon ID: $sijoitteluajoId")
      }
    }
  }
}
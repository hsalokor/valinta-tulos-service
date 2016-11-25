package fi.vm.sade.valintatulosservice.valintarekisteri.domain

import fi.vm.sade.sijoittelu.tulos.dto._
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}

import scala.collection.JavaConverters._

case class SijoitteluajoRecord(sijoitteluajoId:Long, hakuOid:String, startMils:Long, endMils:Long)

case class HakijaRecord(etunimi:String, sukunimi:String, hakemusOid:String, hakijaOid:String)

case class HakutoiveRecord(jonosijaId:Int, hakutoive:Int, hakukohdeOid:String, tarjoajaOid:String,
                           valintatuloksenTila:String, kaikkiJonotsijoiteltu:Boolean)

case class PistetietoRecord(jonosijaId:Int, tunniste:String, arvo:String, laskennallinenArvo:String,
                            osallistuminen:String)

case class SijoittelunHakukohdeRecord(sijoitteluajoId:Long, oid:String, tarjoajaOid:String, kaikkiJonotsijoiteltu:Boolean,
                                      ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet:BigDecimal)

case class ValintatapajonoRecord(tasasijasaanto:String, oid:String, nimi:String, prioriteetti:Int, aloituspaikat:Int,
                                 alkuperaisetAloituspaikat:Int, alinHyvaksyttyPistemaara:BigDecimal,
                                 eiVarasijatayttoa:Boolean, kaikkiEhdonTayttavatHyvaksytaan:Boolean,
                                 poissaOlevaTaytto:Boolean, valintaesitysHyvaksytty:Boolean, hakeneet:Int,
                                 hyvaksytty:Int, varalla:Int, varasijat:Int, varasijanTayttoPaivat:Int,
                                 varasijojaKaytetaanAlkaen:java.sql.Date, varasijojaKaytetaanAsti:java.sql.Date,
                                 tayttoJono:String, hakukohdeOid:String)

case class HakemusRecord(hakijaOid:String, hakemusOid:String, pisteet:BigDecimal, etunimi:String, sukunimi:String,
                         prioriteetti:Int, jonosija:Int, tasasijaJonosija:Int, tila:Valinnantila, tilankuvausId:Long,
                         tarkenteenLisatieto:Option[String], hyvaksyttyHarkinnanvaraisesti:Boolean, varasijaNumero:Int,
                         onkoMuuttunutviimesijoittelusta:Boolean, hakijaryhmaOids:Set[String],
                         siirtynytToisestaValintatapaJonosta:Boolean, valintatapajonoOid:String)

case class TilaHistoriaRecord(tila:String, poistaja:String, selite:String, luotu:java.sql.Date)

case class HakijaryhmaRecord(id:Long, prioriteetti:Int, paikat:Int, oid:String, nimi:String, hakukohdeOid:String,
                             kiintio:Int, kaytaKaikki:Boolean, tarkkaKiintio:Boolean, kaytetaanRyhmaanKuuluvia:Boolean,
                             valintatapajonoOid:String)

abstract class SijoitteluRecordToDTO {
  def hakijaRecordToDTO(hakija: HakijaRecord): HakijaDTO = {
    val hakijaDTO = new HakijaDTO
    hakijaDTO.setHakijaOid(hakija.hakijaOid)
    hakijaDTO.setHakemusOid(hakija.hakemusOid)
    hakijaDTO.setEtunimi(hakija.etunimi)
    hakijaDTO.setSukunimi(hakija.sukunimi)
    return hakijaDTO
  }

  def hakutoiveRecordToDTO(hakutoive: HakutoiveRecord, pistetiedot: List[PistetietoRecord]): HakutoiveDTO = {
    val hakutoiveDTO = new HakutoiveDTO
    hakutoiveDTO.setHakutoive(hakutoive.hakutoive)
    hakutoiveDTO.setHakukohdeOid(hakutoive.hakukohdeOid)
    hakutoiveDTO.setTarjoajaOid(hakutoive.tarjoajaOid)
    //  TODO mites tämä? hakutoiveDTO.setVastaanottotieto(hakutoive.valintatuloksenTila)

    val pistetietoDTOs = pistetiedot.map(p => pistetietoRecordToTDO(p))
    hakutoiveDTO.setPistetiedot(pistetietoDTOs.asJava)
    return hakutoiveDTO
  }

  def pistetietoRecordToTDO(pistetieto: PistetietoRecord): PistetietoDTO = {
    val pistetietoDTO = new PistetietoDTO
    pistetietoDTO.setArvo(pistetieto.arvo)
    pistetietoDTO.setLaskennallinenArvo(pistetieto.laskennallinenArvo)
    pistetietoDTO.setOsallistuminen(pistetieto.osallistuminen)
    pistetietoDTO.setTunniste(pistetieto.tunniste)
    return pistetietoDTO
  }

  def sijoitteluajoRecordToDto(sijoitteluajo: SijoitteluajoRecord): SijoitteluajoDTO = {
    val sijoitteluajoDTO = new SijoitteluajoDTO
    sijoitteluajoDTO.setSijoitteluajoId(sijoitteluajo.sijoitteluajoId)
    sijoitteluajoDTO.setHakuOid(sijoitteluajo.hakuOid)
    sijoitteluajoDTO.setStartMils(sijoitteluajo.startMils)
    sijoitteluajoDTO.setEndMils(sijoitteluajo.endMils)
    sijoitteluajoDTO
  }

  def sijoitteluajoRecordToDto(sijoitteluajo: SijoitteluajoRecord, hakukohteet:List[HakukohdeDTO]): SijoitteluajoDTO = {
    val sijoitteluajoDTO = sijoitteluajoRecordToDto(sijoitteluajo)
    sijoitteluajoDTO.setHakukohteet(hakukohteet.asJava)
    sijoitteluajoDTO
  }

  import scala.collection.JavaConverters._

  def sijoittelunHakukohdeRecordToDTO(hakukohde: SijoittelunHakukohdeRecord): HakukohdeDTO = {
    val hakukohdeDTO = new HakukohdeDTO
    hakukohdeDTO.setSijoitteluajoId(hakukohde.sijoitteluajoId)
    hakukohdeDTO.setOid(hakukohde.oid)
    hakukohdeDTO.setTarjoajaOid(hakukohde.tarjoajaOid)
    hakukohdeDTO.setKaikkiJonotSijoiteltu(hakukohde.kaikkiJonotsijoiteltu)
    hakukohdeDTO.setEnsikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet(
      bigDecimal(hakukohde.ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet))
    hakukohdeDTO
  }

  def valintatapajonoRecordToDTO(jono: ValintatapajonoRecord): ValintatapajonoDTO = {
    val jonoDTO = new ValintatapajonoDTO
    jonoDTO.setTasasijasaanto(fi.vm.sade.sijoittelu.tulos.dto.Tasasijasaanto.valueOf(jono.tasasijasaanto.toUpperCase()))
    jonoDTO.setOid(jono.oid)
    jonoDTO.setNimi(jono.nimi)
    jonoDTO.setPrioriteetti(jono.prioriteetti)
    jonoDTO.setAloituspaikat(jono.aloituspaikat)
    jonoDTO.setAlkuperaisetAloituspaikat(jono.alkuperaisetAloituspaikat)
    jonoDTO.setAlinHyvaksyttyPistemaara(bigDecimal(jono.alinHyvaksyttyPistemaara))
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

  def hakemusRecordToDTO(hakemus:HakemusRecord, tilanKuvaukset:Option[Map[String,String]]): HakemusDTO = {
    val hakemusDTO = new HakemusDTO
    hakemusDTO.setHakijaOid(hakemus.hakijaOid)
    hakemusDTO.setHakemusOid(hakemus.hakemusOid)
    hakemusDTO.setPisteet(bigDecimal(hakemus.pisteet))
    hakemusDTO.setEtunimi(hakemus.etunimi)
    hakemusDTO.setSukunimi(hakemus.sukunimi)
    hakemusDTO.setPrioriteetti(hakemus.prioriteetti)
    hakemusDTO.setJonosija(hakemus.jonosija)
    hakemusDTO.setTasasijaJonosija(hakemus.tasasijaJonosija)
    hakemusDTO.setTila(HakemuksenTila.valueOf(hakemus.tila.valinnantila.name()))
    hakemusDTO.setTilanKuvaukset(tilanKuvaukset.get.asJava)
    hakemusDTO.setHyvaksyttyHarkinnanvaraisesti(hakemus.hyvaksyttyHarkinnanvaraisesti)
    hakemusDTO.setVarasijanNumero(hakemus.varasijaNumero)
    hakemusDTO.setOnkoMuuttunutViimeSijoittelussa(hakemus.onkoMuuttunutviimesijoittelusta)
    hakemusDTO.setHyvaksyttyHakijaryhmista(hakemus.hakijaryhmaOids.asJava)
    hakemusDTO.setSiirtynytToisestaValintatapajonosta(hakemus.siirtynytToisestaValintatapaJonosta)
    hakemusDTO.setValintatapajonoOid(hakemus.valintatapajonoOid)
    hakemusDTO
  }

  def tilaHistoriaRecordToDTO(tila: TilaHistoriaRecord): TilaHistoriaDTO = {
    val tilaDTO = new TilaHistoriaDTO
    tilaDTO.setLuotu(tila.luotu)
    tilaDTO.setTila(tila.tila)
    tilaDTO
  }

  def hakijaryhmaRecordToDTO(hakijaRyhma: HakijaryhmaRecord): HakijaryhmaDTO = {
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

  def hakijaryhmaRecordToDTO(hakijaRyhma: HakijaryhmaRecord, hakemusOidit:List[String]): HakijaryhmaDTO = {
    val ryhmaDTO = hakijaryhmaRecordToDTO(hakijaRyhma)
    ryhmaDTO.setHakemusOid(hakemusOidit.asJava)
    ryhmaDTO
  }

  def bigDecimal(bigDecimal:BigDecimal): java.math.BigDecimal = bigDecimal match {
    case i: BigDecimal => i.bigDecimal
    case _ => null
  }
}
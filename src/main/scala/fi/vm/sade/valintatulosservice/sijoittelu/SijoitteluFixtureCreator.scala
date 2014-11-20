package fi.vm.sade.valintatulosservice.sijoittelu

import java.util
import java.util.Date

import fi.vm.sade.sijoittelu.domain._

object SijoitteluFixtureCreator {
  import collection.JavaConversions._


  def newHakemus(hakemusOid: String, hakijaOid: String, hakutoiveIndex: Int, hakemuksenTila: HakemuksenTila): Hakemus = {
    val hakemus = new Hakemus
    hakemus.setHakijaOid(hakijaOid)
    hakemus.setHakemusOid(hakemusOid)
    hakemus.setEtunimi("Teppo")
    hakemus.setSukunimi("Testaaja")
    hakemus.setPrioriteetti(hakutoiveIndex)
    hakemus.setJonosija(1)
    hakemus.setPisteet(new java.math.BigDecimal(4))
    hakemus.setTasasijaJonosija(1)
    hakemus.setTila(hakemuksenTila)
    val historia: TilaHistoria = new TilaHistoria()
    historia.setLuotu(new Date())
    historia.setTila(hakemuksenTila)
    hakemus.getTilaHistoria.add(historia)
    hakemus
  }

  def newValintatapajono(jonoOid: String, hakemukset: List[Hakemus]) = {
    val jono = new Valintatapajono()
    jono.setTasasijasaanto(Tasasijasaanto.YLITAYTTO)
    jono.setOid(jonoOid)
    jono.setNimi("testijono")
    jono.setPrioriteetti(0)
    jono.setAloituspaikat(3)
    jono.setHakemukset(new util.ArrayList(hakemukset))
    jono
  }

  def newHakukohde(hakukohdeOid: String, tarjoajaOid: String, sijoitteluajoId: Long, kaikkiJonotSijoiteltu: Boolean, jonot: List[Valintatapajono]) = {
    val hakukohde = new Hakukohde()
    hakukohde.setSijoitteluajoId(sijoitteluajoId)
    hakukohde.setOid(hakukohdeOid)
    hakukohde.setTarjoajaOid(tarjoajaOid)
    hakukohde.setKaikkiJonotSijoiteltu(kaikkiJonotSijoiteltu)
    hakukohde.setValintatapajonot(jonot)
    hakukohde
  }

  def newValintatulos(jonoOid: String, hakuOid: String, hakemusOid: String, hakukohdeOid: String, hakijaOid: String, hakutoiveIndex: Int) = {
    val valintatulos = new Valintatulos()
    valintatulos.setHakuOid(hakuOid)
    valintatulos.setValintatapajonoOid(jonoOid)
    valintatulos.setHakemusOid(hakemusOid)
    valintatulos.setHakukohdeOid(hakukohdeOid)
    valintatulos.setHakijaOid(hakijaOid)
    valintatulos.setHakutoive(hakutoiveIndex)
    valintatulos.setJulkaistavissa(true)
    val logEntry: LogEntry = new LogEntry()
    logEntry.setLuotu(new Date)
    logEntry.setMuokkaaja(hakijaOid)
    logEntry.setMuutos("KESKEN")
    logEntry.setSelite("testing")
    valintatulos.getLogEntries.add(logEntry)
    valintatulos
  }

  def newSijoittelu(hakuOid: String, sijoitteluajoId: Long, hakukohdeOids: List[String]): Sijoittelu = {
    val sijoitteluAjo = new SijoitteluAjo
    sijoitteluAjo.setSijoitteluajoId(sijoitteluajoId)
    sijoitteluAjo.setHakuOid(hakuOid)
    sijoitteluAjo.setStartMils(System.currentTimeMillis())
    sijoitteluAjo.setEndMils(System.currentTimeMillis())
    sijoitteluAjo.setHakukohteet(hakukohdeOids.map { hakukohdeOid =>
      val item = new HakukohdeItem()
      item.setOid(hakukohdeOid)
      item
    })

    val sijoittelu = new Sijoittelu()
    sijoittelu.setHakuOid(hakuOid)
    sijoittelu.setSijoitteluId(1l)
    sijoittelu.setCreated(new Date)
    sijoittelu.setSijoittele(true)
    sijoittelu.getSijoitteluajot.add(sijoitteluAjo)
    sijoittelu
  }
}

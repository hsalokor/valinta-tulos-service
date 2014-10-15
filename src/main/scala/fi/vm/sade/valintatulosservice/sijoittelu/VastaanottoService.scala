package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.{EHDOLLISESTI_VASTAANOTTANUT, PERUNUT, VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI}
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import fi.vm.sade.valintatulosservice.domain.{Ilmoittautuminen, Vastaanotettavuustila, Vastaanotto}
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService}
import fi.vm.sade.valintatulosservice.ValintatulosService
import fi.vm.sade.valintatulosservice.domain.Hakemuksentulos
import fi.vm.sade.valintatulosservice.domain.Hakutoiveentulos

class VastaanottoService(valintatulosService: ValintatulosService, dao: ValintatulosDao) {
  def vastaanota(hakuOid: String, hakemusOid: String, vastaanotto: Vastaanotto) {
    val hakutoive = etsiHakutoive(hakuOid, hakemusOid, vastaanotto.hakukohdeOid)
    val tila: ValintatuloksenTila = ValintatuloksenTila.valueOf(vastaanotto.tila.toString)
    tarkistaVastaanotettavuus(hakutoive, tila)
    vastaanota(vastaanotto.hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid, tila, vastaanotto.muokkaaja, vastaanotto.selite)
  }

  def ilmoittaudu(hakuOid: String, hakemusOid: String, ilmoittautuminen: Ilmoittautuminen) {
    val hakutoive = etsiHakutoive(hakuOid, hakemusOid, ilmoittautuminen.hakukohdeOid)
    val valintatulos: Valintatulos = getValintatulos(ilmoittautuminen.hakukohdeOid, hakutoive.valintatapajonoOid, hakemusOid)
    val sopivatTilat = Array(VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI)

    if(!sopivatTilat.contains(valintatulos.getTila)) {
      throw new IllegalArgumentException(s"""Valintatulokselle, jonka tila on
      ${valintatulos.getTila} ei voi tallentaa ilmoittautumistietoa""")
    }
    valintatulos.setIlmoittautumisTila(IlmoittautumisTila.valueOf(ilmoittautuminen.tila.toString))
    addLogEntry(valintatulos, ilmoittautuminen.tila.toString, ilmoittautuminen.muokkaaja, ilmoittautuminen.selite)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  private def etsiHakutoive(hakuOid: String, hakemusOid: String, hakukohdeOid: String): Hakutoiveentulos = {
    val hakemuksenTulos: Option[Hakemuksentulos] = valintatulosService.hakemuksentulos(hakuOid, hakemusOid)

    hakemuksenTulos.flatMap(_.hakutoiveet.find(_.hakukohdeOid == hakukohdeOid)) match {
      case Some(hakutoive) =>
        hakutoive
      case _ =>
        throw new IllegalArgumentException("Hakemusta tai hakutoivetta ei löydy")
    }
  }

  private def tarkistaVastaanotettavuus(hakutoive: Hakutoiveentulos, tila: ValintatuloksenTila) {
    if (!List(VASTAANOTTANUT, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT).contains(tila)) {
      throw new IllegalArgumentException("Ei-hyväksytty vastaanottotila: " + tila)
    }
    if (List(VASTAANOTTANUT, PERUNUT).contains(tila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException("Väärä vastaanotettavuustila: " + hakutoive.vastaanotettavuustila.toString + " (tavoitetila " + tila + ")")
    }
    if (tila == EHDOLLISESTI_VASTAANOTTANUT && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
      throw new IllegalArgumentException(tila.toString())
    }
  }

  private def vastaanota(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String, tila: ValintatuloksenTila, muokkaaja: String, selite: String) {
    val valintatulos = getValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)
    valintatulos.setTila(tila)
    addLogEntry(valintatulos, valintatulos.getTila.name, muokkaaja, selite)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  private def getValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String): Valintatulos = {
    val valintatulos: Valintatulos = dao.loadValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)
    if (valintatulos == null) {
      throw new IllegalArgumentException("Valintatulosta ei löydy")
    }
    valintatulos
  }

  private def addLogEntry(valintatulos: Valintatulos, tila: String, muokkaaja: String, selite: String) {
    val logEntry: LogEntry = new LogEntry
    logEntry.setLuotu(new Date)
    logEntry.setMuokkaaja(muokkaaja)
    logEntry.setSelite(selite)
    logEntry.setMuutos(tila)
    valintatulos.getLogEntries.add(logEntry)
  }
}
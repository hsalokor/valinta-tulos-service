package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date

import fi.vm.sade.sijoittelu.domain.{LogEntry, Valintatulos}
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao

class ValintatulosRepository(dao: ValintatulosDao) {
  def modifyValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String, tilanKuvaus: String, muokkaaja: String, selite: String)(block: (Valintatulos => Unit)) {
    val valintatulos = getValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)
    block(valintatulos)
    addLogEntry(valintatulos, tilanKuvaus, muokkaaja, selite)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  private def addLogEntry(valintatulos: Valintatulos, tila: String, muokkaaja: String, selite: String) {
    val logEntry: LogEntry = new LogEntry
    logEntry.setLuotu(new Date)
    logEntry.setMuokkaaja(muokkaaja)
    logEntry.setSelite(selite)
    logEntry.setMuutos(tila)
    valintatulos.getLogEntries.add(logEntry)
  }

  private def getValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String): Valintatulos = {
    val valintatulos: Valintatulos = dao.loadValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)
    if (valintatulos == null) {
      throw new IllegalArgumentException("Valintatulosta ei löydy")
    }
    valintatulos
  }
}

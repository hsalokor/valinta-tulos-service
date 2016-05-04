package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date

import fi.vm.sade.sijoittelu.domain.{HakemuksenTila, LogEntry, Valintatulos}
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao

class ValintatulosRepository(dao: ValintatulosDao) {
  def modifyValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String, createMissingValintatulos: Unit => Valintatulos)(block: (Valintatulos => Unit)) {
    val valintatulos = getOrCreateValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid, createMissingValintatulos)
    block(valintatulos)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  private def getOrCreateValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String, createMissingValintatulos: Unit => Valintatulos): Valintatulos = {
    Option(dao.loadValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)) match {
      case Some(valintatulos) => valintatulos
      case None =>
        val valintatulos = createMissingValintatulos()
        dao.createOrUpdateValintatulos(valintatulos)
        valintatulos
    }
  }
}

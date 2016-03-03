package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date

import fi.vm.sade.sijoittelu.domain.{LogEntry, Valintatulos}
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao

class ValintatulosRepository(dao: ValintatulosDao) {
  def modifyValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String)(block: (Valintatulos => Unit)) {
    val valintatulos = getValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)
    block(valintatulos)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  private def getValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String): Valintatulos = {
    val valintatulos: Valintatulos = dao.loadValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)
    if (valintatulos == null) {
      throw new IllegalArgumentException("Valintatulosta ei löydy")
    }
    valintatulos
  }
}

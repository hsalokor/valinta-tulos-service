package fi.vm.sade.valintatulosservice.sijoittelu

import fi.vm.sade.sijoittelu.domain.Valintatulos
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao

class ValintatulosRepository(dao: ValintatulosDao) {
  def modifyValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String)(block: (Valintatulos => Unit)) {
    val valintatulos = getValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid)
    block(valintatulos)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  def createOrModifyValintatulos(hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String, hakijaOid: String, hakuOid: String, hakutoive: Int)(block: (Valintatulos => Unit)) {
    val valintatulos: Valintatulos = Option(dao.loadValintatulos(hakukohdeOid, valintatapajonoOid, hakemusOid))
      .getOrElse(new Valintatulos(valintatapajonoOid, hakemusOid, hakukohdeOid, hakijaOid, hakuOid, hakutoive))
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

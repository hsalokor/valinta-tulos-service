package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.HakukohdeRecord

trait HakukohdeRepository {
  def findHakukohde(oid: String): Option[HakukohdeRecord]
  def storeHakukohde(hakukohdeRecord: HakukohdeRecord): Unit
}

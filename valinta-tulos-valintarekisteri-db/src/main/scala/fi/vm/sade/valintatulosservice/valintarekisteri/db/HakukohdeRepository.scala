package fi.vm.sade.valintatulosservice.valintarekisteri.db

import fi.vm.sade.valintatulosservice.valintarekisteri.domain.HakukohdeRecord

trait HakukohdeRepository {
  def findHakukohde(oid: String): Option[HakukohdeRecord]
  def findHaunArbitraryHakukohde(oid: String): Option[HakukohdeRecord]
  def findHaunHakukohteet(oid: String): Set[HakukohdeRecord]
  def all: Set[HakukohdeRecord]
  def findHakukohteet(hakukohdeOids: Set[String]): Set[HakukohdeRecord]
  def storeHakukohde(hakukohdeRecord: HakukohdeRecord): Unit
  def updateHakukohde(hakukohdeRecord: HakukohdeRecord): Boolean
  def hakukohteessaVastaanottoja(oid: String): Boolean
}

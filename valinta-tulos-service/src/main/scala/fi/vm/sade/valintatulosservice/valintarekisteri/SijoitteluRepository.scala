package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.valintatulosservice.domain.{HakijaRecord, HakutoiveRecord, PistetietoRecord, SijoitteluWrapper, _}
import slick.driver.PostgresDriver.backend.Database

trait SijoitteluRepository {
  val db: Database
  def storeSijoitteluajo(sijoitteluajo:SijoitteluAjo): Unit
  def storeSijoittelu(sijoittelu:SijoitteluWrapper)
  def getLatestSijoitteluajoId(hakuOid:String): Option[Long]
  def getSijoitteluajo(hakuOid:String, sijoitteluajoId:Long): Option[SijoitteluajoRecord]
  def getSijoitteluajoHakukohteet(sijoitteluajoId:Long): Option[List[SijoittelunHakukohdeRecord]]
  def getValintatapajonot(sijoitteluajoId:Long): Option[List[ValintatapajonoRecord]]
  def getHakija(hakemusOid:String, sijoitteluajoId:Long): Option[HakijaRecord]
  def getHakutoiveet(hakemusOid:String, sijoitteluajoId:Long): List[HakutoiveRecord]
  def getPistetiedot(jonosijaIds:List[Int]): List[PistetietoRecord]
}

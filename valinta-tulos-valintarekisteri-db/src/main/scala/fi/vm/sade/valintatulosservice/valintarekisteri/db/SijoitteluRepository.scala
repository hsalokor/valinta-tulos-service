package fi.vm.sade.valintatulosservice.valintarekisteri.db

import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import slick.driver.PostgresDriver.backend.Database

trait SijoitteluRepository {
  val db: Database
  def storeSijoittelu(sijoittelu:SijoitteluWrapper)
  def getLatestSijoitteluajoId(hakuOid:String): Option[Long]
  def getSijoitteluajo(hakuOid:String, sijoitteluajoId:Long): Option[SijoitteluajoRecord]
  def getSijoitteluajoHakukohteet(sijoitteluajoId:Long): List[SijoittelunHakukohdeRecord]
  def getValintatapajonot(sijoitteluajoId:Long): List[ValintatapajonoRecord]
  def getHakemuksetForValintatapajonos(sijoitteluajoId:Long, valintatapajonoOids:List[String]): List[HakemusRecord]
  def getHakemukset(sijoitteluajoId:Long): List[HakemusRecord]
  def getSijoitteluajonTilahistoriat(sijoitteluajoId:Long): List[TilaHistoriaRecord]
  def getTilankuvaukset(tilankuvausHashes:List[Int]): Map[Int,TilankuvausRecord]
  def getHakijaryhmat(sijoitteluajoId:Long): List[HakijaryhmaRecord]
  def getHakijaryhmanHakemukset(hakijaryhmaId:Long): List[String]
  def getHakija(hakemusOid:String, sijoitteluajoId:Long): Option[HakijaRecord]
  def getHakutoiveet(hakemusOid:String, sijoitteluajoId:Long): List[HakutoiveRecord]
  def getPistetiedot(jonosijaIds: List[Long]): List[PistetietoRecord]
  def getSijoitteluajonPistetiedot(sijoitteluajoId:Long): List[PistetietoRecord]
}

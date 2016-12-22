package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.time.Instant

import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import slick.dbio.DBIO
import slick.driver.PostgresDriver.backend.Database

trait SijoitteluRepository {
  val db: Database
  def storeSijoittelu(sijoittelu:SijoitteluWrapper)
  def getLatestSijoitteluajoId(hakuOid:String): Option[Long]
  def getSijoitteluajo(hakuOid:String, sijoitteluajoId:Long): Option[SijoitteluajoRecord]
  def getSijoitteluajoHakukohteet(sijoitteluajoId:Long): List[SijoittelunHakukohdeRecord]
  def getValintatapajonot(sijoitteluajoId:Long): List[ValintatapajonoRecord]
  def getHakemuksetForValintatapajonos(sijoitteluajoId:Long, valintatapajonoOids:List[String]): List[HakemusRecord]
  def getValinnanTuloksetForValintatapajono(valintatapajonoOid: String): DBIO[List[(Instant, ValinnanTulos)]]
  def getHakemukset(sijoitteluajoId:Long): List[HakemusRecord]
  def getSijoitteluajonTilahistoriat(sijoitteluajoId:Long): List[TilaHistoriaRecord]
  def getValinnantilanKuvaukset(tilankuvausHashes:List[Int]): Map[Int,TilankuvausRecord]
  def getHakijaryhmat(sijoitteluajoId:Long): List[HakijaryhmaRecord]
  def getHakijaryhmanHakemukset(hakijaryhmaOid:String, sijoitteluajoId:Long): List[String]
  def getHakija(hakemusOid:String, sijoitteluajoId:Long): Option[HakijaRecord]
  def getHakutoiveet(hakemusOid:String, sijoitteluajoId:Long): List[HakutoiveRecord]
  def getPistetiedot(hakemusOids:String, sijoitteluajoId:Long): List[PistetietoRecord]
  def getSijoitteluajonPistetiedot(sijoitteluajoId:Long): List[PistetietoRecord]
}

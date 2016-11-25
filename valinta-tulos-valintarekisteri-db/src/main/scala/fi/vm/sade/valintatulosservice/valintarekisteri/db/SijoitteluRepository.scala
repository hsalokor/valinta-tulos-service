package fi.vm.sade.valintatulosservice.valintarekisteri.db

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
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
  def getHakemuksenTilahistoria(valintatapajonoOid:String, hakemusOid:String): List[TilaHistoriaRecord]
  def getHakemuksenTilankuvaukset(tilankuvausId:Long, tarkenteenLisatieto:Option[String]): Option[Map[String,String]]
  def getHakijaryhmat(sijoitteluajoId:Long): List[HakijaryhmaRecord]
  def getHakijaryhmanHakemukset(hakijaryhmaId:Long): List[String]
  def getHakija(hakemusOid:String, sijoitteluajoId:Long): Option[HakijaRecord]
  def getHakutoiveet(hakemusOid:String, sijoitteluajoId:Long): List[HakutoiveRecord]
  def getPistetiedot(jonosijaIds:List[Int]): List[PistetietoRecord]
  def getPistetiedot(hakemusOid:String, sijoitteluajoId:Long): List[PistetietoRecord]
}

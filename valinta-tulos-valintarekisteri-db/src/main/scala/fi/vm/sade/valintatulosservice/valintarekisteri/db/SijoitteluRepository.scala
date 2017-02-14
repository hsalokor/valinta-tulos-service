package fi.vm.sade.valintatulosservice.valintarekisteri.db

import fi.vm.sade.valintatulosservice.valintarekisteri.db.impl.ValintarekisteriRepository
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._

trait SijoitteluRepository extends ValintarekisteriRepository {
  def storeSijoittelu(sijoittelu:SijoitteluWrapper)

  def getLatestSijoitteluajoId(hakuOid:String): Option[Long]
  def getSijoitteluajo(sijoitteluajoId:Long): Option[SijoitteluajoRecord]
  def getSijoitteluajonHakukohteet(sijoitteluajoId:Long): List[SijoittelunHakukohdeRecord]
  def getSijoitteluajonValintatapajonot(sijoitteluajoId:Long): List[ValintatapajonoRecord]
  def getSijoitteluajonHakemukset(sijoitteluajoId:Long): List[HakemusRecord]
  def getSijoitteluajonHakemustenHakijaryhmat(sijoitteluajoId:Long): Map[String,Set[String]]
  def getSijoitteluajonTilahistoriat(sijoitteluajoId:Long): List[TilaHistoriaRecord]
  def getSijoitteluajonHakijaryhmat(sijoitteluajoId:Long): List[HakijaryhmaRecord]
  def getSijoitteluajonHakijaryhmanHakemukset(hakijaryhmaOid:String, sijoitteluajoId:Long): List[String]
  def getSijoitteluajonPistetiedot(sijoitteluajoId:Long): List[PistetietoRecord]
  def getSijoitteluajonPistetiedotInChunks(sijoitteluajoId:Long, chunkSize:Int = 200): List[PistetietoRecord]
  def getSijoitteluajonHakemuksetInChunks(sijoitteluajoId:Long, chunkSize:Int = 300): List[HakemusRecord]
  def getValinnantilanKuvaukset(tilankuvausHashes:List[Int]): Map[Int,TilankuvausRecord]

  def getHakemuksenHakija(hakemusOid:String, sijoitteluajoId:Long): Option[HakijaRecord]
  def getHakemuksenHakutoiveet(hakemusOid:String, sijoitteluajoId:Long): List[HakutoiveRecord]
  def getHakemuksenPistetiedot(hakemusOid:String, sijoitteluajoId:Long): List[PistetietoRecord]

}

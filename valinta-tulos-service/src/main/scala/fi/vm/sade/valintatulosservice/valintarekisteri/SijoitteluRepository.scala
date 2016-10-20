package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.valintatulosservice.domain.{HakijaRecord, SijoitteluWrapper}
import fi.vm.sade.valintatulosservice.domain.{HakijaRecord, HakutoiveRecord, PistetietoRecord, Sijoitteluajo}
import slick.driver.PostgresDriver.backend.Database

trait SijoitteluRepository {
  val db: Database
  def storeSijoitteluajo(sijoitteluajo:SijoitteluAjo): Unit
  def storeSijoittelu(sijoittelu:SijoitteluWrapper)
  def getHakija(hakemusOid:String, sijoitteluajoId:Int): HakijaRecord
  def getHakutoiveet(hakemusOid:String, sijoitteluajoId:Int): List[HakutoiveRecord]
  def getPistetiedot(jonosijaIds:List[Int]): List[PistetietoRecord]
}

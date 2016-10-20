package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import slick.driver.PostgresDriver.backend.Database
import fi.vm.sade.valintatulosservice.domain.{HakijaRecord, SijoitteluWrapper, Sijoitteluajo}

trait SijoitteluRepository {
  val db: Database
  def storeSijoitteluAjo(sijoitteluajo:Sijoitteluajo): Unit
  def getHakija(hakemusOid: String, sijoitteluajoOid: Int): HakijaRecord
  def storeSijoitteluajo(sijoitteluajo:SijoitteluAjo): Unit
  def storeSijoittelu(sijoittelu:SijoitteluWrapper)
}

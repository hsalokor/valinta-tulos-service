package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.valintatulosservice.domain.SijoitteluWrapper
import slick.driver.PostgresDriver.backend.Database

trait SijoitteluRepository {
  val db: Database
  def storeSijoitteluajo(sijoitteluajo:SijoitteluAjo): Unit
  def storeSijoittelu(sijoittelu:SijoitteluWrapper)
}

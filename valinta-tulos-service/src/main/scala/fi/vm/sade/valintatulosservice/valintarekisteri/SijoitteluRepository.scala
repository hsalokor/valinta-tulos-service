package fi.vm.sade.valintatulosservice.valintarekisteri

import slick.driver.PostgresDriver.backend.Database
import fi.vm.sade.valintatulosservice.domain.Sijoitteluajo

trait SijoitteluRepository {
  val db: Database
  def storeSijoitteluAjo(sijoitteluajo:Sijoitteluajo): Unit
}

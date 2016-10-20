package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import slick.driver.PostgresDriver.backend.Database
import fi.vm.sade.valintatulosservice.domain.{HakijaRecord, Sijoitteluajo}

trait SijoitteluRepository {
  val db: Database
  def storeSijoitteluAjo(sijoitteluajo:Sijoitteluajo): Unit
  def getHakija(hakemusOid: String, sijoitteluajoOid: Int): HakijaRecord
}

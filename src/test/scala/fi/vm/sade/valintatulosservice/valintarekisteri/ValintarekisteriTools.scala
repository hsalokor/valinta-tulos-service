package fi.vm.sade.valintatulosservice.valintarekisteri

import slick.driver.PostgresDriver.api._

object ValintarekisteriTools {
  def deleteVastaanotot(db: ValintarekisteriDb): Unit = {
    db.runBlocking(sqlu"delete from vastaanotot")
  }
}

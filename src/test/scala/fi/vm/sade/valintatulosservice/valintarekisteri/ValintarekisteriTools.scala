package fi.vm.sade.valintatulosservice.valintarekisteri

import slick.driver.PostgresDriver.api._

object ValintarekisteriTools {
  private val deleteFromVastaanotot = sqlu"delete from vastaanotot"

  def deleteAll(db: ValintarekisteriDb): Unit = {
    db.runBlocking(DBIO.seq(
      sqlu"delete from deleted_vastaanotot",
      deleteFromVastaanotot,
      sqlu"delete from hakukohteet",
      sqlu"delete from vanhat_vastaanotot"
      ).transactionally)
  }

  def deleteVastaanotot(db: ValintarekisteriDb): Unit = {
    db.runBlocking(deleteFromVastaanotot)
  }
}

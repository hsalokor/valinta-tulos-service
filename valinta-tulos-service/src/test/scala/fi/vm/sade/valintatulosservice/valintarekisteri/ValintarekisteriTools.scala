package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.valintarekisteri.db.ValintarekisteriDb
import slick.driver.PostgresDriver.api._

object ValintarekisteriTools {
  private val deleteFromVastaanotot = DBIO.seq(
    sqlu"delete from vastaanotot",
    sqlu"delete from deleted_vastaanotot where id <> overriden_vastaanotto_deleted_id()",
    sqlu"delete from henkiloviitteet")

  def deleteAll(db: ValintarekisteriDb): Unit = {
    db.runBlocking(DBIO.seq(
      deleteFromVastaanotot,
      sqlu"delete from hakijaryhman_hakemukset",
      sqlu"delete from hakijaryhmat",
      sqlu"delete from ilmoittautumiset",
      sqlu"delete from pistetiedot",
      sqlu"delete from valinnantulokset",
      sqlu"delete from jonosijat",
      sqlu"delete from valintatapajonot",
      sqlu"delete from sijoitteluajon_hakukohteet",
      sqlu"delete from hakukohteet",
      sqlu"delete from sijoitteluajot",
      sqlu"delete from vanhat_vastaanotot"
      ).transactionally)
  }

  def deleteVastaanotot(db: ValintarekisteriDb): Unit = {
    db.runBlocking(deleteFromVastaanotot)
  }
}

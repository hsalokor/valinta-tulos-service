package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date
import java.util.concurrent.TimeUnit

import fi.vm.sade.valintatulosservice.ITSetup
import fi.vm.sade.valintatulosservice.domain.{VastaanotaSitovasti, VastaanottoEvent, VastaanottoRecord}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSpec extends Specification with ITSetup {
  sequential
  private lazy val db = new ValintarekisteriDb(appConfig.settings.valintaRekisteriDbConfig)
  private val henkiloOid = "1.2.246.562.24.00000000001"
  private val hakukohdeOid = "1.2.246.561.20.00000000001"
  private val hakuOid = "1.2.246.561.29.00000000001"

  step(appConfig.start)
  step(Await.ready(valintarekisteriDb.run(DBIOAction.seq(
        sqlu"""insert into hakukohteet ("hakukohdeOid", "hakuOid", kktutkintoonjohtava, koulutuksen_alkamiskausi)
               values ($hakukohdeOid, $hakuOid, true, '2015K')"""
      ).transactionally), Duration(1, TimeUnit.SECONDS))
  )

  "ValintarekisteriDb" should {
    "store vastaanotto actions" in {
      db.store(VastaanottoEvent(henkiloOid, hakukohdeOid, VastaanotaSitovasti))
      val henkiloOidsFromDb = db.run(sql"select henkilo from vastaanotot where henkilo = $henkiloOid and hakukohde = $hakukohdeOid".as[String])
      valintarekisteriDb.run(sqlu"delete from vastaanotot")
      henkiloOidsFromDb must have size 1
      henkiloOidsFromDb.head mustEqual henkiloOid
    }

    "find vastaanotot rows of person for given haku" in {
      db.store(VastaanottoEvent(henkiloOid, hakukohdeOid, VastaanotaSitovasti))
      val vastaanottoRowsFromDb = db.findHenkilonVastaanototHaussa(henkiloOid, hakuOid)
      vastaanottoRowsFromDb must have size 1
      val VastaanottoRecord(henkiloOidFromDb, hakuOidFromDb, hakukohdeOidFromDb, ilmoittajaFromDb, timestampFromDb) = vastaanottoRowsFromDb.head
      henkiloOidFromDb mustEqual henkiloOid
      hakuOidFromDb mustEqual hakuOid
      hakukohdeOidFromDb mustEqual hakukohdeOid
      ilmoittajaFromDb mustEqual henkiloOid
      timestampFromDb.before(new Date()) mustEqual true
    }
  }
}

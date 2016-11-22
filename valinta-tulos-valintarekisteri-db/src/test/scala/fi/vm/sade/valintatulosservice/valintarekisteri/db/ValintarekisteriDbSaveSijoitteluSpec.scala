package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.Timestamp

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.valintatulosservice.valintarekisteri.{ITSetup, ValintarekisteriDbTools}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSaveSijoitteluSpec extends Specification with ITSetup with ValintarekisteriDbTools with BeforeAfterExample {
  sequential
  private val hakuOid = "1.2.246.561.29.00000000001"

  val now = System.currentTimeMillis
  val nowDatetime = new Timestamp(1)

  step(appConfig.start)
  step(deleteAll())

  "ValintarekisteriDb" should {
    "store sijoitteluajoWrapper fixture" in {
      val wrapper = loadSijoitteluFromFixture("hyvaksytty-korkeakoulu-erillishaku")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      assertSijoittelu(wrapper)
    }
    "store sijoitteluajoWrapper fixture with hakijaryhmä and pistetiedot" in {
      val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      assertSijoittelu(wrapper)
    }
  }

  override protected def before: Unit = {
    deleteAll()
  }
  override protected def after: Unit = {
    deleteAll()
  }

  step(deleteAll())
}

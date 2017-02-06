package fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.logging.PerformanceLogger
import fi.vm.sade.valintatulosservice.valintarekisteri.{ITSetup, ValintarekisteriDbTools}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.junit.Ignore
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterExample

@RunWith(classOf[JUnitRunner])
@Ignore
class SijoitteluPerformanceSpec extends Specification with ITSetup with ValintarekisteriDbTools with BeforeAfterExample with Logging with PerformanceLogger {
  sequential
  step(appConfig.start)
  step(deleteAll())

  lazy val valintarekisteri = new ValintarekisteriService(singleConnectionValintarekisteriDb, hakukohdeRecordService)

  "Store and read huge sijoittelu fast" in pending("Use this test only locally for performance tuning") {
    val wrapper = time("create test data") { createHugeSijoittelu(12345l, "11.22.33.44.55.66", 50) }
    time("Store sijoittelu") {singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)}
    time("Get sijoittelu") { valintarekisteri.getSijoitteluajo("11.22.33.44.55.66", "12345") }
    true must beTrue
  }
  "Reading latest huge sijoitteluajo is not timing out" in pending("Use this test only locally for performance tuning") {
    val numberOfSijoitteluajot = 15
    val wrapper = time("create test data") { createHugeSijoittelu(12345l, "11.22.33.44.55.66", 40) }
    (12345l to (12345l + numberOfSijoitteluajot - 1)).foreach(sijoitteluajoId => {
      wrapper.sijoitteluajo.setSijoitteluajoId(sijoitteluajoId)
      wrapper.hakukohteet.foreach(_.setSijoitteluajoId(sijoitteluajoId))
      time(s"Store sijoittelu ${sijoitteluajoId}") {singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)}
    })
    compareSijoitteluWrapperToDTO(
      wrapper,
      time("Get latest sijoitteluajo") { valintarekisteri.getSijoitteluajo("11.22.33.44.55.66", "latest") }
    )
    true must beTrue
  }

  override protected def before: Unit = {
    deleteAll()
  }
  override protected def after: Unit = {
    deleteAll()
  }

  step(deleteAll())
}

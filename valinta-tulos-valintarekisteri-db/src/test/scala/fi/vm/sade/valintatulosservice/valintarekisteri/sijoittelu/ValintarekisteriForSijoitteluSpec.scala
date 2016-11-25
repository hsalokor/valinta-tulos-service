package fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.{PerformanceTimer, ITSetup, ValintarekisteriDbTools}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample

import scala.collection.JavaConverters._
import scala.util.Try


@RunWith(classOf[JUnitRunner])
class ValintarekisteriForSijoitteluSpec extends Specification with ITSetup with ValintarekisteriDbTools with BeforeAfterExample with Logging with PerformanceTimer {
  sequential
  step(appConfig.start)
  step(deleteAll())

  lazy val valintarekisteri = new ValintarekisteriService(singleConnectionValintarekisteriDb, hakukohdeRecordService)

  "SijoitteluajoDTO with sijoitteluajoId should be fetched from database" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
    time("Store sijoittelu") { singleConnectionValintarekisteriDb.storeSijoittelu(wrapper) }
    compareSijoitteluWrapperToDTO(
      wrapper,
      time("Get sijoitteluajo") { valintarekisteri.getSijoitteluajo("1.2.246.562.29.75203638285", "1476936450191") }
    )
  }
  "SijoitteluajoDTO with latest sijoitteluajoId should be fetched from database" in {
    val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot", "sijoittelu/")
    time("Store sijoittelu") { singleConnectionValintarekisteriDb.storeSijoittelu(wrapper) }
    compareSijoitteluWrapperToDTO(
      wrapper,
      time("Get sijoitteluajo") { valintarekisteri.getSijoitteluajo("korkeakoulu-erillishaku", "latest") }
    )
  }
  "Sijoittelu and hakukohteet should be saved in database 1" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/", false)
    time("Tallenna sijoittelu") { valintarekisteri.tallennaSijoittelu(wrapper.sijoitteluajo, wrapper.hakukohteet.asJava, wrapper.valintatulokset.asJava) }
    assertSijoittelu(wrapper)
  }
  "Sijoittelu and hakukohteet should be saved in database 2" in {
    val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot", "sijoittelu/", false)
    time("Tallenna sijoittelu") { valintarekisteri.tallennaSijoittelu(wrapper.sijoitteluajo, wrapper.hakukohteet.asJava, wrapper.valintatulokset.asJava) }
    assertSijoittelu(wrapper)
  }
  "Sijoitteluajo should be stored in transaction" in {
    val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot", "sijoittelu/", false)
    wrapper.hakukohteet(0).getValintatapajonot.get(0).getHakemukset.get(0).setHakemusOid(null)
    Try(valintarekisteri.tallennaSijoittelu(wrapper.sijoitteluajo, wrapper.hakukohteet.asJava, wrapper.valintatulokset.asJava)).toOption mustEqual None
    findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId) mustEqual None
  }
  "Unknown sijoitteluajo cannot be found" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
    singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
    valintarekisteri.getSijoitteluajo("1.2.246.562.29.75203638285", "1476936450192") must throwA(
      new IllegalArgumentException(s"Sijoitteluajoa 1476936450192 ei löytynyt haulle 1.2.246.562.29.75203638285"))
  }
  "Exception is thrown when no latest sijoitteluajo is found" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
    singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
    valintarekisteri.getSijoitteluajo("1.2.246.562.29.75203638286", "latest") must throwA(
      new IllegalArgumentException("Yhtään sijoitteluajoa ei löytynyt haulle 1.2.246.562.29.75203638286"))
  }
  "Exception is thrown when sijoitteluajoId is malformed" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
    singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
    valintarekisteri.getSijoitteluajo("1.2.246.562.29.75203638285", "1476936450192a") must throwA(
      new IllegalArgumentException("Väärän tyyppinen sijoitteluajon ID: 1476936450192a"))
  }

  override protected def before: Unit = {
    deleteAll()
  }
  override protected def after: Unit = {
    deleteAll()
  }

  step(deleteAll())
}

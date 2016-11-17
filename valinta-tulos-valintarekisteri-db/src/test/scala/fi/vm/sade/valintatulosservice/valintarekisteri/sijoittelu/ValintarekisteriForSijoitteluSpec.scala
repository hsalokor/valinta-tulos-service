package fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu

import fi.vm.sade.sijoittelu.domain.{Hakemus => SijoitteluHakemus}
import fi.vm.sade.sijoittelu.tulos.dto.{HakukohdeDTO, SijoitteluajoDTO}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.SijoitteluWrapper
import fi.vm.sade.valintatulosservice.valintarekisteri.{ITSetup, ValintarekisteriDbTools}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample
import scala.collection.JavaConverters._


@RunWith(classOf[JUnitRunner])
class ValintarekisteriForSijoitteluSpec extends Specification with ITSetup with ValintarekisteriDbTools with BeforeAfterExample {
  sequential
  step(appConfig.start)
  step(deleteAll())

  lazy val valintarekisteri = new ValintarekisteriService(singleConnectionValintarekisteriDb, hakukohdeRecordService)

  "SijoitteluajoDTO should be fetched from database 1" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
    singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
    compareSijoitteluWrapperToDTO(
      wrapper,
      valintarekisteri.getSijoitteluajo("1.2.246.562.29.75203638285", "1476936450191")
    )
  }
  "SijoitteluajoDTO should be fetched from database 2" in {
    val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot", "sijoittelu/")
    singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
    compareSijoitteluWrapperToDTO(
      wrapper,
      valintarekisteri.getSijoitteluajo("korkeakoulu-erillishaku", "1464957466474")
    )
  }
  "Sijoittelu and hakukohteet should be saved in database 1" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/", false)
    valintarekisteri.tallennaSijoittelu(wrapper.sijoitteluajo, wrapper.hakukohteet.asJava, wrapper.valintatulokset.asJava)
    assertSijoittelu(wrapper)
  }
  "Sijoittelu and hakukohteet should be saved in database 2" in {
    val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot", "sijoittelu/", false)
    valintarekisteri.tallennaSijoittelu(wrapper.sijoitteluajo, wrapper.hakukohteet.asJava, wrapper.valintatulokset.asJava)
    assertSijoittelu(wrapper)
  }

  override protected def before: Unit = {
    deleteAll()
  }
  override protected def after: Unit = {
    deleteAll()
  }

  step(deleteAll())
}

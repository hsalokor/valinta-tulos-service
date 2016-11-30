package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.Timestamp
import java.text.SimpleDateFormat

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.valintarekisteri.{ITSetup, ValintarekisteriDbTools}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSaveSijoitteluSpec extends Specification with ITSetup with ValintarekisteriDbTools with BeforeAfterExample with Logging {
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
    "store tilan_viimeisin_muutos correctly" in {
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

      def assertTilanViimeisinMuutos(hakemusOid:String, expected:java.util.Date) = {
        val muutokset = findTilanViimeisinMuutos(hakemusOid)
        muutokset.size mustEqual 1
        logger.info(s"Got date:${dateFormat.format(new java.util.Date(muutokset.head.getTime))}")
        logger.info(s"Expecting date:${dateFormat.format(expected)}")
        muutokset.head.getTime mustEqual expected.getTime
      }

      val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      assertSijoittelu(wrapper)
      assertTilanViimeisinMuutos("1.2.246.562.11.00006465296", dateFormat.parse("2016-10-17T09:08:11.967+0000"))
      assertTilanViimeisinMuutos("1.2.246.562.11.00004685599", dateFormat.parse("2016-10-12T04:11:20.526+0000"))
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

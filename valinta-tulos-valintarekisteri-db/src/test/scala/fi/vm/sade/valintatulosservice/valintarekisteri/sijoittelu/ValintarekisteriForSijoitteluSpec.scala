package fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu

import fi.vm.sade.sijoittelu.domain.{Hakemus => SijoitteluHakemus}
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

  "SijoitteluajoDTO should be fetched from database" in {
    singleConnectionValintarekisteriDb.storeSijoittelu(loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/"))
    val sijoitteluAjo = valintarekisteri.getSijoitteluajo("1.2.246.562.29.75203638285","1476936450191")
    sijoitteluAjo.getHakukohteet.size mustEqual 3
    val hakukohteet = sijoitteluAjo.getHakukohteet.asScala.toList
    val hakukohde1 = hakukohteet.filter(hk => hk.getOid.equals("1.2.246.562.20.26643418986")).head
    hakukohde1.getHakijaryhmat.size mustEqual 2
    hakukohde1.getValintatapajonot.get(0).getHakemukset.size mustEqual 15
    val hakukohde2 = hakukohteet.filter(hk => hk.getOid.equals("1.2.246.562.20.56217166919")).head
    hakukohde2.getValintatapajonot.get(0).getOid mustEqual "14539780970882907815262745035155"
  }
  "Sijoittelu and hakukohteet should be saved in database" in {
    val wrapper = loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/", false)
    valintarekisteri.tallennaSijoittelu(wrapper.sijoitteluajo, wrapper.hakukohteet.asJava, wrapper.valintatulokset.asJava)
    assertSijoittelu(wrapper)
  }
  "Sijoittelu and hakukohteet should be saved in database" in {
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

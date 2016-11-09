package fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu

import fi.vm.sade.sijoittelu.domain.{Hakemus => SijoitteluHakemus}
import fi.vm.sade.valintatulosservice.valintarekisteri.{ITSetup, ValintarekisteriDbTools}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class ValintarekisteriForSijoitteluTest extends Specification with ITSetup with ValintarekisteriDbTools {
  sequential
  step(appConfig.start)
  step(deleteAll())
  step(singleConnectionValintarekisteriDb.storeSijoittelu(loadSijoitteluFromFixture("haku-1.2.246.562.29.75203638285", "QA-import/")))

  lazy val valintarekisteri = new ValintarekisteriForSijoittelu(singleConnectionValintarekisteriDb)

  "SijoitteluajoDTO should be fetched from database" in {
    val sijoitteluAjo = valintarekisteri.getSijoitteluajo("1.2.246.562.29.75203638285","1476936450191")
    sijoitteluAjo.getHakukohteet.size mustEqual 3
    sijoitteluAjo.getHakukohteet.get(0).getHakijaryhmat.size mustEqual 2
    sijoitteluAjo.getHakukohteet.get(0).getValintatapajonot.get(0).getHakemukset.size mustEqual 15
    sijoitteluAjo.getHakukohteet.get(1).getValintatapajonot.get(0).getOid mustEqual "14539780970882907815262745035155"
  }

  step(deleteAll())
}

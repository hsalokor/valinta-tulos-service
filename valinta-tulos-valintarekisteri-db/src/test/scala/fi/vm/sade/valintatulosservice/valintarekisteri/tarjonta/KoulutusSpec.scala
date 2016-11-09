package fi.vm.sade.valintatulosservice.valintarekisteri.tarjonta

import fi.vm.sade.valintatulosservice.koodisto.{Koodi, KoodiUri, KoodistoService, Relaatiot}
import fi.vm.sade.valintatulosservice.tarjonta.Koulutus
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.Kausi
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class KoulutusSpec extends Specification {
  private val koulutuskoodiIlmanRelaatioita =
    Koodi(KoodiUri("koulutus_000000"), 1, None)
  private val koulutuskoodiIlmanTutkintoonjohtavuusrelaatiota =
    Koodi(KoodiUri("koulutus_000000"), 1, Some(Relaatiot(Nil, Nil, Nil)))
  private val eiJohdaTutkintoonKoulutuskoodi =
    Koodi(KoodiUri("koulutus_000000"), 1, Some(Relaatiot(Nil, Nil, List(Koodi(KoodiUri("tutkintoonjohtavakoulutus_0"), 1, None)))))
  private val johtaaTutkintoonKoulutuskoodi =
    Koodi(KoodiUri("koulutus_000000"), 1, Some(Relaatiot(Nil, Nil, List(Koodi(KoodistoService.OnTutkinto, 1, None)))))
  private val k = Koulutus("1.2.3.4.", Kausi("2016S"), "Luonnos", Some(koulutuskoodiIlmanRelaatioita))

  "Koulutus" should {
    "päätellä tutkintotiedosta, johtaako se tutkintoon" in {
      k.johtaaTutkintoon must throwAn[IllegalStateException]
      k.copy(koulutusKoodi = Some(koulutuskoodiIlmanTutkintoonjohtavuusrelaatiota)).johtaaTutkintoon must throwAn[IllegalStateException]
      k.copy(koulutusKoodi = Some(eiJohdaTutkintoonKoulutuskoodi)).johtaaTutkintoon must beFalse
      k.copy(koulutusKoodi = Some(johtaaTutkintoonKoulutuskoodi)).johtaaTutkintoon must beTrue
      k.copy(koulutusKoodi = None).johtaaTutkintoon must beFalse
    }
  }
}

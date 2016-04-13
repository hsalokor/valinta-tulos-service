package fi.vm.sade.valintatulosservice.tarjonta

import fi.vm.sade.valintatulosservice.domain.Kausi
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class KoulutusSpec extends Specification {
  private val k = Koulutus("1.2.3.4.", Kausi("2016S"), "Luonnos", None)

  "Koulutus" should {
    "päätellä tutkintotiedosta, johtaako se tutkintoon" in {
      k.copy(tutkinto = None).johtaaTutkintoon must_== false
      k.copy(tutkinto = Some(HakuService.EiJohdaTutkintoon)).johtaaTutkintoon must_== false
      // tutkinto_338
      k.copy(tutkinto = Some("tutkinto_338")).johtaaTutkintoon must_== true
    }
  }
}

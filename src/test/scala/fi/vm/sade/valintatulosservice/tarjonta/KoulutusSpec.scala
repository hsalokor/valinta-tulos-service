package fi.vm.sade.valintatulosservice.tarjonta

import fi.vm.sade.valintatulosservice.domain.Kausi
import fi.vm.sade.valintatulosservice.koodisto.{KoodistoService, Relaatiot, Koodi, KoodiUri}
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class KoulutusSpec extends Specification {
  private val tutkinnotonKoulutusKoodi = Koodi(KoodiUri("koulutus_000000"), 1, None)
  private val eiTutkintoaKoulutusKoodi = Koodi(KoodiUri("koulutus_000000"), 1, Some(Relaatiot(Nil, Nil, List(Koodi(KoodistoService.EiTutkintoa, 1, None)))))
  private val onTutkintoKoulutusKoodi = Koodi(KoodiUri("koulutus_000000"), 1, Some(Relaatiot(Nil, Nil, List(Koodi(KoodiUri("tutkinto_338"), 1, None)))))
  private val k = Koulutus("1.2.3.4.", Kausi("2016S"), "Luonnos", tutkinnotonKoulutusKoodi)

  "Koulutus" should {
    "päätellä tutkintotiedosta, johtaako se tutkintoon" in {
      k.johtaaTutkintoon must_== false
      k.copy(koulutusKoodi = eiTutkintoaKoulutusKoodi).johtaaTutkintoon must_== false
      k.copy(koulutusKoodi = onTutkintoKoulutusKoodi).johtaaTutkintoon must_== true
    }
  }
}

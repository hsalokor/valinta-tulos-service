package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.{IlmoittautumistilaKonvertteri, ValintatulosKonvertteri}
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IlmoittautumistilaKonvertteri extends Specification {
  val vastaanottanut = HakutoiveenSijoitteluntulos.kesken("","").copy(vastaanottotila = Vastaanottotila.vastaanottanut)
  "Ilmoittautuminen" should {
    "should be enabled in IT" in {
      implicit val appConfig = new AppConfig.IT
      val it = IlmoittautumistilaKonvertteri.getIlmoittautumistila(vastaanottanut, Haku("", true, true, true, None, Set(), Nil), None)
      it.ilmoittauduttavissa must_== true
    }

    "should be disabled by default" in {
      implicit val appConfig = new AppConfig.IT_disabledIlmoittautuminen
      val it = IlmoittautumistilaKonvertteri.getIlmoittautumistila(vastaanottanut, Haku("", true, true, true, None, Set(), Nil), None)
      it.ilmoittauduttavissa must_== false
    }
  }
}

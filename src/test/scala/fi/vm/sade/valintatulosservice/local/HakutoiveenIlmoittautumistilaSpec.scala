package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import org.specs2.mutable.Specification

class HakutoiveenIlmoittautumistilaSpec extends Specification {
  val vastaanottanut = HakutoiveenSijoitteluntulos.kesken("","").copy(vastaanottotila = Vastaanottotila.vastaanottanut)
  "Ilmoittautiminen" should {
    "should be enabled in IT" in {
      implicit val appConfig = new AppConfig.IT
      val it = HakutoiveenIlmoittautumistila.getIlmoittautumistila(vastaanottanut, Haku("", true, true, true, None, Set(), Nil), None)
      it.ilmoittauduttavissa must_== true
    }

    "should be disabled by default" in {
      implicit val appConfig = new AppConfig.IT_disabledIlmoittautuminen
      val it = HakutoiveenIlmoittautumistila.getIlmoittautumistila(vastaanottanut, Haku("", true, true, true, None, Set(), Nil), None)
      it.ilmoittauduttavissa must_== false
    }
  }
}

package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ohjausparametrit.Ohjausparametrit
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import org.joda.time.DateTime

object IlmoittautumistilaKonvertteri {
  def getIlmoittautumistila(sijoitteluTila: HakutoiveenSijoitteluntulos, haku: Haku, ohjausparametrit: Option[Ohjausparametrit])(implicit appConfig: AppConfig): HakutoiveenIlmoittautumistila = {
    val ilmoittautumistapa = if(haku.korkeakoulu) {
      Some(Oili)
    }
    else {
      None
    }
    val ilmottautumisaika = Ilmoittautumisaika(None, ohjausparametrit.flatMap(_.ilmoittautuminenPaattyy.map(new DateTime(_).withTime(23,59,59,999))))
    val ilmottauduttavissa = appConfig.settings.ilmoittautuminenEnabled && sijoitteluTila.vastaanottotila == Vastaanottotila.vastaanottanut && ilmottautumisaika.aktiivinen && sijoitteluTila.ilmoittautumistila == Ilmoittautumistila.ei_tehty
    HakutoiveenIlmoittautumistila(ilmottautumisaika, ilmoittautumistapa, sijoitteluTila.ilmoittautumistila, ilmottauduttavissa)
  }
}

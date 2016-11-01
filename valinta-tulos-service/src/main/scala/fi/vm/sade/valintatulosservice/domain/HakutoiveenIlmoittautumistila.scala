package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila
import fi.vm.sade.valintatulosservice.domain.LanguageMap.LanguageMap
import fi.vm.sade.valintatulosservice.ohjausparametrit.Ohjausparametrit
import fi.vm.sade.valintatulosservice.tarjonta.Haku
import org.joda.time.DateTime

case class HakutoiveenIlmoittautumistila(
  ilmoittautumisaika: Ilmoittautumisaika,
  ilmoittautumistapa: Option[Ilmoittautumistapa],
  ilmoittautumistila: Ilmoittautumistila,
  ilmoittauduttavissa: Boolean
)

case class Ilmoittautumisaika(alku: Option[DateTime], loppu: Option[DateTime]) {
  def aktiivinen = {
    val now = new DateTime
    now.isAfter(alku.getOrElse(now.minusYears(100))) &&
    now.isBefore(loppu.getOrElse(now.plusYears(100)))
  }
}

sealed trait Ilmoittautumistapa {}

case class UlkoinenJärjestelmä(nimi: LanguageMap, url: String) extends Ilmoittautumistapa

object HakutoiveenIlmoittautumistila {

  val oili = UlkoinenJärjestelmä(Map(Language.fi -> "Oili", Language.sv -> "Oili", Language.en -> "Oili"), "/oili/")

  def getIlmoittautumistila(sijoitteluTila: HakutoiveenSijoitteluntulos, haku: Haku, ohjausparametrit: Option[Ohjausparametrit])(implicit appConfig: AppConfig): HakutoiveenIlmoittautumistila = {
    val ilmoittautumistapa = if(haku.korkeakoulu) {
      Some(oili)
    }
    else {
      None
    }
    val ilmottautumisaika = Ilmoittautumisaika(None, ohjausparametrit.flatMap(_.ilmoittautuminenPaattyy.map(new DateTime(_).withTime(23,59,59,999))))
    val ilmottauduttavissa = appConfig.settings.ilmoittautuminenEnabled && sijoitteluTila.vastaanottotila == Vastaanottotila.vastaanottanut && ilmottautumisaika.aktiivinen && sijoitteluTila.ilmoittautumistila == Ilmoittautumistila.ei_tehty
    HakutoiveenIlmoittautumistila(ilmottautumisaika, ilmoittautumistapa, sijoitteluTila.ilmoittautumistila, ilmottauduttavissa)
  }
}
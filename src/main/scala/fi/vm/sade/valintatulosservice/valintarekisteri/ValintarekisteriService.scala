package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.{StubbedExternalDeps, AppConfig}
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{Ensikertalaisuus, EiEnsikertalainen, Ensikertalainen}

trait ValintarekisteriService {
  def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus
}

object ValintarekisteriService {
  def apply(appConfig: AppConfig): ValintarekisteriService = appConfig match {
    case _: StubbedExternalDeps => ValintarekisteriServiceMock
    case _ => new ValintarekisteriServiceImpl(appConfig)
  }
}

object ValintarekisteriServiceMock extends ValintarekisteriService {
  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus = personOid match {
    case person@"1.2.246.561.24.00000000001" => EiEnsikertalainen(person, new Date(koulutuksenAlkamispvm.getTime + 10000))
    case person => Ensikertalainen(person)
  }
}

class ValintarekisteriServiceImpl(appConfig: AppConfig) extends ValintarekisteriService with Logging {
  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus = ???
}


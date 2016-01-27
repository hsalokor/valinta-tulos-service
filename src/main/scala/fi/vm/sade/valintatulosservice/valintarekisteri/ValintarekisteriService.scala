package fi.vm.sade.valintatulosservice.valintarekisteri

import java.util.Date

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.{StubbedExternalDeps, AppConfig}
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{Ensikertalaisuus, EiEnsikertalainen, Ensikertalainen}

trait ValintarekisteriService {
  def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus
  def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamispvm: Date): Set[Ensikertalaisuus]
}

object ValintarekisteriService {
  def apply(appConfig: AppConfig): ValintarekisteriService = appConfig match {
    case _: StubbedExternalDeps => ValintarekisteriServiceMock
    case _ => new ValintarekisteriServiceImpl(appConfig)
  }
}

object ValintarekisteriServiceMock extends ValintarekisteriService {
  private def findOne(koulutuksenAlkamispvm: Date)(personOid: String): Ensikertalaisuus = personOid match {
    case person@"1.2.246.561.24.00000000001" => EiEnsikertalainen(person, new Date(koulutuksenAlkamispvm.getTime + 10000))
    case person => Ensikertalainen(person)
  }

  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus =
    findOne(koulutuksenAlkamispvm)(personOid)

  override def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamispvm: Date): Set[Ensikertalaisuus] =
    personOids.map(findOne(koulutuksenAlkamispvm))
}

class ValintarekisteriServiceImpl(appConfig: AppConfig) extends ValintarekisteriService with Logging {
  override def findEnsikertalaisuus(personOid: String, koulutuksenAlkamispvm: Date): Ensikertalaisuus = ???

  override def findEnsikertalaisuus(personOids: Set[String], koulutuksenAlkamispvm: Date): Set[Ensikertalaisuus] = ???
}


package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.{AppConfig, StubbedExternalDeps}
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluClient

trait ValintatulosService {
  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos]
}

object ValintatulosService {
  def apply(implicit appConfig: AppConfig) = appConfig match {
    case c: StubbedExternalDeps => new MockValintatulosService()
    case c => SijoitteluClient()
  }
}

class MockValintatulosService extends ValintatulosService {
  override def hakemuksentulos(hakuOid: String, hakemusOid: String) = {
    Some(Hakemuksentulos(hakemusOid, List(Hakutoiveentulos(
      "2.3.4.5", "3.4.5.6", "HYVAKSYTTY", Some("ILMOITETTU"), None, "EI_VASTAANOTETTAVISSA", Some(1), None
    ))))
  }
}
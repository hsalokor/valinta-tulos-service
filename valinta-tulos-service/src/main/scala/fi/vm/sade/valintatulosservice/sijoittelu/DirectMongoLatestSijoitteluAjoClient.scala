package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig

/**
  * For testing _only_. Goes directly to raportointiservice without invoking sijoittelu-service REST API.
  */
class DirectMongoLatestSijoitteluAjoClient(appConfig: AppConfig) extends LatestSijoitteluAjoClient(appConfig) {
  private val raportointiService = appConfig.sijoitteluContext.raportointiService

  override def fetchLatestSijoitteluAjoFromSijoitteluService(hakuOid: String, hakukohdeOid: Option[String]): Option[SijoitteluAjo] = {
    hakukohdeOid match {
      case Some(oid) => fromOptional(raportointiService.cachedLatestSijoitteluAjoForHakukohde(hakuOid, oid))
      case None => fromOptional(raportointiService.cachedLatestSijoitteluAjoForHaku(hakuOid))
    }
  }


  def fromOptional[T](opt: Optional[T]) = {
    if (opt.isPresent) {
      Some(opt.get)
    } else {
      None
    }
  }
}

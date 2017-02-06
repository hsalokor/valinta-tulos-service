package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.valintatulosservice.config.VtsAppConfig.VtsAppConfig

/**
  * For testing _only_. Goes directly to raportointiservice without invoking sijoittelu-service REST API.
  */
class DirectMongoSijoittelunTulosRestClient(appConfig: VtsAppConfig) extends SijoittelunTulosRestClient(appConfig) {
  private val raportointiService = appConfig.sijoitteluContext.raportointiService

  override def fetchLatestSijoitteluAjoFromSijoitteluService(hakuOid: String, hakukohdeOid: Option[String]): Option[SijoitteluAjo] = {
    hakukohdeOid match {
      case Some(oid) => fromOptional(raportointiService.latestSijoitteluAjoForHakukohde(hakuOid, oid))
      case None => fromOptional(raportointiService.latestSijoitteluAjoForHaku(hakuOid))
    }
  }


  override def fetchHakemuksenTulos(sijoitteluAjo: SijoitteluAjo, hakemusOid: String) = {
    Option(raportointiService.hakemus(sijoitteluAjo.getHakuOid, sijoitteluAjo.getSijoitteluajoId.toString, hakemusOid))
  }

  def fromOptional[T](opt: Optional[T]) = {
    if (opt.isPresent) {
      Some(opt.get)
    } else {
      None
    }
  }
}

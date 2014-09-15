package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoitteluSpringContext, SijoitteluClient}

class ValintatulosService(sijoitteluSpringContext: SijoitteluSpringContext, hakemusRepository: HakemusRepository) {

  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    val sijoitteluTulos: Hakemuksentulos = sijoitteluSpringContext.sijoitteluClient.sijoittelunTulos(hakuOid, hakemusOid).getOrElse(Hakemuksentulos(hakemusOid, Nil))
    val hakemus: Option[Hakemus] = hakemusRepository.findHakutoiveOids(hakemusOid)

    hakemus.map { h =>
      val tulokset = h.toiveet.map { toive =>
        sijoitteluTulos.hakutoiveet.find { t =>
          t.hakukohdeOid == toive.oid
        }.getOrElse(createKesken(toive.oid, toive.tarjoajaOid))
      }
      Hakemuksentulos(h.oid, tulokset)
    }
  }

  private def createKesken(hakukohdeOid: String, tarjoajaOid: String) = {
    Hakutoiveentulos(
      hakukohdeOid,
      tarjoajaOid,
      Valintatila.kesken,
      Vastaanottotila.kesken,
      Ilmoittautumistila.ei_tehty,
      Vastaanotettavuustila.ei_vastaanottavissa,
      None,
      None,
      None,
      None)
  }
}




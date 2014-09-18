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
      Hakemuksentulos(h.oid, käsitteleKeskeneräiset(tulokset))
    }
  }

  private def käsitteleKeskeneräiset(tulokset: List[Hakutoiveentulos]) = {
    val firstHyvaksytty = tulokset.indexWhere(_.valintatila == Valintatila.hyväksytty)
    val firstKesken = tulokset.indexWhere(_.valintatila == Valintatila.kesken)
    tulokset.zipWithIndex.map {
      case (tulos, index) => {
        if(firstHyvaksytty > -1 && index > firstHyvaksytty && tulos.valintatila == Valintatila.kesken) {
          // hyväksyttyä myöhemmät "kesken" -hakutoiveet peruuntuvat
          tulos.copy(valintatila = Valintatila.peruuntunut)
        } else if(firstKesken > -1 && index > firstKesken && tulos.valintatila == Valintatila.hyväksytty) {
          // "kesken" tulosta alemmat "hyväksytty" -hakutoiveet merkitään keskeneräisiksi
          tulos.copy(valintatila = Valintatila.kesken, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanottavissa)
        } else {
          tulos
        }
      }
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




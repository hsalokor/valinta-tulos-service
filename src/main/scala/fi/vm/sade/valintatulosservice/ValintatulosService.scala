package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluSpringContext

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
      Hakemuksentulos(h.oid, kasitteleKeskenEraiset(tulokset))
    }
  }

  private def kasitteleKeskenEraiset(tulokset: List[Hakutoiveentulos]) = {
    val firstFinished = tulokset.indexWhere { t =>
      List(Valintatila.hyväksytty, Valintatila.varasijalta_hyväksytty, Valintatila.perunut, Valintatila.peruutettu, Valintatila.peruuntunut).contains(t.valintatila)
    }
    val alemmatKeskeneraisetPeruttu = tulokset.zipWithIndex.map {
      case (tulos, index) if(firstFinished > -1 && index > firstFinished && tulos.valintatila == Valintatila.kesken) =>
        tulos.copy(valintatila = Valintatila.peruuntunut)
      case (tulos, _) => tulos
    }
    val firstKesken = alemmatKeskeneraisetPeruttu.indexWhere(_.valintatila == Valintatila.kesken)
    alemmatKeskeneraisetPeruttu.zipWithIndex.map {
      case (tulos, index) if(firstKesken > -1 && index > firstKesken && tulos.valintatila == Valintatila.hyväksytty) =>
        tulos.copy(valintatila = Valintatila.kesken, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanottavissa)
      case (tulos, _) => tulos
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
      None,
      None)
  }
}




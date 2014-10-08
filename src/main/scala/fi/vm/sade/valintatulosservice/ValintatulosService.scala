package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoittelutulosService, SijoitteluSpringContext}
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritService

class ValintatulosService(sijoittelutulosService: SijoittelutulosService, ohjausparametritService: OhjausparametritService, hakemusRepository: HakemusRepository) {

  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    val aikataulu = ohjausparametritService.aikataulu(hakuOid)
    val sijoitteluTulos: Hakemuksentulos = sijoittelutulosService.hakemuksenTulos(hakuOid, hakemusOid)
      .getOrElse(tyhjäHakemuksenTulos(hakemusOid, aikataulu))
      .julkaistavaVersio
    val hakemus: Option[Hakemus] = hakemusRepository.findHakutoiveOids(hakemusOid)

    hakemus.map { h =>
      val tulokset = h.toiveet.map { toive =>
        sijoitteluTulos.hakutoiveet.find { t =>
          t.hakukohdeOid == toive.oid
        }.getOrElse(Hakutoiveentulos.kesken(toive.oid, toive.tarjoajaOid))
      }
      Hakemuksentulos(h.oid, aikataulu, kasitteleKeskenEraiset(tulokset))
    }
  }

  private def tyhjäHakemuksenTulos(hakemusOid: String, aikataulu: Option[Vastaanottoaikataulu]) = Hakemuksentulos(hakemusOid, aikataulu, Nil)

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
        tulos.copy(valintatila = Valintatila.kesken, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
      case (tulos, _) => tulos
    }
  }
}




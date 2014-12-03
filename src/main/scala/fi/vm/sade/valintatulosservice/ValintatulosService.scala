package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import fi.vm.sade.valintatulosservice.ohjausparametrit.{Ohjausparametrit, OhjausparametritService}
import fi.vm.sade.valintatulosservice.sijoittelu.SijoittelutulosService
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService}
import org.joda.time.DateTime

class ValintatulosService(sijoittelutulosService: SijoittelutulosService, ohjausparametritService: OhjausparametritService, hakemusRepository: HakemusRepository, hakuService: HakuService)(implicit appConfig: AppConfig) {
  def this(hakuService: HakuService)(implicit appConfig: AppConfig) = this(appConfig.sijoitteluContext.sijoittelutulosService, appConfig.ohjausparametritService, new HakemusRepository(), hakuService)

  def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    hakuService.getHaku(hakuOid).flatMap { haku =>
      val ohjausparametrit = ohjausparametritService.ohjausparametrit(hakuOid)
      val sijoitteluTulos: HakemuksenSijoitteluntulos = sijoittelutulosService.hakemuksenTulos(haku, hakemusOid)
        .getOrElse(tyhjäHakemuksenTulos(hakemusOid, ohjausparametrit.flatMap(_.vastaanottoaikataulu)))

      val hakemus: Option[Hakemus] = hakemusRepository.findHakemus(hakemusOid)
      hakemus.map(julkaistavaTulos(sijoitteluTulos, haku, ohjausparametrit))
    }
  }

  def hakemuksentuloksetByPerson(hakuOid: String, personOid: String): List[Hakemuksentulos] = {
    hakuService.getHaku(hakuOid).map{ haku =>
      for {
        hakemus <- hakemusRepository.findHakemukset(hakuOid, personOid)
      } yield {
        val ohjausparametrit = ohjausparametritService.ohjausparametrit(hakuOid)
        val sijoitteluTulos: HakemuksenSijoitteluntulos = sijoittelutulosService.hakemuksenTulos(haku, hakemus.oid)
          .getOrElse(tyhjäHakemuksenTulos(hakemus.oid, ohjausparametrit.flatMap(_.vastaanottoaikataulu)))
        julkaistavaTulos(sijoitteluTulos, haku, ohjausparametrit)(hakemus)
      }
    }.getOrElse(List())
  }

  def hakemustenTulos(hakuOid: String): Option[Seq[Hakemuksentulos]] = {
    for (
      haku <- hakuService.getHaku(hakuOid)
    ) yield {
      val ohjausparametrit = ohjausparametritService.ohjausparametrit(hakuOid)
      val sijoitteluTulokset = sijoittelutulosService.hakemustenTulos(haku).groupBy(_.hakemusOid).mapValues(_.head)
      for (
        hakemus: Hakemus <- hakemusRepository.findHakemukset(hakuOid)
      ) yield {
        val sijoitteluTulos = sijoitteluTulokset.getOrElse(hakemus.oid, tyhjäHakemuksenTulos(hakemus.oid, ohjausparametrit.flatMap(_.vastaanottoaikataulu)))
        julkaistavaTulos(sijoitteluTulos, haku, ohjausparametrit)(hakemus)
      }
    }
  }

  private def julkaistavaTulos(sijoitteluTulos: HakemuksenSijoitteluntulos, haku: Haku, ohjausparametrit: Option[Ohjausparametrit])(h:Hakemus)(implicit appConfig: AppConfig) = {
    val tulokset = h.toiveet.map { toive =>
      sijoitteluTulos.hakutoiveet.find { t =>
        t.hakukohdeOid == toive.oid
      }.getOrElse(HakutoiveenSijoitteluntulos.kesken(toive.oid, toive.tarjoajaOid))
    }.map(Hakutoiveentulos.julkaistavaVersio(_, haku, ohjausparametrit))

    val lopullisetTulokset = Välitulos(tulokset, haku, ohjausparametrit)
      .map(peruValmistaAlemmatKeskeneräiset)
      .map(korkeakouluSpecial)
      .tulokset

    Hakemuksentulos(haku.oid, h.oid, sijoitteluTulos.hakijaOid, ohjausparametrit.flatMap(_.vastaanottoaikataulu), lopullisetTulokset)
  }

  private def tyhjäHakemuksenTulos(hakemusOid: String, aikataulu: Option[Vastaanottoaikataulu]) = HakemuksenSijoitteluntulos(hakemusOid, "", Nil)

  private def korkeakouluSpecial(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    val ehdollinenVastaanottoMahdollista: Boolean = {
      ohjausparametrit.getOrElse(Ohjausparametrit(None, None, None, None)).varasijaSaannotAstuvatVoimaan match {
        case None => true
        case Some(varasijaSaannotAstuvatVoimaan) => varasijaSaannotAstuvatVoimaan.isBefore(new DateTime())
      }
    }

    if (haku.korkeakoulu && haku.yhteishaku) {
      val firstVaralla = tulokset.indexWhere(_.valintatila == Valintatila.varalla)
      val firstVastaanotettu = tulokset.indexWhere(_.vastaanottotila == Vastaanottotila.vastaanottanut)
      val firstKesken = tulokset.indexWhere(_.valintatila == Valintatila.kesken)

      tulokset.zipWithIndex.map {
        case (tulos, index) if (Valintatila.isHyväksytty(tulos.valintatila) && tulos.vastaanottotila == Vastaanottotila.kesken) =>
          if (firstVastaanotettu >= 0 && index != firstVastaanotettu)
            // Peru vastaanotettua paikkaa alemmat hyväksytyt hakutoiveet
            tulos.copy(valintatila = Valintatila.peruuntunut, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
          else if (firstVaralla >= 0 && index > firstVaralla) {
           if(ehdollinenVastaanottoMahdollista)
            // Ehdollinen vastaanotto mahdollista
            tulos.copy(vastaanotettavuustila = Vastaanotettavuustila.vastaanotettavissa_ehdollisesti)
           else
            tulos.copy(vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
          }
          else if (firstKesken >= 0 && index > firstKesken)
            tulos.copy(valintatila = Valintatila.kesken, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
          else
            tulos
        case (tulos, index) if (firstVastaanotettu >= 0 && index != firstVastaanotettu && List(Valintatila.varalla, Valintatila.kesken).contains(tulos.valintatila)) =>
          // Peru muut varalla/kesken toiveet, jos jokin muu vastaanotettu
          tulos.copy(valintatila = Valintatila.peruuntunut, vastaanotettavuustila = Vastaanotettavuustila.ei_vastaanotettavissa)
        case (tulos, _) => tulos
      }
    } else {
      tulokset
    }
  }

  private def peruValmistaAlemmatKeskeneräiset(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) = {
    if (haku.käyttääSijoittelua) {
      val firstFinished = tulokset.indexWhere { t =>
        Valintatila.isHyväksytty(t.valintatila) || List(Valintatila.perunut, Valintatila.peruutettu, Valintatila.peruuntunut).contains(t.valintatila)
      }

      tulokset.zipWithIndex.map {
        case (tulos, index) if (haku.käyttääSijoittelua && firstFinished > -1 && index > firstFinished && tulos.valintatila == Valintatila.kesken) =>
          tulos.copy(valintatila = Valintatila.peruuntunut)
        case (tulos, _) => tulos
      }
    } else {
      tulokset
    }
  }

  case class Välitulos(tulokset: List[Hakutoiveentulos], haku: Haku, ohjausparametrit: Option[Ohjausparametrit]) {
    def map(f: (List[Hakutoiveentulos], Haku, Option[Ohjausparametrit]) => List[Hakutoiveentulos]) = {
      Välitulos(f(tulokset, haku, ohjausparametrit), haku, ohjausparametrit)
    }
  }
}




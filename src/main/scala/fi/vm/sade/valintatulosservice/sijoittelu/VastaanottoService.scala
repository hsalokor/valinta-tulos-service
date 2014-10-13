package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date

import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.{EHDOLLISESTI_VASTAANOTTANUT, PERUNUT, VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI}
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import fi.vm.sade.valintatulosservice.domain.{Ilmoittautuminen, Vastaanotettavuustila, Vastaanotto}

class VastaanottoService(yhteenvetoService: YhteenvetoService, dao: ValintatulosDao) {
  def vastaanota(hakuOid: String, hakemusOid: String, vastaanotto: Vastaanotto) {
    vastaanota(hakuOid, hakemusOid, vastaanotto.hakukohdeOid, ValintatuloksenTila.valueOf(vastaanotto.tila.toString), vastaanotto.muokkaaja, vastaanotto.selite)
  }

  def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: ValintatuloksenTila, muokkaaja: String, selite: String) {
    val (hakemus, hakutoive) = etsiHakutoive(hakuOid, hakemusOid, hakukohdeOid)

    tarkistaVastaanotettavuus(hakutoive, tila)
    val tiedot = new ValintatulosPerustiedot(hakuOid, hakukohdeOid, hakutoive.valintatapajono.getValintatapajonoOid, hakemusOid, hakemus.getHakijaOid, hakutoive.hakutoive.getHakutoive)
    vastaanota(tiedot, tila, muokkaaja, selite)

  }

  private def etsiHakutoive(hakuOid: String, hakemusOid: String, hakukohdeOid: String): (HakijaDTO, HakutoiveenYhteenveto) = {
    val yhteenveto: Option[HakemuksenYhteenveto] = yhteenvetoService.hakemuksenYhteenveto(hakuOid, hakemusOid)
    (yhteenveto, yhteenveto.flatMap(_.hakutoiveet.find(_.hakutoive.getHakukohdeOid == hakukohdeOid))) match {
      case (Some(yhteenveto), Some(hakutoive)) =>
        (yhteenveto.hakija, hakutoive)
      case _ =>
        throw new IllegalArgumentException("Hakemusta tai hakutoivetta ei löydy")
    }
  }

  private def tarkistaVastaanotettavuus(hakutoive: HakutoiveenYhteenveto, tila: ValintatuloksenTila) {
    if (!List(VASTAANOTTANUT, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT).contains(tila)) {
      throw new IllegalArgumentException("Ei-hyväksytty vastaanottotila: " + tila)
    }
    if (List(VASTAANOTTANUT, PERUNUT).contains(tila) && !List(Vastaanotettavuustila.vastaanotettavissa_ehdollisesti, Vastaanotettavuustila.vastaanotettavissa_sitovasti).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException(tila.toString())
    }
    if (tila == EHDOLLISESTI_VASTAANOTTANUT && hakutoive.vastaanotettavuustila != Vastaanotettavuustila.vastaanotettavissa_ehdollisesti) {
      throw new IllegalArgumentException(tila.toString())
    }
  }

  private def vastaanota(perustiedot: ValintatulosPerustiedot, tila: ValintatuloksenTila, muokkaaja: String, selite: String) {
    var valintatulos: Valintatulos = dao.loadValintatulos(perustiedot.hakukohdeOid, perustiedot.valintatapajonoOid, perustiedot.hakemusOid)
    if (valintatulos == null) {
      valintatulos = perustiedot.createValintatulos(tila)
    }
    else {
      valintatulos.setTila(tila)
    }
    addLogEntry(valintatulos, valintatulos.getTila.name, muokkaaja, selite)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  def ilmoittaudu(hakuOid: String, hakemusOid: String, ilmoittautuminen: Ilmoittautuminen) {
    val (_, hakutoive) = etsiHakutoive(hakuOid, hakemusOid, ilmoittautuminen.hakukohdeOid)
    var valintatulos: Valintatulos = dao.loadValintatulos(ilmoittautuminen.hakukohdeOid, hakutoive.valintatapajono.getValintatapajonoOid, hakemusOid)
    if (valintatulos == null) {
      throw new IllegalArgumentException("Valintatulosta ei löydy")
    }
    val sopivatTilat = Array(VASTAANOTTANUT, VASTAANOTTANUT_SITOVASTI)

    if(!sopivatTilat.contains(valintatulos.getTila)) {
      throw new IllegalArgumentException(s"""Valintatulokselle, jonka tila on
      ${valintatulos.getTila} ei voi tallentaa ilmoittautumistietoa""")
    }
    valintatulos.setIlmoittautumisTila(IlmoittautumisTila.valueOf(ilmoittautuminen.tila.toString))
    addLogEntry(valintatulos, ilmoittautuminen.tila.toString, ilmoittautuminen.muokkaaja, ilmoittautuminen.selite)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  private def addLogEntry(valintatulos: Valintatulos, tila: String, muokkaaja: String, selite: String) {
    val logEntry: LogEntry = new LogEntry
    logEntry.setLuotu(new Date)
    logEntry.setMuokkaaja(muokkaaja)
    logEntry.setSelite(selite)
    logEntry.setMuutos(tila)
    valintatulos.getLogEntries.add(logEntry)
  }

  private case class ValintatulosPerustiedot (hakuOid: String, hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String, hakijaOid: String, hakutoiveenPrioriteetti: Int) {
    def createValintatulos(tila: ValintatuloksenTila): Valintatulos = {
      val valintatulos: Valintatulos = new Valintatulos
      valintatulos.setHakemusOid(hakemusOid)
      valintatulos.setHakijaOid(hakijaOid)
      valintatulos.setHakukohdeOid(hakukohdeOid)
      valintatulos.setHakuOid(hakuOid)
      valintatulos.setHakutoive(hakutoiveenPrioriteetti)
      valintatulos.setIlmoittautumisTila(IlmoittautumisTila.EI_TEHTY)
      valintatulos.setTila(tila)
      valintatulos.setValintatapajonoOid(valintatapajonoOid)
      return valintatulos
    }
  }
}
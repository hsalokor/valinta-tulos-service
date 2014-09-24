package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.{Date, Optional}

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila
import fi.vm.sade.sijoittelu.domain.LogEntry
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.EHDOLLISESTI_VASTAANOTTANUT
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.PERUNUT
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.VASTAANOTTANUT
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila._
import fi.vm.sade.sijoittelu.domain.Valintatulos
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.Vastaanotettavuustila.VASTAANOTETTAVISSA_EHDOLLISESTI
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.Vastaanotettavuustila.VASTAANOTETTAVISSA_SITOVASTI
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.Vastaanotettavuustila._
import fi.vm.sade.sijoittelu.tulos.service.{HakutoiveenYhteenveto, RaportointiService, YhteenvetoService}
import fi.vm.sade.valintatulosservice.domain.Vastaanotto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO

// This compilation unit is a quick-and-dirty conversion from Java code

case class ValintatulosPerustiedot (hakuOid: String, hakukohdeOid: String, valintatapajonoOid: String, hakemusOid: String, hakijaOid: String, hakutoiveenPrioriteetti: Int) {
  def createValintatulos(tila: ValintatuloksenTila): Valintatulos = {
    val valintatulos: Valintatulos = new Valintatulos
    valintatulos.setHakemusOid(hakemusOid)
    valintatulos.setHakijaOid(hakijaOid)
    valintatulos.setHakukohdeOid(hakukohdeOid)
    valintatulos.setHakuOid(hakuOid)
    valintatulos.setHakutoive(hakutoiveenPrioriteetti)
    valintatulos.setIlmoittautumisTila(IlmoittautumisTila.EI_ILMOITTAUTUNUT)
    valintatulos.setTila(tila)
    valintatulos.setValintatapajonoOid(valintatapajonoOid)
    return valintatulos
  }
}

object Java8Conversions {
  implicit def fromOptional[T](opt: Optional[T]) = {
    if (opt.isPresent) {
      Some(opt.get)
    } else {
      None
    }
  }
}

class VastaanottoService(dao: ValintatulosDao, raportointiService: RaportointiService) {
  import Java8Conversions._
  import collection.JavaConversions._

  def vastaanota(hakuOid: String, hakemusOid: String, vastaanotto: Vastaanotto) {
    vastaanota(hakuOid, hakemusOid, vastaanotto.hakukohdeOid, ValintatuloksenTila.valueOf(vastaanotto.tila.toString), vastaanotto.muokkaaja, vastaanotto.selite)
  }

  def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: ValintatuloksenTila, muokkaaja: String, selite: String) {
    val sijoitteluAjo: Option[SijoitteluAjo] = raportointiService.latestSijoitteluAjoForHaku(hakuOid)
    if (!sijoitteluAjo.isDefined) {
      throw new IllegalArgumentException("Sijoitteluajoa ei löydy")
    }
    val hakemus: HakijaDTO = raportointiService.hakemus(sijoitteluAjo.get, hakemusOid)
    if (hakemus == null) {
      throw new IllegalArgumentException("Hakemusta ei löydy")
    }
    val hakutoiveet: List[HakutoiveenYhteenveto] = YhteenvetoService.hakutoiveidenYhteenveto(hakemus).toList
    val hakutoive: Option[HakutoiveenYhteenveto] = hakutoiveet.find(_.hakutoive.getHakukohdeOid().equals(hakukohdeOid))
    if (!hakutoive.isDefined) {
      throw new IllegalArgumentException("Hakukohdetta ei löydy")
    }
    tarkistaVastaanotettavuus(hakutoive.get, tila)
    val tiedot = new ValintatulosPerustiedot(hakuOid, hakukohdeOid, hakutoive.get.valintatapajono.getValintatapajonoOid, hakemusOid, hakemus.getHakijaOid, hakutoive.get.hakutoive.getHakutoive)
    vastaanota(tiedot, tila, muokkaaja, selite)
  }

  private def tarkistaVastaanotettavuus(hakutoive: HakutoiveenYhteenveto, tila: ValintatuloksenTila) {
    if (!List(VASTAANOTTANUT, EHDOLLISESTI_VASTAANOTTANUT, PERUNUT).contains(tila)) {
      throw new IllegalArgumentException("Ei-hyväksytty vastaantottotila: " + tila)
    }
    if (List(VASTAANOTTANUT, PERUNUT).contains(tila) && !List(VASTAANOTETTAVISSA_EHDOLLISESTI, VASTAANOTETTAVISSA_SITOVASTI).contains(hakutoive.vastaanotettavuustila)) {
      throw new IllegalArgumentException
    }
    if (tila == EHDOLLISESTI_VASTAANOTTANUT && hakutoive.vastaanotettavuustila != VASTAANOTETTAVISSA_EHDOLLISESTI) {
      throw new IllegalArgumentException
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
    addLogEntry(valintatulos, muokkaaja, selite)
    dao.createOrUpdateValintatulos(valintatulos)
  }

  private def addLogEntry(valintatulos: Valintatulos, muokkaaja: String, selite: String) {
    val logEntry: LogEntry = new LogEntry
    logEntry.setLuotu(new Date)
    logEntry.setMuokkaaja(muokkaaja)
    logEntry.setSelite(selite)
    logEntry.setMuutos(valintatulos.getTila.name)
    valintatulos.getLogEntries.add(logEntry)
  }
}


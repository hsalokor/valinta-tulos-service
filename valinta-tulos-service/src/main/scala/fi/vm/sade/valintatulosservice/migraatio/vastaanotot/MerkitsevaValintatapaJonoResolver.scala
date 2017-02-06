package fi.vm.sade.valintatulosservice.migraatio.vastaanotot

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.sijoittelu.JonoFinder

class MerkitsevaValintatapaJonoResolver(raportointiService: RaportointiService) {
  private type HakuOid = String
  private type HakijaOid = String
  private type HakemusOid = String
  private type HakukohdeOid = String
  private type JonoOid = String

  private var latestSijoitteluAjosByHakuOids = Map[HakuOid, SijoitteluAjo]()
  private var merkitsevatJonoOiditByHakuAndHakemusOids = Map[(HakuOid, HakemusOid), List[(HakukohdeOid, JonoOid)]]()

  def hakemuksenKohteidenMerkitsevatJonot(hakuOid: HakuOid, hakemusOid: HakemusOid, hakijaOid: HakijaOid): Option[List[(HakukohdeOid, JonoOid)]] = {
    if (merkitsevatJonoOiditByHakuAndHakemusOids.contains((hakuOid, hakemusOid))) {
      merkitsevatJonoOiditByHakuAndHakemusOids.get((hakuOid, hakemusOid))
    } else {
      for (
        sijoitteluAjo <- findSijoitteluAjo(hakuOid);
        hakija: HakijaDTO <- Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid))
      ) yield merkitsevatJonot(hakija, hakuOid, hakemusOid)
    }
  }

  private def findSijoitteluAjo(hakuOid: HakuOid): Option[SijoitteluAjo] = {
    if (latestSijoitteluAjosByHakuOids.contains(hakuOid)) {
      latestSijoitteluAjosByHakuOids.get(hakuOid)
    } else {
      raportointiService.latestSijoitteluAjoForHaku(hakuOid) match {
        case opt if opt.isPresent =>
          val ajo = opt.get
          latestSijoitteluAjosByHakuOids = latestSijoitteluAjosByHakuOids + (hakuOid -> ajo)
          Some(ajo)
        case notFound => throw new RuntimeException(s"Ei löytynyt sijoitteluajoa haulle $hakuOid merkitsevien jonojen valitsemiseksi")
      }
    }
  }

  private def merkitsevatJonot(hakija: HakijaDTO, hakuOid: HakuOid, hakemusOid: HakemusOid): List[(HakukohdeOid, JonoOid)] = {
    import scala.collection.JavaConversions._
    val merkitsevatJonoOiditHakukohteittain: List[(HakukohdeOid, JonoOid)] = hakija.getHakutoiveet.toList.map { hakutoive: HakutoiveDTO =>
      (hakutoive.getHakukohdeOid, JonoFinder.merkitseväJono(hakutoive).get.getValintatapajonoOid)
    }
    merkitsevatJonoOiditByHakuAndHakemusOids = merkitsevatJonoOiditByHakuAndHakemusOids + ((hakuOid, hakemusOid) -> merkitsevatJonoOiditHakukohteittain)
    merkitsevatJonoOiditHakukohteittain
  }
}

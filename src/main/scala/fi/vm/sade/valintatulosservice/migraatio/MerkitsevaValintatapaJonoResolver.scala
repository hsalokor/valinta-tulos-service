package fi.vm.sade.valintatulosservice.migraatio

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.sijoittelu.JonoFinder

class MerkitsevaValintatapaJonoResolver(raportointiService: RaportointiService) {
  private var latestSijoitteluAjosByHakuOids = Map[String, SijoitteluAjo]()

  def hakemuksenKohteidenMerkitsevatJonot(hakuOid: String, hakemusOid: String, hakijaOid: String): Option[List[(String, String)]] = {
    for (
      sijoitteluAjo <- findSijoitteluAjo(hakuOid);
      hakija: HakijaDTO <- Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid))
    ) yield merkitsevatJonot(hakija)
  }

  private def findSijoitteluAjo(hakuOid: String): Option[SijoitteluAjo] = {
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

  private def merkitsevatJonot(hakija: HakijaDTO): List[(String, String)] = {
    import scala.collection.JavaConversions._
    hakija.getHakutoiveet.toList.map { hakutoive: HakutoiveDTO =>
      (hakutoive.getHakukohdeOid, JonoFinder.merkitseväJono(hakutoive).get.getValintatapajonoOid)
    }
  }
}

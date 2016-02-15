package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.HakukohdeRecord
import fi.vm.sade.valintatulosservice.tarjonta.{HakuService, Hakukohde}

class HakukohdeRecordService(hakuService: HakuService, hakukohdeRepository: HakukohdeRepository) {
  def getHakukohdeRecord(oid: String): HakukohdeRecord = {
    val record: Option[HakukohdeRecord] = hakukohdeRepository.findHakukohde(oid)
    record.getOrElse(fetchAndStoreHakukohdeDetails(oid))
  }

  private def fetchAndStoreHakukohdeDetails(oid: String): HakukohdeRecord = {
    val h = for {
      hakukohde <- hakuService.getHakukohde(oid)
      haku <- hakuService.getHaku(hakukohde.hakuOid)
      koulutukset <- sequence(hakukohde.hakukohteenKoulutusOids.map(hakuService.getKoulutus))
      alkamiskausi <- unique(koulutukset.map(_.koulutuksenAlkamiskausi))
    } yield HakukohdeRecord(hakukohde.oid, haku.oid, haku.yhdenPaikanSaanto.voimassa,
      hakukohdeJohtaaKkTutkintoon(hakukohde), alkamiskausi)
    h.foreach(hakukohdeRepository.storeHakukohde(_))
    h.getOrElse(throw new RuntimeException(s"Could not retrieve details for hakukohde $oid"))
  }

  private def hakukohdeJohtaaKkTutkintoon(hakukohde: Hakukohde): Boolean = {
    hakukohde.koulutusAsteTyyppi == "KORKEAKOULUTUS" && hakukohde.koulutusmoduuliTyyppi == "TUTKINTO"
  }

  private def unique[A](kaudet: Seq[A]): Option[A] = {
    kaudet match {
      case x::rest if rest.forall(x == _) => Some(x)
      case _ => None
    }
  }

  private def sequence[A](xs: Seq[Option[A]]): Option[Seq[A]] = xs match {
    case Nil => Some(Nil)
    case None::_ => None
    case Some(x)::rest => sequence(rest).map(x +: _)
  }
}

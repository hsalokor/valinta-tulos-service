package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.domain.{HakukohdeRecord, Kausi}
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService, Hakukohde, Koulutus}

import scala.util.{Success, Try}

class HakukohdeRecordService(hakuService: HakuService, hakukohdeRepository: HakukohdeRepository, parseLeniently: Boolean) extends Logging {
  private val koulutusTilasToSkipInStrictParsing = List("LUONNOS", "KOPIOITU") // See TarjontaTila in tarjonta-api

  def getHaunKoulutuksenAlkamiskausi(oid: String): Option[Kausi] = {
    val record = hakukohdeRepository.findHaunArbitraryHakukohde(oid)
    record.orElse(hakuService.getHakukohdeOids(oid).headOption.map(fetchAndStoreHakukohdeDetails)).map(_.koulutuksenAlkamiskausi)
  }

  def getHakukohdeRecord(oid: String): HakukohdeRecord = {
    // hakukohdeRecord is cached in DB to enable vastaanotto queries
    val record: Option[HakukohdeRecord] = hakukohdeRepository.findHakukohde(oid)
    record.getOrElse(fetchAndStoreHakukohdeDetails(oid))
  }

  def refreshHakukohdeRecord(oid: String): (HakukohdeRecord, Option[HakukohdeRecord]) = {
    val old = hakukohdeRepository.findHakukohde(oid).get
    val fresh = fetchHakukohdeDetails(oid)
    if (hakukohdeRepository.updateHakukohde(fresh)) {
      (old, Some(fresh))
    } else {
      (old, None)
    }
  }

  private def fetchAndStoreHakukohdeDetails(oid: String): HakukohdeRecord = {
    val fresh = fetchHakukohdeDetails(oid)
    hakukohdeRepository.storeHakukohde(fresh)
    fresh
  }

  private def fetchHakukohdeDetails(oid: String): HakukohdeRecord = {
    (for {
      hakukohde <- withError(hakuService.getHakukohde(oid), s"Could not find hakukohde $oid from tarjonta")
      haku <- withError(hakuService.getHaku(hakukohde.hakuOid), s"Could not find haku ${hakukohde.hakuOid} from tarjonta")
      koulutukset <- withError(sequence(hakukohde.hakukohdeKoulutusOids.map(hakuService.getKoulutus)),
        s"Could not resolve koulutukset ${hakukohde.hakukohdeKoulutusOids}")
      alkamiskausi <- resolveKoulutuksenAlkamiskausi(hakukohde, koulutukset, haku)
    } yield HakukohdeRecord(hakukohde.oid, haku.oid, haku.yhdenPaikanSaanto.voimassa,
      hakukohdeJohtaaKkTutkintoon(hakukohde, koulutukset), alkamiskausi)).get
  }

  private def resolveKoulutuksenAlkamiskausi(hakukohde: Hakukohde, koulutukset: Seq[Koulutus], haku: Haku): Try[Kausi] = {
    def oikeanTilaiset(koulutukset: Seq[Koulutus]): Seq[Koulutus] = koulutukset.filter(k => !koulutusTilasToSkipInStrictParsing.contains(k.tila))
    val errorMessage = s"No unique koulutuksen alkamiskausi in $koulutukset for $hakukohde " +
      s"(koulutukset in correct state: ${oikeanTilaiset(koulutukset)})"

    unique(oikeanTilaiset(koulutukset).map(_.koulutuksenAlkamiskausi)) match {
      case Some(found) => Success(found)
      case None if parseLeniently =>
        logger.warn(s"$errorMessage. Falling back to koulutuksen alkamiskausi from haku: ${haku.koulutuksenAlkamiskausi}")
        withError(haku.koulutuksenAlkamiskausi, s"$errorMessage , and no koulutuksen alkamiskausi on haku $haku")
      case None => withError(None, errorMessage)
    }
  }

  private def hakukohdeJohtaaKkTutkintoon(hakukohde: Hakukohde, koulutukset: Seq[Koulutus]): Boolean = {
    hakukohde.koulutusAsteTyyppi == "KORKEAKOULUTUS" && koulutukset.exists(_.johtaaTutkintoon)
  }

  def withError[T](o: Option[T], errorMessage: String): Try[T] = {
    Try { o.getOrElse(throw new HakukohdeDetailsRetrievalException(errorMessage)) }
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

case class HakukohdeDetailsRetrievalException(message: String) extends RuntimeException(message)

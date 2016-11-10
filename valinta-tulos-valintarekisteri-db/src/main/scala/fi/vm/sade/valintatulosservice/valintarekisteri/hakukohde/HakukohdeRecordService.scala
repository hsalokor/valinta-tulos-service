package fi.vm.sade.valintatulosservice.valintarekisteri.hakukohde

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.koodisto.KoodistoService
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, Koulutus, Hakukohde, HakuService}
import fi.vm.sade.valintatulosservice.valintarekisteri.db.{ValintarekisteriDb, HakukohdeRepository}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.{HakukohdeRecord, Kausi}

import scala.util.{Failure, Success, Try}

class HakukohdeRecordService(hakuService: HakuService, hakukohdeRepository: HakukohdeRepository, parseLeniently: Boolean) extends Logging {
  private val koulutusTilasToSkipInStrictParsing = List("LUONNOS", "KOPIOITU") // See TarjontaTila in tarjonta-api

  def getHaunKoulutuksenAlkamiskausi(oid: String): Either[Throwable, Kausi] = {
    Try(hakukohdeRepository.findHaunArbitraryHakukohde(oid)) match {
      case Success(Some(hakukohde)) => Right(hakukohde.koulutuksenAlkamiskausi)
      case Success(None) => hakuService.getArbitraryPublishedHakukohdeOid(oid)
        .right.flatMap(fetchAndStoreHakukohdeDetails).right.map(_.koulutuksenAlkamiskausi)
      case Failure(e) => Left(e)
    }
  }

  def getHakukohdeRecord(oid: String): Either[Throwable, HakukohdeRecord] = {
    // hakukohdeRecord is cached in DB to enable vastaanotto queries
    Try(hakukohdeRepository.findHakukohde(oid)) match {
      case Success(Some(hakukohde)) => Right(hakukohde)
      case Success(None) => fetchAndStoreHakukohdeDetails(oid)
      case Failure(e) => Left(e)
    }
  }

  def refreshHakukohdeRecord(oid: String): (HakukohdeRecord, Option[HakukohdeRecord]) = {
    val old = hakukohdeRepository.findHakukohde(oid).get
    fetchHakukohdeDetails(oid) match {
      case Right(fresh) => if (hakukohdeRepository.updateHakukohde(fresh)) {
        (old, Some(fresh))
      } else {
        (old, None)
      }
      case Left(t) if parseLeniently =>
        logger.warn(s"Error fetching hakukohde ${old.oid} that exists in db", t)
        (old, None)
      case Left(t) => throw t
    }
  }

  private def fetchAndStoreHakukohdeDetails(oid: String): Either[Throwable, HakukohdeRecord] = {
    val fresh = fetchHakukohdeDetails(oid)
    fresh.right.foreach(hakukohdeRepository.storeHakukohde)
    fresh
  }

  private def fetchHakukohdeDetails(oid: String): Either[Throwable, HakukohdeRecord] = {
    for {
      hakukohde <- hakuService.getHakukohde(oid).right
      haku <- hakuService.getHaku(hakukohde.hakuOid).right
      koulutukset <- sequence(hakukohde.hakukohdeKoulutusOids.toStream.map(hakuService.getKoulutus)).right
      alkamiskausi <- resolveKoulutuksenAlkamiskausi(hakukohde, koulutukset, haku).right
    } yield HakukohdeRecord(hakukohde.oid, haku.oid, haku.yhdenPaikanSaanto.voimassa,
      hakukohdeJohtaaKkTutkintoon(hakukohde, koulutukset), alkamiskausi)
  }

  private def resolveKoulutuksenAlkamiskausi(hakukohde: Hakukohde, koulutukset: Seq[Koulutus], haku: Haku): Either[Throwable, Kausi] = {
    def oikeanTilaiset(koulutukset: Seq[Koulutus]): Seq[Koulutus] = koulutukset.filter(k => !koulutusTilasToSkipInStrictParsing.contains(k.tila))
    val errorMessage = s"No unique koulutuksen alkamiskausi in $koulutukset for $hakukohde " +
      s"(koulutukset in correct state: ${oikeanTilaiset(koulutukset)})"

    unique(oikeanTilaiset(koulutukset).map(_.koulutuksenAlkamiskausi)) match {
      case Some(found) => Right(found)
      case None if parseLeniently =>
        logger.warn(s"$errorMessage. Falling back to koulutuksen alkamiskausi from haku: ${haku.koulutuksenAlkamiskausi}")
        haku.koulutuksenAlkamiskausi.toRight(new IllegalStateException(s"$errorMessage , and no koulutuksen alkamiskausi on haku $haku"))
      case None => Left(new IllegalStateException(errorMessage))
    }
  }

  private def hakukohdeJohtaaKkTutkintoon(hakukohde: Hakukohde, koulutukset: Seq[Koulutus]): Boolean = {
    hakukohde.koulutusAsteTyyppi == "KORKEAKOULUTUS" && koulutukset.exists(_.johtaaTutkintoon)
  }

  private def unique[A](kaudet: Seq[A]): Option[A] = {
    kaudet match {
      case x::rest if rest.forall(x == _) => Some(x)
      case _ => None
    }
  }

  private def sequence[A, B](xs: Stream[Either[B, A]]): Either[B, List[A]] = xs match {
    case Stream.Empty => Right(Nil)
    case Left(e)#::_ => Left(e)
    case Right(x)#::rest => sequence(rest).right.map(x +: _)
  }
}

object HakukohdeRecordService {

  def apply(valintarekisteriDb: ValintarekisteriDb, appConfig: AppConfig) = {
    val koodistoService = new KoodistoService(appConfig)
    val hakuService = HakuService(koodistoService, appConfig)
    new HakukohdeRecordService(hakuService, valintarekisteriDb, appConfig.settings.lenientTarjontaDataParsing)
  }
}

package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.tarjonta.{Haku, HakuService, Hakukohde, Koulutus}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.{Kausi, HakukohdeRecord}
import fi.vm.sade.valintatulosservice.valintarekisteri.db.HakukohdeRepository

import scala.util.{Failure, Success, Try}

class HakukohdeRecordService(hakuService: HakuService, hakukohdeRepository: HakukohdeRepository, parseLeniently: Boolean) extends Logging {
  private val koulutusTilasToSkipInStrictParsing = List("POISTETTU") // See TarjontaTila in tarjonta-api

  def getHaunHakukohdeRecords(oid: String): Either[Throwable, Seq[HakukohdeRecord]] = {
    hakuService.getHakukohdeOids(oid).right.flatMap(getHakukohdeRecords)
  }

  def getHakukohteidenKoulutuksenAlkamiskausi(oids: Seq[String]): Either[Throwable, Seq[(String, Option[Kausi])]] = {
    getHakukohdeRecords(oids).right.map(_.map(hakukohde =>
      (hakukohde.oid, if (hakukohde.yhdenPaikanSaantoVoimassa) Some(hakukohde.koulutuksenAlkamiskausi) else None)))
  }

  def getHaunKoulutuksenAlkamiskausi(oid: String): Either[Throwable, Kausi] = {
    Try(hakukohdeRepository.findHaunArbitraryHakukohde(oid)) match {
      case Success(Some(hakukohde)) => Right(hakukohde.koulutuksenAlkamiskausi)
      case Success(None) => hakuService.getArbitraryPublishedHakukohdeOid(oid)
        .right.flatMap(fetchAndStoreHakukohdeDetails).right.map(_.koulutuksenAlkamiskausi)
      case Failure(e) => Left(e)
    }
  }

  def getHakukohdeRecords(oids: Seq[String]): Either[Throwable, Seq[HakukohdeRecord]] = {
    sequence(for{oid <- oids.toStream} yield getHakukohdeRecord(oid))
  }

  def getHakukohdeRecord(oid: String): Either[Throwable, HakukohdeRecord] = {
    // hakukohdeRecord is cached in DB to enable vastaanotto queries
    Try(hakukohdeRepository.findHakukohde(oid)) match {
      case Success(Some(hakukohde)) => Right(hakukohde)
      case Success(None) => fetchAndStoreHakukohdeDetails(oid)
      case Failure(e) => Left(e)
    }
  }

  def refreshHakukohdeRecord(oid: String): Boolean = {
    refreshHakukohdeRecord(oid, (_, fresh) => hakukohdeRepository.updateHakukohde(fresh))
  }

  def refreshHakukohdeRecordDryRun(oid: String): Boolean = {
    refreshHakukohdeRecord(oid, _ != _)
  }

  private def refreshHakukohdeRecord(oid: String, update: (HakukohdeRecord, HakukohdeRecord) => Boolean): Boolean = {
    val old = hakukohdeRepository.findHakukohde(oid).get
    val vastaanottoja = hakukohdeRepository.hakukohteessaVastaanottoja(oid)
    fetchHakukohdeDetails(oid) match {
      case Right(fresh) =>
        if (update(old, fresh)) {
          if (vastaanottoja) {
            logger.warn(s"Updated hakukohde from $old to $fresh. Hakukohde had vastaanottos.")
          } else {
            logger.info(s"Updated hakukohde from $old to $fresh.")
          }
          true
        } else {
          false
        }
      case Left(t) if vastaanottoja =>
        logger.error(s"Updating hakukohde $oid failed. Hakukohde had vastaanottos.", t)
        false
      case Left(t) =>
        logger.warn(s"Updating hakukohde $oid failed.", t)
        false
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
      koulutukset <- sequence(hakukohde.hakukohdeKoulutusOids.toStream.map(hakuService.getKoulutus)).right
      alkamiskausi <- resolveKoulutuksenAlkamiskausi(hakukohde, koulutukset).right
    } yield HakukohdeRecord(hakukohde.oid, hakukohde.hakuOid, hakukohde.yhdenPaikanSaanto.voimassa,
      hakukohdeJohtaaKkTutkintoon(hakukohde, koulutukset), alkamiskausi)
  }

  private def resolveKoulutuksenAlkamiskausi(hakukohde: Hakukohde, koulutukset: Seq[Koulutus]): Either[Throwable, Kausi] = {
    def oikeanTilaiset(koulutukset: Seq[Koulutus]): Seq[Koulutus] = koulutukset.filter(k => !koulutusTilasToSkipInStrictParsing.contains(k.tila))
    val errorMessage = s"No unique koulutuksen alkamiskausi in $koulutukset for $hakukohde " +
      s"(koulutukset in correct state: ${oikeanTilaiset(koulutukset)})"

    unique(oikeanTilaiset(koulutukset).map(_.koulutuksenAlkamiskausi)) match {
      case Some(found) => Right(found)
      case None if parseLeniently =>
        hakuService.getHaku(hakukohde.hakuOid) match {
          case Right(haku) =>
            logger.warn(s"$errorMessage. Falling back to koulutuksen alkamiskausi from haku: ${haku.koulutuksenAlkamiskausi}")
            haku.koulutuksenAlkamiskausi.toRight(new IllegalStateException(s"$errorMessage , and no koulutuksen alkamiskausi on haku $haku"))
          case Left(e) => Left(e)
        }
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

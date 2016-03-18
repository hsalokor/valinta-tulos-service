package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.HakukohdeRecord
import fi.vm.sade.valintatulosservice.tarjonta.{HakuService, Hakukohde}

import scala.util.Try

class HakukohdeRecordService(hakuService: HakuService, hakukohdeRepository: HakukohdeRepository) {
  def getHakukohdeRecord(oid: String): HakukohdeRecord = {
    // hakukohdeRecord is cached in DB to enable vastaanotto queries
    val record: Option[HakukohdeRecord] = hakukohdeRepository.findHakukohde(oid)
    record.getOrElse(fetchAndStoreHakukohdeDetails(oid))
  }

  private def fetchAndStoreHakukohdeDetails(oid: String): HakukohdeRecord = {
    def withError[T](o: Option[T], errorMessage: String): Try[T] = {
      Try { o.getOrElse(throw new HakukohdeDetailsRetrievalException(errorMessage)) }
    }
    val h = for {
      hakukohde <- withError(hakuService.getHakukohde(oid), s"Could not find hakukohde $oid from tarjonta")
      haku <- withError(hakuService.getHaku(hakukohde.hakuOid), s"Could not find haku ${hakukohde.hakuOid} from tarjonta")
      koulutukset <- withError(sequence(hakukohde.hakukohdeKoulutusOids.map(hakuService.getKoulutus)),
        s"Could not resolve koulutukset ${hakukohde.hakukohdeKoulutusOids}")
      alkamiskausi <- withError(unique(koulutukset.map(_.koulutuksenAlkamiskausi)),
        s"No unique koulutuksen alkamiskausi in $koulutukset for $hakukohde")
    } yield HakukohdeRecord(hakukohde.oid, haku.oid, haku.yhdenPaikanSaanto.voimassa,
      hakukohdeJohtaaKkTutkintoon(hakukohde), alkamiskausi)
    h.foreach(hakukohdeRepository.storeHakukohde)
    h.get
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

case class HakukohdeDetailsRetrievalException(message: String) extends RuntimeException(message)

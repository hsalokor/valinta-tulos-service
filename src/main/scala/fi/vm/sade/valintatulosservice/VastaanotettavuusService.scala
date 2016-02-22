package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakijaVastaanottoRepository, HakukohdeRecordService}

import scala.util.{Try, Success, Failure}

class VastaanotettavuusService(valintatulosService: ValintatulosService,
                               hakuService: HakuService,
                               hakukohdeRecordService: HakukohdeRecordService,
                               hakijaVastaanottoRepository: HakijaVastaanottoRepository) {
  def vastaanotettavuus(henkiloOid: String, hakemusOid: String, hakukohdeOid: String): Vastaanotettavuus = {
    // TODO pitäisikö tässä kohtaa tarkistaa, että haku <-> hakukohde <-> hakemus liittyvät toisiinsa?

    val hakukohdeRecord = hakukohdeRecordService.getHakukohdeRecord(hakukohdeOid)

    (( for {
      hakutoive <- findHyvaksyttyHakutoive(henkiloOid, hakemusOid, hakukohdeRecord)
      _ <- tarkistaAiemmatVastaanotot(henkiloOid, hakukohdeRecord)
    } yield {
      val haku = hakuService.getHaku(hakukohdeRecord.hakuOid).get
      val vastaanotettavissaEhdollisesti = valintatulosService.onkoVastaanotettavissaEhdollisesti(hakutoive, haku)
      Vastaanotettavuus(List(Peru, VastaanotaSitovasti) ++ (if (vastaanotettavissaEhdollisesti) List(VastaanotaEhdollisesti) else Nil))
    }) recover {
      case e: IllegalStateException => Vastaanotettavuus(Nil, Some(e))
      case e: PriorAcceptanceException => Vastaanotettavuus(Nil, Some(e))
    }).get
  }

  private def findHyvaksyttyHakutoive(henkiloOid: String, hakemusOid: String, hakukohdeRecord: HakukohdeRecord): Try[Hakutoiveentulos] = {
    findHakemus(henkiloOid, hakemusOid, hakukohdeRecord).flatMap(findHakutoive(_, hakukohdeRecord)).flatMap(isHakutoiveHyvaksytty)
  }

  private def findHakemus(henkiloOid: String, hakemusOid: String, hakukohdeRecord: HakukohdeRecord) = {
    valintatulosService.hakemuksentulos(hakukohdeRecord.hakuOid, hakemusOid).map(Success(_)).getOrElse(
      Failure(new IllegalStateException(s"Hakemusta $hakemusOid ei löydy")))
  }

  private def findHakutoive(hakemuksenTulos: Hakemuksentulos, hakukohdeRecord: HakukohdeRecord) = {
    hakemuksenTulos.findHakutoive(hakukohdeRecord.oid).map(Success(_)).getOrElse(
      Failure(new IllegalStateException(s"Ei löydy kohteen ${hakukohdeRecord.oid} tulosta hakemuksen tuloksesta $hakemuksenTulos")))
  }

  private def isHakutoiveHyvaksytty(hakutoive: Hakutoiveentulos) = {
    if( Valintatila.isHyväksytty(hakutoive.valintatila) ) {
      Success(hakutoive)
    } else {
      Failure(new IllegalStateException(s"Ei voida ottaa vastaan, koska hakutoiveen valintatila ei ole hyväksytty: ${hakutoive.valintatila}"))
    }
  }

  private def tarkistaAiemmatVastaanotot( henkiloOid: String,
                                          hakukohdeRecord: HakukohdeRecord ): Try[Unit] = {
    val aiemmatVastaanotot = haeAiemmatVastaanotot(hakukohdeRecord, henkiloOid)
    if (aiemmatVastaanotot.isEmpty) {
      Success(())
    } else if (aiemmatVastaanotot.size == 1) {
      val aiempiVastaanotto = aiemmatVastaanotot.head
      Failure(PriorAcceptanceException(aiempiVastaanotto))
    } else {
      Failure(new IllegalStateException(s"Hakijalla ${henkiloOid} useita vastaanottoja: $aiemmatVastaanotot"))
    }
  }

  private def haeAiemmatVastaanotot(hakukohdeRecord: HakukohdeRecord, hakijaOid: String): Set[VastaanottoRecord] = {
    val HakukohdeRecord(_, hakuOid, yhdenPaikanSaantoVoimassa, _, koulutuksenAlkamiskausi) = hakukohdeRecord
    val aiemmatVastaanotot = if (yhdenPaikanSaantoVoimassa) {
      hakijaVastaanottoRepository.findKkTutkintoonJohtavatVastaanotot(hakijaOid, koulutuksenAlkamiskausi)
    } else {
      hakijaVastaanottoRepository.findHenkilonVastaanototHaussa(hakijaOid, hakuOid)
    }
    aiemmatVastaanotot.filter(_.action != Peru)
  }
}

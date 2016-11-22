package fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu

import java.util

import fi.vm.sade.sijoittelu.domain.{Hakukohde, SijoitteluAjo, Valintatulos}
import fi.vm.sade.sijoittelu.tulos.dto.SijoitteluajoDTO
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakijaDTO, HakutoiveDTO}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.config.ValintarekisteriAppConfig
import fi.vm.sade.valintatulosservice.valintarekisteri.db.{SijoitteluRepository, ValintarekisteriDb}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.{SijoitteluRecordToDTO, SijoitteluWrapper}
import fi.vm.sade.valintatulosservice.valintarekisteri.hakukohde.HakukohdeRecordService

import scala.collection.JavaConverters._
import scala.util.Try

class ValintarekisteriForSijoittelu(appConfig:ValintarekisteriAppConfig.ValintarekisteriAppConfig) extends Valintarekisteri {

  def this() = this(ValintarekisteriAppConfig.getDefault())

  def this(properties:java.util.Properties) = this(ValintarekisteriAppConfig.getDefault(properties))

  override val sijoitteluRepository = new ValintarekisteriDb(appConfig.settings.valintaRekisteriDbConfig)
  override val hakukohdeRecordService: HakukohdeRecordService = HakukohdeRecordService(sijoitteluRepository, appConfig)
}

class ValintarekisteriService(override val sijoitteluRepository:SijoitteluRepository,
                              override val hakukohdeRecordService: HakukohdeRecordService) extends Valintarekisteri {
}

abstract class Valintarekisteri extends SijoitteluRecordToDTO with Logging {

  val sijoitteluRepository:SijoitteluRepository
  val hakukohdeRecordService: HakukohdeRecordService

  def tallennaSijoittelu(sijoitteluajo:SijoitteluAjo, hakukohteet:java.util.List[Hakukohde], valintatulokset:java.util.List[Valintatulos]) = {
    val sijoittelu = SijoitteluWrapper(sijoitteluajo, hakukohteet, valintatulokset)
    sijoittelu.hakukohteet.map(_.getOid).foreach(hakukohdeRecordService.getHakukohdeRecord(_))
    sijoitteluRepository.storeSijoittelu(sijoittelu)
  }

  def getSijoitteluajo(hakuOid:String, sijoitteluajoId:String): SijoitteluajoDTO = {
    val latestId = getLatestSijoitteluajoId(sijoitteluajoId, hakuOid)
    sijoitteluRepository.getSijoitteluajo(hakuOid, latestId).map(sijoitteluajo => {
      val valintatapajonotByHakukohde = getSijoitteluajonValintatapajonotGroupedByHakukohde(latestId)

      val hakijaryhmatByHakukohde = sijoitteluRepository.getHakijaryhmat(latestId)
        .map(h => hakijaryhmaRecordToDTO(h, sijoitteluRepository.getHakijaryhmanHakemukset(h.id))
        ).groupBy(_.getHakukohdeOid)

      val hakukohteet = sijoitteluRepository.getSijoitteluajoHakukohteet(latestId).map(sijoittelunHakukohdeRecordToDTO)
      hakukohteet.foreach(h => {
        h.setValintatapajonot(valintatapajonotByHakukohde.get(h.getOid).getOrElse(List()).asJava)
        h.setHakijaryhmat(hakijaryhmatByHakukohde.get(h.getOid).getOrElse(List()).asJava)
      })

      (sijoitteluajoRecordToDto(sijoitteluajo, hakukohteet))
    }).getOrElse(throw new IllegalArgumentException(s"Sijoitteluajoa $sijoitteluajoId ei löytynyt haulle $hakuOid"))
  }

  private def getSijoitteluajonValintatapajonotGroupedByHakukohde(latestId:Long) = {
    val valintatapajonotByHakukohde = sijoitteluRepository.getValintatapajonot(latestId).groupBy(_.hakukohdeOid).map({
      case (x, y) => (x, y.map(valintatapajonoRecordToDTO))
    })

    val valintatapajonoOidit = valintatapajonotByHakukohde.values.flatten.map(_.getOid).toList

    val kaikkiValintatapajonoHakemukset = sijoitteluRepository.getHakemuksetForValintatapajonos(valintatapajonoOidit).map(hakemus => {
      val tilankuvaukset = Map(sijoitteluRepository.getHakemuksenTilankuvaukset(hakemus.tilankuvausId): _*)
      hakemusRecordToDTO(hakemus, tilankuvaukset)
    })
    kaikkiValintatapajonoHakemukset.foreach(h => h.setTilaHistoria(
      sijoitteluRepository.getHakemuksenTilahistoria(h.getValintatapajonoOid, h.getHakemusOid).map(tilaHistoriaRecordToDTO).asJava
    ))

    valintatapajonotByHakukohde.values.flatten.foreach(j => j.setHakemukset(kaikkiValintatapajonoHakemukset.filter(_.getValintatapajonoOid.equals(j.getOid)).asJava))
    valintatapajonotByHakukohde
  }

  def getHakemusBySijoitteluajo(hakuOid:String, sijoitteluajoId:String, hakemusOid:String): HakijaDTO = {
    val latestId = getLatestSijoitteluajoId(sijoitteluajoId, hakuOid)
    val hakija = sijoitteluRepository.getHakija(hakemusOid, latestId)
      .orElse(throw new IllegalArgumentException(s"Hakijaa ei löytynyt hakemukselle $hakemusOid, sijoitteluajoid: $latestId"))
      .map(hakijaRecordToDTO).get

    val hakutoiveet = sijoitteluRepository.getHakutoiveet(hakemusOid, latestId)
    val pistetiedot = sijoitteluRepository.getPistetiedot(hakutoiveet.map(_.jonosijaId))

    val hakutoiveDTOs = hakutoiveet.map(h => hakutoiveRecordToDTO(h, pistetiedot.filter(_.jonosijaId == h.jonosijaId)))
    hakutoiveDTOs.sortBy(_.getHakutoive)

    hakija.setHakutoiveet(hakutoiveDTOs.asInstanceOf[util.SortedSet[HakutoiveDTO]])
    hakija
  }

  def getLatestSijoitteluajoId(sijoitteluajoId:String, hakuOid:String): Long = sijoitteluajoId match {
    case x if "latest".equalsIgnoreCase(x) => sijoitteluRepository.getLatestSijoitteluajoId(hakuOid)
      .getOrElse(throw new IllegalArgumentException(s"Yhtään sijoitteluajoa ei löytynyt haulle $hakuOid"))
    case x => Try(x.toLong).toOption
      .getOrElse(throw new IllegalArgumentException(s"Väärän tyyppinen sijoitteluajon ID: $sijoitteluajoId"))
  }
}

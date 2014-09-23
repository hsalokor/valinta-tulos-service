package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional
import fi.vm.sade.sijoittelu.tulos.service.{RaportointiService, YhteenvetoService}
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao

case class SijoitteluClient(raportointiService: RaportointiService, valintatulosDao: ValintatulosDao) {
  import scala.collection.JavaConversions._

  def yhteenveto(hakuOid: String, hakemusOid: String) = {
    optionalToOption(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).flatMap { sijoitteluAjo =>
      Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid)).map { hakijaDto => YhteenvetoService.yhteenveto(hakijaDto)}
    }
  }

  def sijoittelunTulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    yhteenveto(hakuOid, hakemusOid).map { yhteenveto =>
      Hakemuksentulos(hakemusOid, yhteenveto.hakutoiveet.toList.map { hakutoiveDto =>
        if (hakutoiveDto.julkaistavissa) {
          Hakutoiveentulos(hakutoiveDto.hakukohdeOid,
            hakutoiveDto.tarjoajaOid,
            Valintatila.withName(hakutoiveDto.valintatila.toString),
            Vastaanottotila.withName(hakutoiveDto.vastaanottotila.toString),
            Ilmoittautumistila.withName(hakutoiveDto.ilmoittautumistila.toString),
            Vastaanotettavuustila.withName(hakutoiveDto.vastaanotettavuustila.toString),
            Option(hakutoiveDto.viimeisinVastaanottotilanMuutos),
            Option(hakutoiveDto.jonosija),
            Option(hakutoiveDto.varasijojaKaytetaanAlkaen),
            Option(hakutoiveDto.varasijojaTaytetaanAsti),
            Option(hakutoiveDto.varasijanumero).map(_.toInt)
          )
        } else {
          Hakutoiveentulos(hakutoiveDto.hakukohdeOid,
            hakutoiveDto.tarjoajaOid,
            Valintatila.kesken,
            Vastaanottotila.withName(hakutoiveDto.vastaanottotila.toString),
            Ilmoittautumistila.withName(hakutoiveDto.ilmoittautumistila.toString),
            Vastaanotettavuustila.ei_vastaanottavissa,
            None,
            None,
            Option(hakutoiveDto.varasijojaKaytetaanAlkaen),
            Option(hakutoiveDto.varasijojaTaytetaanAsti),
            None
          )
        }
      })
    }
  }

  def optionalToOption[T](option: Optional[T]) = if (option.isPresent) {
    Some(option.get)
  } else {
    None
  }
}

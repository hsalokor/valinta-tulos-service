package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional

import fi.vm.sade.sijoittelu.tulos.service.{RaportointiService, YhteenvetoService}
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._

case class SijoitteluClient(raportointiService: RaportointiService) {
  import scala.collection.JavaConversions._

  def sijoittelunTulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    optionalToOption(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).flatMap { sijoitteluAjo =>
      Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid)).map { hakijaDto =>
        val yhteenveto = YhteenvetoService.yhteenveto(hakijaDto)
        Hakemuksentulos(hakemusOid, yhteenveto.hakutoiveet.toList.map { hakutoiveDto =>
          Hakutoiveentulos(hakutoiveDto.hakukohdeOid,
            hakutoiveDto.tarjoajaOid,
            Valintatila.withName(hakutoiveDto.valintatila.toString),
            Vastaanottotila.withName(hakutoiveDto.vastaanottotila.toString),
            Ilmoittautumistila.withName(hakutoiveDto.ilmoittautumistila.toString),
            Vastaanotettavuustila.withName(hakutoiveDto.vastaanotettavuustila.toString),
            Option(hakutoiveDto.jonosija),
            Option(hakutoiveDto.varasijojaKaytetaanAlkaen),
            Option(hakutoiveDto.varasijojaTaytetaanAsti),
            Option(hakutoiveDto.varasijanumero).map(_.toInt)
          )
        })
      }
    }
  }

  def optionalToOption[T](option: Optional[T]) = if (option.isPresent) {
    Some(option.get)
  } else {
    None
  }
}

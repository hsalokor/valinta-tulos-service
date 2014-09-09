package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional

import fi.vm.sade.sijoittelu.tulos.service.YhteenvetoService
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.{Hakemuksentulos, Hakutoiveentulos, ValintatulosService}

case class SijoitteluClient()(implicit appConfig: AppConfig) extends ValintatulosService {
  import scala.collection.JavaConversions._

  val raportointiService = appConfig.springContext.raportointiService

  override def hakemuksentulos(hakuOid: String, hakemusOid: String): Option[Hakemuksentulos] = {
    optionalToOption(raportointiService.latestSijoitteluAjoForHaku(hakuOid)).flatMap { sijoitteluAjo =>
      Option(raportointiService.hakemus(sijoitteluAjo, hakemusOid)).map { hakijaDto =>
        val yhteenveto = YhteenvetoService.yhteenveto(hakijaDto)
        Hakemuksentulos(hakemusOid, yhteenveto.hakutoiveet.toList.map { hakutoiveDto =>
          Hakutoiveentulos(hakutoiveDto.hakukohdeOid, hakutoiveDto.tarjoajaOid,
            hakutoiveDto.valintatila.toString, Option(hakutoiveDto.vastaanottotila).map(_.toString), Option(hakutoiveDto.ilmoittautumistila).map(_.toString),
            hakutoiveDto.vastaanotettavuustila.toString, Option(hakutoiveDto.jonosija), Option(hakutoiveDto.varasijanumero).map(_.toInt)
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

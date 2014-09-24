package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Date

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{HakutoiveenValintatapajonoDTO, HakutoiveDTO}
import fi.vm.sade.valintatulosservice.domain.Valintatila.Valintatila
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

protected case class HakutoiveenYhteenveto (hakutoive: HakutoiveDTO, valintatapajono: HakutoiveenValintatapajonoDTO, valintatila: Valintatila, vastaanottotila: Vastaanottotila, vastaanotettavuustila: Vastaanotettavuustila, julkaistavissa: Boolean, viimeisinVastaanottotilanMuutos: Option[Date])

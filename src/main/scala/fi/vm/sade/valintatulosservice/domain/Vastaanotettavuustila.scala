package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.{Vastaanotettavuustila => YhteenvedonVastaantettavuustila}

object Vastaanotettavuustila extends Enumeration {
  type Vastaanotettavuustila = Value
  val ei_vastaanottavissa = Value(YhteenvedonVastaantettavuustila.EI_VASTAANOTETTAVISSA.toString)
  val vastaanotettavissa_sitovasti = Value(YhteenvedonVastaantettavuustila.VASTAANOTETTAVISSA_SITOVASTI.toString)
  val vastaanotettavissa_ehdollisesti = Value(YhteenvedonVastaantettavuustila.VASTAANOTETTAVISSA_EHDOLLISESTI.toString)
}

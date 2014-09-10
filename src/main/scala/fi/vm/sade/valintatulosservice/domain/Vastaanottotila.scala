package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.YhteenvedonVastaanottotila

object Vastaanottotila extends Enumeration {
  type Vastaanottotila = Value
  val kesken = Value(YhteenvedonVastaanottotila.KESKEN.toString)
  val vastaanottanut = Value(YhteenvedonVastaanottotila.VASTAANOTTANUT.toString)
  val ei_vastaanotetu_määräaikana = Value(YhteenvedonVastaanottotila.EI_VASTAANOTETTU_MAARA_AIKANA.toString)
  val perunut = Value(YhteenvedonVastaanottotila.PERUNUT.toString)
  val peruutettu = Value(YhteenvedonVastaanottotila.PERUUTETTU.toString)
  val ehdollisesti_vastaaottanut = Value(YhteenvedonVastaanottotila.EHDOLLISESTI_VASTAANOTTANUT.toString)
}

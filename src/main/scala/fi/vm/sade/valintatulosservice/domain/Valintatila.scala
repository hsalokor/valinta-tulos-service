package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.YhteenvedonValintaTila


object Valintatila extends Enumeration {
  type Valintatila = Value
  val hyväksytty = Value(YhteenvedonValintaTila.HYVAKSYTTY.toString)
  val harkinnanvaraisesti_hyväksytty = Value(YhteenvedonValintaTila.HARKINNANVARAISESTI_HYVAKSYTTY.toString)
  val varalla = Value(YhteenvedonValintaTila.VARALLA.toString)
  val peruutettu = Value(YhteenvedonValintaTila.PERUUTETTU.toString)
  val perunut = Value(YhteenvedonValintaTila.PERUNUT.toString)
  val hylätty = Value(YhteenvedonValintaTila.HYLATTY.toString)
  val peruuntunut = Value(YhteenvedonValintaTila.PERUUNTUNUT.toString)
  val kesken = Value(YhteenvedonValintaTila.KESKEN.toString)
}

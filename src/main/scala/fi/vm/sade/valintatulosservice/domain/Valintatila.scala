package fi.vm.sade.valintatulosservice.domain

object Valintatila extends Enumeration {
  type Valintatila = Value
  val hyväksytty = Value("HYVAKSYTTY")
  val harkinnanvaraisesti_hyväksytty = Value("HARKINNANVARAISESTI_HYVAKSYTTY")
  val varasijalta_hyväksytty = Value("VARASIJALTA_HYVAKSYTTY")
  val varalla = Value("VARALLA")
  val peruutettu = Value("PERUUTETTU")
  val perunut = Value("PERUNUT")
  val hylätty = Value("HYLATTY")
  val peruuntunut = Value("PERUUNTUNUT")
  val kesken = Value("KESKEN")
}

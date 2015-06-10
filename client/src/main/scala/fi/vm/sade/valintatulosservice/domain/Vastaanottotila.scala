package fi.vm.sade.valintatulosservice.domain

object Vastaanottotila extends Enumeration {
  type Vastaanottotila = Value
  val kesken = Value("KESKEN")
  val vastaanottanut = Value("VASTAANOTTANUT")
  val ei_vastaanotettu_määräaikana = Value("EI_VASTAANOTETTU_MAARA_AIKANA")
  val perunut = Value("PERUNUT")
  val peruutettu = Value("PERUUTETTU")
  val ehdollisesti_vastaanottanut = Value("EHDOLLISESTI_VASTAANOTTANUT")
}

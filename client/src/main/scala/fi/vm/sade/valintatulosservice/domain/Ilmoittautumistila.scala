package fi.vm.sade.valintatulosservice.domain

object Ilmoittautumistila extends Enumeration {
  type Ilmoittautumistila = Value
  val ei_tehty = Value("EI_TEHTY")
  val läsnä_koko_lukuvuosi = Value("LASNA_KOKO_LUKUVUOSI")
  val poissa_koko_lukuvuosi = Value("POISSA_KOKO_LUKUVUOSI")
  val ei_ilmoittautunut = Value("EI_ILMOITTAUTUNUT")
  val läsnä_syksy = Value("LASNA_SYKSY")
  val poissa_syksy = Value ("POISSA_SYKSY")
  val läsnä = Value("LASNA")
  val poissa = Value("POISSA")
}

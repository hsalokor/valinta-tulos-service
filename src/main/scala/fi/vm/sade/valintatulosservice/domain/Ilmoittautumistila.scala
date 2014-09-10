package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila

object Ilmoittautumistila extends Enumeration {
  type Ilmoittautumistila = Value
  val ei_tehty = Value(IlmoittautumisTila.EI_TEHTY.toString)
  val läsnä_koko_lukuvuosi = Value(IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI.toString)
  val poissa_koko_lukuvuosi = Value(IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI.toString)
  val ei_ilmoittautunut = Value(IlmoittautumisTila.EI_ILMOITTAUTUNUT.toString)
  val läsnä_syksy = Value(IlmoittautumisTila.LASNA_SYKSY.toString)
  val poissa_syksy = Value (IlmoittautumisTila.POISSA_SYKSY.toString)
  val läsnä = Value(IlmoittautumisTila.LASNA.toString)
  val poissa = Value(IlmoittautumisTila.POISSA.toString)
}

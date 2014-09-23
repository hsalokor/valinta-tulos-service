package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Hakutoive, Hakemus}
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}

class HakemusRepositorySpec extends Specification with ITSetup {
  val repo = new HakemusRepository()

  "HakemusRepository with embedded mongo" should {
    "return list of oids" in {
      val hakutoiveet = repo.findHakutoiveOids("1.2.246.562.11.00000878229")
      hakutoiveet must_== Some(Hakemus("1.2.246.562.11.00000878229", List(Hakutoive("1.2.246.562.20.83060182827","1.2.246.562.10.83122281013"), Hakutoive("1.2.246.562.10.83122281012","1.2.246.562.10.83122281012"))))
    }
  }
}

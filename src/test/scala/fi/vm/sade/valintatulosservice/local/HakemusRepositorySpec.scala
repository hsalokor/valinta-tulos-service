package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.ITSpecification
import fi.vm.sade.valintatulosservice.domain.{Hakemus, Hakutoive}
import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixtures, HakemusRepository}
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures

class HakemusRepositorySpec extends ITSpecification {
  val repo = new HakemusRepository()

  "HakemusRepository" should {
    "return list of oids" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakemusFixtures = HakemusFixtures.defaultFixtures)

      val hakutoiveet = repo.findHakutoiveOids("1.2.246.562.11.00000878229")
      hakutoiveet must_== Some(Hakemus("1.2.246.562.11.00000878229", List(Hakutoive("1.2.246.562.20.83060182827", "1.2.246.562.10.83122281013"), Hakutoive("1.2.246.562.10.83122281012", "1.2.246.562.10.83122281012"))))
    }

    "return all applications from haku" in {
      val hakemukset = repo.findHakemukset("1.2.246.562.5.2013080813081926341928")
      hakemukset must_== Seq(Hakemus("1.2.246.562.11.00000441369", List(Hakutoive("1.2.246.562.5.72607738902", "1.2.246.562.10.591352080610"), Hakutoive("1.2.246.562.5.16303028779", "1.2.246.562.10.455978782510"))))
    }
  }
}

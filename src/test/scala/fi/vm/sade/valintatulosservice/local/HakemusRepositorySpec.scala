package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.ITSpecification
import fi.vm.sade.valintatulosservice.domain.{Hakemus, Hakutoive, Henkilotiedot}
import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixtures, HakemusRepository}

class HakemusRepositorySpec extends ITSpecification {
  val repo = new HakemusRepository()

  "HakemusRepository" should {
    "palauttaa yksittäisen Hakemuksen" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakemusFixtures = HakemusFixtures.defaultFixtures)

      val hakutoiveet = repo.findHakemus("1.2.246.562.11.00000878229")
      hakutoiveet must_== Some(Hakemus("1.2.246.562.11.00000878229", "1.2.246.562.24.14229104472", "FI",
        List(Hakutoive("1.2.246.562.20.83060182827", "1.2.246.562.10.83122281013"), Hakutoive("1.2.246.562.10.83122281012", "1.2.246.562.10.83122281012")),
        Henkilotiedot(Some("Teppo"), None)
      ))
    }

    "palauttaa kaikki Hakuun liittyvät Hakemukset" in {
      val hakemukset = repo.findHakemukset("1.2.246.562.5.2013080813081926341928")
      hakemukset must_== Seq(Hakemus("1.2.246.562.11.00000441369", "1.2.246.562.24.14229104472", "FI",
        List(Hakutoive("1.2.246.562.5.72607738902", "1.2.246.562.10.591352080610"), Hakutoive("1.2.246.562.5.16303028779", "1.2.246.562.10.455978782510")),
        Henkilotiedot(Some("Teppo"), Some("teppo@testaaja.fi")))
      )
    }
  }
}

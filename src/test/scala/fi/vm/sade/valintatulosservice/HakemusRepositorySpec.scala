package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.HakemusRepository
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}

class HakemusRepositorySpec extends Specification {

  implicit val appConfig = new AppConfig.IT
  val repo = new HakemusRepository()

  "HakemusRepository with embedded mongo" should {
    "return list of oids" in {
      val hakutoiveet = repo.findHakutoiveOids("1.2.246.562.11.00000878229")
      hakutoiveet must_== Some(List("1.2.246.562.20.83060182827"))
    }
  }

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

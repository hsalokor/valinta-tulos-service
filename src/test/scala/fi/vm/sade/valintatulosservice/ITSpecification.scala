package fi.vm.sade.valintatulosservice
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}

trait ITSpecification extends Specification with ITSetup {
  sequential

  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}

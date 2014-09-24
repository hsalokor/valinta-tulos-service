package fi.vm.sade.valintatulosservice.sijoittelu

import java.util.Optional

object Java8Conversions {
  implicit def fromOptional[T](opt: Optional[T]) = {
    if (opt.isPresent) {
      Some(opt.get)
    } else {
      None
    }
  }
}

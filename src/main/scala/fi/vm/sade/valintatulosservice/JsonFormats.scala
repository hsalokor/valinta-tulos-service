package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain.{Ilmoittautumistila, Vastaanottotila}
import org.json4s._
import org.json4s.ext.{JodaTimeSerializers, EnumNameSerializer}


object JsonFormats {
  val genericFormats = DefaultFormats ++ JodaTimeSerializers.all + new EnumNameSerializer(Vastaanottotila) + new EnumNameSerializer(Ilmoittautumistila)
  val jsonFormats: Formats = JsonFormats.genericFormats
}

trait JsonFormats {
  implicit val jsonFormats: Formats = JsonFormats.jsonFormats
}




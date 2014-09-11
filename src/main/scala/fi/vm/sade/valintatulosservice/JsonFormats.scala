package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain.{Valintatila, Vastaanotettavuustila, Ilmoittautumistila, Vastaanottotila}
import org.json4s._
import org.json4s.ext.{JodaTimeSerializers, EnumNameSerializer}


object JsonFormats {
  val enumSerializers = List(new EnumNameSerializer(Vastaanottotila), new EnumNameSerializer(Ilmoittautumistila), new EnumNameSerializer(Vastaanotettavuustila), new EnumNameSerializer(Valintatila))
  val genericFormats = DefaultFormats ++ JodaTimeSerializers.all ++ enumSerializers
  val jsonFormats: Formats = JsonFormats.genericFormats
}

trait JsonFormats {
  implicit val jsonFormats: Formats = JsonFormats.jsonFormats
}
package fi.vm.sade.valintatulosservice.json

import fi.vm.sade.valintatulosservice.domain._
import org.json4s._
import org.json4s.ext.{EnumNameSerializer, JodaTimeSerializers}


object JsonFormats {
  val enumSerializers = List(new EnumNameSerializer(Vastaanottotila), new EnumNameSerializer(Ilmoittautumistila), new EnumNameSerializer(Vastaanotettavuustila), new EnumNameSerializer(Valintatila), new EnumNameSerializer(Language))
  val genericFormats = DefaultFormats ++ JodaTimeSerializers.all ++ List(new LanguageMapSerializer()) ++ enumSerializers
  val jsonFormats: Formats = JsonFormats.genericFormats
}

trait JsonFormats {
  implicit val jsonFormats: Formats = JsonFormats.jsonFormats
}
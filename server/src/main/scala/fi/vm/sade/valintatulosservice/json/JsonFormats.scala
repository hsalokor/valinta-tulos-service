package fi.vm.sade.valintatulosservice.json

import fi.vm.sade.utils.json4s.GenericJsonFormats
import fi.vm.sade.valintatulosservice.domain._
import org.json4s._
import org.json4s.ext.EnumNameSerializer

object JsonFormats {
  val enumSerializers = List(new EnumNameSerializer(Vastaanottotila), new EnumNameSerializer(Ilmoittautumistila), new EnumNameSerializer(Vastaanotettavuustila), new EnumNameSerializer(Valintatila), new EnumNameSerializer(Language))
  val jsonFormats: Formats = GenericJsonFormats.genericFormats ++ List(new LanguageMapSerializer()) ++ enumSerializers
}

trait JsonFormats {
  implicit val jsonFormats: Formats = JsonFormats.jsonFormats
}
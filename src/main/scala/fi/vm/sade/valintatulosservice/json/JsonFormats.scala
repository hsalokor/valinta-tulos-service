package fi.vm.sade.valintatulosservice.json

import fi.vm.sade.utils.json4s.GenericJsonFormats
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.EnsikertalaisuusSerializer
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer

object JsonFormats {
  val enumSerializers = List(new EnumNameSerializer(Vastaanottotila), new EnumNameSerializer(Ilmoittautumistila), new EnumNameSerializer(Vastaanotettavuustila), new EnumNameSerializer(Valintatila), new EnumNameSerializer(Language))
  val jsonFormats: Formats = GenericJsonFormats.genericFormats ++ List(new LanguageMapSerializer()) ++ enumSerializers ++ List(new EnsikertalaisuusSerializer)

  def formatJson(found: AnyRef): String = {
    org.json4s.jackson.Serialization.write(found)(jsonFormats)
  }
}

trait JsonFormats {
  implicit val jsonFormats: Formats = JsonFormats.jsonFormats
}
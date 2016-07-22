package fi.vm.sade.valintatulosservice.json

import com.fasterxml.jackson.databind.ObjectMapper
import fi.vm.sade.utils.json4s.GenericJsonFormats
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.EnsikertalaisuusSerializer
import fi.vm.sade.valintatulosservice.{HakijanVastaanottoActionSerializer, VirkailijanVastaanottoActionSerializer}
import org.json4s.Formats
import org.json4s.ext.EnumNameSerializer

object JsonFormats {
  private val enumSerializers = List(new EnumNameSerializer(Ilmoittautumistila), new EnumNameSerializer(Vastaanotettavuustila), new EnumNameSerializer(Valintatila), new EnumNameSerializer(Language))
  val customSerializers = List(new LanguageMapSerializer()) ++ enumSerializers ++ List(new EnsikertalaisuusSerializer,
    new HakijanVastaanottoActionSerializer, new VirkailijanVastaanottoActionSerializer, new HakutoiveentulosSerializer)
  val jsonFormats: Formats = GenericJsonFormats.genericFormats ++ customSerializers

  def formatJson(found: AnyRef): String = {
    org.json4s.jackson.Serialization.write(found)(jsonFormats)
  }

  def javaObjectToJsonString(x: Object): String = new ObjectMapper().writeValueAsString(x)
}

trait JsonFormats {
  implicit val jsonFormats: Formats = JsonFormats.jsonFormats
}

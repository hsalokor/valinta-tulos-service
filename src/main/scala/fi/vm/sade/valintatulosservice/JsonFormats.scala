package fi.vm.sade.valintatulosservice

import org.json4s._

object JsonFormats {
  val genericFormats = DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
  val jsonFormats: Formats = JsonFormats.genericFormats
}

trait JsonFormats {
  implicit val jsonFormats: Formats = JsonFormats.jsonFormats
}




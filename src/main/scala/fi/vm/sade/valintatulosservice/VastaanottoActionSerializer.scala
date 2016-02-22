package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain.VastaanottoAction
import org.json4s.JsonAST.{JField, JString, JValue}
import org.json4s.jackson.compactJson
import org.json4s.{CustomSerializer, Formats, JObject, MappingException}

import scala.util.Try

class VastaanottoActionSerializer extends CustomSerializer[VastaanottoAction]((formats: Formats) => {
  def throwMappingException(json: String, cause: Option[Exception] = None) = {
    val message = s"Can't convert $json to ${classOf[VastaanottoAction].getSimpleName}."
    cause match {
      case Some(e) => throw new MappingException(s"$message : ${e.getMessage}", e)
      case None => throw new MappingException(message)
    }
  }
  ( {
    case json@JObject(JField("action", JString(action)) :: Nil) => Try(VastaanottoAction(action)).recoverWith {
      case cause: Exception => throwMappingException(compactJson(json), Some(cause)) }.get
    case json: JValue => throwMappingException(compactJson(json))
  }, {
    case x: VastaanottoAction => JObject(JField("action", JString(x.toString)))
  })
}
)

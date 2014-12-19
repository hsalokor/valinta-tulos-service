package fi.vm.sade.valintatulosservice.json

import fi.vm.sade.valintatulosservice.domain.LanguageMap.LanguageMap
import org.json4s._

class LanguageMapSerializer extends CustomSerializer[LanguageMap](format => (
  {
    case obj : JObject => obj.extract[LanguageMap](JsonFormats.jsonFormats, Manifest.classType(classOf[LanguageMap]))
  },
  {
    case map: LanguageMap @unchecked =>
      JObject(map.map(translation => JField(translation._1.toString(), JString(translation._2))).toList)
  }
  ))

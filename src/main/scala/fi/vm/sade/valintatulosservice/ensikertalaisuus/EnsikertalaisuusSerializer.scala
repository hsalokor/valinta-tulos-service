package fi.vm.sade.valintatulosservice.ensikertalaisuus

import java.util.Date

import org.json4s.Extraction._
import org.json4s.{Formats, CustomSerializer}
import org.json4s.JsonAST._
import org.json4s.JsonDSL._

class EnsikertalaisuusSerializer extends CustomSerializer[Ensikertalaisuus]((formats: Formats) => (
  {
    case ensikertalaisuus: JObject =>
      val JString(personOid) = ensikertalaisuus \ "personOid"
      implicit val f = formats
      ensikertalaisuus.findField(f => f._1 == "paattyi").map(_._2) match {
        case Some(d: JString) => EiEnsikertalainen(personOid, extract[Date](d))
        case _ => Ensikertalainen(personOid)
      }
  },
  {
    case ensikertalainen: Ensikertalainen =>
      "personOid" -> ensikertalainen.personOid
    case eiEnsikertalainen: EiEnsikertalainen =>
      ("personOid" -> eiEnsikertalainen.personOid) ~ ("paattyi" -> decompose(eiEnsikertalainen.paattyi)(formats))
  }
  )
)
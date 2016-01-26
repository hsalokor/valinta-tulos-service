package fi.vm.sade.valintatulosservice.ensikertalaisuus

import java.text.SimpleDateFormat

import fi.vm.sade.valintatulosservice.ensikertalaisuus.EnsikertalaisuusServlet.dateFormat
import org.json4s.{Formats, CustomSerializer}
import org.json4s.JsonAST._
import org.json4s.JsonDSL._

class EnsikertalaisuusSerializer extends CustomSerializer[Ensikertalaisuus] ((formats: Formats) => (
  {
    case ensikertalaisuus: JObject =>
      val JString(personOid) = ensikertalaisuus \ "personOid"
      ensikertalaisuus.findField(_._1 == "paattyi").map(_._2) match {
        case Some(JString(v)) => EiEnsikertalainen(
          personOid,
          new SimpleDateFormat(dateFormat).parse(v)
        )
        case _ => Ensikertalainen(personOid)
      }
  },
  {
    case ensikertalainen: Ensikertalainen =>
      "personOid" -> ensikertalainen.personOid
    case eiEnsikertalainen: EiEnsikertalainen =>
      ("personOid" -> eiEnsikertalainen.personOid) ~ ("paattyi" -> new SimpleDateFormat(dateFormat).format(eiEnsikertalainen.paattyi))
  }
  )
)
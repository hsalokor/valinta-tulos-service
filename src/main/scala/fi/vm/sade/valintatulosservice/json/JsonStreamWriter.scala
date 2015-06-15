package fi.vm.sade.valintatulosservice.json

import java.io.PrintWriter

import org.json4s.Formats

object JsonStreamWriter {
  def writeJsonStream(objects: Stream[AnyRef], writer: PrintWriter)(implicit formats: Formats): Unit = {
    writer.print("[")
    objects.zipWithIndex.foreach { case (item, index) =>
      if (index > 0) {
        writer.print(",")
      }
      writer.print(org.json4s.jackson.Serialization.write(item))
    }
    writer.print("]")
  }
}

package fi.vm.sade.valintatulosservice

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import scala.util.{Failure, Success}

class HenkiloviiteSynchronizerServlet(henkiloviiteSynchronizer: HenkiloviiteSynchronizer) extends HttpServlet {

  override def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    henkiloviiteSynchronizer.startSync() match {
      case Success(()) =>
        writeResponse(200, "Started", response)
      case Failure(e) =>
        writeResponse(400, e.getMessage, response)
    }
  }

  override def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val state = henkiloviiteSynchronizer.getState
    val (code, msg) = state match {
      case Started(at) if at.isBefore(LocalDateTime.now().minus(5, ChronoUnit.MINUTES)) =>
        (500, "Too much time elapsed since sync started")
      case Stopped(at, _) if at.isBefore(LocalDateTime.now().minus(1, ChronoUnit.DAYS)) =>
        (500, "Too much time elapsed since last sync")
      case Stopped(_, Failure(_)) =>
        (500, "")
      case _ =>
        (200, "")
    }
    writeResponse(code, s"${state.toString} $msg", response)
  }

  private def writeResponse(status:Int, message:String, response: HttpServletResponse ) = {
    response.setStatus(status)
    response.setCharacterEncoding("UTF-8")
    response.setContentType("text/plain")
    response.getOutputStream.println(message)
    response.getOutputStream.close()
  }
}

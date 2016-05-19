package fi.vm.sade.valintatulosservice

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

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
    writeResponse(200, henkiloviiteSynchronizer.status(), response)
  }

  private def writeResponse(status:Int, message:String, response: HttpServletResponse ) = {
    response.setStatus(status)
    response.setCharacterEncoding("UTF-8")
    response.setContentType("text/plain")
    response.getOutputStream.println(message)
    response.getOutputStream.close()
  }
}

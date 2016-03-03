package fi.vm.sade.valintatulosservice

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

import scala.util.{Failure, Success}

class HenkiloviiteSynchronizerServlet(henkiloviiteSynchronizer: HenkiloviiteSynchronizer) extends HttpServlet {
  override def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    henkiloviiteSynchronizer.sync() match {
      case Success(()) =>
        response.setStatus(200)
        response.getOutputStream.println("synced")
        response.getOutputStream.close()
      case Failure(e) =>
        response.setStatus(500)
        response.getOutputStream.println("sync failed")
        response.getOutputStream.close()
    }
  }
}

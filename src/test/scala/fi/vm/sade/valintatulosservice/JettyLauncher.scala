package fi.vm.sade.valintatulosservice

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object JettyLauncher {
  def main(args: Array[String]) {
    val port = 8097
    val server = new Server(port)
    val context = new WebAppContext()
    context.setResourceBase("src/main/webapp")
    context.setContextPath("/valinta-tulos-service")
    context.setDescriptor("src/main/webapp/WEB-INF/web.xml")
    server.setHandler(context)
    server.start
    server.join
  }
}
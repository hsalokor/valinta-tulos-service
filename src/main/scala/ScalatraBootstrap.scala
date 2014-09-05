import javax.servlet.ServletContext

import fi.vm.sade.valintatulosservice.ValintatulosServlet
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(new ValintatulosServlet, "/")
  }
}
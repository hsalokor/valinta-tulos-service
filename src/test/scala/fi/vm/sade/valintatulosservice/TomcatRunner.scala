package fi.vm.sade.valintatulosservice

import java.io.File

import org.apache.catalina.startup.Tomcat

/**
 * Runs the application in an embedded Tomcat. Suitable for running in IDEA or Eclipse.
 */
object TomcatRunner extends App {
  val tomcat = new Tomcat()
  val webappDirLocation = "src/main/webapp/"

  tomcat.setPort(8097)
  tomcat.addWebapp("/valinta-tulos-service", new File(webappDirLocation).getAbsolutePath())

  tomcat.start()
  tomcat.getServer().await()
}

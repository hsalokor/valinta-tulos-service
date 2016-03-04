package fi.vm.sade.valintatulosservice

import java.io.FileInputStream
import java.util.{Calendar, Properties}
import java.util.concurrent.{TimeUnit, ScheduledThreadPoolExecutor}

import scala.collection.JavaConversions._

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletHandler, ServletHolder}

object HenkiloviiteSynchronizerApp {
  def main(args: Array[String]): Unit = {
    val config = readConfig()
    val henkiloviiteClient = new HenkiloviiteClient(config)
    val db = new HenkiloviiteDb(config)
    val synchronizer = new HenkiloviiteSynchronizer(henkiloviiteClient, db)
    val servlet = new HenkiloviiteSynchronizerServlet(synchronizer)

    new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(synchronizer, hoursForSchedulerStart(config), 24, TimeUnit.HOURS)

    val server = new Server(8080)
    val servletHandler = new ServletHandler
    server.setHandler(servletHandler)
    servletHandler.addServletWithMapping(new ServletHolder(servlet), "/*")
    server.start()
    server.join()

  }

  private def readConfig(): Properties = {
    Option(System.getProperty("valintatuloshenkiloviitesynchronizer.properties")) match {
      case Some(configFile) =>
        val config = new Properties()
        config.load(new FileInputStream(configFile))
        for (k <- System.getProperties.stringPropertyNames) {
          config.setProperty(k, System.getProperty(k))
        }
        config
      case None => System.getProperties
    }
  }

  private def hoursForSchedulerStart(config:Properties):Long = {
    val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val startHour = config.getProperty("henkiloviite.scheduler.start.hour", "02").toLong

    if(hourOfDay < startHour) startHour - hourOfDay
    else 24 - hourOfDay + startHour
  }

}

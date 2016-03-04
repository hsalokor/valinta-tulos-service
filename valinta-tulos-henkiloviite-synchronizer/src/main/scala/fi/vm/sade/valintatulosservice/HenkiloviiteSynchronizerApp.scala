package fi.vm.sade.valintatulosservice

import java.io.FileInputStream
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import java.util.{Calendar, Properties}

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletHandler, ServletHolder}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

object HenkiloviiteSynchronizerApp {
  val logger = LoggerFactory.getLogger(HenkiloviiteSynchronizerApp.getClass)

  def main(args: Array[String]): Unit = {
    val config = readConfig()
    val henkiloviiteClient = new HenkiloviiteClient(config)
    val db = new HenkiloviiteDb(config)
    val synchronizer = new HenkiloviiteSynchronizer(henkiloviiteClient, db)
    val servlet = new HenkiloviiteSynchronizerServlet(synchronizer)
    val synchronizerScheduler = new ScheduledThreadPoolExecutor(1)

    hoursUntilSchedulerStart(config) match {
      case Some(delay) =>
        synchronizerScheduler.scheduleAtFixedRate(synchronizer, delay, 24, TimeUnit.HOURS)
      case None =>
        logger.warn("henkiloviite.scheduler.start.hour not given, scheduled synchronization not started.")
    }

    val server = new Server(8080)
    val servletHandler = new ServletHandler
    server.setHandler(servletHandler)
    servletHandler.addServletWithMapping(new ServletHolder(servlet), "/*")
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        server.stop()
        synchronizerScheduler.shutdown()
      }
    }))
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

  private def hoursUntilSchedulerStart(config:Properties): Option[Long] = {
    Option(config.getProperty("henkiloviite.scheduler.start.hour")).map(_.toLong).map(startHour => {
      val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
      if(hourOfDay < startHour) startHour - hourOfDay
      else 24 - hourOfDay + startHour
    })
  }

}

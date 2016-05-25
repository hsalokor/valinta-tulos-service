package fi.vm.sade.valintatulosservice

import java.util.Calendar
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import ch.qos.logback.access.jetty.RequestLogImpl
import org.eclipse.jetty.server.{RequestLog, Server}
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.slf4j.LoggerFactory

object HenkiloviiteSynchronizerApp {
  val logger = LoggerFactory.getLogger(HenkiloviiteSynchronizerApp.getClass)

  def main(args: Array[String]): Unit = {
    val config = Configuration.read()
    val henkiloviiteClient = new HenkiloviiteClient(config.authentication)
    val db = new HenkiloviiteDb(config.db)
    val synchronizer = new HenkiloviiteSynchronizer(henkiloviiteClient, db)
    val servlet = new HenkiloviiteSynchronizerServlet(synchronizer, config.scheduler.intervalHours)
    val buildversionServlet = new BuildversionServlet(config.buildversion)

    val server = new Server(config.port)
    val servletContext = new ServletContextHandler()
    servletContext.setContextPath("/valinta-tulos-henkiloviite-synchronizer")
    servletContext.addServlet(new ServletHolder(buildversionServlet), "/buildversion.txt")
    servletContext.addServlet(new ServletHolder(servlet), "/*")
    server.setHandler(servletContext)

    server.setRequestLog(requestLog(config))

    val synchronizerScheduler = startScheduledSynchronization(config.scheduler, synchronizer)
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = {
        server.stop()
        synchronizerScheduler.shutdown()
      }
    }))
    server.start()
    server.join()
  }

  private def requestLog(config: Configuration): RequestLog = {
    val requestLog = new RequestLogImpl
    requestLog.setFileName(config.accessLogConfigPath)
    requestLog
  }

  private def hoursUntilSchedulerStart(startHour: Long): Long = {
    val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    if (hourOfDay < startHour) startHour - hourOfDay
    else 24 - hourOfDay + startHour
  }

  private def startScheduledSynchronization(config: SchedulerConfiguration,
                                            synchronizer: HenkiloviiteSynchronizer): ScheduledThreadPoolExecutor = {
    val scheduler = new ScheduledThreadPoolExecutor(1)
    (config.startHour.map(hoursUntilSchedulerStart), config.intervalHours) match {
      case (Some(delay), Some(interval)) =>
        scheduler.scheduleAtFixedRate(synchronizer, delay, interval, TimeUnit.HOURS)
        logger.info(s"Scheduled synchronization started, next synchronization in $delay hours.")
      case (_, _) =>
        logger.warn("Scheduler start hour or run interval not given, scheduled synchronization not started.")
    }
    scheduler
  }
}

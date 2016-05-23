package fi.vm.sade.valintatulosservice

import java.util.Calendar
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import ch.qos.logback.access.jetty.RequestLogImpl
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.servlet.{ServletHandler, ServletHolder}
import org.slf4j.LoggerFactory

object HenkiloviiteSynchronizerApp {
  val logger = LoggerFactory.getLogger(HenkiloviiteSynchronizerApp.getClass)

  def main(args: Array[String]): Unit = {
    val config = Configuration.read()
    val henkiloviiteClient = new HenkiloviiteClient(config.authentication)
    val db = new HenkiloviiteDb(config.db)
    val synchronizer = new HenkiloviiteSynchronizer(henkiloviiteClient, db)
    val servlet = new HenkiloviiteSynchronizerServlet(synchronizer)

    val server = new Server(config.port)
    val context = new ContextHandler("/valinta-tulos-henkiloviite-synchronizer")
    val servletHandler = new ServletHandler
    servletHandler.addServletWithMapping(new ServletHolder(servlet), "/*")
    context.setHandler(servletHandler)
    server.setHandler(context)

    val requestLog = new RequestLogImpl
    requestLog.setFileName(config.accessLogConfigPath)
    server.setRequestLog(requestLog)

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

  private def hoursUntilSchedulerStart(startHour: Long): Long = {
        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hourOfDay < startHour) startHour - hourOfDay
        else 24 - hourOfDay + startHour
  }

  private def startScheduledSynchronization(config: SchedulerConfiguration,
                                            synchronizer: HenkiloviiteSynchronizer): ScheduledThreadPoolExecutor = {
    val scheduler = new ScheduledThreadPoolExecutor(1)
    config.startHour.map(hoursUntilSchedulerStart) match {
      case Some(delay) =>
        scheduler.scheduleAtFixedRate(synchronizer, delay, 24, TimeUnit.HOURS)
        logger.info(s"Scheduled synchronization started, next synchronization in $delay hours.")
      case None =>
        logger.warn("Scheduler start hour not given, scheduled synchronization not started.")
    }
    scheduler
  }
}

package fi.vm.sade.valintatulosservice

import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

import scala.util.{Failure, Try, Success}

class HenkiloviiteSynchronizer(henkiloClient: HenkiloviiteClient, db: HenkiloviiteDb) extends Runnable {

  val logger = LoggerFactory.getLogger(classOf[HenkiloviiteSynchronizer])

  def startSync(): Try[Unit] = {
    if(startRunning()) {
      logger.info("Starting henkiloviite sync.")
      new Thread(new HenkiloviiteRunnable).start()
      Success(())
    } else {
      logger.warn("Attempt to start henkiloviite sync while already running.")
      Failure(new IllegalStateException("Already running."))
    }
  }

  def run(): Unit = {
    if(startRunning()) {
      logger.info("Starting henkiloviite sync by scheduler.")
      (new HenkiloviiteRunnable).run()
    } else {
      logger.warn("Attempt to start henkiloviite sync by scheduler while already running.")
    }
  }

  def status(): String = running.get() match {
    case true => "Running"
    case false => lastRunStatus
  }

  private class HenkiloviiteRunnable extends Runnable {
    def run(): Unit = {
      (for {
        henkiloviitteet <- henkiloClient.fetchHenkiloviitteet()
        _ <- db.refresh(henkiloviitteet.toSet)
      } yield {
        logger.info("Henkiloviite sync finished successfully.")
        stopRunning("OK")
      }).recover{
        case e => {
          logger.error("Henkiloviite sync failed.", e)
          stopRunning(s"Not OK. ${e.getMessage}")
        }
      }
    }
  }

  private val running:AtomicBoolean = new AtomicBoolean(false)
  private var lastRunStatus:String = "Not run"

  private def startRunning(): Boolean = {
    running.compareAndSet(false, true)
  }

  private def stopRunning(status:String) = {
    lastRunStatus = status
    running.set(false)
  }
}



package fi.vm.sade.valintatulosservice

import java.util.concurrent.atomic.AtomicBoolean


import scala.util.{Failure, Try, Success}

class HenkiloviiteSynchronizer(henkiloClient: HenkiloviiteClient, db: HenkiloviiteDb) {

  def startSync(): Try[Unit] = {
    if(startRunning()) {
      new Thread(new HenkiloviiteRunnable).start()
      Success(())
    } else {
      Failure(new IllegalStateException("Already running."))
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
        stopRunning("OK")
      }).recover{
        case e => stopRunning(s"Not OK. ${e.getMessage}")
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



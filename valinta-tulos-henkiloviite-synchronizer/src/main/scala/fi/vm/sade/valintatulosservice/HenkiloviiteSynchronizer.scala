package fi.vm.sade.valintatulosservice

import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

case class HenkiloRelation(personOid: String, linkedOid: String)

class HenkiloviiteSynchronizer(henkiloClient: HenkiloviiteClient, db: HenkiloviiteDb) extends Runnable {

  private val logger = LoggerFactory.getLogger(classOf[HenkiloviiteSynchronizer])
  private val running: AtomicBoolean = new AtomicBoolean(false)
  private var lastRunStatus: String = "Not run"

  def startSync(): Try[Unit] = {
    if(startRunning()) {
      logger.info("Starting henkiloviite sync.")
      Try(new Thread(new HenkiloviiteRunnable).start())
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
        henkiloviitteetList <- henkiloClient.fetchHenkiloviitteet()
        _ <- db.refresh(henkiloRelations(henkiloviitteetList))
      } yield ()) match {
        case Success(_) =>
          logger.info("Henkiloviite sync finished successfully.")
          stopRunning("OK")
        case Failure(e) =>
          logger.error("Henkiloviite sync failed.", e)
          stopRunning(s"Not OK. ${e.getMessage}")
      }
    }

    private def relatedHenkilot(henkiloviitteet: Seq[Henkiloviite]): Seq[Seq[String]] = {
      henkiloviitteet
        .groupBy(_.masterOid)
        .map({ case (masterOid, slaves) => (masterOid, masterOid +: slaves.map(_.henkiloOid)) })
        .values.toSeq
    }

    private def allOrderedPairs[A](xs: Seq[A]): Seq[(A, A)] = {
      xs.permutations.map({ case x :: y :: _ => (x, y) }).toSeq
    }

    private def henkiloRelations(henkiloviitteet: Seq[Henkiloviite]): Set[HenkiloRelation] = {
      relatedHenkilot(henkiloviitteet).flatMap(allOrderedPairs(_).map(t => HenkiloRelation(t._1, t._2))).toSet
    }
  }

  private def startRunning(): Boolean = {
    running.compareAndSet(false, true)
  }

  private def stopRunning(status:String) = {
    lastRunStatus = status
    running.set(false)
  }
}



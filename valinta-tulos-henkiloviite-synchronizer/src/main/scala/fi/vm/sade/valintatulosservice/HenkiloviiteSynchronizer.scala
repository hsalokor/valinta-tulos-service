package fi.vm.sade.valintatulosservice

import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

case class HenkiloRelation(personOid: String, linkedOid: String)

sealed trait State

case object NotStarted extends State
case class Started(at: Date) extends State
case class Stopped(at: Date, result: Try[Unit]) extends State

class HenkiloviiteSynchronizer(henkiloClient: HenkiloviiteClient, db: HenkiloviiteDb) extends Runnable {

  private val logger = LoggerFactory.getLogger(classOf[HenkiloviiteSynchronizer])
  private val running: AtomicBoolean = new AtomicBoolean(false)
  private var state: State = NotStarted

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

  def status(): String = state match {
    case NotStarted => "Not started"
    case Started(at) => s"Running since $at"
    case Stopped(at, Success(())) => s"Last run succeeded $at"
    case Stopped(at, Failure(e)) => s"Last run failed $at: ${e.getMessage}"
  }

  private class HenkiloviiteRunnable extends Runnable {
    def run(): Unit = {
      val result: Try[Unit] = for {
        henkiloviitteetList <- henkiloClient.fetchHenkiloviitteet()
        _ <- db.refresh(HenkiloviiteSynchronizer.henkiloRelations(henkiloviitteetList))
      } yield ()
      state = Stopped(new Date(), result)
      result match {
        case Success(_) =>
          logger.info("Henkiloviite sync finished successfully.")
        case Failure(e) =>
          logger.error("Henkiloviite sync failed.", e)
      }
      running.compareAndSet(true, false)
    }
  }

  private def startRunning(): Boolean = {
    if (running.compareAndSet(false, true)) {
      state = Started(new Date())
      true
    } else {
      false
    }
  }
}

object HenkiloviiteSynchronizer {
  def relatedHenkilot(henkiloviitteet: Seq[Henkiloviite]): Seq[Seq[String]] = {
    henkiloviitteet
      .groupBy(_.masterOid)
      .map({ case (masterOid, slaves) => (masterOid, masterOid +: slaves.map(_.henkiloOid)) })
      .values.toSeq
  }

  def allPairs[A](xs: Seq[A]): Seq[(A, A)] = {
    xs.permutations.map({ case x :: y :: _ => (x, y) }).toSeq
  }

  def henkiloRelations(henkiloviitteet: Seq[Henkiloviite]): Set[HenkiloRelation] = {
    relatedHenkilot(henkiloviitteet).flatMap(allPairs(_).map(t => HenkiloRelation(t._1, t._2))).toSet
  }
}

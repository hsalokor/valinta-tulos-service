package fi.vm.sade.valintatulosservice.config

import java.io.File
import java.nio.file.Files

import fi.vm.sade.utils.slf4j.Logging
import org.apache.commons.io.FileUtils

import scala.sys.process.stringToProcess

class ItPostgres extends Logging {
  val dataDirName = "valintarekisteri-it-db"
  val dbName = "valintarekisteri"
  val port = 65432
  val startStopRetries = 30
  val startStopRetryIntervalMillis = 100
  private val dataDirFile = new File(dataDirName)
  if (!dataDirFile.isDirectory) {
    Files.createDirectory(dataDirFile.toPath)
  }
  val dataDirPath = dataDirFile.getAbsolutePath
  logger.info(s"starting postgres to port $port with data directory $dataDirPath")
  s"initdb -D $dataDirPath".!

  private def isAcceptingConnections(): Boolean = {
    s"pg_isready -q -t 1 -h localhost -p $port -d $dbName".! == 0
  }

  private def readPid: Option[Int] = {
    val pidFile = new File(dataDirFile, "postmaster.pid")
    if (!pidFile.canRead) {
      None
    } else {
      Some(FileUtils.readFileToString(pidFile).split("\n")(0).toInt)
    }
  }

  private def tryTimes(times: Int, sleep: Int)(thunk: () => Boolean): Boolean = times match {
    case n if n < 1 => false
    case 1 => thunk()
    case n => thunk() || { Thread.sleep(sleep); tryTimes(n - 1, sleep)(thunk) }
  }

  def start() {
    readPid match {
      case Some(pid) => {
        println(s"PostgreSQL pid $pid is found in pid file, not touching the database.")
      }
      case None => {
        println(s"PostgreSQL pid file cannot be read, starting:")
        s"postgres --config_file=postgresql/postgresql.conf -D $dataDirPath -p $port".run()
        if (!tryTimes(startStopRetries, startStopRetryIntervalMillis)(isAcceptingConnections)) {
          throw new RuntimeException(s"postgres not accepting connections in port $port after $startStopRetries attempts with $startStopRetryIntervalMillis ms intervals")
        }
        s"dropdb -p $port --if-exists $dbName".!
        s"createdb -p $port $dbName".!
      }
    }
  }

  def stop() {
    readPid match {
      case Some(pid) => {
        println(s"Killing PostgreSQL process $pid")
        s"kill -s SIGTERM $pid".!
        if (!tryTimes(startStopRetries, startStopRetryIntervalMillis)(() => !isAcceptingConnections())) {
          logger.error(s"postgres in pid $pid did not stop gracefully after $startStopRetries attempts with $startStopRetryIntervalMillis ms intervals")
        }
      }
      case None => println("No PostgreSQL pid found, not trying to stop it.")
    }
  }
}

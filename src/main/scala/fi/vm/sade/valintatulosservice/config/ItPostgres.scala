package fi.vm.sade.valintatulosservice.config

import java.io.File
import java.nio.file.Files

import scala.sys.process.stringToProcess
import fi.vm.sade.utils.slf4j.Logging

class ItPostgres extends Logging {
  val dataDirName = "valintarekisteri-it-db"
  val dbName = "valintarekisteri"
  val port = 65432
  val startRetries = 30
  val startRetriesIntervalMillis = 100
  private val dataDirFile = new File(dataDirName)
  if (!dataDirFile.isDirectory) {
    Files.createDirectory(dataDirFile.toPath)
  }
  val dataDirPath = dataDirFile.getAbsolutePath
  logger.info(s"starting postgres to port $port with data directory $dataDirPath")
  s"initdb -D $dataDirPath".!
  private val dbProcess = s"postgres --config_file=postgresql/postgresql.conf -D $dataDirPath -p $port".run()

  private def isAcceptingConnections(): Boolean = {
    s"pg_isready -q -t 1 -h localhost -p $port -d $dbName".! == 0
  }

  def start() {
    var retries = startRetries
    while (!isAcceptingConnections() && retries > 0) {
      retries -= 1
      Thread.sleep(startRetriesIntervalMillis)
    }
    if (!isAcceptingConnections()) {
      throw new RuntimeException(s"postgres not accepting connections in port $port after $startRetries attempts with $startRetriesIntervalMillis ms intervals")
    }
    s"dropdb -p $port --if-exists $dbName".!
    s"createdb -p $port $dbName".!
  }

  def stop() {
    logger.warn("TODO: stop db if needed")
  }
}

package fi.vm.sade.valintatulosservice.config

import java.io.File
import java.nio.file.Files

import scala.sys.process.stringToProcess

class ItPostgres {
  val dataDirName = "valintarekisteri-it-db"
  val dbName = "valintarekisteri"
  val port = 65432
  val startTimeoutRetries = 30
  val startTimeoutPollingIntervalMillis = 100
  private val dataDirFile = new File(dataDirName)
  if (!dataDirFile.isDirectory) {
    Files.createDirectory(dataDirFile.toPath)
  }
  val dataDirPath = dataDirFile.getAbsolutePath
  s"initdb -D $dataDirPath".!
  private val dbProcess = s"postgres --config_file=postgresql/postgresql.conf -D $dataDirName -p $port".run()

  private def isAcceptingConnections(): Boolean = {
    s"pg_isready -q -t 1 -h localhost -p $port -d $dbName".! == 0
  }

  def start() {
    var retries = startTimeoutRetries
    while (!isAcceptingConnections() && retries > 0) {
      retries -= 1
      Thread.sleep(startTimeoutPollingIntervalMillis)
    }
    if (!isAcceptingConnections()) {
      val msg = s"postgres not accepting connections in port $port within $startTimeoutRetries attempts of $startTimeoutPollingIntervalMillis ms intervals"
      println(msg)
      throw new RuntimeException(msg)
    }
    s"dropdb -p $port --if-exists $dbName".!
    s"createdb -p $port $dbName".!
  }

  def stop() {
    println("TODO: stop db if needed")
  }
}

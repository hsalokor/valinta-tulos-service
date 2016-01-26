package fi.vm.sade.valintatulosservice.config

import java.io.File
import java.nio.file.Files

import scala.sys.process.stringToProcess

class ItPostgres {
  val dataDirName = "valintarekisteri-it-db"
  val dbName = "valintarekisteri"
  val port = 65432
  private val dataDirFile = new File(dataDirName)
  if (!dataDirFile.isDirectory) {
    Files.createDirectory(dataDirFile.toPath)
  }
  val dataDirPath = dataDirFile.getAbsolutePath
  s"initdb -D $dataDirPath".!
  s"postgres --config_file=postgresql/postgresql.conf -D $dataDirName -p $port".run()

  def start() {
    Thread.sleep(5000) // TODO replace for real wait
    s"dropdb -p $port --if-exists $dbName".!
    s"createdb -p $port $dbName".!
  }

  def stop() {
    println("TODO: stop db if needed")
  }
}

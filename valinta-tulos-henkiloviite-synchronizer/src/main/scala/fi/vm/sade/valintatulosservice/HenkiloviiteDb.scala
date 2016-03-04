package fi.vm.sade.valintatulosservice

import java.util.Properties

import fi.vm.sade.utils.slf4j.Logging

import java.sql._

class HenkiloviiteDb(dbConfig: Properties) extends Logging {
  val user = dbConfig.getProperty("henkiloviite.database.username")
  val password = dbConfig.getProperty("henkiloviite.database.password")
  val url = getConfiguration("henkiloviite.database.url")

  logger.info(s"Using database configuration user=${user} and url=${url} with password")

  Class.forName("org.postgresql.Driver")

  def refresh(henkiloviitteet: Set[Henkiloviite]): Unit = {
    var connection:Connection = null
    var statement:PreparedStatement = null

    try {
      connection = DriverManager.getConnection(url, user, password)
      connection.setAutoCommit(false)

      logger.info(s"Emptying henkiloviitteet table")
      val delete = "delete from henkiloviitteet"

      statement = connection.prepareStatement(delete)
      statement.execute()

      statement.close()

      val insert = "insert into henkiloviitteet (master_oid, henkilo_oid) values (?, ?)"
      statement = connection.prepareStatement(insert)

      logger.info(s"Inserting ${henkiloviitteet.size} henkiloviite")

      for((henkiloviite, i) <- henkiloviitteet.zipWithIndex) {
        statement.setString(1, henkiloviite.masterOid)
        statement.setString(2, henkiloviite.henkiloOid)
        statement.addBatch()

        if(0 == i % 1000) {
          statement.executeBatch()
          statement.clearBatch()
        }

        statement.clearParameters()
      }

      statement.executeBatch()
      connection.commit()

      logger.info("Henkiloviitteet updated nicely")

    } catch {
      case e:Exception if null != connection => try {
        logger.error("Something when wrong. Going to rollback.", e)
        connection.rollback()
      } catch {
        case e: Exception => logger.error("Rollback failed.", e)
      }
    }
    finally {
      closeInTry(statement)
      closeInTry(connection)

    }
  }

  private def closeInTry(closeable:AutoCloseable) = {
    if(null != closeable) {
      try {
        closeable.close()
      } catch {
        case e:Exception => logger.error("Closing a database resource failed.", e)
      }
    }
  }

  private def getConfiguration(key:String):String = {
    dbConfig.getProperty(key) match {
      case null => throw new RuntimeException(s"Configuration $key is missing")
      case conf => conf
    }
  }

}
package fi.vm.sade.valintatulosservice.config

import java.io.File

import com.typesafe.config._
import fi.vm.sade.valintatulosservice.{SecuritySettings, Logging}

import scala.collection.JavaConversions._

object ApplicationSettings extends Logging {
  def loadSettings(fileLocation: String): ApplicationSettings = {
    val configFile = new File(fileLocation)
    if (configFile.exists()) {
      logger.info("Using configuration file " + configFile)
      val settings: Config = ConfigFactory.load(ConfigFactory.parseFile(configFile))
      val applicationSettings = new ApplicationSettings(settings)
      applicationSettings
    } else {
      throw new RuntimeException("Configuration file not found: " + fileLocation)
    }
  }
}

case class ApplicationSettings(config: Config) {
  val hakemusMongoConfig: MongoConfig = getMongoConfig(config.getConfig("hakemus.mongodb"))
  val valintatulosMongoConfig: MongoConfig = getMongoConfig(config.getConfig("sijoittelu-service.mongodb"))
  val ohjausparametritUrl = config.getString("valinta-tulos-service.ohjausparametrit.url")
  val tarjontaUrl = config.getString("tarjonta-service.url")
  val securitySettings = new SecuritySettings(config)
  val ilmoittautuminenEnabled = {
    val value = config.getString("valinta-tulos-service.ilmoittautuminen.enabled")
    if(value.trim.length > 0) {
      value.toBoolean
    }
    else {
      false
    }
  }

  def withOverride(keyValuePair : (String, String)) = {
    ApplicationSettings(config.withValue(keyValuePair._1, ConfigValueFactory.fromAnyRef(keyValuePair._2)))
  }

  private def getMongoConfig(config: Config) = {
    MongoConfig(
      config.getString("uri"),
      config.getString("dbname")
    )
  }

  def toProperties = {
    val keys = config.entrySet().toList.map(_.getKey)
    keys.map { key =>
      (key, config.getString(key))
    }.toMap
  }
}
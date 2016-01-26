package fi.vm.sade.valintatulosservice.config

import com.typesafe.config.{ConfigFactory, Config}
import fi.vm.sade.utils.config.MongoConfig
import fi.vm.sade.valintatulosservice.SecuritySettings

case class ApplicationSettings(config: Config) extends fi.vm.sade.utils.config.ApplicationSettings(config) {
  val hakemusMongoConfig: MongoConfig = getMongoConfig(config.getConfig("hakemus.mongodb"))
  val valintatulosMongoConfig: MongoConfig = getMongoConfig(config.getConfig("sijoittelu-service.mongodb"))
  val ohjausparametritUrl = config.getString("valinta-tulos-service.ohjausparametrit.url")
  val tarjontaUrl = config.getString("tarjonta-service.url")
  val securitySettings = new SecuritySettings(config)
  val valintaRekisteriDbConfig = ValintaRekisteriDbConfig(config)

  val ilmoittautuminenEnabled = {
    val value = config.getString("valinta-tulos-service.ilmoittautuminen.enabled")
    if(value.trim.length > 0) {
      value.toBoolean
    }
    else {
      false
    }
  }
}

object ApplicationSettingsParser extends fi.vm.sade.utils.config.ApplicationSettingsParser[ApplicationSettings] {
  override def parse(config: Config) = ApplicationSettings(config)
}

case class ValintaRekisteriDbConfig(url: String, user: String, password: String) {
  def toConfig: Config = {
    ConfigFactory.parseString(s"""{ "db": { "username" : "$user","password": "$password","url": "$url"} }""")
  }
}


object ValintaRekisteriDbConfig {
  def apply(config: Config): ValintaRekisteriDbConfig = {
    ValintaRekisteriDbConfig(config.getString("valinta-tulos-service.valintarekisteri.db.url"),
      config.getString("valinta-tulos-service.valintarekisteri.db.user"),
      config.getString("valinta-tulos-service.valintarekisteri.db.password"))
  }
}

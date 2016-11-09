package fi.vm.sade.valintatulosservice.valintarekisteri.config

import com.typesafe.config.Config
import fi.vm.sade.utils.config.MongoConfig
import org.apache.commons.lang3.BooleanUtils

case class ValintarekisteriApplicationSettings(config: Config) extends ApplicationSettings(config) {
  //val hakemusMongoConfig: MongoConfig = getMongoConfig(config.getConfig("hakemus.mongodb"))
  //val valintatulosMongoConfig: MongoConfig = getMongoConfig(config.getConfig("sijoittelu-service.mongodb"))
  //val ohjausparametritUrl = withConfig(_.getString("valinta-tulos-service.ohjausparametrit.url"))
  val tarjontaUrl = withConfig(_.getString("tarjonta-service.url"))
  //val sijoitteluServiceRestUrl = withConfig(_.getString("sijoittelu-service.rest.url"))
  //val securitySettings = new SecuritySettings(config)
  val valintaRekisteriDbConfig = withConfig(_.getConfig("valinta-tulos-service.valintarekisteri.db"))
  //val valintaRekisteriEnsikertalaisuusMaxPersonOids = withConfig(_.getInt("valinta-tulos-service.valintarekisteri.ensikertalaisuus.max.henkilo.oids"))
  //val lenientTarjontaDataParsing: Boolean = BooleanUtils.isTrue(withConfig(_.getBoolean("valinta-tulos-service.parseleniently.tarjonta")))
  //val lenientSijoitteluntuloksetParsing: Boolean = BooleanUtils.isTrue(withConfig(_.getBoolean("valinta-tulos-service.parseleniently.sijoitteluajontulos")))
  val koodistoUrl = withConfig(_.getString("koodisto-service.rest.url"))

  val ilmoittautuminenEnabled = {
    val value = config.getString("valinta-tulos-service.ilmoittautuminen.enabled")
    if(value.trim.length > 0) {
      value.toBoolean
    }
    else {
      false
    }
  }

  private def withConfig[T](operation: Config => T): T = {
    try {
      operation(config)
    } catch {
      case e: Throwable =>
        System.err.println(s"Cannot instantiate ${classOf[ValintarekisteriApplicationSettings]} : ${e.getMessage}")
        e.printStackTrace()
        throw e
    }
  }
}

object ValintarekisteriApplicationSettingsParser extends fi.vm.sade.utils.config.ApplicationSettingsParser[ValintarekisteriApplicationSettings] {
  override def parse(config: Config) = ValintarekisteriApplicationSettings(config)
}

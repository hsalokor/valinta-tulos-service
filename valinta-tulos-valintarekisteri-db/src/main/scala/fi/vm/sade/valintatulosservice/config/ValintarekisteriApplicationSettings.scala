package fi.vm.sade.valintatulosservice.config

import com.typesafe.config.Config

abstract class ApplicationSettings(config: Config) extends fi.vm.sade.utils.config.ApplicationSettings(config) {
  val tarjontaUrl = withConfig(_.getString("tarjonta-service.url"))
  val valintaRekisteriDbConfig = withConfig(_.getConfig("valinta-tulos-service.valintarekisteri.db"))
  val koodistoUrl = withConfig(_.getString("koodisto-service.rest.url"))

  protected def withConfig[T](operation: Config => T): T = {
    try {
      operation(config)
    } catch {
      case e: Throwable =>
        System.err.println(s"Cannot instantiate ${classOf[ApplicationSettings]} : ${e.getMessage}")
        e.printStackTrace()
        throw e
    }
  }
}

case class ValintarekisteriApplicationSettings(config: Config) extends ApplicationSettings(config) {}

object ValintarekisteriApplicationSettingsParser extends fi.vm.sade.utils.config.ApplicationSettingsParser[ValintarekisteriApplicationSettings] {
  override def parse(config: Config) = ValintarekisteriApplicationSettings(config)
}

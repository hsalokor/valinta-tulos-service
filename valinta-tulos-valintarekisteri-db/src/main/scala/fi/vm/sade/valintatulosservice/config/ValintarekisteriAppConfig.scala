package fi.vm.sade.valintatulosservice.config

import java.net.URL

import com.typesafe.config.ConfigFactory
import fi.vm.sade.utils.config.{ApplicationSettingsLoader, ConfigTemplateProcessor}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.utils.tcp.PortFromSystemPropertyOrFindFree

object ValintarekisteriAppConfig extends Logging {
  //def getProfileProperty() = System.getProperty("valintatulos.profile", "default")
  private implicit val settingsParser = ValintarekisteriApplicationSettingsParser
  //1private val embeddedMongoPortChooser = new PortFromSystemPropertyOrFindFree("valintatulos.embeddedmongo.port")
  private val itPostgresPortChooser = new PortFromSystemPropertyOrFindFree("valintatulos.it.postgres.port")

  def getDefault() = new Default

  class Default extends ValintarekisteriAppConfig {
    val settings = settingsParser.parse(ConfigFactory.load())
  }

  class IT extends ExampleTemplatedProps {
    private lazy val itPostgres = new ITPostgres(itPostgresPortChooser)

    override def start {

      itPostgres.start()
    }

    override lazy val settings = loadSettings
      .withOverride(("valinta-tulos-service.valintarekisteri.ensikertalaisuus.max.henkilo.oids", "100"))
      .withOverride("valinta-tulos-service.valintarekisteri.db.url", s"jdbc:postgresql://localhost:${itPostgresPortChooser.chosenPort}/valintarekisteri")
      .withoutPath("valinta-tulos-service.valintarekisteri.db.user")
      .withoutPath("valinta-tulos-service.valintarekisteri.db.password")
  }

  trait ExternalProps {
    def configFile = System.getProperty("user.home") + "/oph-configuration/valinta-tulos-service.properties"
    lazy val settings = ApplicationSettingsLoader.loadSettings(configFile)
  }

  trait ExampleTemplatedProps extends ValintarekisteriAppConfig with TemplatedProps {
    def templateAttributesURL = getClass.getResource("/oph-configuration/dev-vars.yml")
  }

  trait TemplatedProps {
    logger.info("Using template variables from " + templateAttributesURL)
    lazy val settings = loadSettings
    def loadSettings = ConfigTemplateProcessor.createSettings(
      getClass.getResource("/oph-configuration/valinta-tulos-service-devtest.properties.template"),
      templateAttributesURL
    )
    def templateAttributesURL: URL
  }

  trait ValintarekisteriAppConfig extends AppConfig {

    def start {}

    override def settings: ValintarekisteriApplicationSettings

    def properties: Map[String, String] = settings.toProperties

  }
}

trait StubbedExternalDeps {

}


package fi.vm.sade.valintatulosservice.valintarekisteri


import java.net.URL

import fi.vm.sade.utils.config.{ApplicationSettingsLoader, ConfigTemplateProcessor}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.utils.tcp.PortFromSystemPropertyOrFindFree
import fi.vm.sade.valintatulosservice.valintarekisteri.config.ITPostgres
import fi.vm.sade.valintatulosservice.valintarekisteri.sijoittelu.{ApplicationSettings, ApplicationSettingsParser}

object ITAppConfig extends Logging {
  private implicit val settingsParser = ApplicationSettingsParser
  private val itPostgresPortChooser = new PortFromSystemPropertyOrFindFree("valintatulos.it.postgres.port")



  /**
   *  IT (integration test) profiles. Uses embedded mongo and PostgreSQL databases, and stubbed external deps
   */
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

  trait ExampleTemplatedProps extends AppConfig with TemplatedProps {
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

  trait AppConfig {

    def start {}

    def settings: ApplicationSettings

    def properties: Map[String, String] = settings.toProperties

  }
}


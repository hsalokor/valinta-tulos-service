package fi.vm.sade.valintatulosservice.config

import com.typesafe.config.Config
import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoitteluSpringConfiguration, SijoitteluSpringContext}


object AppConfig extends Logging {

  def getProfileProperty() = System.getProperty("valintatulos.profile", "default")

  def fromSystemProperty: AppConfig = {
    val profile: String = getProfileProperty
    logger.info("Using valintatulos.profile=" + profile)
    profile match {
      case "default" => new Default
      case "templated" => new LocalTestingWithTemplatedVars
      case "dev" => new Dev
      case name => throw new IllegalArgumentException("Unknown value for valintatulos.profile: " + name);
    }
  }

  class Default extends AppConfig with ExternalProps {
    def springConfiguration = new SijoitteluSpringContext.Default()
  }

  class LocalTestingWithTemplatedVars(val templateAttributesFile: String = System.getProperty("valintatulos.vars")) extends AppConfig with TemplatedProps {
    def springConfiguration = new SijoitteluSpringContext.Default()
  }

  class Dev extends AppConfig with ExampleTemplatedProps {
    def springConfiguration = new SijoitteluSpringContext.Dev()

    override def properties = super.properties +
      ("sijoittelu-service.mongodb.uri" -> "mongodb://localhost:27017") +
      ("sijoittelu-service.mongodb.dbname" -> "sijoittelu")
  }

  trait ExternalProps {
    def configFile = System.getProperty("user.home") + "/oph-configuration/valinta-tulos-service.properties"
    lazy val settings = ApplicationSettings.loadSettings(configFile)
  }

  trait ExampleTemplatedProps extends AppConfig with TemplatedProps {
    def templateAttributesFile = "src/main/resources/oph-configuration/dev-vars.yml"
  }

  trait TemplatedProps {
    logger.info("Using template variables from " + templateAttributesFile)
    lazy val settings = ConfigTemplateProcessor.createSettings(templateAttributesFile)
    def templateAttributesFile: String
  }

  trait StubbedExternalDeps {
  }


  trait AppConfig {
    def springConfiguration: SijoitteluSpringConfiguration
    lazy val springContext = new SijoitteluSpringContext(SijoitteluSpringContext.createApplicationContext(this))

    final def start {
    }
    final def stop {
    }
    def withConfig[T](f: (AppConfig => T)): T = {
      start
      try {
        f(this)
      } finally {
        stop
      }
    }


    def settings: ApplicationSettings

    def properties: Map[String, String] = settings.toProperties
  }
}

case class RemoteApplicationConfig(url: String, username: String, password: String, ticketConsumerPath: String, config: Config)
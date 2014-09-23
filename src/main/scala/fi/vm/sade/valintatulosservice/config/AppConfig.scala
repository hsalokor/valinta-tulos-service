package fi.vm.sade.valintatulosservice.config

import com.typesafe.config.Config
import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.fixtures.HakemusFixtureImporter
import fi.vm.sade.valintatulosservice.mongo.{EmbeddedMongo, MongoServer}
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoitteluFixtures, SijoitteluSpringContext}

object AppConfig extends Logging {
  def getProfileProperty() = System.getProperty("valintatulos.profile", "default")

  def fromSystemProperty: AppConfig = {
    val profile: String = getProfileProperty
    logger.info("Using valintatulos.profile=" + profile)
    profile match {
      case "default" => new Default
      case "templated" => new LocalTestingWithTemplatedVars
      case "dev" => new Dev
      case "it" => new IT
      case name => throw new IllegalArgumentException("Unknown value for valintatulos.profile: " + name);
    }
  }

  /**
   * Default profile, uses ~/oph-configuration/valinta-tulos-service.properties
   */
  class Default extends AppConfig with ExternalProps {
  }

  /**
   * Templated profile, uses config template with vars file located by system property valintatulos.vars
   */
  class LocalTestingWithTemplatedVars(val templateAttributesFile: String = System.getProperty("valintatulos.vars")) extends AppConfig with TemplatedProps {
  }

  /**
   * Dev profile, uses local mongo db
   */
  class Dev extends AppConfig with ExampleTemplatedProps {
    override def properties = super.properties +
      ("sijoittelu-service.mongodb.uri" -> "mongodb://localhost:27017") +
      ("sijoittelu-service.mongodb.dbname" -> "sijoittelu")

    override lazy val settings = loadSettings.withOverride(("hakemus.mongodb.uri", "mongodb://localhost:27017"))
  }

  /**
   * IT profile, uses embedded mongo
   */
  class IT extends ExampleTemplatedProps {

    private var mongo: Option[MongoServer] = None

    override def start {
      mongo = EmbeddedMongo.start
      try {
        SijoitteluFixtures.importFixture(sijoitteluContext.database, "hyvaksytty-ilmoitettu.json")
        new HakemusFixtureImporter(settings.hakemusMongoConfig).importData
      } catch {
        case e: Exception =>
          stop
          throw e
      }
    }
    override def stop {
      mongo.foreach(_.stop)
      mongo = None
    }

    override lazy val settings = loadSettings.withOverride(("hakemus.mongodb.uri", "mongodb://localhost:28019"))

    override def properties = super.properties +
      ("sijoittelu-service.mongodb.uri" -> "mongodb://localhost:28019") +
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
    lazy val settings = loadSettings
    def loadSettings = ConfigTemplateProcessor.createSettings(templateAttributesFile)
    def templateAttributesFile: String
  }

  trait AppConfig {
    lazy val sijoitteluContext = new SijoitteluSpringContext(SijoitteluSpringContext.createApplicationContext(this))

    def start {}
    def stop {}

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

case class MongoConfig(url: String, dbname: String, collection: String)

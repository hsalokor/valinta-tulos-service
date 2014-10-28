package fi.vm.sade.valintatulosservice.config

import com.typesafe.config.Config
import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.mongo.{EmbeddedMongo, MongoServer}
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoitteluFixtures, SijoitteluSpringContext}
import fi.vm.sade.valintatulosservice.ohjausparametrit._
import fi.vm.sade.valintatulosservice.tarjonta.HakuService

object AppConfig extends Logging {
  def getProfileProperty() = System.getProperty("valintatulos.profile", "default")

  def fromOptionalString(profile: Option[String]) = {
    fromString(profile.getOrElse(getProfileProperty))
  }

  def fromSystemProperty: AppConfig = {
    fromString(getProfileProperty)
  }

  def fromString(profile: String) = {
    logger.info("Using valintatulos.profile=" + profile)
    profile match {
      case "default" => new Default
      case "templated" => new LocalTestingWithTemplatedVars
      case "dev" => new Dev
      case "it" => new IT
      case "it-externalHakemus" => new IT_externalHakemus
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
   *  IT (integration test) profiles. Uses embedded mongo database and stubbed external deps
   */
  class IT extends ExampleTemplatedProps with StubbedExternalDeps {
    private var mongo: Option[MongoServer] = None

    override def start {
      mongo = EmbeddedMongo.start
      try {
        importFixturesToSijoitteluDatabase
        importFixturesToHakemusDatabase
      } catch {
        case e: Exception =>
          stop
          throw e
      }
    }

    protected def importFixturesToSijoitteluDatabase {
      SijoitteluFixtures.importFixture(sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json")
    }

    protected def importFixturesToHakemusDatabase {
      HakemusFixtures()(this).importData
    }

    override def stop {
      mongo.foreach(_.stop)
      mongo = None
    }

    override lazy val settings = loadSettings.withOverride(("hakemus.mongodb.uri", "mongodb://localhost:" + EmbeddedMongo.port))

    override def properties = super.properties +
      ("sijoittelu-service.mongodb.uri" -> ("mongodb://localhost:" + EmbeddedMongo.port)) +
      ("sijoittelu-service.mongodb.dbname" -> "sijoittelu")
  }

  /**
   * IT profile, uses embedded mongo for sijoittelu, external mongo for Hakemus and stubbed external deps
   */
  class IT_externalHakemus extends IT {
    override lazy val settings = loadSettings.withOverride("hakemus.mongodb.uri", "mongodb://localhost:" + System.getProperty("hakemus.embeddedmongo.port", "28018"))

    override def importFixturesToHakemusDatabase { /* Don't import initial fixtures, as database is considered external */ }
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
    lazy val sijoitteluContext = new SijoitteluSpringContext(this, SijoitteluSpringContext.createApplicationContext(this), HakuService(this))

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

    lazy val ohjausparametritService = this match {
      case _ : StubbedExternalDeps => new StubbedOhjausparametritService()
      case _ => CachedRemoteOhjausparametritService(this)
    }

    def settings: ApplicationSettings

    def properties: Map[String, String] = settings.toProperties
  }

  trait StubbedExternalDeps {
  }

}

case class RemoteApplicationConfig(url: String, username: String, password: String, ticketConsumerPath: String, config: Config)

case class MongoConfig(url: String, dbname: String, collection: String)

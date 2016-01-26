package fi.vm.sade.valintatulosservice.config

import java.io.File
import java.net.URL
import fi.vm.sade.security.ldap.LdapUser
import fi.vm.sade.security.mock.MockSecurityContext
import fi.vm.sade.security.{ProductionSecurityContext, SecurityContext}
import fi.vm.sade.utils.cas.CasClient
import fi.vm.sade.utils.config.{ApplicationSettingsLoader, ConfigTemplateProcessor}
import fi.vm.sade.utils.mongo.{EmbeddedMongo, MongoServer}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.utils.tcp.PortFromSystemPropertyOrFindFree
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.ohjausparametrit._
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoitteluFixtures, SijoitteluSpringContext}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService

object AppConfig extends Logging {
  def getProfileProperty() = System.getProperty("valintatulos.profile", "default")
  private implicit val settingsParser = ApplicationSettingsParser
  private val embeddedMongoPortChooser = new PortFromSystemPropertyOrFindFree("valintatulos.embeddedmongo.port")

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
  class Default extends AppConfig with ExternalProps with CasLdapSecurity {
  }

  /**
   * Templated profile, uses config template with vars file located by system property valintatulos.vars
   */
  class LocalTestingWithTemplatedVars(val templateAttributesFile: String = System.getProperty("valintatulos.vars")) extends AppConfig with TemplatedProps with CasLdapSecurity {
    override def templateAttributesURL = new File(templateAttributesFile).toURI.toURL
  }

  /**
   * Dev profile, uses local mongo db
   */
  class Dev extends AppConfig with ExampleTemplatedProps with CasLdapSecurity with StubbedExternalDeps {
    override lazy val settings = loadSettings
      .withOverride(("hakemus.mongodb.uri", "mongodb://localhost:27017"))
      .withOverride(("sijoittelu-service.mongodb.uri", "mongodb://localhost:27017"))
      .withOverride("sijoittelu-service.mongodb.dbname", "sijoittelu")
  }

  /**
   *  IT (integration test) profiles. Uses embedded mongo and PostgreSQL databases, and stubbed external deps
   */
  class IT extends ExampleTemplatedProps with StubbedExternalDeps with MockSecurity {
    private var mongo: Option[MongoServer] = None
    private lazy val itPostgres = new ItPostgres()

    override def start {
      mongo = EmbeddedMongo.start(embeddedMongoPortChooser)
      itPostgres.start()
      try {
        importFixturesToSijoitteluDatabase
        importFixturesToHakemusDatabase
      } catch {
        case e: Exception =>
          throw e
      }
    }

    protected def importFixturesToSijoitteluDatabase {
      SijoitteluFixtures(sijoitteluContext.database).importFixture("hyvaksytty-kesken-julkaistavissa.json")
    }

    protected def importFixturesToHakemusDatabase {
      HakemusFixtures()(this).clear.importDefaultFixtures
    }

    override def stop {
      mongo.foreach(_.stop)
      mongo = None
      itPostgres.stop()
    }

    override lazy val settings = loadSettings
      .withOverride(("hakemus.mongodb.uri", "mongodb://localhost:" + embeddedMongoPortChooser.chosenPort))
      .withOverride(("sijoittelu-service.mongodb.uri", "mongodb://localhost:" + embeddedMongoPortChooser.chosenPort))
      .withOverride(("sijoittelu-service.mongodb.dbname", "sijoittelu"))
      .withOverride("valinta-tulos-service.valintarekisteri.db.url", "jdbc:postgresql://localhost:65432/valintarekisteri")
  }

  /**
   * IT profile, uses embedded mongo for sijoittelu, external mongo for Hakemus and stubbed external deps
   */
  class IT_externalHakemus extends IT {
    override lazy val settings = loadSettings
      .withOverride("hakemus.mongodb.uri", "mongodb://localhost:" + System.getProperty("hakemus.embeddedmongo.port", "28018"))
      .withOverride(("sijoittelu-service.mongodb.uri", "mongodb://localhost:" + embeddedMongoPortChooser.chosenPort))
      .withOverride(("sijoittelu-service.mongodb.dbname", "sijoittelu"))
    
    override def importFixturesToHakemusDatabase { /* Don't import initial fixtures, as database is considered external */ }
  }

  class IT_disabledIlmoittautuminen extends IT {
    override lazy val settings = loadSettings.withOverride("valinta-tulos-service.ilmoittautuminen.enabled", "")
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
      getClass.getResource("/oph-configuration/valinta-tulos-service.properties.template"),
      templateAttributesURL
    )
    def templateAttributesURL: URL
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

    def securityContext: SecurityContext
  }

  trait StubbedExternalDeps {

  }

  trait MockSecurity extends AppConfig {
    lazy val securityContext = {
      new MockSecurityContext(
        settings.securitySettings.casServiceIdentifier,
        settings.securitySettings.requiredLdapRoles,
        Map((settings.securitySettings.casUsername -> LdapUser(settings.securitySettings.requiredLdapRoles, "Mock", "User", "mockoid")))
      )
    }
  }

  trait CasLdapSecurity extends AppConfig {
    lazy val securityContext: SecurityContext = {
      val casClient = new CasClient(settings.securitySettings.casUrl, org.http4s.client.blaze.defaultClient)
      new ProductionSecurityContext(settings.securitySettings.ldapConfig, casClient, settings.securitySettings.casServiceIdentifier, settings.securitySettings.requiredLdapRoles)
    }
  }
}


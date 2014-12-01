package fi.vm.sade.valintatulosservice.config

import fi.vm.sade.security.ldap.LdapUser
import fi.vm.sade.security.mock.MockSecurityContext
import fi.vm.sade.security.{ProductionSecurityContext, SecurityContext}
import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.mongo.{EmbeddedMongo, MongoServer}
import fi.vm.sade.valintatulosservice.ohjausparametrit._
import fi.vm.sade.valintatulosservice.sijoittelu.{SijoitteluFixtures, SijoitteluSpringContext}
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
  class Default extends AppConfig with ExternalProps with CasLdapSecurity {
  }

  /**
   * Templated profile, uses config template with vars file located by system property valintatulos.vars
   */
  class LocalTestingWithTemplatedVars(val templateAttributesFile: String = System.getProperty("valintatulos.vars")) extends AppConfig with TemplatedProps with CasLdapSecurity {
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
   *  IT (integration test) profiles. Uses embedded mongo database and stubbed external deps
   */
  class IT extends ExampleTemplatedProps with StubbedExternalDeps with MockSecurity {
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
      HakemusFixtures()(this).importDefaultFixtures
    }

    override def stop {
      mongo.foreach(_.stop)
      mongo = None
    }

    override lazy val settings = loadSettings
      .withOverride(("hakemus.mongodb.uri", "mongodb://localhost:" + EmbeddedMongo.port))
      .withOverride(("sijoittelu-service.mongodb.uri", "mongodb://localhost:" + EmbeddedMongo.port))
      .withOverride(("sijoittelu-service.mongodb.dbname", "sijoittelu"))
  }

  /**
   * IT profile, uses embedded mongo for sijoittelu, external mongo for Hakemus and stubbed external deps
   */
  class IT_externalHakemus extends IT {
    override lazy val settings = loadSettings
      .withOverride("hakemus.mongodb.uri", "mongodb://localhost:" + System.getProperty("hakemus.embeddedmongo.port", "28018"))
      .withOverride(("sijoittelu-service.mongodb.uri", "mongodb://localhost:" + EmbeddedMongo.port))
      .withOverride(("sijoittelu-service.mongodb.dbname", "sijoittelu"))
    
    override def importFixturesToHakemusDatabase { /* Don't import initial fixtures, as database is considered external */ }
  }

  class IT_disabledIlmoittautuminen extends IT {
    override lazy val settings = loadSettings.withOverride("valinta-tulos-service.ilmoittautuminen.enabled", "")
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

    def securityContext: SecurityContext
  }

  trait StubbedExternalDeps {

  }

  trait MockSecurity extends AppConfig {
    lazy val securityContext = {
      new MockSecurityContext(settings.securitySettings.casServiceIdentifier, settings.securitySettings.requiredLdapRoles, Map((settings.securitySettings.ticketRequest.username -> LdapUser(settings.securitySettings.requiredLdapRoles))))
    }
  }

  trait CasLdapSecurity extends AppConfig {
    lazy val securityContext: SecurityContext = {
      new ProductionSecurityContext(settings.securitySettings.ldapConfig, settings.securitySettings.casConfig, settings.securitySettings.casServiceIdentifier, settings.securitySettings.requiredLdapRoles)
    }
  }
}

case class MongoConfig(url: String, dbname: String)

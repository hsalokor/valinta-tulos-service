package fi.vm.sade.valintatulosservice.sijoittelu

import com.mongodb._
import fi.vm.sade.sijoittelu.tulos.service.{RaportointiService}
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.HakuService
import org.mongodb.morphia.{Datastore, Morphia}
import org.springframework.beans.factory.annotation.{Value, Autowired}
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation._
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.{MapPropertySource, MutablePropertySources}
import scala.collection.JavaConversions._
import fi.vm.sade.sijoittelu.tulos.dao.ValintatulosDao

class SijoitteluSpringContext(config: AppConfig, context: ApplicationContext, hakuService: HakuService) {
  def database = context.getBean(classOf[DB])

  lazy val valintatulosDao = context.getBean(classOf[ValintatulosDao])
  lazy val raportointiService = context.getBean(classOf[RaportointiService])
  lazy val yhteenvetoService = new YhteenvetoService(raportointiService, config.ohjausparametritService)
  lazy val sijoittelutulosService = new SijoittelutulosService(yhteenvetoService)
  lazy val vastaanottoService = new VastaanottoService(yhteenvetoService, valintatulosDao, hakuService)
}

object SijoitteluSpringContext {
  def check {}

  def createApplicationContext(configuration: AppConfig): AnnotationConfigApplicationContext = {
    val appContext: AnnotationConfigApplicationContext = new AnnotationConfigApplicationContext
    val springConfiguration = new Default
    println("Using spring configuration " + springConfiguration)
    appContext.getEnvironment.setActiveProfiles(springConfiguration.profile)
    customPropertiesHack(appContext, configuration)
    appContext.register(springConfiguration.getClass)
    appContext.refresh
    return appContext
  }

  private def customPropertiesHack(appContext: AnnotationConfigApplicationContext, configuration: AppConfig) {
    val configurer: PropertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer()
    val sources: MutablePropertySources = new MutablePropertySources()

    val properties: Map[String, String] = configuration.properties

    sources.addFirst(new MapPropertySource("custom props", mapAsJavaMap(properties)))
    configurer.setPropertySources(sources)
    appContext.addBeanFactoryPostProcessor(configurer)
  }

  @Configuration
  @ComponentScan(basePackages = Array(
    "fi.vm.sade.sijoittelu.tulos.service",
    "fi.vm.sade.sijoittelu.tulos.dao",
    "fi.vm.sade.valintatulosservice.sijoittelu.spring"))
  @Import(Array(classOf[SijoitteluMongoConfiguration]))
  class Default extends SijoitteluSpringConfiguration {
    val profile = "default"
  }
}

trait SijoitteluSpringConfiguration {
  def profile: String // <- should be able to get from annotation
}

@Configuration
class SijoitteluMongoConfiguration {
  @Bean
  def datastore(@Value("${sijoittelu-service.mongodb.dbname}") dbName: String, @Value("${sijoittelu-service.mongodb.uri}") dbUri: String): Datastore = {
    val mongoClient: MongoClient = createMongoClient(dbUri)
    new Morphia().createDatastore(mongoClient, dbName)
  }

  @Bean
  def database(@Value("${sijoittelu-service.mongodb.dbname}") dbName: String, @Value("${sijoittelu-service.mongodb.uri}") dbUri: String): DB = {
    createMongoClient(dbUri).getDB(dbName)
  }

  private def createMongoClient(dbUri: String): MongoClient = {
    val options = new MongoClientOptions.Builder().writeConcern(WriteConcern.FSYNCED)
    val mongoClientURI: MongoClientURI = new MongoClientURI(dbUri, options)
    val mongoClient: MongoClient = new MongoClient(mongoClientURI)
    mongoClient
  }
}
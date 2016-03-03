package fi.vm.sade.valintatulosservice.sijoittelu

import com.mongodb._
import fi.vm.sade.sijoittelu.tulos.dao.{HakukohdeDao, SijoitteluDao, ValintatulosDao}
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.mongodb.morphia.{Datastore, Morphia}
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation._
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.{MapPropertySource, MutablePropertySources}

import scala.collection.JavaConversions._

class SijoitteluSpringContext(config: AppConfig, context: ApplicationContext) {
  def database = context.getBean(classOf[DB])

  lazy val morphiaDs = context.getBean(classOf[Datastore])
  lazy val valintatulosDao = context.getBean(classOf[ValintatulosDao])
  lazy val hakukohdeDao = context.getBean(classOf[HakukohdeDao])
  lazy val sijoitteluDao = context.getBean(classOf[SijoitteluDao])
  lazy val raportointiService = context.getBean(classOf[RaportointiService])
  lazy val valintatulosRepository = new ValintatulosRepository(valintatulosDao)
}

object SijoitteluSpringContext {
  def check() {}

  def createApplicationContext(configuration: AppConfig): AnnotationConfigApplicationContext = {
    val appContext: AnnotationConfigApplicationContext = new AnnotationConfigApplicationContext
    val springConfiguration = new Default
    println("Using spring configuration " + springConfiguration)
    appContext.getEnvironment.setActiveProfiles(springConfiguration.profile)
    customPropertiesHack(appContext, configuration)
    appContext.register(springConfiguration.getClass)
    appContext.refresh()
    appContext
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

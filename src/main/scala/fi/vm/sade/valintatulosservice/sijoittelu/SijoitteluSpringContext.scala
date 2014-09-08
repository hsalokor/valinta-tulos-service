package fi.vm.sade.valintatulosservice.sijoittelu

import com.mongodb.MongoClient
import fi.vm.sade.sijoittelu.tulos.service.RaportointiService
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.mongodb.morphia.{Datastore, Morphia}
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation._
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.core.env.{MapPropertySource, MutablePropertySources}

import scala.collection.JavaConversions._

class SijoitteluSpringContext(context: ApplicationContext) {
  def raportointiService = context.getBean(classOf[RaportointiService])
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
    "fi.vm.sade.sijoittelu.tulos.dao"))
  class Default extends SijoitteluSpringConfiguration {
    val profile = "default"

    @Bean
    def datastore: Datastore = {
      // TODO: use actual mongo configuration
      val mongo = new MongoClient("localhost")
      new Morphia().createDatastore(mongo, "sijoittelu")
    }
  }

  @Configuration
  @ComponentScan(basePackages = Array("fi.vm.sade.haku"))
  class Dev extends SijoitteluSpringConfiguration {
    val profile = "dev"
  }

  @Configuration
  @ComponentScan(basePackages = Array("fi.vm.sade.haku"))
  class DevWithAuditLog extends SijoitteluSpringConfiguration {
    val profile = "dev"
  }

}

trait SijoitteluSpringConfiguration {
  def profile: String // <- should be able to get from annotation
}
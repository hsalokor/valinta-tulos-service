package fi.vm.sade.valintatulosservice.hakemus

import java.io.{File, FileInputStream}
import java.util.HashMap

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.`type`.MapType
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.mongodb.{DBObject, BasicDBObject}
import com.mongodb.util.JSON
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.domain.Hakutoive
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import org.bson.types.ObjectId
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.support.{URLTemplateSource, FileTemplateSource}
import org.springframework.core.io.{ClassPathResource, Resource}

class HakemusFixtures(config: MongoConfig) {
  lazy val db = MongoFactory.createDB(config)

  if (config.url.indexOf("localhost") < 0)
    throw new IllegalArgumentException("HakemusFixtureImporter can only be used with IT profile")

  def clear = {
    db.getCollection("application").remove(new BasicDBObject())
    this
  }

  def importDefaultFixtures: HakemusFixtures = {
    HakemusFixtures.defaultFixtures.foreach(importFixture(_))
    this
  }

  def importFixture(fixtureName: String): HakemusFixtures = {
    val filename = "fixtures/hakemus/" + fixtureName + ".json"
    MongoMockData.insertData(db.underlying, MongoMockData.readJson(filename))
    this
  }

  val filename = "fixtures/hakemus/hakemus-template.json"
  val templateObject: BasicDBObject = MongoMockData.readJson(filename).asInstanceOf[BasicDBObject]

  def importTemplateFixture(hakemus: HakemusFixture) = {
    templateObject.put("_id", new ObjectId())
    templateObject.put("oid", hakemus.hakemusOid)
    val hakutoiveetDbObject = templateObject.get("answers").asInstanceOf[BasicDBObject].get("hakutoiveet").asInstanceOf[BasicDBObject]

    hakemus.hakutoiveet.foreach { hakutoive =>
      hakutoiveetDbObject.put("preference" + hakutoive.index + "-Koulutus-id", hakutoive.hakukohdeOid)
      hakutoiveetDbObject.put("preference" + hakutoive.index + "-Opetuspiste-id", hakutoive.tarjoajaOid)
    }

    db.underlying.getCollection("application").insert(templateObject)
    this
  }
}

object HakemusFixtures {
  val defaultFixtures = List("00000878229", "00000441369", "00000441370")

  def apply()(implicit appConfig: AppConfig) = {
    new HakemusFixtures(appConfig.settings.hakemusMongoConfig)
  }
}

case class HakemusFixture(hakemusOid: String, hakutoiveet: List[HakutoiveFixture])
case class HakutoiveFixture(index: Int, tarjoajaOid: String, hakukohdeOid: String)
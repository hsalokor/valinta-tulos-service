package fi.vm.sade.valintatulosservice.hakemus

import java.io.{File, FileInputStream}
import java.util.HashMap

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.`type`.MapType
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.mongodb._
import com.mongodb.util.JSON
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData
import fi.vm.sade.utils.Timer
import fi.vm.sade.utils.config.MongoConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
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
    val data: DBObject = MongoMockData.readJson(filename)
    insertData(db.underlying, data)
    this
  }

  private def insertData(db: DB, data: DBObject) {
    import scala.collection.JavaConversions._
    for (collection <- data.keySet) {
      val collectionData: BasicDBList = data.get(collection).asInstanceOf[BasicDBList]
      val c: DBCollection = db.getCollection(collection)
      import scala.collection.JavaConversions._
      for (dataObject <- collectionData) {
        val dbObject: DBObject = dataObject.asInstanceOf[DBObject]
        val id: AnyRef = dbObject.get("_id")
        c.insert(dbObject)
      }
    }
  }

  private var builder:BulkWriteOperation = null

  def startBulkOperationInsert() = {
    builder = db.underlying.getCollection("application").initializeUnorderedBulkOperation
  }

  def importTemplateFixture(hakemus: HakemusFixture) = {
    val currentTemplateObject = MongoMockData.readJson("fixtures/hakemus/hakemus-template.json").asInstanceOf[BasicDBObject]
    currentTemplateObject.put("_id", new ObjectId())
    currentTemplateObject.put("oid", hakemus.hakemusOid)
    currentTemplateObject.put("applicationSystemId", hakemus.hakuOid)
    currentTemplateObject.put("personOid", hakemus.hakemusOid)
    val hakutoiveetDbObject = currentTemplateObject.get("answers").asInstanceOf[BasicDBObject].get("hakutoiveet").asInstanceOf[BasicDBObject]
    val hakutoiveetMetaDbList = currentTemplateObject
      .get("authorizationMeta").asInstanceOf[BasicDBObject]
      .get("applicationPreferences").asInstanceOf[BasicDBList]

    hakemus.hakutoiveet.foreach { hakutoive =>
      hakutoiveetDbObject.put("preference" + hakutoive.index + "-Koulutus-id", hakutoive.hakukohdeOid)
      hakutoiveetDbObject.put("preference" + hakutoive.index + "-Opetuspiste-id", hakutoive.tarjoajaOid)
      hakutoiveetMetaDbList.add(BasicDBObjectBuilder.start()
        .add("ordinal", hakutoive.index)
        .push("preferenceData")
        .add("Koulutus-id", hakutoive.hakukohdeOid)
        .add("Opetuspiste-id", hakutoive.tarjoajaOid)
        .pop()
        .get())
    }
    builder.insert(currentTemplateObject)
  }

  def commitBulkOperationInsert = {
    import scala.collection.JavaConverters._
    try {
      builder.execute(WriteConcern.UNACKNOWLEDGED)
    } catch {
      case e:BulkWriteException => {
        e.printStackTrace()
        for(error <- e.getWriteErrors.asScala) println(error.getMessage)
      }
    }
  }
}

object HakemusFixtures {
  val defaultFixtures = List("00000878229", "00000441369", "00000441370", "00000878230", "00000878231", "00000878229-SE")

  def apply()(implicit appConfig: AppConfig) = {
    new HakemusFixtures(appConfig.settings.hakemusMongoConfig)
  }
}

case class HakemusFixture(hakuOid: String, hakemusOid: String, hakutoiveet: List[HakutoiveFixture])
case class HakutoiveFixture(index: Int, tarjoajaOid: String, hakukohdeOid: String)

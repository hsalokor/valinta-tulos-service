package fi.vm.sade.valintatulosservice.experimental

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.ValintatulosService
import fi.vm.sade.valintatulosservice.config.AppConfig.LocalTestingWithTemplatedVars
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.tarjonta.HakuService

object MailPollerProdPerfTester extends App {
  val url = System.getProperty("mongo.uri")
  val valintatulos = MongoFactory.createCollection(MongoConfig(url, "sijoitteludb"), "Valintatulos")
  val query = MongoDBObject(
    "hakuOid" -> "1.2.246.562.5.2013080813081926341927",
    "julkaistavissa" -> true,
    "tila" -> "KESKEN",
    "lahetetty" -> MongoDBObject("$ne" -> true)
  )
  val cursor = valintatulos.find(query).sort(MongoDBObject("wat" -> 1)).limit(1000)

  val appConfig = new LocalTestingWithTemplatedVars("../deploy/vars/environments/ophp_vars.yml")

  private val list: List[Imports.DBObject] = cursor.toList

  list.foreach { valintatulos: DBObject =>
    val hakemusOid: String = valintatulos.get("hakemusOid").asInstanceOf[String]
    val hakuOid: String = valintatulos.get("hakuOid").asInstanceOf[String]
    try {
      val (code, _, body) = DefaultHttpClient.httpGet("https://localhost:9000/valinta-tulos-service/haku/"+hakuOid+"/hakemus/"+hakemusOid)
        .responseWithHeaders
      println(body)
    } catch {
      case e: Exception => println("ERROR: " + e)
    }
  }
}

package fi.vm.sade.valintatulosservice.experimental

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import fi.vm.sade.valintatulosservice.ValintatulosService
import fi.vm.sade.valintatulosservice.config.AppConfig.{LocalTestingWithTemplatedVars, AppConfig}
import fi.vm.sade.valintatulosservice.config.MongoConfig
import fi.vm.sade.valintatulosservice.mongo.MongoFactory
import fi.vm.sade.valintatulosservice.tarjonta.HakuService

object MailPollerQA extends App {
  val url = System.getProperty("mongo.uri")
  val valintatulos = MongoFactory.createCollection(MongoConfig(url, "sijoitteludb", "Valintatulos"))
  val query = MongoDBObject(
  )
  val cursor = valintatulos.find(query).sort(MongoDBObject("wat" -> 1)).limit(1000)

  val appConfig = new LocalTestingWithTemplatedVars("../deploy/vars/environments/ophp_vars.yml")

  lazy val hakuService = HakuService(appConfig)
  lazy val valintatulosService = new ValintatulosService(hakuService)(appConfig)

  private val list: List[Imports.DBObject] = cursor.toList

  list.foreach { valintatulos: DBObject =>
    val hakemusOid: String = valintatulos.get("hakemusOid").asInstanceOf[String]
    val hakuOid: String = valintatulos.get("hakuOid").asInstanceOf[String]
    try {
    valintatulosService.hakemuksentulos(hakuOid, hakemusOid).foreach { tulos =>
      println(tulos.hakemusOid)
    }
    } catch {
    case e: Exception => println("ERROR: " + e)
    }
  }
}

package fi.vm.sade.valintatulosservice.performance

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigValueFactory
import fi.vm.sade.utils.http.{DefaultHttpClient, DefaultHttpRequest}
import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.SharedJetty
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{Ensikertalaisuus, EnsikertalaisuusServlet}
import fi.vm.sade.valintatulosservice.valintarekisteri.ValintarekisteriDb
import org.json4s.jackson.Serialization
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random
import scalaj.http.Http

object EnsikertalaisuusBatchAPITester extends App with Logging {
  implicit val formats = EnsikertalaisuusServlet.ensikertalaisuusJsonFormats
  implicit val appConfig = new AppConfig.IT
  private val dbConfig = appConfig.settings.valintaRekisteriDbConfig
  lazy val valintarekisteriDb = new ValintarekisteriDb(
    dbConfig.withValue("connectionPool", ConfigValueFactory.fromAnyRef("disabled"))).db
  SharedJetty.start
  val oids = 1.to(1000000).map(i => s"1.2.246.562.24.$i")

  Await.ready(valintarekisteriDb.run(
    sqlu"""insert into hakukohteet ("hakukohdeOid", "hakuOid", kktutkintoonjohtava, koulutuksen_alkamiskausi)
             values ('1.2.246.561.20.00000000001', '1.2.246.561.29.00000000001', true, '2015K')"""
  ), Duration(1, TimeUnit.SECONDS))
  Await.ready(valintarekisteriDb.run(SimpleDBIO[Unit](jdbcActionContext => {
    val insertStatement = jdbcActionContext.connection.prepareStatement(
      """insert into vastaanotot (henkilo, hakukohde, active, ilmoittaja, "timestamp", deleted)
         values (?, '1.2.246.561.20.00000000001', true, 'ilmoittaja', 1000, null)""")
    try {
      oids.foreach(oid => {
        insertStatement.setString(1, oid)
        insertStatement.addBatch()
      })
      insertStatement.executeBatch()
    } finally {
      insertStatement.close()
    }
  })), Duration(2, TimeUnit.MINUTES))
  val fetchOids = Random.shuffle(oids.zipWithIndex.filter(t => (t._2 % 10) == 0).map(t => t._1))
  var start = System.currentTimeMillis()
  val (status, _, result) = new DefaultHttpRequest(Http(s"http://localhost:${SharedJetty.port}/valinta-tulos-service/ensikertalaisuus?koulutuksenAlkamiskausi=2014K")
    .method("POST")
    .options(DefaultHttpClient.defaultOptions)
    .header("Content-Type", "application/json")
    .postData(Serialization.write(fetchOids))).responseWithHeaders()
  println(s"request of size ${fetchOids.size} took ${System.currentTimeMillis() - start} ms")
  start = System.currentTimeMillis()
  println(s"parsing response of size ${Serialization.read[List[Ensikertalaisuus]](result).size} took ${System.currentTimeMillis() - start} ms")
}

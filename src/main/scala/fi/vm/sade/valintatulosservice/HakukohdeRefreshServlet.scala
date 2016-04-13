package fi.vm.sade.valintatulosservice


import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import fi.vm.sade.utils.slf4j.Logging
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakukohdeRecordService, HakukohdeRepository}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.{SwaggerSupport, Swagger}
import org.scalatra._

import scala.collection.parallel.{ForkJoinTaskSupport, ThreadPoolTaskSupport}
import scala.concurrent.Future
import scala.concurrent.forkjoin.ForkJoinPool

case class Status(started: Date)

class HakukohdeRefreshServlet(hakukohdeRepository: HakukohdeRepository,
                              hakukohdeRecordService: HakukohdeRecordService)
                             (implicit val swagger: Swagger) extends ScalatraServlet with Logging with JacksonJsonSupport with JsonFormats with SwaggerSupport with UrlGeneratorSupport {
  override val applicationName = Some("virkistys")

  override protected def applicationDescription: String = "Hakukohdetietojen virkistys API"

  private val running = new AtomicReference[Option[Date]](None)

  val statusSwagger = (apiOperation[Status]("virkistysStatus")
    summary "Virkistyksen tila")
  val statusController = get("/", operation(statusSwagger)) {
    running.get() match {
      case Some(started) => Ok(Status(started))
      case None => NoContent()
    }
  }

  val virkistaSwagger = (apiOperation[Unit]("virkistaHakukohteet")
    summary "Virkistä hakukohteiden tiedot")
  post("/", operation(virkistaSwagger)) {
    val started = Some(new Date())
    if (running.compareAndSet(None, started)) {
      import scala.concurrent.ExecutionContext.Implicits.global
      Future {
        try {
          val hakukohteet = hakukohdeRepository.all.par
          val i = new AtomicInteger(0)
          val pool = new ForkJoinPool(4)
          hakukohteet.tasksupport = new ForkJoinTaskSupport(pool)
          logger.info(s"Refreshing ${hakukohteet.size} hakukohdetta")
          hakukohteet.foreach(hakukohde => {
            val (old, fresh) = hakukohdeRecordService.refreshHakukohdeRecord(hakukohde.oid)
            fresh.foreach(hakukohdeRecord => {
              logger.info(s"Updated hakukohde ${old.oid} from $old to $fresh")
              i.incrementAndGet()
            })
          })
          pool.shutdown()
          pool.awaitTermination(30, TimeUnit.SECONDS)
          logger.info(s"Updated $i hakukohdetta")
        } catch {
          case e: Exception => logger.error("Refreshing hakukohteet failed", e)
        } finally {
          running.compareAndSet(started, None)
        }
      }
      logger.info("Hakukohde refresh started")
    }
    SeeOther(url(statusController))
  }
}

package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.ServletSpecification
import fi.vm.sade.valintatulosservice.ensikertalaisuus.EnsikertalaisuusServlet._
import fi.vm.sade.valintatulosservice.ensikertalaisuus.{EiEnsikertalainen, Ensikertalainen, Ensikertalaisuus, EnsikertalaisuusServlet}
import fi.vm.sade.valintatulosservice.valintarekisteri.ValintarekisteriTools
import org.joda.time.{DateTime, DateTimeZone}
import org.json4s.jackson.Serialization._
import org.junit.runner.RunWith
import org.specs2.mutable.BeforeAfter
import org.specs2.runner.JUnitRunner
import org.specs2.specification.After
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._

@RunWith(classOf[JUnitRunner])
class EnsikertalaisuusServletSpec extends ServletSpecification with After {
  override implicit val formats = EnsikertalaisuusServlet.ensikertalaisuusJsonFormats
  val henkilo = "1.2.246.562.24.00000000001"
  val vastaanottamaton_henkilo = "1.2.246.562.24.00000000002"
  val vanha_henkilo = "1.2.246.562.24.00000000003"
  val hakukohde = "1.2.246.561.20.00000000001"
  val vanha_hakukohde = "Vanhan hakukohteen nimi:101"
  val vanha_tarjoaja = "1.2.246.562.10.00000000001"
  val haku = "1.2.246.561.29.00000000001"
  val koulutus = "1.2.246.561.21.00000000001"
  val timestamp = new DateTime(2014, 7, 1, 0, 0, 10, DateTimeZone.forID("Europe/Helsinki"))
  val vanha_timestamp = new DateTime(2014, 6, 19, 0, 0, 10, DateTimeZone.forID("Europe/Helsinki"))

  step(ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb))
  step({
    singleConnectionValintarekisteriDb.runBlocking(DBIOAction.seq(
          sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
                 values ($hakukohde, $haku, true, true, '2015K')""",
          sqlu"""insert into vastaanotot
                 (henkilo, hakukohde, action, active, ilmoittaja, "timestamp", deleted)
                 values ($henkilo, $hakukohde, 'VastaanotaSitovasti'::vastaanotto_action, true, 'ilmoittaja', ${timestamp.getMillis}, null)""",
          sqlu"""insert into vanhat_vastaanotot (henkilo, hakukohde, tarjoaja, koulutuksen_alkamiskausi, kk_tutkintoon_johtava, ilmoittaja, "timestamp", deleted)
                 values ($henkilo, $vanha_hakukohde, $vanha_tarjoaja, '2014S', true, 'KAYTTAJA', ${vanha_timestamp.getMillis}, null)""",
          sqlu"""insert into vanhat_vastaanotot (henkilo, hakukohde, tarjoaja, koulutuksen_alkamiskausi, kk_tutkintoon_johtava, ilmoittaja, "timestamp", deleted)
                 values ($vanha_henkilo, $vanha_hakukohde, $vanha_tarjoaja, '2014S', true, 'KAYTTAJA', ${vanha_timestamp.getMillis}, null)"""
        ).transactionally)
  })

  "GET /ensikertalaisuus/:henkiloOid" should {
    "return 200 OK" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamiskausi" -> "2015K"), Map("Content-Type" -> "application/json")) {
        status mustEqual 200
      }
    }

    "return EiEnsikertalainen" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamiskausi" -> "2015K"), Map("Content-Type" -> "application/json")) {
        body mustEqual """{"personOid":"1.2.246.562.24.00000000001","paattyi":"2014-07-01T00:00:10.000+03"}"""
        read[EiEnsikertalainen](body) mustEqual EiEnsikertalainen(henkilo, timestamp.toDate)
      }
    }

    "return Ensikertalainen" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000002", Map("koulutuksenAlkamiskausi" -> "2015K"), Map("Content-Type" -> "application/json")) {
        read[Ensikertalaisuus](body) mustEqual Ensikertalainen(vastaanottamaton_henkilo)
      }
    }

    "return EiEnsikertalainen based on vanhat_vastaanotot" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamiskausi" -> "2014K"), Map("Content-Type" -> "application/json")) {
        read[Ensikertalaisuus](body) mustEqual EiEnsikertalainen(henkilo, vanha_timestamp.toDate)
      }
    }

    "return 400 Bad Request for invalid henkilo oid" in {
      get("ensikertalaisuus/foo", Map("koulutuksenAlkamiskausi" -> "2015K"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for invalid koulutuksenAlkamiskausi" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map("koulutuksenAlkamiskausi" -> "foo"), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }

    "return 400 Bad Request for missing koulutuksenAlkamiskausi" in {
      get("ensikertalaisuus/1.2.246.562.24.00000000001", Map(), Map("Content-Type" -> "application/json")) {
        status mustEqual 400
      }
    }
  }

  "POST /ensikertalaisuus" should {
    "return 200 OK" in {
      postJSON("ensikertalaisuus?koulutuksenAlkamiskausi=2014K", write(Seq(henkilo)), Map()) {
        status mustEqual 200
      }
    }

    "return a sequence of EiEnsikertalainen" in {
      val personOidsToQuery = Seq(henkilo, vastaanottamaton_henkilo, vanha_henkilo)
      postJSON("ensikertalaisuus?koulutuksenAlkamiskausi=2015K", write(personOidsToQuery), Map()) {
        val ensikertalaisuuses = read[Seq[Ensikertalaisuus]](body).sortBy(_.personOid)
        ensikertalaisuuses must have size 3
        ensikertalaisuuses.head mustEqual EiEnsikertalainen(henkilo, timestamp.toDate)
        ensikertalaisuuses(1) mustEqual Ensikertalainen(vastaanottamaton_henkilo)
        ensikertalaisuuses(2) mustEqual Ensikertalainen(vanha_henkilo)
      }
    }

    "return a sequence of EiEnsikertalainen based on vanhat_vastaanotot" in {
      val personOidsToQuery = Seq(henkilo, vastaanottamaton_henkilo, vanha_henkilo)
      postJSON("ensikertalaisuus?koulutuksenAlkamiskausi=2014K", write(personOidsToQuery), Map()) {
        val ensikertalaisuuses = read[Seq[Ensikertalaisuus]](body).sortBy(_.personOid)
        ensikertalaisuuses must have size 3
        ensikertalaisuuses.head mustEqual EiEnsikertalainen(henkilo, vanha_timestamp.toDate)
        ensikertalaisuuses(1) mustEqual Ensikertalainen(vastaanottamaton_henkilo)
        ensikertalaisuuses(2) mustEqual EiEnsikertalainen(vanha_henkilo, vanha_timestamp.toDate)
      }
    }

    "return 400 Bad Request if too many henkilo oids is sent" in {
      postJSON("ensikertalaisuus?koulutuksenAlkamiskausi=2015K", write((1 to (maxHenkiloOids + 1)).map(i => s"1.2.246.562.24.$i")), Map()) {
        status mustEqual 400
      }
    }
  }

  override def after: Unit = ValintarekisteriTools.deleteVastaanotot(singleConnectionValintarekisteriDb)
}

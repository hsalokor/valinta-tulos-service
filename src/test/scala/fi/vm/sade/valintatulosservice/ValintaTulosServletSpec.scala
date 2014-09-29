package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, Valintatila, Vastaanotettavuustila, Vastaanottotila}
import fi.vm.sade.valintatulosservice.fixtures.HakemusFixtureImporter
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import org.joda.time.DateTimeUtils
import org.json4s.jackson.Serialization
import org.scalatra.test.specs2.MutableScalatraSpec
import org.specs2.specification.{Fragments, Step}
import java.util.Date
import org.scalatra.swagger.Swagger

class ValintaTulosServletSpec extends MutableScalatraSpec with TimeWarp {
  implicit val appConfig: AppConfig = new AppConfig.IT
  implicit val swagger: Swagger = new ValintatulosSwagger
  implicit val formats = JsonFormats.jsonFormats
  val hakemusFixtureImporter = new HakemusFixtureImporter(appConfig.settings.hakemusMongoConfig)

  sequential

  "GET /haku/:hakuId/hakemus/:hakemusId" should {
    "palauttaa valintatulokset" in {
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ilmoitettu.json", true)
      get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
        body must_== """{"hakemusOid":"1.2.246.562.11.00000441369","aikataulu":{"vastaanottoEnd":"2100-01-10T12:00:00Z","vastaanottoBufferDays":14},"hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","tarjoajaOid":"1.2.246.562.10.591352080610","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":"EI_TEHTY","vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T19:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T19:05:23Z","julkaistavissa":true},{"hakukohdeOid":"1.2.246.562.5.16303028779","tarjoajaOid":"1.2.246.562.10.455978782510","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":"EI_TEHTY","vastaanotettavuustila":"EI_VASTAANOTETTAVISSA","julkaistavissa":true}]}"""
      }
    }
    "hakutoiveet, joilta puuttuu julkaistu-flägi" in {
      "merkitään tilaan KESKEN" in {
        SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken.json", true)
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos = Serialization.read[Hakemuksentulos](body)
          val hyvaksytty = tulos.hakutoiveet.head
          hyvaksytty.valintatila must_== Valintatila.kesken
        }
      }
    }
    "sijoittelusta puuttuvat hakutoiveet" in {
      "merkitään tilaan KESKEN" in {
        SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ilmoitettu.json", true)
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441370") {
          status must_== 200
          val tulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.size must_== 2
          val puuttuva1 = tulos.hakutoiveet.head
          val puuttuva2 = tulos.hakutoiveet.last
          puuttuva1.valintatila must_== Valintatila.kesken
          puuttuva1.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
          puuttuva2.valintatila must_== Valintatila.kesken
          puuttuva2.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
        }
      }

      "hyväksyttyä hakutoivetta alemmat puuttuvat merkitään tilaan PERUUNTUNUT" in {
        SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-julkaisematon-hyvaksytty.json", true)
        hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369-3.json")
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.size must_== 3
          tulos.hakutoiveet(0).valintatila must_== Valintatila.hyväksytty
          tulos.hakutoiveet(0).vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
          tulos.hakutoiveet(1).valintatila must_== Valintatila.peruuntunut
          tulos.hakutoiveet(1).vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
          tulos.hakutoiveet(2).valintatila must_== Valintatila.hyväksytty
          tulos.hakutoiveet(2).vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
        }
      }

      "jos hyväksyttyä hakutoivetta ylempi puuttuu, merkitään hyväksytty hakutoive tilaan KESKEN" in {
        hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369-flipped.json")
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos = Serialization.read[Hakemuksentulos](body)
          val puuttuva = tulos.hakutoiveet.head
          val hyvaksytty = tulos.hakutoiveet.last
          puuttuva.valintatila must_== Valintatila.kesken
          puuttuva.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
          hyvaksytty.valintatila must_== Valintatila.kesken
          hyvaksytty.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
        }
      }

      "varasijalta hyväksyttyä alemmat puuttuvat merkitään tilaan PERUUNTUNUT" in {
        hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369.json")
        SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-varasijalta-kesken-julkaistavissa.json", true)
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos = Serialization.read[Hakemuksentulos](body)
          val varasijaltaHyvaksytty = tulos.hakutoiveet.head
          val puuttuva = tulos.hakutoiveet.last
          varasijaltaHyvaksytty.valintatila must_== Valintatila.varasijalta_hyväksytty
          varasijaltaHyvaksytty.vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
          puuttuva.valintatila must_== Valintatila.peruuntunut
          puuttuva.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanotettavissa
        }
      }
    }

    "kun hakemusta ei löydy" in {
      "404" in {
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.LOLLERSTRÖM") {
          status must_== 404
          body must_== "Not found"
        }
      }
    }
  }

  "POST /haku:hakuId/hakemus/:hakemusId/vastaanota" should {
    val beforeSave = new Date()
    "vastaanottaa opiskelupaikan" in {
      hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369.json")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ilmoitettu.json", true)
      post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila must_== Vastaanottotila.vastaanottanut
          tulos.hakutoiveet.head.viimeisinVastaanottotilanMuutos.isDefined must beTrue
          tulos.hakutoiveet.head.viimeisinVastaanottotilanMuutos.get.getTime() must be ~(System.currentTimeMillis() +/- 2000)
        }
      }
    }

    "peruu opiskelupaikan" in {
      hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369.json")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ilmoitettu.json", true)
      post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"PERUNUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila.toString must_== "PERUNUT"
          tulos.hakutoiveet.head.viimeisinVastaanottotilanMuutos.isDefined must beTrue
          tulos.hakutoiveet.head.viimeisinVastaanottotilanMuutos.get.getTime() must be ~(System.currentTimeMillis() +/- 2000)
        }
      }
    }

    "vastaanottaa ehdollisesti" in {
      hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369.json")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ylempi-varalla.json", true)
      withFixedDateTime("15.8.2014 12:00") {
        post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
          """{"hakukohdeOid":"1.2.246.562.5.16303028779","tila":"EHDOLLISESTI_VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
          status must_== 200

          get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
            val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
            tulos.hakutoiveet.head.valintatila must_== Valintatila.varalla
            tulos.hakutoiveet.head.vastaanottotila.toString must_== "KESKEN"
            tulos.hakutoiveet.head.viimeisinVastaanottotilanMuutos.isDefined must beFalse
            tulos.hakutoiveet.last.vastaanottotila.toString must_== "EHDOLLISESTI_VASTAANOTTANUT"
            tulos.hakutoiveet.last.viimeisinVastaanottotilanMuutos.isDefined must beTrue
            tulos.hakutoiveet.last.viimeisinVastaanottotilanMuutos.get.getTime() must be ~(System.currentTimeMillis() +/- 2000)
          }
        }
      }
    }
  }

  addServlet(new ValintatulosServlet(), "/haku")
  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}
package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat

import fi.vm.sade.sijoittelu.tulos.testfixtures.FixtureImporter
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.Vastaanotettavuustila
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, Vastaanottotila, Vastaanotettavuustila, Valintatila}
import fi.vm.sade.valintatulosservice.fixtures.HakemusFixtureImporter
import fi.vm.sade.sijoittelu.tulos.testfixtures.{FixtureImporter => SijoitteluFixtureImporter}
import org.joda.time.DateTimeUtils
import org.json4s.jackson.Serialization
import org.scalatra.test.specs2.MutableScalatraSpec
import org.specs2.specification.{Fragments, Step}

class ValintaTulosServletSpec extends MutableScalatraSpec {
  implicit val appConfig: AppConfig = new AppConfig.IT
  implicit val formats = JsonFormats.jsonFormats
  val hakemusFixtureImporter = new HakemusFixtureImporter(appConfig.settings.hakemusMongoConfig)

  sequential

  "GET /haku/:hakuId/hakemus/:hakemusId" should {
    "palauttaa valintatulokset" in {
      get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
        body must_== """{"hakemusOid":"1.2.246.562.11.00000441369","hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","tarjoajaOid":"1.2.246.562.10.591352080610","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":"EI_TEHTY","vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T19:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T19:05:23Z"},{"hakukohdeOid":"1.2.246.562.5.16303028779","tarjoajaOid":"1.2.246.562.10.455978782510","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":"EI_TEHTY","vastaanotettavuustila":"EI_VASTAANOTETTAVISSA"}]}"""
      }
    }
    "sijoittelusta puuttuvat hakutoiveet" in {
      "merkitään tilaan KESKEN" in {
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441370") {
          status must_== 200
          val tulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.size must_== 2
          val puuttuva1 = tulos.hakutoiveet.head
          val puuttuva2 = tulos.hakutoiveet.last
          puuttuva1.valintatila must_== Valintatila.kesken
          puuttuva1.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanottavissa
          puuttuva2.valintatila must_== Valintatila.kesken
          puuttuva2.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanottavissa
        }
      }

      "hyväksyttyä hakutoivetta alemmat puuttuvat merkitään tilaan PERUUNTUNUT" in {
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.size must_== 2
          val hyvaksytty = tulos.hakutoiveet.head
          val puuttuva = tulos.hakutoiveet.last
          hyvaksytty.valintatila must_== Valintatila.hyväksytty
          hyvaksytty.vastaanotettavuustila must_== Vastaanotettavuustila.vastaanotettavissa_sitovasti
          puuttuva.valintatila must_== Valintatila.peruuntunut
          puuttuva.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanottavissa
        }
      }

      "jos hyväksyttyä hakutoivetta ylempi puuttuu, merkitään hyväksytty hakutoive tilaan KESKEN" in {
        hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369-flipped.json")
        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos = Serialization.read[Hakemuksentulos](body)
          val puuttuva = tulos.hakutoiveet.head
          val hyvaksytty = tulos.hakutoiveet.last
          puuttuva.valintatila must_== Valintatila.kesken
          puuttuva.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanottavissa
          hyvaksytty.valintatila must_== Valintatila.kesken
          hyvaksytty.vastaanotettavuustila must_== Vastaanotettavuustila.ei_vastaanottavissa
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
    "vastaanottaa opiskelupaikan" in {
      hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369.json")
      post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila must_== Vastaanottotila.vastaanottanut
        }
      }
    }

    "peruu opiskelupaikan" in {
      hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369.json")
      SijoitteluFixtureImporter.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ilmoitettu.json")
      post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"PERUNUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila.toString must_== "PERUNUT"
        }
      }
    }

    "vastaanottaa ehdollisesti" in {
      hakemusFixtureImporter.clear.importData("fixtures/hakemus/00000441369.json")
      SijoitteluFixtureImporter.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ylempi-varalla.json")
      withFixedDate("15.8.2014") {
        post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
          """{"hakukohdeOid":"1.2.246.562.5.72607738903","tila":"EHDOLLISESTI_VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
          status must_== 200

          get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
            val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
            tulos.hakutoiveet.head.vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaaottanut
          }
        }
      }
    }
  }

  private def withFixedDate[T](date: String)(f: => T) = {
    DateTimeUtils.setCurrentMillisFixed(new SimpleDateFormat("d.M.yyyy").parse(date).getTime)
    try {
      f
    }
    finally {
      DateTimeUtils.setCurrentMillisSystem
    }
  }

  addServlet(new ValintatulosServlet(), "/*")
  override def map(fs: => Fragments) = {
    Step(appConfig.start) ^ super.map(fs)
  }
}
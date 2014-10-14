package fi.vm.sade.valintatulosservice.local

import java.util.Date
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain.{Hakemuksentulos, _}
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import fi.vm.sade.valintatulosservice.{JsonFormats, TimeWarp, ValintatulosServlet, ValintatulosSwagger}
import org.json4s.jackson.Serialization
import org.scalatra.swagger.Swagger
import org.scalatra.test.specs2.MutableScalatraSpec
import org.specs2.specification.{Fragments, Step}
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures

class ValintaTulosServletSpec extends MutableScalatraSpec with TimeWarp {
  implicit val appConfig: AppConfig = new AppConfig.IT
  implicit val swagger: Swagger = new ValintatulosSwagger
  implicit val formats = JsonFormats.jsonFormats
  val hakemusFixtureImporter = HakemusFixtures()

  sequential

  "GET /haku/:hakuId/hakemus/:hakemusId" should {
    HakuFixtures.activeFixture = HakuFixtures.korkeakouluYhteishaku
    "palauttaa valintatulokset" in {
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json", true)
      get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
        body must_== """{"hakemusOid":"1.2.246.562.11.00000441369","aikataulu":{"vastaanottoEnd":"2100-01-10T12:00:00Z","vastaanottoBufferDays":14},"hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","tarjoajaOid":"1.2.246.562.10.591352080610","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":"EI_TEHTY","vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","viimeisinValintatuloksenMuutos":"2014-08-26T19:05:23Z","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T19:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T19:05:23Z","julkaistavissa":true,"tilanKuvaukset":{}},{"hakukohdeOid":"1.2.246.562.5.16303028779","tarjoajaOid":"1.2.246.562.10.455978782510","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":"EI_TEHTY","vastaanotettavuustila":"EI_VASTAANOTETTAVISSA","julkaistavissa":true,"tilanKuvaukset":{}}]}"""
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
    HakuFixtures.activeFixture = HakuFixtures.korkeakouluYhteishaku
    "vastaanottaa opiskelupaikan" in {
      hakemusFixtureImporter.clear.importData("00000441369")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json", true)
      post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila must_== Vastaanottotila.vastaanottanut
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.isDefined must beTrue
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.get.getTime() must be ~(System.currentTimeMillis() +/- 2000)
        }
      }

      post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/ilmoittaudu",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"LASNA_KOKO_LUKUVUOSI","muokkaaja":"OILI","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.ilmoittautumistila must_== Ilmoittautumistila.läsnä_koko_lukuvuosi
        }
      }
    }

    "peruu opiskelupaikan" in {
      hakemusFixtureImporter.clear.importData("00000441369")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json", true)
      post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"PERUNUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila.toString must_== "PERUNUT"
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.isDefined must beTrue
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.get.getTime() must be ~(System.currentTimeMillis() +/- 2000)
        }
      }
    }

    "vastaanottaa ehdollisesti" in {
      hakemusFixtureImporter.clear.importData("00000441369")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ylempi-varalla.json", true)
      withFixedDateTime("15.8.2014 12:00") {
        post("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
          """{"hakukohdeOid":"1.2.246.562.5.16303028779","tila":"EHDOLLISESTI_VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
          status must_== 200

          get("/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
            val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
            tulos.hakutoiveet.head.valintatila must_== Valintatila.varalla
            tulos.hakutoiveet.head.vastaanottotila.toString must_== "KESKEN"
            tulos.hakutoiveet.last.vastaanottotila.toString must_== "EHDOLLISESTI_VASTAANOTTANUT"
            val muutosAika = tulos.hakutoiveet.last.viimeisinValintatuloksenMuutos.get
            tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.get.before(muutosAika) must beTrue
            muutosAika.getTime() must be ~(System.currentTimeMillis() +/- 2000)
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
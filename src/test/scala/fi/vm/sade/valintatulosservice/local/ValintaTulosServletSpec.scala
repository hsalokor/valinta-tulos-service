package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.security.cas.CasTicketRequest
import fi.vm.sade.security.ldap.LdapUser
import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.hakemus.HakemusFixtures
import fi.vm.sade.valintatulosservice.json.JsonFormats
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtures
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures
import fi.vm.sade.valintatulosservice.tcp.PortChecker
import org.json4s.jackson.Serialization
import org.scalatra.swagger.Swagger
import org.scalatra.test.HttpComponentsClient
import org.specs2.mutable.Specification
import org.specs2.specification.{Fragments, Step}

class ValintaTulosServletSpec extends Specification with TimeWarp with HttpComponentsClient {
  implicit val appConfig: AppConfig = new AppConfig.IT
  implicit val swagger: Swagger = new ValintatulosSwagger
  implicit val formats = JsonFormats.jsonFormats
  val hakemusFixtureImporter = HakemusFixtures()
  HakuFixtures.activeFixture = HakuFixtures.korkeakouluYhteishaku

  lazy val jettyLauncher = new JettyLauncher(PortChecker.findFreeLocalPort, Some("it"))

  def baseUrl = "http://localhost:" + jettyLauncher.port + "/valinta-tulos-service"

  override def map(fs: => Fragments) = {
    Step(jettyLauncher.start) ^ super.map(fs)
  }

  sequential

  "GET /haku/:hakuId/hakemus/:hakemusId" should {
    "palauttaa valintatulokset" in {
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json", true)
      get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
        body must_== """{"hakemusOid":"1.2.246.562.11.00000441369","hakijaOid":"1.2.246.562.24.14229104472","aikataulu":{"vastaanottoEnd":"2100-01-10T12:00:00Z","vastaanottoBufferDays":14},"hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","tarjoajaOid":"1.2.246.562.10.591352080610","valintatapajonoOid":"14090336922663576781797489829886","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","viimeisinValintatuloksenMuutos":"2014-08-26T19:05:23Z","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T19:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T19:05:23Z","julkaistavissa":true,"tilanKuvaukset":{},"pisteet":4.0},{"hakukohdeOid":"1.2.246.562.5.16303028779","tarjoajaOid":"1.2.246.562.10.455978782510","valintatapajonoOid":"","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"EI_VASTAANOTETTAVISSA","julkaistavissa":true,"tilanKuvaukset":{}}]}"""
      }
    }

    "kun hakemusta ei löydy" in {
      "404" in {
        get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.LOLLERSTRÖM") {
          status must_== 404
          body must_== "Not found"
        }
      }
    }
  }

  "GET /cas/haku/:hakuId/hakemus/:hakemusId" should {
    "estää pääsyn ilman tikettiä" in {
      get("cas/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
        status must_== 401
      }
    }
    "mahdolistaa pääsyn validilla tiketillä" in {
      val ticketRequest: CasTicketRequest = appConfig.settings.securitySettings.ticketRequest
      val ticket = appConfig.securityContext.ticketClient.getServiceTicket(ticketRequest).get.toString
      get("cas/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369", ("ticket", ticket)) {
        status must_== 200
      }
    }
  }

  "GET /haku/:hakuOid" should {
    "palauttaa koko haun valintatulokset" in {
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json", true)
      get("haku/1.2.246.562.5.2013080813081926341928") {
        status must_== 200
        body must_== """[{"hakemusOid":"1.2.246.562.11.00000441369","hakijaOid":"1.2.246.562.24.14229104472","aikataulu":{"vastaanottoEnd":"2100-01-10T12:00:00Z","vastaanottoBufferDays":14},"hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","tarjoajaOid":"1.2.246.562.10.591352080610","valintatapajonoOid":"14090336922663576781797489829886","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","viimeisinValintatuloksenMuutos":"2014-08-26T19:05:23Z","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T19:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T19:05:23Z","julkaistavissa":true,"tilanKuvaukset":{},"pisteet":4.0},{"hakukohdeOid":"1.2.246.562.5.16303028779","tarjoajaOid":"1.2.246.562.10.455978782510","valintatapajonoOid":"","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"EI_VASTAANOTETTAVISSA","julkaistavissa":true,"tilanKuvaukset":{}}]}]"""
      }
    }

    "kun hakua ei löydy" in {
      "404" in {
        HakuFixtures.activeFixture = "notfound"
        get("haku/1.2.246.562.5.foo") {
          status must_== 404
          body must_== "Not found"
        }
      }
    }
  }


  "POST /haku:hakuId/hakemus/:hakemusId/vastaanota" should {
    "vastaanottaa opiskelupaikan" in {
      HakuFixtures.activeFixture = HakuFixtures.korkeakouluYhteishaku
      hakemusFixtureImporter.clear.importData("00000441369")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json", true)
      post("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila must_== Vastaanottotila.vastaanottanut
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.isDefined must beTrue
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.get.getTime() must be ~ (System.currentTimeMillis() +/- 2000)
        }
      }

      post("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/ilmoittaudu",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"LASNA_KOKO_LUKUVUOSI","muokkaaja":"OILI","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.ilmoittautumistila must_== HakutoiveenIlmoittautumistila(Ilmoittautumisaika(None, None), None, Ilmoittautumistila.läsnä_koko_lukuvuosi, false)
        }
      }
    }

    "peruu opiskelupaikan" in {
      hakemusFixtureImporter.clear.importData("00000441369")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-kesken-julkaistavissa.json", true)
      post("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
        """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":"PERUNUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
        status must_== 200

        get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila.toString must_== "PERUNUT"
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.isDefined must beTrue
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.get.getTime() must be ~ (System.currentTimeMillis() +/- 2000)
        }
      }
    }

    "vastaanottaa ehdollisesti" in {
      hakemusFixtureImporter.clear.importData("00000441369")
      SijoitteluFixtures.importFixture(appConfig.sijoitteluContext.database, "hyvaksytty-ylempi-varalla.json", true)
      withFixedDateTime("15.8.2014 12:00") {
        post("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/vastaanota",
          """{"hakukohdeOid":"1.2.246.562.5.16303028779","tila":"EHDOLLISESTI_VASTAANOTTANUT","muokkaaja":"Teppo Testi","selite":"Testimuokkaus"}""".getBytes("UTF-8"), Map("Content-type" -> "application/json")) {
          status must_== 200

          get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
            val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
            tulos.hakutoiveet.head.valintatila must_== Valintatila.varalla
            tulos.hakutoiveet.head.vastaanottotila.toString must_== "KESKEN"
            tulos.hakutoiveet.last.vastaanottotila.toString must_== "EHDOLLISESTI_VASTAANOTTANUT"
            val muutosAika = tulos.hakutoiveet.last.viimeisinValintatuloksenMuutos.get
            tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.get.before(muutosAika) must beTrue
            muutosAika.getTime() must be ~ (System.currentTimeMillis() +/- 2000)
          }
        }
      }
    }
  }
}
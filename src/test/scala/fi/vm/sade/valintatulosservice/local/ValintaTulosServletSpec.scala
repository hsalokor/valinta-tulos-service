package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures
import org.joda.time.DateTime
import org.json4s.jackson.Serialization
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValintaTulosServletSpec extends ServletSpecification {
  "GET /haku/:hakuOid/hakukohde/:hakukohdeOid" should {
    "palauttaa yksittäisen hakukohteen valintatulokset" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      get("haku/1.2.246.562.5.2013080813081926341928/hakukohde/1.2.246.562.5.72607738902") {
        status must_== 200
        body must_== """[{"hakuOid":"1.2.246.562.5.2013080813081926341928","hakemusOid":"1.2.246.562.11.00000441369","hakijaOid":"1.2.246.562.24.14229104472","aikataulu":{"vastaanottoEnd":"2100-01-10T10:00:00Z","vastaanottoBufferDays":14},"hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","hakukohdeNimi":"stevari amk hakukohde","tarjoajaOid":"1.2.246.562.10.591352080610","tarjoajaNimi":"Saimaan ammattikorkeakoulu, Skinnarilan kampus, Lappeenranta","valintatapajonoOid":"14090336922663576781797489829886","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{"loppu":"2100-01-10T21:59:59Z"},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","vastaanottoDeadline":"2100-01-10T10:00:00Z","viimeisinHakemuksenTilanMuutos":"2014-08-26T15:12:40Z","viimeisinValintatuloksenMuutos":"2014-08-26T16:05:23Z","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T16:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T16:05:23Z","julkaistavissa":true,"tilanKuvaukset":{},"pisteet":4.0},{"hakukohdeOid":"1.2.246.562.5.16303028779","hakukohdeNimi":"","tarjoajaOid":"1.2.246.562.10.455978782510","tarjoajaNimi":"","valintatapajonoOid":"","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{"loppu":"2100-01-10T21:59:59Z"},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"EI_VASTAANOTETTAVISSA","julkaistavissa":true,"tilanKuvaukset":{}}]}]"""
      }
    }

    "kun hakukohdetta ei löydy" in {
      "404" in {
        HakuFixtures.useFixture("notfound")
        get("haku/1.2.246.562.5.2013080813081926341928/hakukohde/1.2.246.562.5.foo") {
          status must_== 404
          body must_== """{"error":"Not found"}"""
        }
      }
    }
  }

  "GET /haku/:hakuId/hakemus/:hakemusId" should {
    "palauttaa valintatulokset" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")
      get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
        body must_== """{"hakuOid":"1.2.246.562.5.2013080813081926341928","hakemusOid":"1.2.246.562.11.00000441369","hakijaOid":"1.2.246.562.24.14229104472","aikataulu":{"vastaanottoEnd":"2100-01-10T10:00:00Z","vastaanottoBufferDays":14},"hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","hakukohdeNimi":"stevari amk hakukohde","tarjoajaOid":"1.2.246.562.10.591352080610","tarjoajaNimi":"Saimaan ammattikorkeakoulu, Skinnarilan kampus, Lappeenranta","valintatapajonoOid":"14090336922663576781797489829886","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{"loppu":"2100-01-10T21:59:59Z"},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","vastaanottoDeadline":"2100-01-10T10:00:00Z","viimeisinHakemuksenTilanMuutos":"2014-08-26T15:12:40Z","viimeisinValintatuloksenMuutos":"2014-08-26T16:05:23Z","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T16:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T16:05:23Z","julkaistavissa":true,"tilanKuvaukset":{},"pisteet":4.0},{"hakukohdeOid":"1.2.246.562.5.16303028779","hakukohdeNimi":"","tarjoajaOid":"1.2.246.562.10.455978782510","tarjoajaNimi":"","valintatapajonoOid":"","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{"loppu":"2100-01-10T21:59:59Z"},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"EI_VASTAANOTETTAVISSA","julkaistavissa":true,"tilanKuvaukset":{}}]}"""
      }
    }

    "kun hakemusta ei löydy" in {
      "404" in {
        get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.LOLLERSTRÖM") {
          body must_== """{"error":"Not found"}"""
          status must_== 404
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
      get("cas/haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369", ("ticket", getTicket)) {
        status must_== 200
      }
    }
  }
  "GET /haku/:hakuOid" should {
    "palauttaa koko haun valintatulokset" in {
      //SijoitteluFixtures(appConfig.sijoitteluContext.database).importFixture("hyvaksytty-kesken-julkaistavissa.json", true)
      get("haku/1.2.246.562.5.2013080813081926341928") {
        status must_== 200
        body must_== """[{"hakuOid":"1.2.246.562.5.2013080813081926341928","hakemusOid":"1.2.246.562.11.00000441369","hakijaOid":"1.2.246.562.24.14229104472","aikataulu":{"vastaanottoEnd":"2100-01-10T10:00:00Z","vastaanottoBufferDays":14},"hakutoiveet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","hakukohdeNimi":"stevari amk hakukohde","tarjoajaOid":"1.2.246.562.10.591352080610","tarjoajaNimi":"Saimaan ammattikorkeakoulu, Skinnarilan kampus, Lappeenranta","valintatapajonoOid":"14090336922663576781797489829886","valintatila":"HYVAKSYTTY","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{"loppu":"2100-01-10T21:59:59Z"},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"VASTAANOTETTAVISSA_SITOVASTI","vastaanottoDeadline":"2100-01-10T10:00:00Z","viimeisinHakemuksenTilanMuutos":"2014-08-26T15:12:40Z","viimeisinValintatuloksenMuutos":"2014-08-26T16:05:23Z","jonosija":1,"varasijojaKaytetaanAlkaen":"2014-08-26T16:05:23Z","varasijojaTaytetaanAsti":"2014-08-26T16:05:23Z","julkaistavissa":true,"tilanKuvaukset":{},"pisteet":4.0},{"hakukohdeOid":"1.2.246.562.5.16303028779","hakukohdeNimi":"","tarjoajaOid":"1.2.246.562.10.455978782510","tarjoajaNimi":"","valintatapajonoOid":"","valintatila":"PERUUNTUNUT","vastaanottotila":"KESKEN","ilmoittautumistila":{"ilmoittautumisaika":{"loppu":"2100-01-10T21:59:59Z"},"ilmoittautumistapa":{"nimi":{"fi":"Oili","sv":"Oili","en":"Oili"},"url":"/oili/"},"ilmoittautumistila":"EI_TEHTY","ilmoittauduttavissa":false},"vastaanotettavuustila":"EI_VASTAANOTETTAVISSA","julkaistavissa":true,"tilanKuvaukset":{}}]}]"""
      }
    }

    "kun hakua ei löydy" in {
      "404" in {
        HakuFixtures.useFixture("notfound")
        get("haku/1.2.246.562.5.foo") {
          status must_== 404
          body must_== """{"error":"Not found"}"""
        }
      }
    }
  }


  "POST /haku/:hakuId/hakemus/:hakemusId/ilmoittaudu" should {
    "merkitsee ilmoittautuneeksi" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")

      vastaanota("VastaanotaSitovasti") {
        ilmoittaudu("LASNA_KOKO_LUKUVUOSI") {
          status must_== 200

          get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
            val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
            tulos.hakutoiveet.head.ilmoittautumistila must_== HakutoiveenIlmoittautumistila(Ilmoittautumisaika(None, Some(new DateTime(2100, 1, 10, 23, 59, 59))), None, Ilmoittautumistila.läsnä_koko_lukuvuosi, false)
          }
        }
      }
    }

    "hyväksyy ilmoittautumisen vain jos vastaanotettu ja ilmoittauduttavissa" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")

      ilmoittaudu("LASNA_KOKO_LUKUVUOSI") {
        body must_== """{"error":"Hakutoive 1.2.246.562.5.72607738902 ei ole ilmoittauduttavissa: ilmoittautumisaika: {\"loppu\":\"2100-01-10T21:59:59Z\"}, ilmoittautumistila: EI_TEHTY, valintatila: HYVAKSYTTY, vastaanottotila: KESKEN"}"""
        status must_== 400
      }
    }

    "raportoi virheellisen pyynnön" in {
      postJSON("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/ilmoittaudu",
        ("oops")) {
        body must startWith("{\"error\":\"No usable value for hakukohdeOid")
        status must_== 400
      }
    }

    "raportoi puuttuvan/väärän content-typen" in {
      ilmoittaudu("LASNA_KOKO_LUKUVUOSI", headers = Map(("Content-type" -> "application/xml"))) {
        body must startWith("{\"error\":\"Only application/json accepted")
        status must_== 415
      }
    }
  }

  "POST /cas/haku/:hakuId/hakemus/:hakemusId/ilmoittaudu" should {
    "estää pääsyn ilman tikettiä" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")

      ilmoittaudu("LASNA_KOKO_LUKUVUOSI", juuri = "cas/haku") {
        status must_== 401
        body must_== """{"error":"CAS ticket required"}"""
      }
    }

    "toimii tiketillä" in {
      vastaanota("VastaanotaSitovasti") {
        ilmoittaudu("LASNA_KOKO_LUKUVUOSI", juuri = "cas/haku", headers = Map("ticket" -> getTicket)) {
          status must_== 200
        }
      }
    }
  }

  "POST /haku/:hakuId/hakemus/:hakemusId/vastaanota" should {
    "vastaanottaa opiskelupaikan" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")

      vastaanota("VastaanotaSitovasti") {
        status must_== 200

        get("haku/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369") {
          val tulos: Hakemuksentulos = Serialization.read[Hakemuksentulos](body)
          tulos.hakutoiveet.head.vastaanottotila must_== Vastaanottotila.vastaanottanut
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.isDefined must beTrue
          tulos.hakutoiveet.head.viimeisinValintatuloksenMuutos.get.getTime() must be ~ (System.currentTimeMillis() +/- 2000)
        }
      }
    }

    "peruu opiskelupaikan" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")

      vastaanota("Peru") {
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
      useFixture("hyvaksytty-ylempi-varalla.json")

      vastaanota("VastaanotaEhdollisesti", hakukohde = "1.2.246.562.5.16303028779") {
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

  def vastaanota[T](action: String, hakukohde: String = "1.2.246.562.5.72607738902", personOid: String = "1.2.246.562.24.14229104472", hakemusOid: String = "1.2.246.562.11.00000441369")(block: => T) = {
    postJSON(s"""vastaanotto/henkilo/$personOid/hakemus/$hakemusOid/hakukohde/$hakukohde""",
      s"""{"action":"$action"}""") {
      block
    }
  }

  def ilmoittaudu[T](tila: String, juuri:String = "haku", headers: Map[String, String] = Map.empty)(block: => T) = {
    postJSON(juuri + "/1.2.246.562.5.2013080813081926341928/hakemus/1.2.246.562.11.00000441369/ilmoittaudu",
      """{"hakukohdeOid":"1.2.246.562.5.72607738902","tila":""""+tila+"""","muokkaaja":"OILI","selite":"Testimuokkaus"}""", headers) {
      block
    }
  }

  def getTicket = {
    val ticket = appConfig.securityContext.casClient.ticketFor(appConfig.settings.securitySettings.casServiceIdentifier, "testuser")
    ticket
  }
}

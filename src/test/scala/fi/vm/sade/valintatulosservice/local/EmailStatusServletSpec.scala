package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.vastaanottomeili.{LahetysKuittaus, VastaanotettavuusIlmoitus}
import fi.vm.sade.valintatulosservice.{ServletSpecification, TimeWarp}
import org.json4s.jackson.Serialization

class EmailStatusServletSpec extends ServletSpecification with TimeWarp {
  "GET /vastaanottoposti" should {
    "Lista lähetettävistä sähköposteista" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakemusFixtures = List("00000441369"))

      withFixedDateTime("10.10.2014 12:00") {
        get("vastaanottoposti") {
          status must_== 200
          body must_== """[{"hakemusOid":"1.2.246.562.11.00000441369","hakijaOid":"1.2.246.562.24.14229104472","asiointikieli":"FI","etunimi":"Teppo","email":"teppo@testaaja.fi","deadline":"2100-01-10T10:00:00Z","hakukohteet":["1.2.246.562.5.72607738902"]}]"""
        }
      }
    }
    "Tyhjä lista lähetettävistä sähköposteista, kun ei lähetettävää" in {
      useFixture("hylatty-ei-valintatulosta.json", hakemusFixtures = List("00000441369"))
      withFixedDateTime("10.10.2014 12:00") {
        get("vastaanottoposti") {
          status must_== 200
          body must_== """[]"""
        }
      }
    }

    "Kun email-osoite puuttuu" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakemusFixtures = List("00000441369-no-email"))

      withFixedDateTime("10.10.2014 12:00") {
        get("vastaanottoposti") {
          status must_== 200
          body must_== """[]"""
        }
      }
    }

    "Kun henkilötunnus puuttuu" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakemusFixtures = List("00000441369-no-hetu"))

      withFixedDateTime("10.10.2014 12:00") {
        get("vastaanottoposti") {
          status must_== 200
          body must_== """[]"""
        }
      }
    }
  }

  "POST /vastaanottoposti" should {
    "Merkitsee postitukset tehdyiksi" in {
      get("vastaanottoposti") {
        val mailsToSend = Serialization.read[List[VastaanotettavuusIlmoitus]](body)
        val kuittaukset = mailsToSend.map { mail =>
          LahetysKuittaus(mail.hakemusOid, mail.hakukohteet, List("email"))
        }
        postJSON("vastaanottoposti", Serialization.write(kuittaukset)) {
          status must_== 200
          get("vastaanottoposti") {
            body must_== "[]"
          }
        }
      }
    }
  }

}

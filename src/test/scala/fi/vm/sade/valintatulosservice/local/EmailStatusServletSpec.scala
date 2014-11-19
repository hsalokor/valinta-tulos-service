package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice.{ServletSpecification, TimeWarp}

class EmailStatusServletSpec extends ServletSpecification with TimeWarp {
  "GET /vastaanottoposti" should {
    "Lista lähtettävistä sähköposteista" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json")

      withFixedDateTime("10.10.2014 12:00") {
        get("vastaanottoposti") {
          status must_== 200

          // TODO: deadline pitää laskea oikein

          body must_== """[{"hakemusOid":"1.2.246.562.11.00000441369","hakijaOid":"1.2.246.562.24.14229104472","etunimi":"Teppo","email":"teppo@testaaja.fi","deadline":"2014-11-10T10:00:00Z"}]"""
        }
      }

    }
  }

  /*"POST /vastaanottoposti" should {
    "Merkitsee postitukset tehdyiksi" in {
      get("vastaanottoposti") {
        postJSON("vastaanottoposti", body) {
          status must_== 200
          get("vastaanottoposti") {
            body must_== "[]"
          }
        }
      }
    }
  }*/
}

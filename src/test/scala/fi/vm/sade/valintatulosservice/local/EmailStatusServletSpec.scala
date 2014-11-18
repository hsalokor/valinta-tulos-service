package fi.vm.sade.valintatulosservice.local

class EmailStatusServletSpec extends ServletSpecification {
  "GET /vastaanottoposti" should {
    "Lista lähtettävistä sähköposteista" in {
      get("vastaanottoposti") {
        status must_== 200
        body must_== """[{"hakemusOid":"1.2.246.562.11.00000441369","hakukohteet":[{"hakukohdeOid":"1.2.246.562.5.72607738902","valintatapajonoOid":"14090336922663576781797489829886","shouldMail":true},{"hakukohdeOid":"1.2.246.562.5.16303028779","valintatapajonoOid":"","shouldMail":false}]}]"""
      }
    }
  }
}

package fi.vm.sade.valintatulosservice.fixture

import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixture, HakutoiveFixture}

object ExampleFixture {
  val hakuOid = "1"
  val hakemusOid = "1"
  val hakukohdeOid = "1"
  val tarjoajaOid = "1"
  val sijoitteluajoId = 1l

  val hakemus = HakemusFixture(hakemusOid, List(
    HakutoiveFixture(1, tarjoajaOid, hakukohdeOid)
  ))

  val fixture = GeneratedFixture(hakuOid, sijoitteluajoId, List(hakemus))
}
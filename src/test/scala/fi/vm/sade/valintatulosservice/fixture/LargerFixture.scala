package fi.vm.sade.valintatulosservice.fixture

import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixture, HakutoiveFixture}

class LargerFixture(hakukohteita: Int, hakemuksia: Int, randomize: Boolean = false) {
  val hakuOid = "1"
  val sijoitteluajoId = 1l
  val kaikkiJonotSijoiteltu = true

  val hakutoiveet: List[HakutoiveFixture] = (1 to hakukohteita).map { hakukohdeNumero =>
    val hakukohdeOid = hakukohdeNumero.toString
    val tarjoajaOid = hakukohdeNumero.toString
    HakutoiveFixture(hakukohdeNumero, tarjoajaOid, hakukohdeOid)
  }.toList

  val hakemukset: List[HakemusFixture] = (1 to hakemuksia).map { hakemusNumero =>
    val hakemusOid = hakemusNumero.toString
    HakemusFixture(hakemusOid, hakutoiveet)
  }.toList

  val fixture = GeneratedFixture(hakuOid, sijoitteluajoId, hakemukset, randomize = randomize)
}


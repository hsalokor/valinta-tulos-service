package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtureCreator

object ExampleFixture {
  val hakuOid = "1"
  val jonoHylatty = "1"
  val jonoHyvaksytty = "2"
  val hakemusOid = "1"
  val hakukohdeOid = "1"
  val hakijaOid = "1"
  val tarjoajaOid = "1"
  val hakutoiveIndex = 1
  val sijoitteluajoId = 1l
  val kaikkiJonotSijoiteltu = true

  val valintatulosHylatty = SijoitteluFixtureCreator.newValintatulos(jonoHylatty, hakuOid, hakemusOid, hakukohdeOid, hakijaOid, hakutoiveIndex)
  val valintatulosHyvaksytty = SijoitteluFixtureCreator.newValintatulos(jonoHyvaksytty, hakuOid, hakemusOid, hakukohdeOid, hakijaOid, hakutoiveIndex)

  val jonot = List(
    SijoitteluFixtureCreator.newValintatapajono(jonoHylatty, List(SijoitteluFixtureCreator.newHakemus(hakemusOid, hakijaOid, hakutoiveIndex, HakemuksenTila.HYLATTY))),
    SijoitteluFixtureCreator.newValintatapajono(jonoHyvaksytty, List(SijoitteluFixtureCreator.newHakemus(hakemusOid, hakijaOid, hakutoiveIndex, HakemuksenTila.HYVAKSYTTY)))
  )
  val hakukohde = SijoitteluFixtureCreator.newHakukohde(hakukohdeOid, tarjoajaOid, sijoitteluajoId, kaikkiJonotSijoiteltu, jonot)
  val sijoittelu: Sijoittelu = SijoitteluFixtureCreator.newSijoittelu(hakuOid, sijoitteluajoId, List(hakukohdeOid))

  val fixture = GeneratedFixture(sijoittelu, List(hakukohde), List(valintatulosHylatty, valintatulosHyvaksytty))
}


package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.sijoittelu.domain.{Sijoittelu, HakemuksenTila, Hakemus}
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtureCreator

object ExampleFixture {
  val hakuOid = "1"
  val jonoOid = "1"
  val hakemusOid = "1"
  val hakukohdeOid = "1"
  val hakijaOid = "1"
  val tarjoajaOid = "1"
  val hakutoiveIndex = 1
  val sijoitteluajoId = 1l
  val kaikkiJonotSijoiteltu = true
  val hakemuksenTila = HakemuksenTila.HYVAKSYTTY

  val valintatulos = SijoitteluFixtureCreator.newValintatulos(jonoOid, hakemusOid, hakukohdeOid, hakijaOid, hakutoiveIndex)
  val hakemus: Hakemus = SijoitteluFixtureCreator.newHakemus(hakemusOid, hakijaOid, hakutoiveIndex, hakemuksenTila)
  val jonot = List(SijoitteluFixtureCreator.newValintatapajono(jonoOid, List(hakemus)))
  val hakukohde = SijoitteluFixtureCreator.newHakukohde(hakukohdeOid, tarjoajaOid, sijoitteluajoId, kaikkiJonotSijoiteltu, jonot)
  val sijoittelu: Sijoittelu = SijoitteluFixtureCreator.newSijoittelu(hakuOid, sijoitteluajoId, List(hakukohdeOid))

  def apply(appConfig: AppConfig) {
    appConfig.sijoitteluContext.sijoitteluDao.persistSijoittelu(sijoittelu)
    appConfig.sijoitteluContext.hakukohdeDao.persistHakukohde(hakukohde)
    appConfig.sijoitteluContext.valintatulosDao.createOrUpdateValintatulos(valintatulos)
  }
}

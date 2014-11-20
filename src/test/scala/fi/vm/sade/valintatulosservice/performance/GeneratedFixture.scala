package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.sijoittelu.domain.{HakemuksenTila, Valintatulos, Hakukohde, Sijoittelu}
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixtures, HakemusFixture, HakutoiveFixture}
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtureCreator
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures

case class GeneratedFixture(
  hakuOid: String,
  sijoitteluajoId: Long,
  hakemukset: List[HakemusFixture],
  kaikkiJonotSijoiteltu: Boolean = true,
  hakuFixture: String = HakuFixtures.korkeakouluYhteishaku)
{
  import collection.JavaConversions._

  val hakukohteet = hakemukset(0).hakutoiveet.map { hakutoive: HakutoiveFixture =>
    val jonot = List(
      jono(hakutoive.hakukohdeOid + ".1", HakemuksenTila.HYLATTY, hakutoive.index),
      jono(hakutoive.hakukohdeOid + ".2", HakemuksenTila.HYVAKSYTTY, hakutoive.index)
    )
    SijoitteluFixtureCreator.newHakukohde(hakutoive.hakukohdeOid, hakutoive.tarjoajaOid, sijoitteluajoId, kaikkiJonotSijoiteltu, jonot)
  }.toList

  def jono(jonoId: String, tila: HakemuksenTila, hakutoiveIndex: Int) = {
    val hakemusObjects = hakemukset.map { hakemus =>
      SijoitteluFixtureCreator.newHakemus(hakemus.hakemusOid, hakemus.hakemusOid, hakutoiveIndex, tila)
    }.toList
    SijoitteluFixtureCreator.newValintatapajono(jonoId, hakemusObjects)
  }

  val valintatulokset = for {
    hakukohde <- hakukohteet
    jono <- hakukohde.getValintatapajonot
    hakemus <- jono.getHakemukset
  } yield {
    SijoitteluFixtureCreator.newValintatulos(jono.getOid, hakuOid, hakemus.getHakemusOid, hakukohde.getOid, hakemus.getHakijaOid, hakemus.getPrioriteetti)
  }

  val sijoittelu: Sijoittelu = SijoitteluFixtureCreator.newSijoittelu(hakuOid, sijoitteluajoId, hakukohteet.map(_.getOid))

  def apply(implicit appConfig: AppConfig) {
    HakuFixtures.useFixture(hakuFixture, sijoittelu.getHakuOid)

    val hakemusFixtures = HakemusFixtures()
    hakemukset.foreach(hakemusFixtures.importTemplateFixture(_))

    appConfig.sijoitteluContext.sijoitteluDao.persistSijoittelu(sijoittelu)
    hakukohteet.foreach(appConfig.sijoitteluContext.hakukohdeDao.persistHakukohde(_))
    valintatulokset.foreach(appConfig.sijoitteluContext.valintatulosDao.createOrUpdateValintatulos(_))
  }
}

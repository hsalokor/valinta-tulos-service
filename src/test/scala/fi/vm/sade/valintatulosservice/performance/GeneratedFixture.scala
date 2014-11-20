package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.sijoittelu.domain.{Valintatulos, Hakukohde, Sijoittelu}
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixtures, HakemusFixture, HakutoiveFixture}
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures

case class GeneratedFixture(
  sijoittelu: Sijoittelu,
  hakukohteet: List[Hakukohde],
  valintatulokset: List[Valintatulos],
  hakuFixture: String = HakuFixtures.korkeakouluYhteishaku)
{
  import collection.JavaConversions._

  def apply(implicit appConfig: AppConfig) {
    HakuFixtures.activeFixture = hakuFixture
    HakuFixtures.hakuOids = List(sijoittelu.getHakuOid)
    val hakemuksetJaHakutoiveet = (for (hakukohde <- hakukohteet; jono <- hakukohde.getValintatapajonot.toList; hakemus <- jono.getHakemukset.toList) yield {
      (hakemus.getHakemusOid, HakutoiveFixture(hakemus.getPrioriteetti, hakukohde.getTarjoajaOid, hakukohde.getOid))
    })
    val hakemukset: Set[HakemusFixture] = hakemuksetJaHakutoiveet.map(_._1).toSet.map { hakemusOid: String =>
      HakemusFixture(hakemusOid, hakemuksetJaHakutoiveet.filter(_._1 == hakemusOid).map(_._2))
    }
    val hakemusFixtures = HakemusFixtures()
    hakemukset.foreach(hakemusFixtures.importTemplateFixture(_))

    appConfig.sijoitteluContext.sijoitteluDao.persistSijoittelu(sijoittelu)
    hakukohteet.foreach(appConfig.sijoitteluContext.hakukohdeDao.persistHakukohde(_))
    valintatulokset.foreach(appConfig.sijoitteluContext.valintatulosDao.createOrUpdateValintatulos(_))
  }
}

package fi.vm.sade.valintatulosservice.fixture

import fi.vm.sade.sijoittelu.domain.{Hakemus, HakemuksenTila, Sijoittelu}
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixture, HakemusFixtures, HakutoiveFixture}
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtureCreator
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures

class GeneratedFixture {
  def hakuOid: String = "1"

  def sijoitteluajoId: Long = 1l

  def hakemukset = List(HakemuksenTulosFixture("1", List(
    HakemuksenHakukohdeFixture("1", "1")
  )))

  def hakuFixture: String = HakuFixtures.korkeakouluYhteishaku

  def kaikkiJonotSijoiteltu: Boolean = true

  lazy val hakukohteet = {
    (for {
      hakemus <- hakemukset
      (hakutoive, index) <- hakemus.hakutoiveet.zipWithIndex
    } yield {
      (hakutoive.tarjoajaOid, hakutoive.hakukohdeOid, hakemus, index + 1)
    }).groupBy{case (tarjoaja: String, hakukohde: String, hakemus: HakemuksenTulosFixture, hakutoiveNumero: Int) => (tarjoaja, hakukohde)}
    .map { case ((tarjoajaId, hakukohdeId), values) =>
      val hakemuksetHakutoiveNumerolla = values.map { case (_, hakukohdeOid, hakemus, hakutoiveNumero) =>
        val jonot: List[ValintatapaJonoFixture] = hakemus.hakutoiveet.find( hakutoive => hakutoive.hakukohdeOid == hakukohdeOid).get.jonot
        (hakemus, jonot, hakutoiveNumero)
      }
      val jonoja = hakemuksetHakutoiveNumerolla.map { case (hakemus, jonot, hakutoiveNumero) => jonot.length }.max
      val jonot = (1 to jonoja).toList.map { jonoNumero =>
        val hakemukset: List[Hakemus] = for {
          (hakemus, jonot, hakutoiveNumero) <- hakemuksetHakutoiveNumerolla
          if (jonot.length >= jonoNumero)
        } yield {
          val hakemuksenJono = jonot(jonoNumero - 1)
          SijoitteluFixtureCreator.newHakemus(hakemus.hakemusOid, hakemus.hakemusOid, hakutoiveNumero, hakemuksenJono.tulos)
        }
        SijoitteluFixtureCreator.newValintatapajono(hakukohdeId + "." + jonoNumero, hakemukset)
      }
      SijoitteluFixtureCreator.newHakukohde(hakukohdeId, tarjoajaId, sijoitteluajoId, kaikkiJonotSijoiteltu, jonot)
    }
    .toList
  }

  import scala.collection.JavaConversions._

  lazy val valintatulokset = for {
    hakukohde <- hakukohteet
    jono <- hakukohde.getValintatapajonot
    hakemus <- jono.getHakemukset
  } yield {
    SijoitteluFixtureCreator.newValintatulos(jono.getOid, hakuOid, hakemus.getHakemusOid, hakukohde.getOid, hakemus.getHakijaOid, hakemus.getPrioriteetti)
  }

  lazy val sijoittelu: Sijoittelu = SijoitteluFixtureCreator.newSijoittelu(hakuOid, sijoitteluajoId, hakukohteet.map(_.getOid))

  def apply(implicit appConfig: AppConfig) {
    HakuFixtures.useFixture(hakuFixture, sijoittelu.getHakuOid)

    val hakemusFixtures = HakemusFixtures()
    hakemusFixtures.clear
    hakemukset
      .map { hakemus => HakemusFixture(hakemus.hakemusOid, hakemus.hakutoiveet.zipWithIndex.map{ case (hakutoive, index) => HakutoiveFixture(index+1, hakutoive.tarjoajaOid, hakutoive.hakukohdeOid) })}
      .foreach(hakemusFixtures.importTemplateFixture(_))


    MongoMockData.clear(appConfig.sijoitteluContext.database)
    appConfig.sijoitteluContext.sijoitteluDao.persistSijoittelu(sijoittelu)
    hakukohteet.foreach(appConfig.sijoitteluContext.hakukohdeDao.persistHakukohde(_))
    valintatulokset.foreach(appConfig.sijoitteluContext.valintatulosDao.createOrUpdateValintatulos(_))
  }
}

case class HakemuksenTulosFixture(hakemusOid: String, hakutoiveet: List[HakemuksenHakukohdeFixture])
case class HakemuksenHakukohdeFixture(tarjoajaOid: String, hakukohdeOid: String, jonot: List[ValintatapaJonoFixture] = List(ValintatapaJonoFixture(HakemuksenTila.HYVAKSYTTY)))
case class ValintatapaJonoFixture(tulos: HakemuksenTila)
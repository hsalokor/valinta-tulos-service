package fi.vm.sade.valintatulosservice.generatedfixtures

import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData
import fi.vm.sade.valintatulosservice.Logging
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixture, HakemusFixtures, HakutoiveFixture}
import fi.vm.sade.valintatulosservice.ohjausparametrit.OhjausparametritFixtures
import fi.vm.sade.valintatulosservice.sijoittelu.SijoitteluFixtureCreator
import fi.vm.sade.valintatulosservice.tarjonta.HakuFixtures

import scala.collection.immutable.Iterable

class GeneratedFixture(haut: List[GeneratedHakuFixture] = List(new GeneratedHakuFixture("1"))) extends Logging {
  def this(haku: GeneratedHakuFixture) = this(List(haku))

  def hakuFixture: String = HakuFixtures.korkeakouluYhteishaku

  def ohjausparametritFixture = OhjausparametritFixtures.vastaanottoLoppuu2100

  def apply(implicit appConfig: AppConfig) {
    HakuFixtures.useFixture(hakuFixture, haut.map(_.hakuOid))

    val hakemusFixtures = HakemusFixtures()
    logger.info("Clearing...")
    hakemusFixtures.clear
    OhjausparametritFixtures.activeFixture = ohjausparametritFixture
    MongoMockData.clear(appConfig.sijoitteluContext.database)

    logger.info("Iterating...")

    haut.foreach { haku =>
      logger.info("Generating for Haku " + haku.hakuOid)
      logger.info("Valintatulos...")
      haku.valintatulokset.foreach(appConfig.sijoitteluContext.valintatulosDao.createOrUpdateValintatulos(_))
      logger.info("Sijoittelu...")
      appConfig.sijoitteluContext.sijoitteluDao.persistSijoittelu(haku.sijoittelu)
      logger.info("Hakukohde...")
      haku.hakukohteet.foreach(appConfig.sijoitteluContext.hakukohdeDao.persistHakukohde(_))
      logger.info("Hakemus-kantaan...")
      haku.hakemukset
        .map { hakemus => HakemusFixture(hakemus.hakemusOid, hakemus.hakutoiveet.zipWithIndex.map{ case (hakutoive, index) => HakutoiveFixture(index+1, hakutoive.tarjoajaOid, hakutoive.hakukohdeOid) })}
        .foreach(hakemusFixtures.importTemplateFixture(_))
    }
    logger.info("Done")
  }
}
class GeneratedHakuFixture(val hakuOid: String = "1") {
  val sijoitteluajoId: Long = hakuOid.toLong

  def hakemukset = List(HakemuksenTulosFixture("1", List(
    HakemuksenHakukohdeFixture("1", "1")
  )))

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

  def julkaistavissa(hakukohde: Hakukohde, hakemus: Hakemus) = {
    (for {
      hakemuksenTulos <- hakemukset
      if (hakemuksenTulos.hakemusOid == hakemus.getHakemusOid)
      hakutoive <- hakemuksenTulos.hakutoiveet
      if (hakutoive.hakukohdeOid == hakukohde.getOid)
    } yield (hakutoive.julkaistavissa)).headOption.getOrElse(true)
  }

  import scala.collection.JavaConversions._

  lazy val valintatulokset = for {
    hakukohde <- hakukohteet
    jono: Valintatapajono <- hakukohde.getValintatapajonot
    hakemus: Hakemus <- jono.getHakemukset
  } yield {
    SijoitteluFixtureCreator.newValintatulos(jono.getOid, hakuOid, hakemus.getHakemusOid, hakukohde.getOid, hakemus.getHakijaOid, hakemus.getPrioriteetti, julkaistavissa(hakukohde, hakemus))
  }

  lazy val sijoittelu: Sijoittelu = SijoitteluFixtureCreator.newSijoittelu(hakuOid, sijoitteluajoId, hakukohteet.map(_.getOid))
}

case class HakemuksenTulosFixture(hakemusOid: String, hakutoiveet: List[HakemuksenHakukohdeFixture])
case class HakemuksenHakukohdeFixture(tarjoajaOid: String, hakukohdeOid: String, jonot: List[ValintatapaJonoFixture] = List(ValintatapaJonoFixture(HakemuksenTila.HYVAKSYTTY)), julkaistavissa: Boolean = true)
case class ValintatapaJonoFixture(tulos: HakemuksenTila)
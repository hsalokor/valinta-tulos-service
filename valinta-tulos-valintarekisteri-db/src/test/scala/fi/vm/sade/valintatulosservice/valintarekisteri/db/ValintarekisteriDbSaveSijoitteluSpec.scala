package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.Timestamp

import fi.vm.sade.sijoittelu.domain.{Hakemus => SijoitteluHakemus, Hakijaryhma, Hakukohde, Pistetieto, SijoitteluAjo, Valintatapajono}
import fi.vm.sade.valintatulosservice.valintarekisteri.{ValintarekisteriTools, ITSetup}
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.json4s.native.JsonMethods
import org.json4s.native.JsonMethods._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeAfterExample
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult

@RunWith(classOf[JUnitRunner])
class ValintarekisteriDbSaveSijoitteluSpec extends Specification with ITSetup with BeforeAfterExample {
  sequential
  private val hakuOid = "1.2.246.561.29.00000000001"

  val now = System.currentTimeMillis
  val nowDatetime = new Timestamp(1)

  step(appConfig.start)
  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))

  "ValintarekisteriDb" should {
    "store sijoitteluajo" in {
      val sijoitteluajo = createSijoitteluajo()
      singleConnectionValintarekisteriDb.storeSijoitteluajo(sijoitteluajo)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(sijoitteluajo)
    }
    "store sijoitteluajo fixture" in {
      val wrapper = loadSijoitteluFromFixture("hyvaksytty-korkeakoulu-erillishaku")
      singleConnectionValintarekisteriDb.storeSijoitteluajo(wrapper.sijoitteluajo)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
    }
    "store sijoitteluajoWrapper fixture" in {
      val wrapper = loadSijoitteluFromFixture("hyvaksytty-korkeakoulu-erillishaku")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
      val storedHakukohteet: Seq[Hakukohde] = findSijoitteluajonHakukohteet(stored.get.getSijoitteluajoId)
      wrapper.hakukohteet.foreach(hakukohde => {
        val storedHakukohde = storedHakukohteet.find(_.getOid.equals(hakukohde.getOid))
        storedHakukohde.isDefined must beTrue
        SijoitteluajonHakukohdeWrapper(hakukohde) mustEqual SijoitteluajonHakukohdeWrapper(storedHakukohde.get)
        val storedValintatapajonot = findHakukohteenValintatapajonot(hakukohde.getOid)
        import scala.collection.JavaConverters._
        hakukohde.getValintatapajonot.asScala.toList.foreach(valintatapajono => {
          val storedValintatapajono = storedValintatapajonot.find(_.getOid.equals(valintatapajono.getOid))
          storedValintatapajono.isDefined must beTrue
          SijoitteluajonValintatapajonoWrapper(valintatapajono) mustEqual SijoitteluajonValintatapajonoWrapper(storedValintatapajono.get)
          val storedJonosijat = findValintatapajononJonosijat(valintatapajono.getOid)
          valintatapajono.getHakemukset.asScala.toList.foreach(hakemus => {
            val storedJonosija = storedJonosijat.find(_.getHakemusOid.equals(hakemus.getHakemusOid))
            storedJonosija.isDefined must beTrue
            SijoitteluajonHakemusWrapper(hakemus) mustEqual SijoitteluajonHakemusWrapper(storedJonosija.get)
          })
        })
        storedValintatapajonot.length mustEqual hakukohde.getValintatapajonot.size
      })
      storedHakukohteet.length mustEqual wrapper.hakukohteet.length
    }
    "store sijoitteluajoWrapper fixture with hakijaryhmä and pistetiedot" in {
      val wrapper = loadSijoitteluFromFixture("valintatapajono_hakijaryhma_pistetiedot")
      singleConnectionValintarekisteriDb.storeSijoittelu(wrapper)
      val stored: Option[SijoitteluAjo] = findSijoitteluajo(wrapper.sijoitteluajo.getSijoitteluajoId)
      stored.isDefined must beTrue
      SijoitteluajoWrapper(stored.get) mustEqual SijoitteluajoWrapper(wrapper.sijoitteluajo)
      val storedHakukohteet: Seq[Hakukohde] = findSijoitteluajonHakukohteet(stored.get.getSijoitteluajoId)
      wrapper.hakukohteet.foreach(hakukohde => {
        val storedHakukohde = storedHakukohteet.find(_.getOid.equals(hakukohde.getOid))
        storedHakukohde.isDefined must beTrue
        SijoitteluajonHakukohdeWrapper(hakukohde) mustEqual SijoitteluajonHakukohdeWrapper(storedHakukohde.get)
        val storedValintatapajonot = findHakukohteenValintatapajonot(hakukohde.getOid)
        import scala.collection.JavaConverters._
        hakukohde.getValintatapajonot.asScala.toList.foreach(valintatapajono => {
          val storedValintatapajono = storedValintatapajonot.find(_.getOid.equals(valintatapajono.getOid))
          storedValintatapajono.isDefined must beTrue
          SijoitteluajonValintatapajonoWrapper(valintatapajono) mustEqual SijoitteluajonValintatapajonoWrapper(storedValintatapajono.get)
          val storedJonosijat = findValintatapajononJonosijat(valintatapajono.getOid)
          valintatapajono.getHakemukset.asScala.toList.foreach(hakemus => {
            val storedJonosija = storedJonosijat.find(_.getHakemusOid.equals(hakemus.getHakemusOid))
            storedJonosija.isDefined must beTrue
            SijoitteluajonHakemusWrapper(hakemus) mustEqual SijoitteluajonHakemusWrapper(storedJonosija.get)

            val storedPistetiedot = findHakemuksenPistetiedot(hakemus.getHakemusOid)
            hakemus.getPistetiedot.size mustEqual storedPistetiedot.size
            hakemus.getPistetiedot.asScala.foreach(pistetieto => {
              val storedPistetieto = storedPistetiedot.find(_.getTunniste.equals(pistetieto.getTunniste))
              storedPistetieto.isDefined must beTrue
              SijoitteluajonPistetietoWrapper(pistetieto) mustEqual SijoitteluajonPistetietoWrapper(storedPistetieto.get)
            })
          })
        })
        val storedHakijaryhmat = findHakukohteenHakijaryhmat(hakukohde.getOid)
        storedHakijaryhmat.length mustEqual hakukohde.getHakijaryhmat.size
        hakukohde.getHakijaryhmat.asScala.toList.foreach(hakijaryhma => {
          val storedHakijaryhma = storedHakijaryhmat.find(_.getOid.equals(hakijaryhma.getOid))
          storedHakijaryhma.isDefined must beTrue
          storedHakijaryhma.get.getHakemusOid.addAll(findHakijaryhmanHakemukset(hakijaryhma.getOid).asJava)
          SijoitteluajonHakijaryhmaWrapper(hakijaryhma) mustEqual SijoitteluajonHakijaryhmaWrapper(storedHakijaryhma.get)
        })
        storedValintatapajonot.length mustEqual hakukohde.getValintatapajonot.size
      })
      storedHakukohteet.length mustEqual wrapper.hakukohteet.length
    }
  }

  def loadSijoitteluFromFixture(fixture: String, path: String = "sijoittelu/"):SijoitteluWrapper = {
    val json = parse(getClass.getClassLoader.getResourceAsStream("fixtures/" + path + fixture + ".json"))
    ValintarekisteriTools.sijoitteluWrapperFromJson(json, singleConnectionValintarekisteriDb)
  }

  def createSijoitteluajo(): SijoitteluAjo = {
    SijoitteluajoWrapper(now, hakuOid, now-1000, now).sijoitteluajo
  }

  private implicit val getSijoitteluajoResult = GetResult(r => {
    SijoitteluajoWrapper(r.nextLong, r.nextString, r.nextTimestamp.getTime, r.nextTimestamp.getTime).sijoitteluajo
  })

  def findSijoitteluajo(sijoitteluajoId:Long): Option[SijoitteluAjo] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select id, haku_oid, "start", "end"
            from sijoitteluajot
            where id = ${sijoitteluajoId}""".as[SijoitteluAjo]).headOption
  }

  private implicit val getSijoitteluajonHakukohdeResult = GetResult(r => {
    SijoitteluajonHakukohdeWrapper(r.nextLong, r.nextString, r.nextString, r.nextBoolean).hakukohde
  })

  def findSijoitteluajonHakukohteet(sijoitteluajoId:Long): Seq[Hakukohde] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select sijoitteluajo_id, hakukohde_oid as oid, tarjoaja_oid, kaikki_jonot_sijoiteltu
            from sijoitteluajon_hakukohteet
            where sijoitteluajo_id = ${sijoitteluajoId}""".as[Hakukohde])
  }

  private implicit val getSijoitteluajonValintatapajonoResult = GetResult(r => {
    SijoitteluajonValintatapajonoWrapper(r.nextString, r.nextString, r.nextInt, Tasasijasaanto(r.nextString()), r.nextInt, r.nextIntOption, r.nextBoolean,
      r.nextBoolean, r.nextBoolean, r.nextInt, r.nextInt, r.nextDateOption, r.nextDateOption, r.nextStringOption(),
      r.nextIntOption, r.nextIntOption, r.nextBigDecimalOption, None).valintatapajono
  })

  def findHakukohteenValintatapajonot(hakukohdeOid:String): Seq[Valintatapajono] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select oid, nimi, prioriteetti, tasasijasaanto, aloituspaikat, alkuperaiset_aloituspaikat, ei_varasijatayttoa,
            kaikki_ehdon_tayttavat_hyvaksytaan, poissaoleva_taytto,
            varasijat, varasijatayttopaivat, varasijoja_kaytetaan_alkaen, varasijoja_taytetaan_asti, tayttojono,
            hyvaksytty, varalla, alin_hyvaksytty_pistemaara
            from valintatapajonot
            inner join sijoitteluajon_hakukohteet sh on sh.id = valintatapajonot.sijoitteluajon_hakukohde_id
            and sh.hakukohde_oid = ${hakukohdeOid}""".as[Valintatapajono])
  }

  private implicit val getSijoitteluajonHakijaryhmaResult = GetResult(r => {
    SijoitteluajonHakijaryhmaWrapper(r.nextString, r.nextString,
      r.nextIntOption(), r.nextIntOption, r.nextIntOption, r.nextBooleanOption,
      r.nextBooleanOption, r.nextBooleanOption, r.nextBigDecimalOption, List()).hakijaryhma
  })

  def findHakukohteenHakijaryhmat(hakukohdeOid:String): Seq[Hakijaryhma] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select h.oid, h.nimi, h.prioriteetti, h.paikat, h.kiintio, h.kayta_kaikki,
            h.tarkka_kiintio, h.kaytetaan_ryhmaan_kuuluvia, h.alin_hyvaksytty_pistemaara
            from hakijaryhmat h
            inner join sijoitteluajon_hakukohteet sh on sh.id = h.sijoitteluajon_hakukohde_id
            where sh.hakukohde_oid = ${hakukohdeOid}""".as[Hakijaryhma]
    )
  }

  def findHakijaryhmanHakemukset(hakijaryhmaOid:String): Seq[String] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select hh.hakemus_oid from hakijaryhman_hakemukset hh
            inner join hakijaryhmat h ON hh.hakijaryhma_id = h.id
            where h.oid = ${hakijaryhmaOid}""".as[String]
    )
  }

  private implicit val getSijoitteluajonJonosijaResult = GetResult(r => {
    SijoitteluajonHakemusWrapper(r.nextString, r.nextString, r.nextString, r.nextString, r.nextInt, r.nextInt,
      r.nextIntOption, r.nextBooleanOption, r.nextBigDecimalOption, r.nextIntOption, r.nextBooleanOption,
      r.nextBooleanOption, r.nextBooleanOption, Valinnantila(r.nextString), r.nextStringOption().map(ValinnantilanTarkenne(_))).hakemus
  })

  def findValintatapajononJonosijat(valintatapajonoOid:String): Seq[SijoitteluHakemus] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select j.hakemus_oid, j.hakija_oid, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija, j.varasijan_numero, j.onko_muuttunut_viime_sijoittelussa,
            j.pisteet, j.tasasijajonosija, j.hyvaksytty_harkinnanvaraisesti, j.hyvaksytty_hakijaryhmasta, j.siirtynyt_toisesta_valintatapajonosta,
            v.tila, v.tarkenne
            from jonosijat j
            left join valinnantulokset v on j.valintatapajono_oid = v.valintatapajono_oid and j.hakemus_oid = v.hakemus_oid
            where j.valintatapajono_oid = ${valintatapajonoOid}
         """.as[SijoitteluHakemus])
  }

  private implicit val getSijoitteluajonPistetietoResult = GetResult(r => {
    SijoitteluajonPistetietoWrapper(r.nextString, r.nextStringOption, r.nextStringOption, r.nextStringOption).pistetieto
  })

  def findHakemuksenPistetiedot(hakemusOid:String): Seq[Pistetieto] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select p.tunniste, p.arvo, p.laskennallinen_arvo, p.osallistuminen
            from pistetiedot p
            inner join jonosijat j on j.id = p.jonosija_id
            where j.hakemus_oid = ${hakemusOid}""".as[Pistetieto])
  }

  override protected def before: Unit = {
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }
  override protected def after: Unit = {
    ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb)
  }

  step(ValintarekisteriTools.deleteAll(singleConnectionValintarekisteriDb))
}

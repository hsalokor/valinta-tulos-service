package fi.vm.sade.valintatulosservice.local

import fi.vm.sade.valintatulosservice._
import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.sijoittelu.SijoittelutulosService
import fi.vm.sade.valintatulosservice.tarjonta.{HakuFixtures, HakuService}
import fi.vm.sade.valintatulosservice.valintarekisteri.{HakukohdeRecordService, ValintarekisteriDb}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.specs2.execute.{FailureException, Result}
import org.specs2.matcher.ThrownMessages
import org.specs2.runner.JUnitRunner

import scala.util.{Failure, Success, Try}

@RunWith(classOf[JUnitRunner])
class VastaanottoServiceVirkailijanaSpec extends ITSpecification with TimeWarp with ThrownMessages {
  val hakuOid: String = "1.2.246.562.5.2013080813081926341928"
  val hakukohdeOid: String = "1.2.246.562.5.16303028779"
  val vastaanotettavissaHakuKohdeOid = "1.2.246.562.5.72607738902"
  val hakemusOid: String = "1.2.246.562.11.00000441369"
  val muokkaaja: String = "Teppo Testi"
  val personOid: String = "1.2.246.562.24.14229104472"
  val valintatapajonoOid: String = "2013080813081926341928"
  val selite: String = "Testimuokkaus"
  val ilmoittautumisaikaPaattyy2100: Ilmoittautumisaika = Ilmoittautumisaika(None, Some(new DateTime(2100, 1, 10, 23, 59, 59, 999)))

  "vastaanotaVirkailijana" in {
    "vastaanota sitovasti yksi hakija" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
    }
    "vastaanota ehdollisesti yksi hakija" in {
      useFixture("hyvaksytty-ylempi-varalla.json", Nil, hakuFixture = "korkeakoulu-yhteishaku", yhdenPaikanSaantoVoimassa = true, kktutkintoonJohtava = true)
      hakemuksenTulos.hakutoiveet(0).valintatila must_== Valintatila.varalla
      hakemuksenTulos.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      val alemmanHakutoiveenHakukohdeOid = hakemuksenTulos.hakutoiveet(1).hakukohdeOid
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, alemmanHakutoiveenHakukohdeOid, hakuOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      hakemuksenTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaanottanut
    }
    "ehdollinen vastaanotto on mahdollinen vain ylimmälle hyväksytylle hakutoiveelle ja siten että on vielä ylempi hakutoive varalla" in {
      useFixture("varalla-hyvaksytty-hyvaksytty.json", Nil, hakuFixture = "korkeakoulu-yhteishaku", hakemusFixtures = List("00000441369-3"),
        yhdenPaikanSaantoVoimassa = true, kktutkintoonJohtava = true)
      hakemuksenTulos.hakutoiveet.foreach(h => println(h))

      hakemuksenTulos.hakutoiveet(0).valintatila must_== Valintatila.varalla
      hakemuksenTulos.hakutoiveet(1).valintatila must_== Valintatila.hyväksytty
      hakemuksenTulos.hakutoiveet(2).valintatila must_== Valintatila.hyväksytty

      val ylinHakukohdeOid = hakemuksenTulos.hakutoiveet(0).hakukohdeOid
      val keskimmainenHakukohdeOid = hakemuksenTulos.hakutoiveet(1).hakukohdeOid
      val alinHakukohdeOid = hakemuksenTulos.hakutoiveet(2).hakukohdeOid

      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, keskimmainenHakukohdeOid, hakuOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      hakemuksenTulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaanottanut
      hakemuksenTulos.hakutoiveet(2).vastaanottotila must_== Vastaanottotila.kesken
      // TODO assert other cases and implement them
    }
    "peru yksi hakija" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.perunut, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
    }
    "peruuta yhden hakijan vastaanotto" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.peruutettu, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.peruutettu
    }
    "hakija ei voi vastaanottaa peruutettua hakutoivetta" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.peruutettu, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.peruutettu
      expectFailure {
        vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      }
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.peruutettu
    }
    "vastaanota yksi hakija joka ottanut vastaan toisen kk paikan -> error" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanottanut.json"), hakuFixture = HakuFixtures.korkeakouluYhteishaku, yhdenPaikanSaantoVoimassa = true, kktutkintoonJohtava = true)
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja).head
      r.result.status must_== 403
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ottanut_vastaan_toisen_paikan
    }
    "peru yksi hakija jonka paikka ei vastaanotettavissa -> success" in {
      useFixture("hylatty-ei-valintatulosta.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.perunut, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
    }
    "poista yhden hakijan vastaanotto" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      val r = vastaanotaVirkailijana(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.kesken, muokkaaja).head
      r.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
    }
    "vastaanota sitovasti yksi hakija vaikka toinen vastaanotto ei onnistu" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      val r = vastaanotaVirkailijana(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite"),
        VastaanottoEventDto(valintatapajonoOid, "1234", "1234", "1234", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite")
      ))
      r.size must_== 2
      r.head.result.message must_== Some("Hakemusta ei löydy")
      r.head.result.status must_== 400
      r.tail.head.result.status must_== 200
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
    }
  }
  "vastaanotaVirkailijanaInTransaction" in {
    "älä vastaanota sitovasti hakijaa, kun toinen vastaanotto ei onnistu" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite"),
        VastaanottoEventDto(valintatapajonoOid, "1234", "1234", "1234", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite")
      )).isFailure must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
    }
    "vastaanota sitovasti yksi hakija" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite")
      )).isSuccess must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
    }
    "vastaanota ehdollisesti yksi hakija" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja, "testiselite")
      )).isSuccess must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaanottanut
    }
    "peru yksi hakija" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.perunut, muokkaaja, "testiselite")
      )).isSuccess must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
    }
    "peruuta yhden hakijan vastaanotto" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.peruutettu, muokkaaja, "testiselite")
      )).isSuccess must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.peruutettu
    }
    "hakija ei voi vastaanottaa peruutettua hakutoivetta" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.peruutettu, muokkaaja, "testiselite")
      )).isSuccess must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.peruutettu
      expectFailure {
        vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      }
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.peruutettu
    }
    "vastaanota yksi hakija joka ottanut vastaan toisen kk paikan -> error" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", List("lisahaku-vastaanottanut.json"), hakuFixture = HakuFixtures.korkeakouluYhteishaku, yhdenPaikanSaantoVoimassa = true, kktutkintoonJohtava = true)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite")
      )).isFailure must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.ottanut_vastaan_toisen_paikan
    }
    "peru yksi hakija jonka paikka ei vastaanotettavissa -> success" in {
      useFixture("hylatty-ei-valintatulosta.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.perunut, muokkaaja, "testiselite")
      )).isSuccess must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.perunut
    }
    "poista yhden hakijan vastaanotto" in {
      useFixture("hyvaksytty-kesken-julkaistavissa.json", hakuFixture = HakuFixtures.korkeakouluYhteishaku)
      vastaanota(hakuOid, hakemusOid, "1.2.246.562.5.72607738902", Vastaanottotila.vastaanottanut, muokkaaja, selite, personOid)
      vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, "1.2.246.562.5.72607738902", hakuOid, Vastaanottotila.kesken, muokkaaja, "testiselite")
      )).isSuccess must beTrue
      hakemuksenTulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
    }
    "siirrä ehdollinen vastaanotto ylemmälle hakutoiveelle" in {
      val alinHyvaksyttyHakutoiveOid = "1.2.246.562.5.16303028779"
      val ylempiHakutoiveOid = "1.2.246.562.5.72607738902"

      useFixture("hyvaksytty-ylempi-varalla.json", Nil, hakuFixture = "korkeakoulu-yhteishaku", yhdenPaikanSaantoVoimassa = true, kktutkintoonJohtava = true)
      vastaanota(hakuOid, hakemusOid, alinHyvaksyttyHakutoiveOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja, selite, personOid)

      val vastaanotonTulos = vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, alinHyvaksyttyHakutoiveOid, hakuOid, Vastaanottotila.kesken, muokkaaja, "testiselite"),
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, ylempiHakutoiveOid, hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite")
      ))
      val hakemuksentulos = hakemuksenTulos(hakuOid, hakemusOid)
      vastaanotonTulos must_== Success()
      hakemuksentulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.vastaanottanut
      hakemuksentulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.kesken
      hakemuksentulos.hakutoiveet(1).valintatila must_== domain.Valintatila.peruuntunut
    }
    "estä useampi vastaanotto kun yhden paikan sääntö on voimassa" in {
      val alinHyvaksyttyHakutoiveOid = "1.2.246.562.5.16303028779"
      val ylempiHakutoiveOid = "1.2.246.562.5.72607738902"

      useFixture("hyvaksytty-ylempi-varalla.json", Nil, hakuFixture = "korkeakoulu-yhteishaku", yhdenPaikanSaantoVoimassa = true, kktutkintoonJohtava = true)
      vastaanota(hakuOid, hakemusOid, alinHyvaksyttyHakutoiveOid, Vastaanottotila.ehdollisesti_vastaanottanut, muokkaaja, selite, personOid)

      val vastaanotonTulos = vastaanotaVirkailijanaTransaktiossa(List(
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, alinHyvaksyttyHakutoiveOid, hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite"),
        VastaanottoEventDto(valintatapajonoOid, personOid, hakemusOid, ylempiHakutoiveOid, hakuOid, Vastaanottotila.vastaanottanut, muokkaaja, "testiselite")
      ))
      val hakemuksentulos = hakemuksenTulos(hakuOid, hakemusOid)
      vastaanotonTulos match {
        case Failure(cae: ConflictingAcceptancesException) => cae.conflictingVastaanottos.map(_.hakukohdeOid) must_== Vector(ylempiHakutoiveOid, alinHyvaksyttyHakutoiveOid)
        case x => fail(s"Should have failed on several conflicting records but got $x")
      }

      hakemuksentulos.hakutoiveet(0).vastaanottotila must_== Vastaanottotila.kesken
      hakemuksentulos.hakutoiveet(1).vastaanottotila must_== Vastaanottotila.ehdollisesti_vastaanottanut
    }
  }

  step(valintarekisteriDb.db.shutdown)

  lazy val hakuService = HakuService(null, appConfig)
  lazy val valintarekisteriDb = new ValintarekisteriDb(appConfig.settings.valintaRekisteriDbConfig)
  lazy val hakukohdeRecordService = new HakukohdeRecordService(hakuService, valintarekisteriDb, true)
  lazy val sijoittelutulosService = new SijoittelutulosService(appConfig.sijoitteluContext.raportointiService, appConfig.ohjausparametritService, valintarekisteriDb)
  lazy val vastaanotettavuusService = new VastaanotettavuusService(hakukohdeRecordService, valintarekisteriDb)
  lazy val valintatulosService = new ValintatulosService(vastaanotettavuusService, sijoittelutulosService, valintarekisteriDb, hakuService, hakukohdeRecordService)(appConfig)
  lazy val vastaanottoService = new VastaanottoService(hakuService, hakukohdeRecordService, vastaanotettavuusService, valintatulosService,
    valintarekisteriDb, valintarekisteriDb, appConfig.sijoitteluContext.valintatulosRepository)
  lazy val ilmoittautumisService = new IlmoittautumisService(valintatulosService,
    appConfig.sijoitteluContext.valintatulosRepository, valintarekisteriDb)

  private def hakemuksenTulos: Hakemuksentulos = hakemuksenTulos(hakuOid, hakemusOid)
  private def hakemuksenTulos(hakuOid: String, hakemusOid: String) = valintatulosService.hakemuksentulos(hakuOid, hakemusOid).get

  private def vastaanota(hakuOid: String, hakemusOid: String, hakukohdeOid: String, tila: Vastaanottotila, muokkaaja: String, selite: String, personOid: String) = {
    vastaanottoService.vastaanotaHakijana(HakijanVastaanotto(personOid, hakemusOid, hakukohdeOid, HakijanVastaanottoAction.getHakijanVastaanottoAction(tila))).get
    success
  }

  private def vastaanotaVirkailijana(valintatapajonoOid: String, henkiloOid: String, hakemusOid: String, hakukohdeOid: String, hakuOid: String, tila: Vastaanottotila, ilmoittaja: String) = {
    vastaanottoService.vastaanotaVirkailijana(List(VastaanottoEventDto(valintatapajonoOid, henkiloOid, hakemusOid, hakukohdeOid, hakuOid, tila, ilmoittaja, "testiselite")))
  }

  private def vastaanotaVirkailijana(vastaanotot: List[VastaanottoEventDto]) = {
    vastaanottoService.vastaanotaVirkailijana(vastaanotot)
  }

  private def vastaanotaVirkailijanaTransaktiossa(vastaanotot: List[VastaanottoEventDto]): Try[Unit] = {
    vastaanottoService.vastaanotaVirkailijanaInTransaction(vastaanotot)
  }

  private def expectFailure[T](block: => T): Result = expectFailure[T](None)(block)

  private def expectFailure[T](assertErrorMsg: Option[String])(block: => T): Result = {
    try {
      block
      failure("Expected exception")
    } catch {
      case fe: FailureException => throw fe
      case e: Exception => assertErrorMsg match {
        case Some(msg) => e.getMessage must_== msg
        case None => success
      }
    }
  }
}

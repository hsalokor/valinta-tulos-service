package fi.vm.sade.valintatulosservice.valintarekisteri

import java.text.SimpleDateFormat
import java.util.{Comparator, Date}

import fi.vm.sade.sijoittelu.domain._
import fi.vm.sade.sijoittelu.tulos.dto.SijoitteluajoDTO
import fi.vm.sade.valintatulosservice.valintarekisteri.db.ValintarekisteriDb
import fi.vm.sade.valintatulosservice.valintarekisteri.domain.Tasasijasaanto
import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods._
import org.json4s.{CustomSerializer, DefaultFormats}
import org.specs2.mutable.Specification
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._
import slick.jdbc.GetResult

import scala.collection.JavaConverters._

trait ValintarekisteriDbTools extends Specification {

  val singleConnectionValintarekisteriDb:ValintarekisteriDb

  class NumberLongSerializer extends CustomSerializer[Long](format => ( {
    case JObject(List(JField("$numberLong", JString(longValue)))) => longValue.toLong
  }, {
    case x: Long => JObject(List(JField("$numberLong", JString("" + x))))
  }))

  class DateSerializer extends  CustomSerializer[Date](format => ({
    case JObject(List(JField("$date", JString(dateValue)))) if (dateValue.endsWith("Z")) =>
      new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(dateValue)
    case JObject(List(JField("$date", JString(dateValue)))) =>
      new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(dateValue)
  }, {
    case x: Date => JObject(List(JField("$date", JString("" + x))))
  }))

  class TasasijasaantoSerializer extends CustomSerializer[Tasasijasaanto](format => ( {
    case JString(tasasijaValue) => Tasasijasaanto.getTasasijasaanto(fi.vm.sade.sijoittelu.domain.Tasasijasaanto.valueOf(tasasijaValue))
  }, {
    case x: Tasasijasaanto => JString(x.tasasijasaanto.toString)
  }))

  class ValinnantilaSerializer extends CustomSerializer[Valinnantila](format => ( {
    case JString(tilaValue) => Valinnantila.getValinnantila(fi.vm.sade.sijoittelu.domain.HakemuksenTila.valueOf(tilaValue))
  }, {
    case x: Valinnantila => JString(x.valinnantila.toString)
  }))

  implicit val formats = DefaultFormats ++ List(new NumberLongSerializer, new TasasijasaantoSerializer, new ValinnantilaSerializer, new DateSerializer)

  def hakijaryhmaOidsToSet(hakijaryhmaOids:Option[String]): Set[String] = {
    hakijaryhmaOids match {
      case Some(oids) if !oids.isEmpty => oids.split(",").toSet
      case _ => Set()
    }
  }

  private val deleteFromVastaanotot = DBIO.seq(
    sqlu"delete from vastaanotot",
    sqlu"delete from deleted_vastaanotot where id <> overriden_vastaanotto_deleted_id()",
    sqlu"delete from henkiloviitteet")

  def deleteAll(): Unit = {
    singleConnectionValintarekisteriDb.runBlocking(DBIO.seq(
      deleteFromVastaanotot,
      sqlu"delete from hakijaryhman_hakemukset",
      sqlu"delete from hakijaryhmat",
      sqlu"delete from ilmoittautumiset",
      sqlu"delete from pistetiedot",
      sqlu"delete from valinnantulokset",
      sqlu"delete from jonosijat",
      sqlu"delete from valintatapajonot",
      sqlu"delete from sijoitteluajon_hakukohteet",
      sqlu"delete from hakukohteet",
      sqlu"delete from sijoitteluajot",
      sqlu"delete from vanhat_vastaanotot"
      ).transactionally)
  }

  def dateStringToTimestamp(str:String): Date = {
    new java.sql.Timestamp(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(str).getTime)
  }

  def deleteVastaanotot(): Unit = {
    singleConnectionValintarekisteriDb.runBlocking(deleteFromVastaanotot)
  }

  def compareSijoitteluWrapperToDTO(wrapper:SijoitteluWrapper, dto:SijoitteluajoDTO) = {
    val simpleFormat = new SimpleDateFormat("dd-MM-yyyy")

    def format(date:java.util.Date) = date match {
      case null => null
      case x:java.util.Date => simpleFormat.format(x)
    }

    dto.getSijoitteluajoId mustEqual wrapper.sijoitteluajo.getSijoitteluajoId
    dto.getHakuOid mustEqual wrapper.sijoitteluajo.getHakuOid
    dto.getStartMils mustEqual wrapper.sijoitteluajo.getStartMils
    dto.getEndMils mustEqual wrapper.sijoitteluajo.getEndMils

    dto.getHakukohteet.size mustEqual wrapper.hakukohteet.size
    dto.getHakukohteet.asScala.toList.foreach(dhakukohde => {
      val whakukohde = wrapper.hakukohteet.find(_.getOid.equals(dhakukohde.getOid)).head
      dhakukohde.getSijoitteluajoId mustEqual wrapper.sijoitteluajo.getSijoitteluajoId
      dhakukohde.getEnsikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet mustEqual null
      dhakukohde.getTarjoajaOid mustEqual whakukohde.getTarjoajaOid
      dhakukohde.isKaikkiJonotSijoiteltu mustEqual whakukohde.isKaikkiJonotSijoiteltu

      dhakukohde.getHakijaryhmat.size mustEqual whakukohde.getHakijaryhmat.size
      dhakukohde.getHakijaryhmat.asScala.toList.foreach(dhakijaryhma => {
        val whakijaryhma = whakukohde.getHakijaryhmat.asScala.toList.find(_.getOid.equals(dhakijaryhma.getOid)).head
        dhakijaryhma.getPrioriteetti mustEqual whakijaryhma.getPrioriteetti
        dhakijaryhma.getPaikat mustEqual whakijaryhma.getPaikat
        dhakijaryhma.getNimi mustEqual whakijaryhma.getNimi
        dhakijaryhma.getHakukohdeOid mustEqual whakukohde.getOid
        dhakijaryhma.getKiintio mustEqual whakijaryhma.getKiintio
        dhakijaryhma.isKaytaKaikki mustEqual whakijaryhma.isKaytaKaikki
        dhakijaryhma.isTarkkaKiintio mustEqual whakijaryhma.isTarkkaKiintio
        dhakijaryhma.isKaytetaanRyhmaanKuuluvia mustEqual whakijaryhma.isKaytetaanRyhmaanKuuluvia
        dhakijaryhma.getHakemusOid.asScala.toList.diff(whakijaryhma.getHakemusOid.asScala.toList) mustEqual List()
      })

      dhakukohde.getValintatapajonot.size mustEqual whakukohde.getValintatapajonot.size
      dhakukohde.getValintatapajonot.asScala.toList.foreach(dvalintatapajono => {
        val wvalintatapajono = whakukohde.getValintatapajonot.asScala.toList.find(_.getOid.equals(dvalintatapajono.getOid)).head
        dvalintatapajono.getAlinHyvaksyttyPistemaara mustEqual wvalintatapajono.getAlinHyvaksyttyPistemaara
        dvalintatapajono.getAlkuperaisetAloituspaikat mustEqual wvalintatapajono.getAlkuperaisetAloituspaikat
        dvalintatapajono.getAloituspaikat mustEqual wvalintatapajono.getAloituspaikat
        dvalintatapajono.getEiVarasijatayttoa mustEqual wvalintatapajono.getEiVarasijatayttoa
        dvalintatapajono.getHakeneet mustEqual wvalintatapajono.getHyvaksytty + wvalintatapajono.getVaralla
        dvalintatapajono.getKaikkiEhdonTayttavatHyvaksytaan mustEqual wvalintatapajono.getKaikkiEhdonTayttavatHyvaksytaan
        dvalintatapajono.getNimi mustEqual wvalintatapajono.getNimi
        dvalintatapajono.getPoissaOlevaTaytto mustEqual wvalintatapajono.getPoissaOlevaTaytto
        dvalintatapajono.getPrioriteetti mustEqual wvalintatapajono.getPrioriteetti
        dvalintatapajono.getTasasijasaanto.toString mustEqual wvalintatapajono.getTasasijasaanto.toString
        dvalintatapajono.getTayttojono mustEqual wvalintatapajono.getTayttojono
        wvalintatapajono.getValintaesitysHyvaksytty match { //TODO is this correct?
          case null => dvalintatapajono.getValintaesitysHyvaksytty mustEqual false
          case x => dvalintatapajono.getValintaesitysHyvaksytty mustEqual x
        }
        dvalintatapajono.getVaralla mustEqual wvalintatapajono.getVaralla
        dvalintatapajono.getVarasijat mustEqual wvalintatapajono.getVarasijat
        dvalintatapajono.getVarasijaTayttoPaivat mustEqual wvalintatapajono.getVarasijaTayttoPaivat
        format(dvalintatapajono.getVarasijojaKaytetaanAlkaen) mustEqual format(wvalintatapajono.getVarasijojaKaytetaanAlkaen)
        format(dvalintatapajono.getVarasijojaTaytetaanAsti) mustEqual format(wvalintatapajono.getVarasijojaTaytetaanAsti)

        dvalintatapajono.getHakemukset.size mustEqual wvalintatapajono.getHakemukset.size
        dvalintatapajono.getHakemukset.asScala.toList.foreach(dhakemus => {
          val whakemus = wvalintatapajono.getHakemukset.asScala.toList.find(_.getHakemusOid.equals(dhakemus.getHakemusOid)).head
          dhakemus.getHakijaOid mustEqual whakemus.getHakijaOid
          dhakemus.getHakemusOid mustEqual whakemus.getHakemusOid
          dhakemus.getPisteet mustEqual whakemus.getPisteet
          // TODO: ei datassa? dhakemus.getPaasyJaSoveltuvuusKokeenTulos mustEqual whakemus.getPaasyJaSoveltuvuusKokeenTulos
          dhakemus.getEtunimi mustEqual whakemus.getEtunimi
          dhakemus.getSukunimi mustEqual whakemus.getSukunimi
          dhakemus.getPrioriteetti mustEqual whakemus.getPrioriteetti
          dhakemus.getJonosija mustEqual whakemus.getJonosija
          dhakemus.getTasasijaJonosija mustEqual whakemus.getTasasijaJonosija
          dhakemus.getTila.toString mustEqual whakemus.getTila.toString
          dhakemus.getTilanKuvaukset mustEqual whakemus.getTilanKuvaukset
          // TODO tallennetaanko historia vanhoista? dhakemus.getTilahistoria mustEqual whakemus.getTilahistoria
          dhakemus.isHyvaksyttyHarkinnanvaraisesti mustEqual whakemus.isHyvaksyttyHarkinnanvaraisesti
          // TODO tätä ei taida tulla datassa? dhakemus.getVarasijanNumero mustEqual whakemus.getVarasijanNumero
          dhakemus.getValintatapajonoOid mustEqual wvalintatapajono.getOid
          dhakemus.isOnkoMuuttunutViimeSijoittelussa mustEqual whakemus.isOnkoMuuttunutViimeSijoittelussa
          dhakemus.getHyvaksyttyHakijaryhmista mustEqual whakemus.getHyvaksyttyHakijaryhmista
          dhakemus.getSiirtynytToisestaValintatapajonosta mustEqual whakemus.getSiirtynytToisestaValintatapajonosta
          //TODO: ?? dhakemus.getTodellinenJonosija mustEqual whakemus.getJonosija
        })
      })
    })
    true must beTrue
  }



  def sijoitteluWrapperFromJson(json: JValue, tallennaHakukohteet: Boolean = true): SijoitteluWrapper = {
    val JArray(sijoittelut) = (json \ "Sijoittelu")
    val JArray(sijoitteluajot) = (sijoittelut(0) \ "sijoitteluajot")
    val sijoitteluajo: SijoitteluAjo = sijoitteluajot(0).extract[SijoitteluajoWrapper].sijoitteluajo

    val JArray(jsonHakukohteet) = (json \ "Hakukohde")
    val hakukohteet: List[Hakukohde] = jsonHakukohteet.map(hakukohdeJson => {
      val hakukohde = hakukohdeJson.extract[SijoitteluajonHakukohdeWrapper].hakukohde
      hakukohde.setValintatapajonot({
        val JArray(valintatapajonot) = (hakukohdeJson \ "valintatapajonot")
        valintatapajonot.map(valintatapajono => {
          val valintatapajonoExt = valintatapajono.extract[SijoitteluajonValintatapajonoWrapper].valintatapajono
          val JArray(hakemukset) = (valintatapajono \ "hakemukset")
          valintatapajonoExt.setHakemukset(hakemukset.map(hakemus => {
            val hakemusExt = hakemus.extract[SijoitteluajonHakemusWrapper].hakemus
            (hakemus \ "pistetiedot") match {
              case JArray(pistetiedot) => hakemusExt.setPistetiedot(pistetiedot.map(pistetieto => pistetieto.extract[SijoitteluajonPistetietoWrapper].pistetieto).asJava)
              case _ =>
            }
            hakemusExt
          }).asJava)
          valintatapajonoExt
        }).asJava
      })
      (hakukohdeJson \ "hakijaryhmat") match {
        case JArray(hakijaryhmat) => hakukohde.setHakijaryhmat(hakijaryhmat.map(hakijaryhma => hakijaryhma.extract[SijoitteluajonHakijaryhmaWrapper].hakijaryhma).asJava)
        case _ =>
      }
      hakukohde
    })

    val JArray(jsonValintatulokset) = (json \ "Valintatulos")
    val valintatulokset: List[Valintatulos] = jsonValintatulokset.map(valintaTulos => {
      val tulos = valintaTulos.extract[SijoitteluajonValinnantulosWrapper].valintatulos
      (valintaTulos \ "logEntries") match {
        case JArray(entries) => tulos.setOriginalLogEntries(entries.map(e => e.extract[LogEntryWrapper].entry).asJava)
        case _ =>
      }
      tulos.setMailStatus((valintaTulos \ "mailStatus").extract[MailStatusWrapper].status)
      tulos
    })

    val wrapper: SijoitteluWrapper = SijoitteluWrapper(sijoitteluajo, hakukohteet.filter(h => {
      h.getSijoitteluajoId.equals(sijoitteluajo.getSijoitteluajoId)
    }), valintatulokset)
    if(tallennaHakukohteet) {
      hakukohteet.foreach(h => insertHakukohde(h.getOid, sijoitteluajo.getHakuOid))
    }
    wrapper
  }

  def insertHakukohde(hakukohdeOid:String, hakuOid:String) = {
    singleConnectionValintarekisteriDb.runBlocking(DBIOAction.seq(
      sqlu"""insert into hakukohteet (hakukohde_oid, haku_oid, kk_tutkintoon_johtava, yhden_paikan_saanto_voimassa, koulutuksen_alkamiskausi)
           values ($hakukohdeOid, $hakuOid, true, true, '2015K')"""))
  }

  def loadSijoitteluFromFixture(fixture: String, path: String = "sijoittelu/", tallennaHakukohteet: Boolean = true):SijoitteluWrapper = {
    val json = parse(getClass.getClassLoader.getResourceAsStream("fixtures/" + path + fixture + ".json"))
    sijoitteluWrapperFromJson(json, tallennaHakukohteet)
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
      r.nextBoolean, r.nextBoolean, r.nextInt, r.nextInt, r.nextTimestampOption(), r.nextTimestampOption(), r.nextStringOption(),
      r.nextIntOption, r.nextIntOption, r.nextBigDecimalOption, None).valintatapajono
  })

  def findHakukohteenValintatapajonot(hakukohdeOid:String): Seq[Valintatapajono] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select oid, nimi, prioriteetti, tasasijasaanto, aloituspaikat, alkuperaiset_aloituspaikat, ei_varasijatayttoa,
            kaikki_ehdon_tayttavat_hyvaksytaan, poissaoleva_taytto,
            varasijat, varasijatayttopaivat, varasijoja_kaytetaan_alkaen, varasijoja_taytetaan_asti, tayttojono,
            hyvaksytty, varalla, alin_hyvaksytty_pistemaara
            from valintatapajonot
            where hakukohde_oid = ${hakukohdeOid}""".as[Valintatapajono])
  }

  private implicit val getSijoitteluajonHakijaryhmaResult = GetResult(r => {
    SijoitteluajonHakijaryhmaWrapper(r.nextString, r.nextString,
      r.nextIntOption(), r.nextIntOption, r.nextIntOption, r.nextBooleanOption,
      r.nextBooleanOption, r.nextBooleanOption, r.nextBigDecimalOption, List()).hakijaryhma
  })

  def findHakukohteenHakijaryhmat(hakukohdeOid:String): Seq[Hakijaryhma] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select oid, nimi, prioriteetti, paikat, kiintio, kayta_kaikki,
            tarkka_kiintio, kaytetaan_ryhmaan_kuuluvia, alin_hyvaksytty_pistemaara
            from hakijaryhmat
            where hakukohde_oid = ${hakukohdeOid}""".as[Hakijaryhma]
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
      r.nextBooleanOption, Valinnantila(r.nextString), getHakemuksenTilankuvaukset(r.nextLong), r.nextString, hakijaryhmaOidsToSet(r.nextStringOption)).hakemus
  })

  def findValintatapajononJonosijat(valintatapajonoOid:String): Seq[Hakemus] = {
    singleConnectionValintarekisteriDb.runBlocking(
      sql"""select j.hakemus_oid, j.hakija_oid, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija, j.varasijan_numero,
            j.onko_muuttunut_viime_sijoittelussa, j.pisteet, j.tasasijajonosija, j.hyvaksytty_harkinnanvaraisesti,
            j.siirtynyt_toisesta_valintatapajonosta, v.tila, v.tilankuvaus_id, t.tilankuvauksen_tarkenne, array_to_string(array_agg(hr.oid) , ',')
            from jonosijat j
            left join hakijaryhman_hakemukset as hh on hh.hakemus_oid = j.hakemus_oid
            left join hakijaryhmat as hr on hr.id = hh.hakijaryhma_id
            left join valinnantulokset v on j.valintatapajono_oid = v.valintatapajono_oid and j.hakemus_oid = v.hakemus_oid
            left join tilankuvaukset as t on t.id = v.tilankuvaus_id
            where j.valintatapajono_oid = ${valintatapajonoOid}
            group by j.hakemus_oid, j.hakija_oid, j.etunimi, j.sukunimi, j.prioriteetti, j.jonosija, j.varasijan_numero,
            j.onko_muuttunut_viime_sijoittelussa, j.pisteet, j.tasasijajonosija, j.hyvaksytty_harkinnanvaraisesti,
            j.siirtynyt_toisesta_valintatapajonosta, v.tila, v.tilankuvaus_id, t.tilankuvauksen_tarkenne
         """.as[Hakemus])
  }

  def getHakemuksenTilankuvaukset(tilankuvausId: Long): Option[Map[String,String]] = {
    Option(Map(singleConnectionValintarekisteriDb.runBlocking(
      sql"""select kieli, teksti
            from tilankuvausten_tekstit
            where tilankuvaus_id = ${tilankuvausId}""".as[(String,String)]) : _*))
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

  def assertSijoittelu(wrapper:SijoitteluWrapper) = {
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
        def createSortedHakijaryhmaWrapper(hakijaryhma:Hakijaryhma) = {
          java.util.Collections.sort(hakijaryhma.getHakemusOid)
          SijoitteluajonHakijaryhmaWrapper(hakijaryhma)
        }
        createSortedHakijaryhmaWrapper(hakijaryhma) mustEqual createSortedHakijaryhmaWrapper(storedHakijaryhma.get)
      })
      storedValintatapajonot.length mustEqual hakukohde.getValintatapajonot.size
    })
    storedHakukohteet.length mustEqual wrapper.hakukohteet.length
  }
}

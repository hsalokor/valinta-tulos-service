package fi.vm.sade.valintatulosservice.valintarekisteri.db

import java.sql.JDBCType
import java.time.Instant
import java.util.UUID

import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import slick.jdbc.{GetResult, PositionedParameters, SetParameter}

abstract class ValintarekisteriResultExtractors {

  protected implicit val getVastaanottoResult = GetResult(r => VastaanottoRecord(
    henkiloOid = r.nextString,
    hakuOid = r.nextString,
    hakukohdeOid = r.nextString,
    action = VastaanottoAction(r.nextString),
    ilmoittaja = r.nextString,
    timestamp = r.nextTimestamp))

  protected implicit val getHakukohdeResult = GetResult(r => HakukohdeRecord(
    oid = r.nextString,
    hakuOid = r.nextString,
    yhdenPaikanSaantoVoimassa = r.nextBoolean,
    kktutkintoonJohtava = r.nextBoolean,
    koulutuksenAlkamiskausi = Kausi(r.nextString)))

  protected implicit val getHakijaResult = GetResult(r => HakijaRecord(
    etunimi = r.nextString,
    sukunimi = r.nextString,
    hakemusOid = r.nextString,
    hakijaOid = r.nextString))

  protected implicit val getHakutoiveResult = GetResult(r => HakutoiveRecord(
    hakemusOid = r.nextString,
    hakutoive = r.nextInt,
    hakukohdeOid = r.nextString,
    tarjoajaOid = r.nextString,
    valintatuloksenTila = r.nextString,
    kaikkiJonotsijoiteltu = r.nextBoolean))

  protected implicit val getPistetiedotResult = GetResult(r => PistetietoRecord(
    valintatapajonoOid = r.nextString,
    hakemusOid = r.nextString,
    tunniste = r.nextString,
    arvo = r.nextString,
    laskennallinenArvo = r.nextString,
    osallistuminen = r.nextString))

  protected implicit val getSijoitteluajoResult = GetResult(r => SijoitteluajoRecord(
    sijoitteluajoId = r.nextLong,
    hakuOid = r.nextString,
    startMils = r.nextTimestamp.getTime,
    endMils = r.nextTimestamp.getTime))

  protected implicit val getSijoitteluajoHakukohteetResult = GetResult(r => SijoittelunHakukohdeRecord(
    sijoitteluajoId = r.nextLong,
    oid = r.nextString,
    tarjoajaOid = r.nextString,
    kaikkiJonotsijoiteltu = r.nextBoolean))

  protected implicit val getValintatapajonotResult = GetResult(r => ValintatapajonoRecord(
    tasasijasaanto = r.nextString,
    oid = r.nextString,
    nimi = r.nextString,
    prioriteetti = r.nextInt,
    aloituspaikat = r.nextIntOption,
    alkuperaisetAloituspaikat = r.nextIntOption,
    alinHyvaksyttyPistemaara = r.nextBigDecimal,
    eiVarasijatayttoa = r.nextBoolean,
    kaikkiEhdonTayttavatHyvaksytaan = r.nextBoolean,
    poissaOlevaTaytto = r.nextBoolean,
    valintaesitysHyvaksytty = r.nextBooleanOption,
    hakeneet = 0,
    hyvaksytty = r.nextInt,
    varalla = r.nextInt , varasijat = r.nextIntOption,
    varasijanTayttoPaivat = r.nextIntOption,
    varasijojaKaytetaanAlkaen = r.nextDateOption,
    varasijojaKaytetaanAsti = r.nextDateOption,
    tayttoJono = r.nextStringOption,
    hakukohdeOid = r.nextString))

  protected implicit val getHakemuksetForValintatapajonosResult = GetResult(r => HakemusRecord(
    hakijaOid = r.nextStringOption,
    hakemusOid = r.nextString,
    pisteet = r.nextBigDecimalOption,
    etunimi = r.nextStringOption,
    sukunimi = r.nextStringOption,
    prioriteetti = r.nextInt,
    jonosija = r.nextInt,
    tasasijaJonosija = r.nextInt,
    tila = Valinnantila(r.nextString),
    tilankuvausHash = r.nextInt,
    tarkenteenLisatieto = r.nextStringOption,
    hyvaksyttyHarkinnanvaraisesti = r.nextBoolean,
    varasijaNumero = r.nextIntOption,
    onkoMuuttunutviimesijoittelusta = r.nextBoolean,
    hakijaryhmaOids = hakijaryhmaOidsToSet(r.nextStringOption),
    siirtynytToisestaValintatapaJonosta = r.nextBoolean,
    valintatapajonoOid = r.nextString))

  protected implicit val getHakemuksenTilahistoriaResult = GetResult(r => TilaHistoriaRecord(
    valintatapajonoOid = r.nextString,
    hakemusOid = r.nextString,
    tila = Valinnantila(r.nextString),
    luotu = r.nextTimestamp))

  protected implicit val getHakijaryhmatResult = GetResult(r => HakijaryhmaRecord(
    prioriteetti = r.nextInt,
    oid = r.nextString,
    nimi = r.nextString,
    hakukohdeOid = r.nextStringOption,
    kiintio = r.nextInt,
    kaytaKaikki = r.nextBoolean,
    sijoitteluajoId = r.nextLong,
    tarkkaKiintio = r.nextBoolean,
    kaytetaanRyhmaanKuuluvia = r.nextBoolean,
    valintatapajonoOid = r.nextStringOption,
    hakijaryhmatyyppikoodiUri = r.nextString))

  protected implicit val getTilankuvauksetResult = GetResult(r => TilankuvausRecord(
    hash = r.nextInt,
    tilankuvauksenTarkenne = ValinnantilanTarkenne(r.nextString),
    textFi = r.nextStringOption,
    textSv = r.nextStringOption,
    textEn = r.nextStringOption
  ))

  protected implicit val getValinnanTulosResult: GetResult[ValinnanTulos] = GetResult(r => ValinnanTulos(
    hakuOid = r.nextString,
    hakukohdeOid = r.nextString,
    valintatapajonoOid = r.nextString,
    hakemusOid = r.nextString,
    valinnantila = Valinnantila(r.nextString),
    ehdollisestiHyvaksyttavissa = r.nextBoolean,
    julkaistavissa = r.nextBoolean,
    vastaanottotila = r.nextStringOption.map(VastaanottoAction(_)).getOrElse(Poista),
    ilmoittautumistila = r.nextStringOption.map(SijoitteluajonIlmoittautumistila(_)).getOrElse(EiTehty)
  ))

  protected implicit val getValinnanTulosWithLastModifiedResult: GetResult[(Instant, ValinnanTulos)] = GetResult(r => (
    (List(r.nextDate()) ++ r.nextDateOption() ++ r.nextDateOption()).map(_.toInstant).max,
    getValinnanTulosResult(r)))

  protected def hakijaryhmaOidsToSet(hakijaryhmaOids:Option[String]): Set[String] = {
    hakijaryhmaOids match {
      case Some(oids) if !oids.isEmpty => oids.split(",").toSet
      case _ => Set()
    }
  }

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) {
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
    }
  }
}

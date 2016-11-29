package fi.vm.sade.valintatulosservice.valintarekisteri.db

import fi.vm.sade.valintatulosservice.valintarekisteri.domain._
import slick.jdbc.GetResult

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
    jonosijaId = r.nextInt,
    hakutoive = r.nextInt,
    hakukohdeOid = r.nextString,
    tarjoajaOid = r.nextString,
    valintatuloksenTila = r.nextString,
    kaikkiJonotsijoiteltu = r.nextBoolean))

  protected implicit val getPistetiedotResult = GetResult(r => PistetietoRecord(
    jonosijaId = r.nextInt,
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
    kaikkiJonotsijoiteltu = r.nextBoolean,
    ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet = r.nextBigDecimal))

  protected implicit val getValintatapajonotResult = GetResult(r => ValintatapajonoRecord(
    tasasijasaanto = r.nextString,
    oid = r.nextString,
    nimi = r.nextString,
    prioriteetti = r.nextInt,
    aloituspaikat = r.nextInt,
    alkuperaisetAloituspaikat = r.nextInt,
    alinHyvaksyttyPistemaara = r.nextBigDecimal,
    eiVarasijatayttoa = r.nextBoolean,
    kaikkiEhdonTayttavatHyvaksytaan = r.nextBoolean,
    poissaOlevaTaytto = r.nextBoolean,
    valintaesitysHyvaksytty = r.nextBoolean,
    hakeneet = 0,
    hyvaksytty = r.nextInt,
    varalla = r.nextInt , varasijat = r.nextInt,
    varasijanTayttoPaivat = r.nextInt,
    varasijojaKaytetaanAlkaen = r.nextDate,
    varasijojaKaytetaanAsti = r.nextDate,
    tayttoJono = r.nextString,
    hakukohdeOid = r.nextString))

  protected implicit val getHakemuksetForValintatapajonosResult = GetResult(r => HakemusRecord(
    hakijaOid = r.nextString,
    hakemusOid = r.nextString,
    pisteet = r.nextBigDecimal,
    etunimi = r.nextString,
    sukunimi = r.nextString,
    prioriteetti = r.nextInt,
    jonosija = r.nextInt,
    tasasijaJonosija = r.nextInt,
    tila = Valinnantila(r.nextString),
    tilankuvausId = r.nextLong,
    tarkenteenLisatieto = r.nextStringOption,
    hyvaksyttyHarkinnanvaraisesti = r.nextBoolean,
    varasijaNumero = r.nextIntOption,
    onkoMuuttunutviimesijoittelusta = r.nextBoolean,
    hakijaryhmaOids = hakijaryhmaOidsToSet(r.nextStringOption),
    siirtynytToisestaValintatapaJonosta = r.nextBoolean,
    valintatapajonoOid = r.nextString))

  protected implicit val getHakemuksenTilahistoriaResult = GetResult(r => TilaHistoriaRecord(
    tila = r.nextString,
    poistaja = r.nextString,
    selite = r.nextString,
    luotu = r.nextDate))

  protected implicit val getHakijaryhmatResult = GetResult(r => HakijaryhmaRecord(
    id = r.nextLong,
    prioriteetti = r.nextInt,
    paikat = r.nextInt,
    oid = r.nextString,
    nimi = r.nextString,
    hakukohdeOid = r.nextString,
    kiintio = r.nextInt,
    kaytaKaikki = r.nextBoolean,
    tarkkaKiintio = r.nextBoolean,
    kaytetaanRyhmaanKuuluvia = r.nextBoolean,
    valintatapajonoOid = r.nextString,
    hakijaryhmatyyppikoodiUri = r.nextString))

  protected def hakijaryhmaOidsToSet(hakijaryhmaOids:Option[String]): Set[String] = {
    hakijaryhmaOids match {
      case Some(oids) if !oids.isEmpty => oids.split(",").toSet
      case _ => Set()
    }
  }
}

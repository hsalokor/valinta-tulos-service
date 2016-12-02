package fi.vm.sade.valintatulosservice.production

import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import fi.vm.sade.utils.http.{DefaultHttpClient, DefaultHttpRequest}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
import org.junit.Ignore
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.matcher.MatcherMacros

import scalaj.http.Http
import scalaz.concurrent.Task

import scala.language.experimental.macros

@RunWith(classOf[JUnitRunner])
class SijoitteluRestTest extends Specification with MatcherMacros with Logging {
  val host = "https://itest-virkailija.oph.ware.fi"
  val cas_user = "xxxx"
  val cas_password = "xxxx"
  val cas_url = host + "/cas"
  val haku_oid = "1.2.246.562.29.10152859822"
  //val haku_oid = "1.2.246.562.29.14662042044"
  //val haku_oid = "1.2.246.562.29.95390561488"

  implicit val formats = DefaultFormats

  val vanhaSijoitteluCasClient = createCasClient("/sijoittelu-service")

  "New sijoittelu (valintarekisteri) and old sijoittelu (sijoitteluDb)" should {
    "contain same information" in {
      val uusiSijoittelu:Sijoitteluajo = get[Sijoitteluajo](getNewSijoittelu)
      val vanhaSijoittelu = createVanhaSijoitteluajo()

      uusiSijoittelu must matchA[Sijoitteluajo]
        .sijoitteluajoId(vanhaSijoittelu.sijoitteluajoId)
        .hakuOid(vanhaSijoittelu.hakuOid)
        .startMils(vanhaSijoittelu.startMils)
        .endMils(vanhaSijoittelu.endMils)

      uusiSijoittelu.hakukohteet.size mustEqual vanhaSijoittelu.hakukohteet.size

      uusiSijoittelu.hakukohteet.foreach(uusiHakukohde => {
        logger.info(s"Hakukohde ${uusiHakukohde.oid}")
        val vanhaHakukohde = vanhaSijoittelu.hakukohteet.find(_.oid.equals(uusiHakukohde.oid)).get
        uusiHakukohde must matchA[Hakukohde]
          .sijoitteluajoId(vanhaHakukohde.sijoitteluajoId)
          .tila(vanhaHakukohde.tila)
          .tarjoajaOid(vanhaHakukohde.tarjoajaOid)
          .kaikkiJonotSijoiteltu(vanhaHakukohde.kaikkiJonotSijoiteltu)
          .ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet(vanhaHakukohde.ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet)

        uusiHakukohde.valintatapajonot.size mustEqual vanhaHakukohde.valintatapajonot.size
        uusiHakukohde.valintatapajonot.foreach(uusiValintatapajono => {
          logger.info(s"Valintatapajono ${uusiValintatapajono.oid}")
          val vanhaValintatapajono = vanhaHakukohde.valintatapajonot.find(_.oid.equals(uusiValintatapajono.oid)).get
          uusiValintatapajono must matchA[Valintatapajono]
            .tasasijasaanto(vanhaValintatapajono.tasasijasaanto)
            .tila(vanhaValintatapajono.tila)
            .prioriteetti(vanhaValintatapajono.prioriteetti)
            .aloituspaikat(vanhaValintatapajono.aloituspaikat)
            .alkuperaisetAloituspaikat(vanhaValintatapajono.alkuperaisetAloituspaikat)
            .alinHyvaksyttyPistemaara(vanhaValintatapajono.alinHyvaksyttyPistemaara)
            .eiVarasijatayttoa(vanhaValintatapajono.eiVarasijatayttoa)
            .kaikkiEhdonTayttavatHyvaksytaan(vanhaValintatapajono.kaikkiEhdonTayttavatHyvaksytaan)
            .poissaOlevaTaytto(vanhaValintatapajono.poissaOlevaTaytto)
            .hakeneet(vanhaValintatapajono.hakeneet)
            .hyvaksytty(vanhaValintatapajono.hyvaksytty)
            .varalla(vanhaValintatapajono.varalla)
            .varasijat(vanhaValintatapajono.varasijat)
            .varasijaTayttoPaivat(vanhaValintatapajono.varasijaTayttoPaivat)
            .varasijojaTaytetaanAsti(vanhaValintatapajono.varasijojaTaytetaanAsti)
            .tayttojono(vanhaValintatapajono.tayttojono)

          uusiValintatapajono.valintaesitysHyvaksytty mustEqual vanhaValintatapajono.valintaesitysHyvaksytty

          uusiValintatapajono.hakemukset.size mustEqual vanhaValintatapajono.hakemukset.size
          uusiValintatapajono.hakemukset.foreach(uusiHakemus => {
            logger.info(s"Hakemus ${uusiHakemus.hakemusOid}")
            val vanhaHakemus = vanhaValintatapajono.hakemukset.find(_.hakemusOid.equals(uusiHakemus.hakemusOid)).get
            uusiHakemus must matchA[Hakemus]
              .hakijaOid(vanhaHakemus.hakijaOid)
              .pisteet(vanhaHakemus.pisteet)
              .paasyJaSoveltuvuusKokeenTulos(vanhaHakemus.paasyJaSoveltuvuusKokeenTulos)
              .etunimi(vanhaHakemus.etunimi)
              .sukunimi(vanhaHakemus.sukunimi)
              .prioriteetti(vanhaHakemus.prioriteetti)
              .jonosija(vanhaHakemus.jonosija)
              .tasasijaJonosija(vanhaHakemus.tasasijaJonosija)
              .tila(vanhaHakemus.tila)
              .hyvaksyttyHarkinnanvaraisesti(vanhaHakemus.hyvaksyttyHarkinnanvaraisesti)
              .varasijanNumero(vanhaHakemus.varasijanNumero)
              .valintatapajonoOid(vanhaHakemus.valintatapajonoOid)
              .hakuOid(vanhaHakemus.hakuOid)
              .onkoMuuttunutViimeSijoittelussa(vanhaHakemus.onkoMuuttunutViimeSijoittelussa)
              .siirtynytToisestaValintatapajonosta(vanhaHakemus.siirtynytToisestaValintatapajonosta)

            logger.info(s"Tilankuvaukset ${uusiHakemus.tilanKuvaukset}")

            uusiHakemus.tilanKuvaukset must matchA[Tilankuvaus]
              .EN(vanhaHakemus.tilanKuvaukset.EN)
              .FI(vanhaHakemus.tilanKuvaukset.FI)
              .SV(vanhaHakemus.tilanKuvaukset.SV)

            uusiHakemus.pistetiedot.size mustEqual vanhaHakemus.pistetiedot.size
            uusiHakemus.pistetiedot.foreach(uusiPistetieto => {
              val vanhaPistetieto = vanhaHakemus.pistetiedot.find(_.tunniste.equals(uusiPistetieto.tunniste)).get
              logger.info(s"Pistetieto ${uusiPistetieto.tunniste}")
              uusiPistetieto must matchA[Pistetieto]
                .tunniste(vanhaPistetieto.tunniste)
                .arvo(vanhaPistetieto.arvo)
                .laskennallinenArvo(vanhaPistetieto.laskennallinenArvo)
                .osallistuminen(vanhaPistetieto.osallistuminen)
                .tyypinKoodiUri(vanhaPistetieto.tyypinKoodiUri)
                .tilastoidaan(vanhaPistetieto.tilastoidaan)
            })
          })
        })
        uusiHakukohde.hakijaryhmat.size mustEqual vanhaHakukohde.hakijaryhmat.size
        uusiHakukohde.hakijaryhmat.foreach(uusiHakijaryhma => {
          logger.info(s"Hakijaryhma ${uusiHakijaryhma.oid}")
          val vanhaHakijaryhma = vanhaHakukohde.hakijaryhmat.find(_.oid.equals(uusiHakijaryhma.oid)).get
          uusiHakijaryhma must matchA[Hakijaryhma]
            .prioriteetti(vanhaHakijaryhma.prioriteetti)
            .paikat(vanhaHakijaryhma.paikat)
            .nimi(vanhaHakijaryhma.nimi)
            .hakukohdeOid(vanhaHakijaryhma.hakukohdeOid)
            .kiintio(vanhaHakijaryhma.kiintio)
            .kaytaKaikki(vanhaHakijaryhma.kaytaKaikki)
            .tarkkaKiintio(vanhaHakijaryhma.tarkkaKiintio)
            .kaytetaanRyhmaanKuuluvia(vanhaHakijaryhma.kaytetaanRyhmaanKuuluvia)
            .hakijaryhmatyyppikoodiUri(vanhaHakijaryhma.hakijaryhmatyyppikoodiUri)
            .valintatapajonoOid(vanhaHakijaryhma.valintatapajonoOid)
          uusiHakijaryhma.hakemusOid.size mustEqual vanhaHakijaryhma.hakemusOid.size
          uusiHakijaryhma.hakemusOid.diff(vanhaHakijaryhma.hakemusOid) mustEqual List()
        })
      })
      true must beTrue
    }
  }

  private def createVanhaSijoitteluajo() = {
    val sijoitteluajo = get[Sijoitteluajo](getSijoitteluajo)
    sijoitteluajo.copy(hakukohteet = sijoitteluajo.hakukohteet.map(h => get[Hakukohde](getHakukohde(h.oid))))
  }

  private def get[T](fetch:() => String)(implicit m: Manifest[T]): T = parse(fetch()).extract[T]

  private def getSijoitteluajo():String = getOld(s"${host}/sijoittelu-service/resources/sijoittelu/${haku_oid}/sijoitteluajo/latest")
  private def getHakukohde(oid:String) = () => getOld(s"${host}/sijoittelu-service/resources/sijoittelu/${haku_oid}/sijoitteluajo/latest/hakukohde/${oid}")

  private def getNewSijoittelu() = {
    val (_, _, result) = new DefaultHttpRequest(Http(s"${host}/valinta-tulos-service/sijoittelu/${haku_oid}/sijoitteluajo/latest")
      .method("GET")
      .options(DefaultHttpClient.defaultOptions)
      .header("Content-Type", "application/json")).responseWithHeaders()
    result
  }


  private def getOld(uriString:String) = {
    vanhaSijoitteluCasClient.prepare(Request(method = Method.GET, uri = createUri(uriString))).flatMap {
      case r if 200 == r.status.code => r.as[String]
      case r => Task.fail(new RuntimeException(r.toString))
    }.run
  }

  private def createUri(uriString:String): Uri = Uri.fromString(uriString).getOrElse(throw new RuntimeException(s"Invalid uri"))

  private def createCasClient(target:String): Client = {
    val casParams = CasParams(target, cas_user, cas_password)
    new CasAuthenticatingClient(
      new CasClient(cas_url, org.http4s.client.blaze.defaultClient),
      casParams,
      org.http4s.client.blaze.defaultClient,
      null
    )
  }


}
case class Sijoitteluajo(sijoitteluajoId:Long, hakuOid:String, startMils:Long, endMils:Long, hakukohteet:List[Hakukohde])

case class Hakukohde(sijoitteluajoId:Option[Long], oid:String, tila:Option[String], tarjoajaOid:Option[String],
                     kaikkiJonotSijoiteltu:Boolean, ensikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet:Option[Long],
                     valintatapajonot:List[Valintatapajono], hakijaryhmat:List[Hakijaryhma])

case class Hakijaryhma(prioriteetti:Option[Long], paikat:Option[Long], oid:String, nimi:Option[String],
                       hakukohdeOid:Option[String], kiintio:Option[Long],
                       kaytaKaikki:Boolean, tarkkaKiintio:Boolean, kaytetaanRyhmaanKuuluvia:Boolean,
                       hakijaryhmatyyppikoodiUri:Option[String], valintatapajonoOid:Option[String], hakemusOid:List[String])

case class Valintatapajono(tasasijasaanto:Option[String], tila:Option[String], oid:String,
                           prioriteetti:Option[Long], aloituspaikat:Option[Long], alkuperaisetAloituspaikat:Option[Long], alinHyvaksyttyPistemaara:Option[Long],
                           eiVarasijatayttoa:Boolean, kaikkiEhdonTayttavatHyvaksytaan:Boolean, poissaOlevaTaytto:Boolean, valintaesitysHyvaksytty:Option[Boolean],
                           hakeneet:Option[Long], hyvaksytty:Option[Long], varalla:Option[Long], varasijat:Option[Long], hakemukset:List[Hakemus],
                           varasijaTayttoPaivat:Option[java.util.Date], varasijojaTaytetaanAsti:Option[java.util.Date], tayttojono:Option[String])

case class Hakemus(hakijaOid:Option[String], hakemusOid:String, pisteet:Option[Long], paasyJaSoveltuvuusKokeenTulos:Option[Long],
                   etunimi:Option[String], sukunimi:Option[String], prioriteetti:Option[Long], jonosija:Option[Long], tasasijaJonosija:Option[Long],
                   tila:Option[String], hyvaksyttyHarkinnanvaraisesti:Boolean, varasijanNumero:Option[Long], sijoitteluajoId:Option[Long],
                   hakukohdeOid:Option[String], tarjoajaOid:Option[String], valintatapajonoOid:Option[String],
                   hakuOid:Option[String], onkoMuuttunutViimeSijoittelussa:Boolean, siirtynytToisestaValintatapajonosta:Boolean,
                   tilanKuvaukset:Tilankuvaus, pistetiedot:List[Pistetieto])

case class Pistetieto(tunniste:String, arvo:String, laskennallinenArvo:String, osallistuminen:String, tyypinKoodiUri:String, tilastoidaan:Boolean)

case class Tilankuvaus(SV:Option[String], FI:Option[String], EN:Option[String])
package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.domain._
import fi.vm.sade.valintatulosservice.json.JsonStreamWriter
import fi.vm.sade.valintatulosservice.ohjausparametrit.Ohjausparametrit
import fi.vm.sade.valintatulosservice.tarjonta.{YhdenPaikanSaanto, Haku, Hakuaika}
import org.joda.time.DateTime
import org.json4s.Extraction
import org.scalatra._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._

import scala.util.Try

abstract class ValintatulosServlet(valintatulosService: ValintatulosService, vastaanottoService: VastaanottoService, ilmoittautumisService: IlmoittautumisService)(implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  lazy val exampleHakemuksenTulos = Hakemuksentulos(
    "2.2.2.2",
    "4.3.2.1",
    "1.3.3.1",
    Some(Vastaanottoaikataulu(Some(new DateTime()), Some(14))),
    List(
      Hakutoiveentulos.julkaistavaVersioSijoittelunTuloksesta(
        HakutoiveenSijoitteluntulos.kesken("1.2.3.4", "4.4.4.4"),
        Hakutoive("1.2.3.4", "4.4.4.4", "Hakukohde1", "Tarjoaja1"),
        Haku("5.5.5.5", true, true, true, false, true, None, Set(),
          List(Hakuaika("12345", Some(System.currentTimeMillis()), Some(System.currentTimeMillis()))),
          YhdenPaikanSaanto(false, "")),
        Some(Ohjausparametrit(None, Some(DateTime.now().plusDays(10)), Some(DateTime.now().plusDays(30)), Some(DateTime.now().plusDays(60)))))
    )
  )

  // Real return type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
  lazy val getHakemusSwagger: OperationBuilder = (apiOperation[Unit]("getHakemus")
    summary "Hae hakemuksen tulokset."
    notes "Palauttaa tyyppiä Hakemuksentulos. Esim:\n" +
      pretty(Extraction.decompose(exampleHakemuksenTulos))
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka tulokset halutaan")
  )
  get("/:hakuOid/hakemus/:hakemusOid", operation(getHakemusSwagger)) {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    valintatulosService.hakemuksentulos(hakuOid, hakemusOid) match {
      case Some(tulos) => tulos
      case _ => NotFound("error" -> "Not found")
    }
  }

  lazy val getHakemuksetSwagger: OperationBuilder = (apiOperation[Unit]("getHakemukset")
    summary "Hae haun kaikkien hakemusten tulokset."
    notes "Palauttaa tyyppiä Seq[Hakemuksentulos]. Esim:\n" +
      pretty(Extraction.decompose(Seq(exampleHakemuksenTulos)))
    parameter pathParam[String]("hakuOid").description("Haun oid")
  )
  get("/:hakuOid", operation(getHakemuksetSwagger)) {
    val hakuOid = params("hakuOid")
    serveStreamingResults({ valintatulosService.hakemustenTulosByHaku(hakuOid) })
  }

  get("/:hakuOid/hakukohde/:hakukohdeOid", operation(getHakukohteenHakemuksetSwagger)) {
    val hakuOid = params("hakuOid")
    val hakukohdeOid = params("hakukohdeOid")
    serveStreamingResults({ valintatulosService.hakemustenTulosByHakukohde(hakuOid, hakukohdeOid) })
  }

  lazy val getHakukohteenHakemuksetSwagger: OperationBuilder = (apiOperation[Unit]("getHakukohteenHakemukset")
    summary "Hae hakukohteen kaikkien hakemusten tulokset."
    notes "Palauttaa tyyppiä Seq[Hakemuksentulos]. Esim:\n" +
    pretty(Extraction.decompose(Seq(exampleHakemuksenTulos)))
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid")
  )
  val postVastaanottoSwagger: OperationBuilder = (apiOperation[Unit]("getHakemus")
    summary "Tallenna hakukohteelle uusi vastaanottotila"
    // Real body param type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
    notes "Bodyssä tulee antaa tieto hakukohteen vastaanotttilan muutoksesta Vastaanotto tyyppinä. Esim:\n" +
      pretty(Extraction.decompose(
        Vastaanotto(
            "1.2.3.4",
            Vastaanottotila.vastaanottanut,
            "henkilö: 5.5.5.5",
            "kuvaus mitä kautta muokkaus tehty"
        )
      )) + ".\nMahdolliset vastaanottotilat: " + vastaanottoService.sallitutVastaanottotilat
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka vastaanottotilaa ollaan muokkaamassa")
  )
  post("/:hakuOid/hakemus/:hakemusOid/vastaanota", operation(postVastaanottoSwagger)) {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    val vastaanotto = parsedBody.extract[Vastaanotto]


    Try(vastaanottoService.vastaanota(hakemusOid, vastaanotto)).map((_) => Ok()).recover{
      case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage)
    }.get
  }

  lazy val getHakukohteenVastaanotettavuusSwagger: OperationBuilder = (apiOperation[Unit]("getHakukohteenHakemukset")
    summary "Palauttaa 200 jos hakutoive vastaanotettavissa, 403 ja virheviestin jos henkilöllä estävä aikaisempi vastaanotto"
    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen oid")
    )
  get("/:hakuOid/hakemus/:hakemusOid/hakukohde/:hakukohdeOid/vastaanotettavuus", operation(getHakukohteenVastaanotettavuusSwagger)) {
    Try(vastaanottoService.tarkistaVastaanotettavuus(params("hakemusOid"), params("hakukohdeOid")))
      .map((_) => Ok())
      .recover({ case pae:PriorAcceptanceException => Forbidden("error" -> pae.getMessage) })
      .get
  }

  val postIlmoittautuminenSwagger: OperationBuilder = (apiOperation[Unit]("ilmoittaudu")
    summary "Tallenna hakukohteelle uusi ilmoittautumistila"
    // Real body param type cannot be used because of unsupported scala enumerations: https://github.com/scalatra/scalatra/issues/343
    notes "Bodyssä tulee antaa tieto hakukohteen ilmoittautumistilan muutoksesta Ilmoittautuminen tyyppinä. Esim:\n" +
    pretty(Extraction.decompose(
      Ilmoittautuminen(
        "1.2.3.4",
        Ilmoittautumistila.läsnä_koko_lukuvuosi,
        "henkilö: 5.5.5.5",
        "kuvaus mitä kautta muokkaus tehty"
      )
    )) + ".\nMahdolliset ilmoittautumistilat: " + Ilmoittautumistila.values

    parameter pathParam[String]("hakuOid").description("Haun oid")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen oid, jonka vastaanottotilaa ollaan muokkaamassa")
    )
  post("/:hakuOid/hakemus/:hakemusOid/ilmoittaudu", operation(postIlmoittautuminenSwagger)) {
    val hakuOid = params("hakuOid")
    val hakemusOid = params("hakemusOid")
    val ilmoittautuminen = parsedBody.extract[Ilmoittautuminen]

    ilmoittautumisService.ilmoittaudu(hakuOid, hakemusOid, ilmoittautuminen)
  }

  private def serveStreamingResults(fetchData: => Option[Iterator[Hakemuksentulos]]): Any = {
    HakemustenTulosHakuLock.synchronized {
      fetchData match {
        case Some(tulos) => JsonStreamWriter.writeJsonStream(tulos, response.writer)
        case _ => NotFound("error" -> "Not found")
      }
    }
  }
}

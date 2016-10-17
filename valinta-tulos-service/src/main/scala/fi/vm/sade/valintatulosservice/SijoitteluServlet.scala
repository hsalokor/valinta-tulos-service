package fi.vm.sade.valintatulosservice

import fi.vm.sade.valintatulosservice.domain.Sijoitteluajo
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.json4s.jackson.Serialization._
import org.scalatra.{NotImplemented, Ok}
import org.scalatra.swagger._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder

class SijoitteluServlet(sijoitteluService: SijoitteluService) (implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("sijoittelu")

  override protected def applicationDescription: String = "Sijoittelun REST API"

  lazy val postSijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("postSijoitteluajoSwagger")
    summary "Tallentaa sijoitteluajon"
    parameter bodyParam[Sijoitteluajo]("sijoitteluajo").description("Sijoitteluajon data"))
  post("/sijoitteluajo", operation(postSijoitteluajoSwagger)) {
    val sijoitteluajo = read[Sijoitteluajo](request.body)
    Ok(sijoitteluService.luoSijoitteluajo(sijoitteluajo))
  }

  // Sijoittelu-service
  lazy val getSijoitteluajoByHakuOidSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoByHakuOidSwagger")
    summary "Hakee sijoittelun tiedot haulle. Paa-asiallinen kaytto sijoitteluajojen tunnisteiden hakuun."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/", operation(getSijoitteluajoByHakuOidSwagger)) {
    val hakuOid = params("hakuOid")
    //TODO Ok(sijoitteluService.getSijoitteluajo(hakuOid))
    NotImplemented()
  }

  lazy val getSijoitteluajoHyvaksytytSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoHyvaksytytSwagger")
    summary "Sivutettu listaus hakemuksien/hakijoiden listaukseen. Yksityiskohtainen listaus kaikista hakutoiveista ja niiden valintatapajonoista."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/hyvaksytyt/", operation(getSijoitteluajoHyvaksytytSwagger)) {
    val hakuOid = params("hakuOid")
    //TODO Ok(sijoitteluService.getSijoitteluajoHyvaksytyt(hakuOid))
    NotImplemented()
  }

  lazy val getSijoitteluajoHyvaksytytByHakukohdeSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoHyvaksytytByHakukohdeSwagger")
    summary "Sivutettu listaus hakemuksien/hakijoiden listaukseen. Yksityiskohtainen listaus kaikista hakutoiveista ja niiden valintatapajonoista hakukohteen perusteella."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/hyvaksytyt/hakukohde/:hakukohdeOid/", operation(getSijoitteluajoHyvaksytytByHakukohdeSwagger)) {
    val hakuOid = params("hakuOid")
    val hakukohdeOid = params("hakukohdeOid")
    //TODO Ok(sijoitteluService.getSijoitteluajoHyvaksytytByHakukohde(hakuOid, hakukohdeOid))
    NotImplemented()
  }

  lazy val getSijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoSwagger")
    summary "Hakee sijoitteluajon tiedot. Paasiallinen kaytto sijoitteluun osallistuvien hakukohteiden hakemiseen."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste") //TODO tarpeeton?
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana."))
  get("/sijoittelu/:hakuOid/hyvaksytyt/hakukohde/:sijoitteluajoOid/", operation(getSijoitteluajoSwagger)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    //TODO Ok(sijoitteluService.getSijoitteluajo(hakuOid, sijoitteluajoOid))
    NotImplemented()
  }

  lazy val getHakemusetBySijoitteluajo: OperationBuilder = (apiOperation[Unit]("getHakemusetBySijoitteluajo")
    summary "Sivutettu listaus hakemuksien/hakijoiden listaukseen. Yksityiskohtainen listaus kaikista hakutoiveista ja niiden valintatapajonoista."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana."))
  get("/sijoittelu/:hakuOid/hyvaksytyt/hakukohde/:sijoitteluajoOid/hakemukset/", operation(getHakemusetBySijoitteluajo)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    //TODO Ok(sijoitteluService.getHakemuksetBySijoitteluajo(hakuOid, sijoitteluajoOid))
    NotImplemented()
  }

  lazy val getHakemusBySijoitteluajo: OperationBuilder = (apiOperation[Unit]("getHakemusBySijoitteluajo")
    summary "Nayttaa yksittaisen hakemuksen kaikki hakutoiveet ja tiedot kaikista valintatapajonoista."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana.")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/hyvaksytyt/hakukohde/:sijoitteluajoOid/hakemus/:hakemusOid", operation(getHakemusBySijoitteluajo)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    val hakemusOid = params("hakemusOid")
    //TODO Ok(sijoitteluService.getHakemusBySijoitteluajo(hakuOid, sijoitteluajoOid, hakemusOid))
    NotImplemented()
  }

  lazy val getHakukohdeBySijoitteluajo: OperationBuilder = (apiOperation[Unit]("getHakukohdeBySijoitteluajo")
    summary "Hakee hakukohteen tiedot tietyssa sijoitteluajossa."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana.")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/hyvaksytyt/hakukohde/:sijoitteluajoOid/hakukohde/:hakukohdeOid", operation(getHakukohdeBySijoitteluajo)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    val hakukohdeOid = params("hakukohdeOid")
    //TODO Ok(sijoitteluService.getHakukohdeBySijoitteluajo(hakuOid, sijoitteluajoOid, hakukohdeOid))
    NotImplemented()
  }

  lazy val getHakukohdeDtoBySijoitteluajo: OperationBuilder = (apiOperation[Unit]("getHakukohdeDtoBySijoitteluajo")
    summary "Hakee hakukohteen tiedot tietyssa sijoitteluajossa."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana.")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/hyvaksytyt/hakukohde/:sijoitteluajoOid/hakukohdedto/:hakukohdeOid", operation(getHakukohdeDtoBySijoitteluajo)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    val hakukohdeOid = params("hakukohdeOid")
    //TODO Ok(sijoitteluService.getHakukohdeDtoBySijoitteluajo(hakuOid, sijoitteluajoOid, hakukohdeOid))
    NotImplemented()
  }

  lazy val valintatapajonoIsInSijoittelu: OperationBuilder = (apiOperation[Unit]("valintatapajonoIsInSijoittelu")
    summary "Kertoo jos valintatapajono on sijoittelun käytössä."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("valintatapajonoOid").description("Valintatapajonon yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/valintatapajono-in-use/:valintatapajonoOid", operation(valintatapajonoIsInSijoittelu)) {
    val hakuOid = params("hakuOid")
    val valintatapajonoOid = params("valintatapajonoOid")
    //TODO Ok(sijoitteluService.valintatapajonoIsInSijoittelu(hakuOid, valintatapajonoOid))
    NotImplemented()
  }
}

package fi.vm.sade.valintatulosservice

import fi.vm.sade.sijoittelu.domain.SijoitteluAjo
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.json4s.jackson.Serialization._
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger._
import org.scalatra.{NotImplemented, Ok}

class SijoitteluServlet(sijoitteluService: SijoitteluService) (implicit val swagger: Swagger, appConfig: AppConfig) extends VtsServletBase {

  override val applicationName = Some("sijoittelu")

  override protected def applicationDescription: String = "Sijoittelun REST API"

  lazy val postSijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("postSijoitteluajoSwagger")
    summary "Tallentaa sijoitteluajon"
    parameter bodyParam[SijoitteluAjo]("sijoitteluajo").description("Sijoitteluajon data"))
  post("/sijoitteluajo", operation(postSijoitteluajoSwagger)) {
    val sijoitteluajo = read[SijoitteluAjo](request.body)
    Ok(sijoitteluService.luoSijoitteluajo(sijoitteluajo))
  }

  lazy val getSijoitteluajoMaxIntervalSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoMaxIntervalSwagger")
    summary "Hakee sijoittelun tiedot haulle. Pääasiallinen kaytto sijoitteluajojen tunnisteiden hakuun.")
  get("/session/maxinterval", operation(getSijoitteluajoMaxIntervalSwagger)) {
    //TODO Ok(sijoitteluService.getMaxInterval(hakuOid))
    NotImplemented()
  }

  // Sijoittelu-service
  lazy val getSijoitteluajoByHakuOidSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoByHakuOidSwagger")
    summary "Hakee sijoittelun tiedot haulle. Pääasiallinen kaytto sijoitteluajojen tunnisteiden hakuun."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid", operation(getSijoitteluajoByHakuOidSwagger)) {
    val hakuOid = params("hakuOid")
    //TODO Ok(sijoitteluService.getSijoitteluajoByHakuOid(hakuOid))
    NotImplemented()
  }

  lazy val getSijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoSwagger")
    summary "Hakee sijoitteluajon tiedot. Pääsiallinen kaytto sijoitteluun osallistuvien hakukohteiden hakemiseen."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste") //TODO tarpeeton?
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana."))
  get("/sijoittelu/:hakuOid/sijoitteluajo/:sijoitteluajoOid", operation(getSijoitteluajoSwagger)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    //TODO Ok(sijoitteluService.getSijoitteluajo(hakuOid, sijoitteluajoOid))
    NotImplemented()
  }

  lazy val getHakemusetBySijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("getHakemusetBySijoitteluajoSwagger")
    summary "Sivutettu listaus hakemuksien/hakijoiden listaukseen. Yksityiskohtainen listaus kaikista hakutoiveista ja niiden valintatapajonoista."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana."))
  get("/sijoittelu/:hakuOid/sijoitteluajo/:sijoitteluajoOid/hakemukset", operation(getHakemusetBySijoitteluajoSwagger)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    //TODO Ok(sijoitteluService.getHakemuksetBySijoitteluajo(hakuOid, sijoitteluajoOid))
    NotImplemented()
  }

  lazy val getHakemusBySijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("getHakemusBySijoitteluajoSwagger")
    summary "Nayttaa yksittaisen hakemuksen kaikki hakutoiveet ja tiedot kaikista valintatapajonoista."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana.")
    parameter pathParam[String]("hakemusOid").description("Hakemuksen yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/sijoitteluajo/:sijoitteluajoOid/hakemus/:hakemusOid", operation(getHakemusBySijoitteluajoSwagger)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    val hakemusOid = params("hakemusOid")
    // TODO Ok(sijoitteluService.getHakemusBySijoitteluajo(hakuOid, sijoitteluajoOid, hakemusOid))
    NotImplemented()
  }

  lazy val getHakukohdeBySijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("getHakukohdeBySijoitteluajoSwagger")
    summary "Hakee hakukohteen tiedot tietyssa sijoitteluajossa."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana.")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen yksilöllinen tunniste"))
  get("/sijoittelu/:hakuOid/sijoitteluajo/:sijoitteluajoOid/hakukohde/:hakukohdeOid", operation(getHakukohdeBySijoitteluajoSwagger)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    val hakukohdeOid = params("hakukohdeOid")
    // TODO Ok(sijoitteluService.getHakukohdeBySijoitteluajo(hakuOid, sijoitteluajoOid, hakukohdeOid))
    NotImplemented()
  }

  lazy val getHakukohdeErillissijoitteluSwagger: OperationBuilder = (apiOperation[Unit]("getHakukohdeErillissijoitteluSwagger")
    summary "Hakee hakukohteen erillissijoittelun tiedot tietyssa sijoitteluajossa."
    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana.")
    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen yksilöllinen tunniste"))
  get("/erillissijoittelu/:hakuOid/sijoitteluajo/:sijoitteluajoId/hakukohde/:hakukohdeOid", operation(getHakukohdeErillissijoitteluSwagger)) {
    val hakuOid = params("hakuOid")
    val sijoitteluajoOid = params("sijoitteluajoOid")
    val hakukohdeOid = params("hakukohdeOid")
    //TODO Ok(sijoitteluService.getHakukohdeErillissijoittelu(hakuOid, sijoitteluajoOid, hakukohdeOid))
    NotImplemented()
  }

// TODO not in use?
//  lazy val getSijoitteluajoHyvaksytytSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoHyvaksytytSwagger")
//    summary "Sivutettu listaus hakemuksien/hakijoiden listaukseen. Yksityiskohtainen listaus kaikista hakutoiveista ja niiden valintatapajonoista."
//    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste"))
//  get("/sijoittelu/:hakuOid/hyvaksytyt", operation(getSijoitteluajoHyvaksytytSwagger)) {
//    val hakuOid = params("hakuOid")
//    //TODO Ok(sijoitteluService.getSijoitteluajoHyvaksytyt(hakuOid))
//    NotImplemented()
//  }

//  lazy val getSijoitteluajoHyvaksytytByHakukohdeSwagger: OperationBuilder = (apiOperation[Unit]("getSijoitteluajoHyvaksytytByHakukohdeSwagger")
//    summary "Sivutettu listaus hakemuksien/hakijoiden listaukseen. Yksityiskohtainen listaus kaikista hakutoiveista ja niiden valintatapajonoista hakukohteen perusteella."
//    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
//    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen yksilöllinen tunniste"))
//  get("/sijoittelu/:hakuOid/hyvaksytyt/hakukohde/:hakukohdeOid", operation(getSijoitteluajoHyvaksytytByHakukohdeSwagger)) {
//    val hakuOid = params("hakuOid")
//    val hakukohdeOid = params("hakukohdeOid")
//    //TODO Ok(sijoitteluService.getSijoitteluajoHyvaksytytByHakukohde(hakuOid, hakukohdeOid))
//    NotImplemented()
//  }

//  lazy val getHakukohdeDtoBySijoitteluajoSwagger: OperationBuilder = (apiOperation[Unit]("getHakukohdeDtoBySijoitteluajoSwagger")
//    summary "Hakee hakukohteen tiedot tietyssa sijoitteluajossa."
//    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
//    parameter pathParam[String]("sijoitteluajoOid").description("Sijoitteluajon yksilöllinen tunniste, tai 'latest' avainsana.")
//    parameter pathParam[String]("hakukohdeOid").description("Hakukohteen yksilöllinen tunniste"))
//  get("/sijoittelu/:hakuOid/sjoitteluajo/:sijoitteluajoOid/hakukohdedto/:hakukohdeOid", operation(getHakukohdeDtoBySijoitteluajoSwagger)) {
//    val hakuOid = params("hakuOid")
//    val sijoitteluajoOid = params("sijoitteluajoOid")
//    val hakukohdeOid = params("hakukohdeOid")
//    //TODO Ok(sijoitteluService.getHakukohdeDtoBySijoitteluajo(hakuOid, sijoitteluajoOid, hakukohdeOid))
//    NotImplemented()
//  }

//  lazy val valintatapajonoIsInSijoitteluSwagger: OperationBuilder = (apiOperation[Unit]("valintatapajonoIsInSijoitteluSwagger")
//    summary "Kertoo jos valintatapajono on sijoittelun käytössä."
//    parameter pathParam[String]("hakuOid").description("Haun yksilöllinen tunniste")
//    parameter pathParam[String]("valintatapajonoOid").description("Valintatapajonon yksilöllinen tunniste"))
//  get("/sijoittelu/:hakuOid/valintatapajono-in-use/:valintatapajonoOid", operation(valintatapajonoIsInSijoitteluSwagger)) {
//    val hakuOid = params("hakuOid")
//    val valintatapajonoOid = params("valintatapajonoOid")
//    //TODO Ok(sijoitteluService.valintatapajonoIsInSijoittelu(hakuOid, valintatapajonoOid))
//    NotImplemented()
//  }
}

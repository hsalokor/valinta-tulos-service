package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.ValintatulosService
import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixtures, HakutoiveFixture}
import fi.vm.sade.valintatulosservice.tarjonta.HakuService

object PerformanceTester extends App {
  implicit val appConfig: AppConfig = AppConfig.fromSystemProperty
  val hakuService = HakuService(appConfig)
  appConfig.start
  ExampleFixture(appConfig)

  HakemusFixtures().importTemplateFixture(ExampleFixture.hakemusOid, List(HakutoiveFixture(1, ExampleFixture.tarjoajaOid, ExampleFixture.hakukohdeOid)))

  val haku = hakuService.getHaku(ExampleFixture.hakuOid).get
  println(appConfig.sijoitteluContext.sijoittelutulosService.hakemuksenTulos(haku, ExampleFixture.hakemusOid))

  println(new ValintatulosService(hakuService).hakemuksentulos(ExampleFixture.hakuOid, ExampleFixture.hakemusOid))
}
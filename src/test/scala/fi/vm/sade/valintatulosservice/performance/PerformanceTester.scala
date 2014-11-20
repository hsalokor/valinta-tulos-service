package fi.vm.sade.valintatulosservice.performance

import fi.vm.sade.valintatulosservice.config.AppConfig
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import fi.vm.sade.valintatulosservice.tarjonta.HakuService

object PerformanceTester extends App {
  implicit val appConfig: AppConfig = AppConfig.fromSystemProperty
  appConfig.start
  ExampleFixture(appConfig)
  val haku = HakuService(appConfig).getHaku(ExampleFixture.hakuOid).get
  println(appConfig.sijoitteluContext.sijoittelutulosService.hakemuksenTulos(haku, ExampleFixture.hakemusOid))
}
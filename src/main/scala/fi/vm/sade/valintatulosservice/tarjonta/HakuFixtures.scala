package fi.vm.sade.valintatulosservice.tarjonta

object HakuFixtures {
  val defaultHakuOids = List("1.2.246.562.5.2013080813081926341928")
  var hakuOids = defaultHakuOids
  val korkeakouluYhteishaku = "korkeakoulu-yhteishaku"
  val korkeakouluErillishaku = "korkeakoulu-erillishaku"
  val toinenAsteYhteishaku = "toinen-aste-yhteishaku"
  val toinenAsteErillishakuEiSijoittelua = "toinen-aste-erillishaku-ei-sijoittelua"
  var activeFixture = korkeakouluYhteishaku
}

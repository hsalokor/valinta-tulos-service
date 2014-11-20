package fi.vm.sade.valintatulosservice.tarjonta

import org.json4s.jackson.JsonMethods._

object HakuFixtures extends HakuService with JsonHakuService {
  val defaultHakuOid = "1.2.246.562.5.2013080813081926341928"
  val korkeakouluYhteishaku = "korkeakoulu-yhteishaku"
  val korkeakouluErillishaku = "korkeakoulu-erillishaku"
  val toinenAsteYhteishaku = "toinen-aste-yhteishaku"
  val toinenAsteErillishakuEiSijoittelua = "toinen-aste-erillishaku-ei-sijoittelua"

  private var hakuOid = defaultHakuOid
  private var activeFixture = korkeakouluYhteishaku

  def useFixture(fixtureName: String, hakuOid: String = defaultHakuOid) {
    this.hakuOid = hakuOid
    this.activeFixture = fixtureName
  }

  override def getHaku(oid: String) = {
    val fileName = "/fixtures/tarjonta/haku/" + HakuFixtures.activeFixture + ".json"
    Option(getClass.getResourceAsStream(fileName))
      .map(io.Source.fromInputStream(_).mkString)
      .map { response =>
      val hakuTarjonnassa = (parse(response) \ "result").extract[HakuTarjonnassa]
      hakuTarjonnassa.toHaku.copy(oid = oid)
    }
  }

  override def kaikkiHaut = getHaku(hakuOid).toList
}

package fi.vm.sade.valintatulosservice.tarjonta

import org.json4s.jackson.JsonMethods._

object HakuFixtures extends HakuService with JsonHakuService {
  val defaultHakuOid = "1.2.246.562.5.2013080813081926341928"
  val korkeakouluYhteishaku = "korkeakoulu-yhteishaku"
  val korkeakouluLisahaku1 = "korkeakoulu-lisahaku1"
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

  override def findLiittyvatHaut(haku: Haku) = {
    haku.varsinaisenHaunOid match {
      case Some(parent) => {
        val oldFixture = this.activeFixture
        try {
          this.activeFixture = parent
          super.findLiittyvatHaut(haku)
        }
        finally {
          this.activeFixture = oldFixture
        }
      }
      case None => super.findLiittyvatHaut(haku)
    }
  }

  override def kaikkiHaut = getHaku(hakuOid).toList
}

package fi.vm.sade.valintatulosservice.tarjonta

import java.io.InputStream

import fi.vm.sade.valintatulosservice.domain.Kausi
import org.json4s.jackson.JsonMethods._

object HakuFixtures extends HakuService with JsonHakuService {
  val defaultHakuOid = "1.2.246.562.5.2013080813081926341928"
  val korkeakouluYhteishaku = "korkeakoulu-yhteishaku"
  val korkeakouluLisahaku1 = "korkeakoulu-lisahaku1"
  val korkeakouluErillishaku = "korkeakoulu-erillishaku"
  val toinenAsteYhteishaku = "toinen-aste-yhteishaku"
  val toinenAsteErillishakuEiSijoittelua = "toinen-aste-erillishaku-ei-sijoittelua"

  private var hakuOids = List(defaultHakuOid)
  private var activeFixture = korkeakouluYhteishaku

  def useFixture(fixtureName: String, hakuOids: List[String] = List(defaultHakuOid)) {
    this.hakuOids = hakuOids
    this.activeFixture = fixtureName
  }

  override def getHaku(oid: String) = {
    getHakuFixture(oid).map(toHaku(_).copy(oid = oid))
  }

  private def getHakuFixture(oid: String): Option[HakuTarjonnassa] = {
    getHakuFixtureAsStream(oid)
      .map(io.Source.fromInputStream(_).mkString)
      .map { response =>
        (parse(response) \ "result").extract[HakuTarjonnassa]
    }
  }

  private def getHakuFixtureAsStream(oid: String): Option[InputStream] = {
    val default = getFixtureAsStream(oid)
    if(default.isDefined) {
      default
    }
    else {
      getFixtureAsStream(activeFixture)
    }
  }

  private def getFixtureAsStream(baseFilename: String) = {
    Option(getClass.getResourceAsStream("/fixtures/tarjonta/haku/" + baseFilename + ".json"))
  }

  override def kaikkiJulkaistutHaut = {
    hakuOids.flatMap { hakuOid =>
      getHakuFixture(hakuOid).toList.filter {_.julkaistu}.map(toHaku(_).copy(oid = hakuOid))
    }
  }

  override def getKoulutuksenAlkamiskausi(hakukohde: Hakukohde): Option[Kausi] = ???

  override def getHakukohde(oid: String): Option[Hakukohde] = ???
}

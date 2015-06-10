package fi.vm.sade.valintatulosservice.sijoittelu

import com.mongodb.DB
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData

case class SijoitteluFixtures(db: DB) {
  def importFixture(fixtureName: String, clear: Boolean = false) {
    if (clear) {
      clearFixtures
    }
    val tulokset = MongoMockData.readJson("fixtures/sijoittelu/" + fixtureName)
    MongoMockData.insertData(db, tulokset)
  }

  def clearFixtures {
    MongoMockData.clear(db)
    val base = MongoMockData.readJson("fixtures/sijoittelu/sijoittelu-basedata.json")
    MongoMockData.insertData(db, base)
  }
}

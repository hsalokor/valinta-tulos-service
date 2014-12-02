package fi.vm.sade.valintatulosservice.sijoittelu

import com.mongodb.DB
import fi.vm.sade.sijoittelu.tulos.testfixtures.MongoMockData

object SijoitteluFixtures {
  def importFixture(db: DB, fixtureName: String, clear: Boolean = false) {
    if (clear) {
      MongoMockData.clear(db)
      val base = MongoMockData.readJson("fixtures/sijoittelu/sijoittelu-basedata.json")
      MongoMockData.insertData(db, base)
    }
    val tulokset = MongoMockData.readJson("fixtures/sijoittelu/" + fixtureName)
    MongoMockData.insertData(db, tulokset)
  }
}

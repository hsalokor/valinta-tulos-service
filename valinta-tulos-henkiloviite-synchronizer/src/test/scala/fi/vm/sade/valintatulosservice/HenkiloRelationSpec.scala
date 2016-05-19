package fi.vm.sade.valintatulosservice

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HenkiloRelationSpec extends Specification {
  "relatedHenkilot" >> {
    "returns a seq per master oid" >> {
      val related = HenkiloviiteSynchronizer.relatedHenkilot(
        List(
          Henkiloviite("master1", "slave1"),
          Henkiloviite("master2", "slave2")
        )
      )
      related.find(_.contains("master1")) must beSome[Seq[String]]
      related.find(_.contains("master2")) must beSome[Seq[String]]
      related.find(_.contains("master1")).get must contain("slave1")
      related.find(_.contains("master2")).get must contain("slave2")
    }
    "returns a seqs that contain master with all slaves" >> {
      val related = HenkiloviiteSynchronizer.relatedHenkilot(
        List(
          Henkiloviite("master1", "slave1"),
          Henkiloviite("master1", "slave2"),
          Henkiloviite("master2", "slave3")
        )
      )
      val master1 = related.find(_.contains("master1"))
      master1 must beSome[Seq[String]]
      master1.get must contain("slave2")
      master1.get must not contain("slave3")
    }
  }
  "allPairs" >> {
    val pairs = HenkiloviiteSynchronizer.allPairs(List("A", "B", "C"))
    pairs must contain(("A", "B"))
    pairs must contain(("B", "A"))
    pairs must contain(("A", "C"))
    pairs must contain(("C", "A"))
    pairs must contain(("B", "C"))
    pairs must contain(("C", "B"))
  }
  "henkiloRelations" >> {
    val relations = HenkiloviiteSynchronizer.henkiloRelations(List(
      Henkiloviite("master1", "slave1"),
      Henkiloviite("master1", "slave2"),
      Henkiloviite("master1", "slave3"),
      Henkiloviite("master2", "slave4"),
      Henkiloviite("master2", "slave5"),
      Henkiloviite("master3", "slave6")
    ))
    relations must contain(HenkiloRelation("master1", "slave1"))
    relations must contain(HenkiloRelation("slave1", "master1"))
    relations must contain(HenkiloRelation("master1", "slave2"))
    relations must contain(HenkiloRelation("slave2", "master1"))
    relations must contain(HenkiloRelation("master1", "slave3"))
    relations must contain(HenkiloRelation("slave3", "master1"))

    relations must contain(HenkiloRelation("master2", "slave4"))
    relations must contain(HenkiloRelation("slave4", "master2"))
    relations must contain(HenkiloRelation("master2", "slave5"))
    relations must contain(HenkiloRelation("slave5", "master2"))

    relations must contain(HenkiloRelation("master3", "slave6"))
    relations must contain(HenkiloRelation("slave6", "master3"))
  }
}

package fi.vm.sade.valintatulosservice.domain

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OidValidatorSpec extends Specification {
  "OidValidator" should {
    "recognize strings with only numbers and letters as oids" in {
      OidValidator.isOid("1.2.3") must_== true
      OidValidator.isOid("1.1") must_== true
    }
    "recognize clearly non-oid strings as not oids" in {
      OidValidator.isOid("1") must_== false
      OidValidator.isOid(".") must_== false
      OidValidator.isOid("1.") must_== false
      OidValidator.isOid(".1") must_== false
      OidValidator.isOid("a") must_== false
      OidValidator.isOid("1.a.2") must_== false
      OidValidator.isOid(";-- drop table students") must_== false
    }
  }
}

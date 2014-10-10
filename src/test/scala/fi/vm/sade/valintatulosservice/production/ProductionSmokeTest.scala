package fi.vm.sade.valintatulosservice.production

import fi.vm.sade.valintatulosservice.http.DefaultHttpClient
import org.specs2.mutable.Specification

class ProductionSmokeTest extends Specification {

  val hakuOid = "1.2.246.562.5.2014022711042555034240"
  val hyväksytty = "1.2.246.562.11.00000923132"
  val perunut = "1.2.246.562.11.00000901332"
  val varalla = "1.2.246.562.11.00000887126"
  val url = "https://virkailija.opintopolku.fi/valinta-tulos-service/haku/" + hakuOid + "/hakemus/"

  "tuotantoympäristön valintatuloksissa " should {
    "hakija hakemuksella " + hyväksytty + " on hyväksytty ylempään hakutoiveeseen ja alempi on peruuntunut" in {
      val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url + hyväksytty).responseWithHeaders
      resultString must_== "{\"hakemusOid\":\"1.2.246.562.11.00000923132\",\"aikataulu\":{},\"hakutoiveet\":[{\"hakukohdeOid\":\"1.2.246.562.14.2014040212501070122979\",\"tarjoajaOid\":\"1.2.246.562.10.87157719573\",\"valintatila\":\"HYVAKSYTTY\",\"vastaanottotila\":\"VASTAANOTTANUT\",\"ilmoittautumistila\":\"LASNA_KOKO_LUKUVUOSI\",\"vastaanotettavuustila\":\"EI_VASTAANOTETTAVISSA\",\"viimeisinValintatuloksenMuutos\":\"2014-10-09T11:15:34Z\",\"jonosija\":1,\"julkaistavissa\":true},{\"hakukohdeOid\":\"1.2.246.562.14.2014030412260146204631\",\"tarjoajaOid\":\"1.2.246.562.10.74355268397\",\"valintatila\":\"PERUUNTUNUT\",\"vastaanottotila\":\"KESKEN\",\"ilmoittautumistila\":\"EI_TEHTY\",\"vastaanotettavuustila\":\"EI_VASTAANOTETTAVISSA\",\"julkaistavissa\":false}]}"
    }

    "hakija hakemuksella " + perunut + " on perunut paikan kahdessa ylimmässä hakutoiveessa ja alin on peruuntunut" in {
      val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url + perunut).responseWithHeaders
      resultString must_== "{\"hakemusOid\":\"1.2.246.562.11.00000901332\",\"aikataulu\":{},\"hakutoiveet\":[{\"hakukohdeOid\":\"1.2.246.562.14.2014040212501070122979\",\"tarjoajaOid\":\"1.2.246.562.10.87157719573\",\"valintatila\":\"PERUNUT\",\"vastaanottotila\":\"PERUNUT\",\"ilmoittautumistila\":\"EI_TEHTY\",\"vastaanotettavuustila\":\"EI_VASTAANOTETTAVISSA\",\"viimeisinValintatuloksenMuutos\":\"2014-10-09T11:15:36Z\",\"jonosija\":18,\"julkaistavissa\":true},{\"hakukohdeOid\":\"1.2.246.562.14.2014040911484746864599\",\"tarjoajaOid\":\"1.2.246.562.10.62617526421\",\"valintatila\":\"PERUNUT\",\"vastaanottotila\":\"PERUNUT\",\"ilmoittautumistila\":\"EI_TEHTY\",\"vastaanotettavuustila\":\"EI_VASTAANOTETTAVISSA\",\"viimeisinValintatuloksenMuutos\":\"2014-08-25T15:56:53Z\",\"jonosija\":17,\"julkaistavissa\":true},{\"hakukohdeOid\":\"1.2.246.562.14.2014032515364377020097\",\"tarjoajaOid\":\"1.2.246.562.10.20485193278\",\"valintatila\":\"PERUUNTUNUT\",\"vastaanottotila\":\"KESKEN\",\"ilmoittautumistila\":\"EI_TEHTY\",\"vastaanotettavuustila\":\"EI_VASTAANOTETTAVISSA\",\"julkaistavissa\":false}]}"
    }

    "hakija hakemuksella " + varalla + " on varasijalla ylemmässä hakutoiveessa ja alempi on kesken" in {
      val (responseCode, _, resultString) = DefaultHttpClient.httpGet(url + varalla).responseWithHeaders
      resultString must_== "{\"hakemusOid\":\"1.2.246.562.11.00000887126\",\"aikataulu\":{},\"hakutoiveet\":[{\"hakukohdeOid\":\"1.2.246.562.14.2014040212501070122979\",\"tarjoajaOid\":\"1.2.246.562.10.87157719573\",\"valintatila\":\"VARALLA\",\"vastaanottotila\":\"KESKEN\",\"ilmoittautumistila\":\"EI_TEHTY\",\"vastaanotettavuustila\":\"EI_VASTAANOTETTAVISSA\",\"viimeisinValintatuloksenMuutos\":\"2014-10-09T11:15:36Z\",\"jonosija\":32,\"varasijanumero\":1,\"julkaistavissa\":true},{\"hakukohdeOid\":\"1.2.246.562.14.2014032014424092013626\",\"tarjoajaOid\":\"1.2.246.562.10.16538823663\",\"valintatila\":\"KESKEN\",\"vastaanottotila\":\"KESKEN\",\"ilmoittautumistila\":\"EI_TEHTY\",\"vastaanotettavuustila\":\"EI_VASTAANOTETTAVISSA\",\"julkaistavissa\":false}]}"
    }
  }
}

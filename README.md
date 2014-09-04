valinta-tulos-service
=====================

Valintatuloksien REST-rajapinta, DRAFT

Tavoitteena luoda kaikkien hakujen valintatuloksille yhteinen rajapinta. 

Rajapinnan kautta voi valintatulosten lisäksi lisätä hakemuksen hakukohteelle vastaanottotieto.

Alkuvaiheessa rajapinta toteutetaan käyttäen tietovarastona sijoittelu-tietokantaa ja lisähauillle (jotka eivät sijoittelun piirissä) hakulomake-tietokantaa. Tavoitteena on jatkossa siirtää tulokset uuteen yhteiseen tietokantaan.

## GET /valinta-tulos-service/hakemus/1.2.3.4

Palauttaa hakemuksen valintatuloksen.

Huom! Tämä ehdotettu tietomalli on suora kopio sijoittelu-servicen yhteenveto-rajapinnasta.

```json
{
  "hakemusOid": "1.2.3.4",
  "hakutoiveet": [
    {
      "hakukohdeOid": "2.3.4.5",
      "tarjoajaOid": "3.4.5.6",
      "valintatila": "HYVAKSYTTY",
      "vastaanottotila": "ILMOITETTU",
      "ilmoittautumistila": null,
      "vastaanotettavuustila": "VASTAANOTETTAVISSA_SITOVASTI",
      "jonosija": 1,
      "varasijanumero": null
    }
  ]
}
```

## POST /valinta-tulos-service/hakemus/1.2.3.4/hakukohde/2.3.4.5/vastaanotto/VASTAANOTTANUT

Tallentaa vastaanottotiedon.

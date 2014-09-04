valinta-tulos-service
=====================

Valintatuloksien REST-rajapinta, DRAFT

Tavoitteena luoda kaikkien hakujen valintatuloksille yhteinen rajapinta. 

Rajapinnan kautta voi valintatulosten lisäksi lisätä hakemuksen hakukohteelle vastaanottotieto.

Alkuvaiheessa rajapinta toteutetaan käyttäen tietovarastona sijoittelu-tietokantaa ja lisähauillle (jotka eivät sijoittelun piirissä) hakulomake-tietokantaa. Tavoitteena on jatkossa siirtää tulokset uuteen yhteiseen tietokantaan.

## GET /valinta-tulos-service/haku/1.2.3.4/hakemus/5.6.7.8

Palauttaa hakemuksen valintatuloksen.

Huom! Tämä ehdotettu tietomalli on suora kopio sijoittelu-servicen yhteenveto-rajapinnasta.

```json
{
  "hakemusOid": "1.2.246.562.11.00000441369",
  "hakutoiveet": [
    {
      "hakukohdeOid": "1.2.246.562.5.72607738902",
      "tarjoajaOid": "1.2.246.562.10.591352080610",
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

## POST /valinta-tulos-service/haku/1.2.3.4/hakemus/5.6.7.8/hakukohde/2.3.4.5/vastaanotto/VASTAANOTTANUT

Tallentaa vastaanottotiedon.

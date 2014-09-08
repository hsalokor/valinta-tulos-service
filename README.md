valinta-tulos-service
=====================

Valintatuloksien REST-rajapinta, DRAFT

Tavoitteena luoda kaikkien hakujen valintatuloksille yhteinen rajapinta. 

Rajapinnan kautta voi valintatulosten lisäksi lisätä hakemuksen hakukohteelle vastaanottotieto.

Alkuvaiheessa rajapinta toteutetaan käyttäen tietovarastona sijoittelu-tietokantaa ja lisähauillle (jotka eivät sijoittelun piirissä) hakulomake-tietokantaa. Tavoitteena on jatkossa siirtää tulokset uuteen yhteiseen tietokantaan.

## GET /valinta-tulos-service/haku/0.1.2.3/hakemus/1.2.3.4

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

## SBT-buildi

### Generoi projekti

Eclipseen:

`./sbt eclipse`

... tai IDEAan:

`./sbt 'gen-idea no-sbt-build-module'`

### Yksikkötestit

`./sbt test`

### War-paketointi

`./sbt package`

### Käännä ja käynnistä (aina muutosten yhteydessä automaattisesti) ##

```sh
$ ./sbt
> ~container:start
```

Avaa selaimessa [http://localhost:8080/valinta-tulos-service/](http://localhost:8080/valinta-tulos-service/).

### Käynnistä IDEAsta/Eclipsestä

Aja TomcatRunner-luokka.

### Asetukset

Sovellus tukee eri profiileita. Profiili määritellään `valintatulos.profile` system propertyllä, esim `-Dvalintatulos.profile=dev`.
Profiili määrittää lähinnä, mistä propertyt haetaan, mutta sen avulla myös voidaan mockata palveluita. Ks `AppConfig.scala`.

### dev-profiili

Näillä asetuksilla käytetään mockattuja ulkoisia järjestelmiä. kehityskäyttöön soveltuvat arvot ovat `dev.conf` tiedostossa versionhallinnassa.

### default-profiili

Oletusasetuksilla käytetään ulkoista konfiguraatiotiedostoa `valinta-tulos-service.properties`. `valinta-tulos-service.properties` tiedoston etsintäjärjestys:
`valinta-tulos-service.configFile` system property  - kehityksessä IDE:stä käytettävä tapa, jos haluaa ajaa eri asetuksilla serveriä
`~/oph-configuration/valinta-tulos-service.properties` - sovelluspalvelimilla  käytettävä tapa

### templated-profiili

Templated profiililla voi käyttää konfiguraatiota, jossa template-konfiguraatioon asettaan arvot ulkoisesta konfiguraatiosta. Käytä system propertyä `-Dvalintatulos.profile=templated`
ja aseta muuttujat sisältävän tiedoston sijainti system propertyssä, esim. `-Dvalintatulos.vars={HAKEMISTO}/oph_vars.yml` - mallia vars-tiedostoon voi ottaa tiedostosta `src/main/resources/oph-configuration/dev-vars.yml`

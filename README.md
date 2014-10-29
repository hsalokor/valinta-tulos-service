valinta-tulos-service
=====================

Valintatuloksien REST-rajapinta, DRAFT

Tavoitteena luoda kaikkien hakujen valintatuloksille yhteinen rajapinta.

Rajapinnan kautta voi valintatulosten lisäksi lisätä hakemuksen hakukohteelle vastaanottotieto.

Alkuvaiheessa rajapinta toteutetaan käyttäen tietovarastona sijoittelu-tietokantaa. Tavoitteena on jatkossa siirtää tulokset uuteen yhteiseen tietokantaan.

## SBT-buildi

### Generoi projekti

Eclipseen:

`./sbt eclipse`

... tai IDEAan:

`./sbt 'gen-idea no-sbt-build-module'`

### Yksikkötestit

`./sbt test`

testit on jaettu ympäristöjen mukaan alipaketteihin.
Esim. jos haluat ajaa vain lokaali testit niin aja:
`sbt "testOnly fi.vm.sade.valintatulosservice.local.*"`

### War-paketointi

`./sbt package`

### Käynnistä IDEAsta/Eclipsestä

Aja JettyLauncher-luokka.

IT-profiililla, eli embedded mongo-kannalla: `-Dvalintatulos.profile=it`

externalHakemus-profiililla omatsivut-mocha-testien ajamista varten: `-Dvalintatulos.profile=it-externalHakemus`

### Käynnistä komentoriviltä

IT-profiililla, eli embedded mongo-kannalla

`./sbt "test:run-main fi.vm.sade.valintatulosservice.JettyLauncher" -Dvalintatulos.profile=it`

externalHakemus-profiililla omatsivut-mocha-testien ajamista varten

`./sbt "test:run-main fi.vm.sade.valintatulosservice.JettyLauncher" -Dvalintatulos.profile=it-externalHakemus`

### Avaa selaimessa

Avaa selaimessa http://localhost:8097/valinta-tulos-service/

### Asetukset

Sovellus tukee eri profiileita. Profiili määritellään `valintatulos.profile` system propertyllä, esim `-Dvalintatulos.profile=dev`.
Profiili määrittää lähinnä, mistä propertyt haetaan, mutta sen avulla myös voidaan mockata palveluita. Ks `AppConfig.scala`.

### it-profiili

Käytetään embedded mongoa, johon syötetään fixtuuridataa. Tätä käytetään myös automaattisissa testeissä kuten `ValintaTulosServletSpec`.

### dev-profiili

Näillä asetuksilla käytetään lokaalia mongo-kantaa.

### default-profiili

Oletusasetuksilla käytetään ulkoista konfiguraatiotiedostoa `~/oph-configuration/valinta-tulos-service.properties`.

### templated-profiili

Templated profiililla voi käyttää konfiguraatiota, jossa template-konfiguraatioon asettaan arvot ulkoisesta konfiguraatiosta. Käytä system propertyä `-Dvalintatulos.profile=templated`
ja aseta muuttujat sisältävän tiedoston sijainti system propertyssä, esim. `-Dvalintatulos.vars={HAKEMISTO}/oph_vars.yml` - mallia vars-tiedostoon voi ottaa tiedostosta `src/main/resources/oph-configuration/dev-vars.yml`

## API-dokumentaatio

Swaggerilla generoitu dokomentaatio.

[http://localhost:8097/valinta-tulos-service/api-docs/index.html](http://localhost:8097/valinta-tulos-service/api-docs/index.html)

## Urleja

Urleja lokaaliin testaukseen eri konfiguraatioilla

```
Luokka: http://localhost:8097/valinta-tulos-service/haku/1.2.246.562.29.92478804245/hakemus/1.2.246.562.11.00000441369
Reppu (plain): http://localhost:8097/valinta-tulos-service/haku/1.2.246.562.5.2014022413473526465435/hakemus/1.2.246.562.11.00000442406
Reppu (CAS, korvaa tiketti uudella): http://localhost:8097/valinta-tulos-service/cas/haku/1.2.246.562.5.2014022413473526465435/hakemus/1.2.246.562.11.00000442406?ticket=xxx
QA: https://testi.virkailija.opintopolku.fi/valinta-tulos-service/haku/1.2.246.562.29.173465377510/hakemus/1.2.246.562.11.00001021871
QA (CAS, korvaa tiketti uudella): https://testi.virkailija.opintopolku.fi/valinta-tulos-service/cas/haku/1.2.246.562.29.173465377510/hakemus/1.2.246.562.11.00001021871?ticket=xxx
```
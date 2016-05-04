valinta-tulos-service
=====================

Valintatuloksien REST-rajapinta, DRAFT

Tavoitteena luoda kaikkien hakujen valintatuloksille yhteinen rajapinta.

Rajapinnan kautta voi valintatulosten lisäksi lisätä hakemuksen hakukohteelle vastaanottotieto.

Alkuvaiheessa rajapinta toteutetaan käyttäen tietovarastona sijoittelu-tietokantaa. Tavoitteena on jatkossa siirtää tulokset uuteen yhteiseen tietokantaan.

## Maven-buildi

### Testit

Aja kaikki testit

`mvn test`

Aja vain paikalliset testit (ei tuotantoa tai testiympäristöä vasten):

`mvn test '-Dtest=fi.vm.sade.valintatulosservice.local.**'`

### War-paketointi

`mvn package`

### Käynnistä IDEAsta/Eclipsestä

Aja JettyLauncher-luokka.

IT-profiililla, eli embedded mongo-kannalla: `-Dvalintatulos.profile=it`

externalHakemus-profiililla omatsivut-mocha-testien ajamista varten: `-Dvalintatulos.profile=it-externalHakemus`

### Käynnistä komentoriviltä

IT-profiililla, eli embedded mongo-kannalla

`mvn exec:java -Dvalintatulos.profile=it`

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

## Vastaanottosähköpostit

Palvelu `valinta-tulos-emailer` käyttää valinta-tulos-serviceä hakemaan listan lähetettävistä vastaanottosähköposteista. Ks MailPoller.scala.

Yksinkertaistetusti pollauksessa haetaan ensimmäisessä vaiheessa joukko kandidaattituloksia Valintatulos-collectionista (sijoittelun mongossa). Kandidaatteihin merkitään `mailStatus.previousCheck` -kenttään aikaleima, jonka avulla samat kandidaatit blokataan seuraavista kyselyistä.

Tarkistusaikaleimojen nollauksen voi tehdä mongoon seuraavasti (muokkaa minimiaikaleima sopivaksi):

    db.Valintatulos.update({"mailStatus.previousCheck": {"$gte": ISODate("2015-07-20T18:00:00.000Z")}}, {$unset: {"mailStatus.previousCheck": ""}}, {multi:true})

## Paikallinen PostgreSQL-kanta opiskelijavalintarekisteriä varten

Tyhjän testikannan ensimmäistä käynnistystä varten voi luoda esimerkiksi seuraavasti (vaatii PostgreSQL-clientin sekä Dockerin ja mahdollisesti boot2docker:in).
`-h` on Windows/OS X ympäristössä `boot2docker ip`:n antama osoite, Linuxilla `localhost`. `-p` kertoo mitä porttia kanta kuuntelee.

Muista konfiguroida käynnistyksessä annettavaan `.properties`-tiedostoon pystyttämäsi testikannan tiedot.

1. `docker run -p 5432:5432 postgres`
2. `psql -h192.168.59.103 -p5432 -Upostgres postgres -c "CREATE DATABASE valintarekisteri;"`
3. `psql -h192.168.59.103 -p5432 -Upostgres postgres -c "CREATE ROLE OPH;"`

Embedattu tietokanta käynnistyy sovelluksessa ja vaatii paikallisen PostgreSQL:n ja lisäksi 
* polkuun monta komentoa (esim /usr/lib/postgresql/9.5/bin/ :inistä)
** postgres
** initdb
** createdb
** dropdb
* todennäköisesti käyttäjäsi tulee olla postgres-ryhmässä, jotta lukkotiedoston kirjoittaminen onnistuu (tämä selviää kokeilemalla)

## TODO: Tietokanta

Palvelinympäristöissä tietokantaa on tarkoitus käyttää oph-nimisellä roolilla.


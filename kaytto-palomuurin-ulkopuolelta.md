## Palvelun käyttö OPH:n palomuurin ulkopuolelta

Osa palvelusta on käytettävissä myös OPH:n palomuurin ulkopuolelta. Palomuurin ulkopuolinen käyttö vaatii CAS-tiketin käyttöä.

Tuotannossa CAS-suojattu base-UR on

    https://virkailija.opintopolku.fi/valinta-tulos-service/cas/haku

Ja QA-ympäristössä

    https://testi.virkailija.opintopolku.fi/valinta-tulos-service/cas/haku

Alla muutama esimerkki, joissa toimitaan QA-ympäristössä.
Niissä käytetty "http" -komento on [httpie](http://httpie.org/) . curl, Postman tms käy yhtä hyvin POST-pyyntöjen tekemiseen.
Ajettava esimerkki service ticketin hausta on [oheisessa skriptissä](./get-cas-ticket.bash)

## Esimerkki 1: hakemuksen tila

1. Haetaan ticket granting ticket

    http --pretty none --form --print h POST https://testi.virkailija.opintopolku.fi/cas/v1/tickets username=$USERNAME password=$PASSWORD

    =>

    HTTP-vastauksen Location-headerissä on ticket granting ticketin URL

2. Haetaan service ticket

    http --form --print b POST https://testi.virkailija.opintopolku.fi/cas/v1/tickets/TGT-1769606-cJ0FKe945kjDtRZga9mDyQPZTvXLabsZAEfXFH2fAT7ePxQBSn-cas.koe service=https://testi.virkailija.opintopolku.fi/valinta-tulos-service

    =>

    HTTP-vastauksen bodyssä on service ticket

3. Kutsutaan palvelua

    https://testi.virkailija.opintopolku.fi/valinta-tulos-service/cas/haku/<haku-id>/hakemus/<hakemus-id>?ticket=<ticket>

## Esimerkki 2: ilmoittautuminen koulutukseen

1. Haetaan ticket granting ticket

2. Haetaan service ticket

3. Kutsutaan palvelua

POST-pyynnön URL

    https://testi.virkailija.opintopolku.fi/valinta-tulos-service/cas/haku/<haku-id>/hakemus/<hakemus-id>/ilmoittaudu

POST-pyynnön sisältö

```json
{"hakukohdeOid":"<hakukohde>","tila":"LASNA_KOKO_LUKUVUOSI","muokkaaja":"henkilö:<oid>","selite":"Ilmoittautuminen Oili palvelussa"}
```

Service ticket toimitetaan `ticket` headerissa.

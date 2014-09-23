package fi.vm.sade.valintatulosservice

import org.scalatra.swagger.{ApiInfo, Swagger}

class ValintatulosSwagger extends Swagger(
	Swagger.SpecVersion,
    BuildInfo.version,
    ApiInfo("valintatulosservice",
            "Valintojen tulos palvelu",
            "https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/",
            "verkkotoimitus_opintopolku@oph.fi",
            "EUPL 1.1 or latest approved by the European Commission",
            "http://www.osor.eu/eupl/"))


package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.domain.Vastaanottotila.Vastaanottotila

case class Vastaanotto(hakukohdeOid: String, tila: Vastaanottotila, muokkaaja: String, selite: String, personOid: String)

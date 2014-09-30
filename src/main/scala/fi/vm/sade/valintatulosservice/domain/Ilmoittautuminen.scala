package fi.vm.sade.valintatulosservice.domain

import fi.vm.sade.valintatulosservice.domain.Ilmoittautumistila.Ilmoittautumistila

case class Ilmoittautuminen(hakukohdeOid: String, tila: Ilmoittautumistila, muokkaaja: String, selite: String)

package fi.vm.sade.valintatulosservice.domain

case class Hakutoive(oid: String, tarjoajaOid: String)
case class Hakemus(oid: String, toiveet: List[Hakutoive])

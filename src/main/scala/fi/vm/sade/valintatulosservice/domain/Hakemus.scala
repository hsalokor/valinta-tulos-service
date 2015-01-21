package fi.vm.sade.valintatulosservice.domain

case class Hakutoive(oid: String, tarjoajaOid: String)
case class Hakemus(oid: String, henkiloOid: String, asiointikieli: String, toiveet: List[Hakutoive], henkilotiedot: Henkilotiedot)
case class Henkilotiedot(kutsumanimi: Option[String], email: Option[String], hasHetu: Boolean)

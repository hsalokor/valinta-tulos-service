package fi.vm.sade.valintatulosservice.domain

import java.util.Date

case class Sijoitteluajo(sijoitteluajoId:Long, hakuOid:String, startMils:Long, endMils:Long)

case class HakijaRecord(etunimi:String, sukunimi:String, hakemusOid:String, hakijaOid:String)
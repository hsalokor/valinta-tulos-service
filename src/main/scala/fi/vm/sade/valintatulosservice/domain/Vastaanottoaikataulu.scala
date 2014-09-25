package fi.vm.sade.valintatulosservice.domain

import java.util.Date

case class Vastaanottoaikataulu(vastaanottoEnd: Option[Date], vastaanottoBufferDays: Option[Int])

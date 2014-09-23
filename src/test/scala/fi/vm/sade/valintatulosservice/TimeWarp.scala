package fi.vm.sade.valintatulosservice

import java.text.SimpleDateFormat

import org.joda.time.DateTimeUtils

trait TimeWarp {
  def getMillis(date: String) = {
    new SimpleDateFormat("d.M.yyyy").parse(date).getTime
  }

  def withFixedDate[T](date: String)(f: => T) = {
    DateTimeUtils.setCurrentMillisFixed(getMillis(date))
    try {
      f
    }
    finally {
      DateTimeUtils.setCurrentMillisSystem
    }
  }
}

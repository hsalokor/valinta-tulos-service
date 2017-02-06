package fi.vm.sade.valintatulosservice.logging

import java.util.concurrent.TimeUnit

import fi.vm.sade.utils.slf4j.Logging

trait PerformanceLogger extends Logging {

  def time[R](desciption:String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    logger.info(s"$desciption: ${TimeUnit.NANOSECONDS.toMillis(t1 - t0)} ms")
    result
  }
}

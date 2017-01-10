package fi.vm.sade.valintatulosservice.logging

import fi.vm.sade.utils.slf4j.Logging

trait PerformanceLogger extends Logging {

  def time[R](desciption:String)(block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    logger.info(s"${desciption}: ${t1 - t0} ms")
    result
  }
}

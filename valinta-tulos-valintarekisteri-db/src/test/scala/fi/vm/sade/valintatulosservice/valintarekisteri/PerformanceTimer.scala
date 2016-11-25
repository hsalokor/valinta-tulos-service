package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.utils.slf4j.Logging

trait PerformanceTimer extends Logging {
  def time[R](desciption:String = "Operation")(block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    logger.info(s"${desciption} took: ${t1 - t0} ms")
    result
  }
}

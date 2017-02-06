package fi.vm.sade.valintatulosservice.valintarekisteri.domain

import java.util.regex.Pattern

object OidValidator {
  private val oidPattern = Pattern.compile("""^[\d][\d\.]+[\d]$""")

  def isOid(s: String): Boolean = oidPattern.matcher(s).matches
}

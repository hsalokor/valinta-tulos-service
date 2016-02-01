package fi.vm.sade.valintatulosservice.domain

case class Kausi(year: Int, kausi: String) {
  def toKausiSpec = year.toString + kausi
}

object Kausi {
  private val validLastCharacters = Set("K", "S")
  def apply(kausiSpec: String): Kausi = {
    if (kausiSpec.length != 5) {
      throw new IllegalArgumentException(s"Bad kausi spefication '$kausiSpec'. Expected format YYYY(S|K), e.g. 2016S")
    }
    Kausi(parseYear(kausiSpec), parseKausi(kausiSpec))
  }

  private def parseYear(kausiSpec: String): Int = {
    try {
      val year = kausiSpec.substring(0, kausiSpec.length - 1)
      Integer.parseInt(year)
    } catch {
      case e: Exception => throw new IllegalArgumentException(s"Could not parse year from '$kausiSpec'. Expected format YYYY(S|K), e.g. 2016S", e)
    }
  }

  private def parseKausi(kausiSpec: String): String = {
    val lastCharacter = kausiSpec.substring(kausiSpec.length - 1)
    if (validLastCharacters.contains(lastCharacter)) {
      lastCharacter
    } else {
      throw new IllegalArgumentException(s"Illegal last character '$lastCharacter'. Valid values are $validLastCharacters")
    }
  }
}

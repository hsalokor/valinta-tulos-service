package fi.vm.sade.valintatulosservice

object HenkiloviiteSynchronizerAppRunner {
  def main(args: Array[String]): Unit = {
    System.setProperty("henkiloviite.properties", "src/test/resources/luokka.properties")
    System.setProperty("logback.access", "")
    HenkiloviiteSynchronizerApp.main(args)
  }
}

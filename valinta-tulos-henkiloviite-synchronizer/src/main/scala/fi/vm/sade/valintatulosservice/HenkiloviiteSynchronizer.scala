package fi.vm.sade.valintatulosservice

import scala.util.Try

class HenkiloviiteSynchronizer(henkiloClient: HenkiloviiteClient, db: Db) {
  def sync(): Try[Unit] = {
    for {
      henkiloviitteet <- henkiloClient.fetchHenkiloviitteet()
    } yield db.refresh(henkiloviitteet)
  }
}

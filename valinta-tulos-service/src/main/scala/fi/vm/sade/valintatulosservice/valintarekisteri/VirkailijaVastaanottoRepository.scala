package fi.vm.sade.valintatulosservice.valintarekisteri

import fi.vm.sade.valintatulosservice.domain.Kausi
import slick.dbio.DBIO

trait VirkailijaVastaanottoRepository extends VastaanottoRepository {
  def findHenkilonVastaanototHaussa(henkiloOid: String, hakuOid: String): DBIO[Set[VastaanottoRecord]]
  def findHaunVastaanotot(hakuOid: String): Set[VastaanottoRecord]
  def findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(kausi: Kausi): Set[VastaanottoRecord]
  def findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(kaudet: Set[Kausi]): Map[Kausi, Set[VastaanottoRecord]] =
    kaudet.map(kausi => kausi -> findkoulutuksenAlkamiskaudenVastaanottaneetYhdenPaikanSaadoksenPiirissa(kausi)).toMap
  def aliases(henkiloOid: String): DBIO[Set[String]]
}

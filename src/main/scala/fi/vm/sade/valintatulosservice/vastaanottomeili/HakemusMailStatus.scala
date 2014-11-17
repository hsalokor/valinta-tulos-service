package fi.vm.sade.valintatulosservice.vastaanottomeili

import fi.vm.sade.valintatulosservice.domain.Hakutoiveentulos

case class HakemusMailStatus(hakemusOid: String, hakukohteet: List[HakukohdeMailStatus])

case class HakukohdeMailStatus(hakukohdeOid: String, valintatapajonoOid: String, shouldMail: Boolean)
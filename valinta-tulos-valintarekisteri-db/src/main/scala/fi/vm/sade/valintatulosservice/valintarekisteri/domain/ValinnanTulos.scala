package fi.vm.sade.valintatulosservice.valintarekisteri.domain

case class ValinnanTulos(hakuOid: String,
                         hakukohdeOid: String,
                         valintatapajonoOid: String,
                         hakemusOid: String,
                         valinnantila: Valinnantila,
                         ehdollisestiHyvaksyttavissa: Boolean,
                         julkaistavissa: Boolean,
                         vastaanottotila: VastaanottoAction,
                         ilmoittautumistila: SijoitteluajonIlmoittautumistila)

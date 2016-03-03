package fi.vm.sade.valintatulosservice.domain

case class HakukohdeRecord(oid: String, hakuOid: String, yhdenPaikanSaantoVoimassa: Boolean,
                           kktutkintoonJohtava: Boolean, koulutuksenAlkamiskausi: Kausi)

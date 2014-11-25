package fi.vm.sade.valintatulosservice.fixture

import fi.vm.sade.sijoittelu.domain.HakemuksenTila

/**
 * Luo annetun määrän hakukohteita ja hakemuksia. Kaikki hakemukset hakevat kaikkiin hakukohteisiin.
 * Joka toinen on HYVÄKSYTTY, joka toinen HYLÄTTY
 *
 * @param hakukohteita
 * @param hakemuksia
 * @param hakuOid
 */
case class SimpleGeneratedHakuFixture(hakukohteita: Int, hakemuksia: Int, override val hakuOid: String = "1") extends GeneratedHakuFixture(hakuOid) {
  override val hakemukset: List[HakemuksenTulosFixture] = (1 to hakemuksia).map { hakemusNumero =>
    val hakutoiveet: List[HakemuksenHakukohdeFixture] = (1 to hakukohteita).map { hakukohdeNumero =>
      val hakukohdeOid = hakukohdeNumero.toString
      val tarjoajaOid = hakukohdeNumero.toString
      val totalIndex = (hakemusNumero-1) * hakukohteita + (hakukohdeNumero-1)
      val tila = if (totalIndex % 2 == 0) { HakemuksenTila.HYVAKSYTTY } else { HakemuksenTila.HYLATTY}

      HakemuksenHakukohdeFixture(tarjoajaOid, hakukohdeOid, jonot = List(ValintatapaJonoFixture(tila)))
    }.toList

    val hakemusOid = hakuOid + "." + hakemusNumero.toString
    HakemuksenTulosFixture(hakemusOid, hakutoiveet)
  }.toList
}


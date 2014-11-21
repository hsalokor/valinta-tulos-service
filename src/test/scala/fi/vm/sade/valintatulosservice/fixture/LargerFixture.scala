package fi.vm.sade.valintatulosservice.fixture

import fi.vm.sade.valintatulosservice.hakemus.{HakemusFixture, HakutoiveFixture}

class LargerFixture(hakukohteita: Int, hakemuksia: Int) extends GeneratedFixture {
  private val hakutoiveet: List[HakemuksenHakukohdeFixture] = (1 to hakukohteita).map { hakukohdeNumero =>
    val hakukohdeOid = hakukohdeNumero.toString
    val tarjoajaOid = hakukohdeNumero.toString
    HakemuksenHakukohdeFixture(tarjoajaOid, hakukohdeOid)
  }.toList

  override val hakemukset: List[HakemuksenTulosFixture] = (1 to hakemuksia).map { hakemusNumero =>
    val hakemusOid = hakemusNumero.toString
    HakemuksenTulosFixture(hakemusOid, hakutoiveet)
  }.toList
}


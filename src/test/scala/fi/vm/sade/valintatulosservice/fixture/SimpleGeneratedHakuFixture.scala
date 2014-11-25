package fi.vm.sade.valintatulosservice.fixture

case class SimpleGeneratedHakuFixture(hakukohteita: Int, hakemuksia: Int) extends GeneratedHakuFixture {
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


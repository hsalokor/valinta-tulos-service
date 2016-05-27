create or replace view newest_vastaanotto_events as
  select distinct on (henkilo, hakukohde)
    coalesce(henkiloviitteet.person_oid, vastaanotot.henkilo) as henkilo,
    haku_oid,
    hakukohde,
    action,
    ilmoittaja,
    timestamp,
    selite,
    kk_tutkintoon_johtava,
    koulutuksen_alkamiskausi,
    yhden_paikan_saanto_voimassa
  from vastaanotot
    join hakukohteet on hakukohteet.hakukohde_oid = vastaanotot.hakukohde
    left outer join henkiloviitteet on henkiloviitteet.linked_oid = vastaanotot.henkilo or henkiloviitteet.person_oid = vastaanotot.henkilo
  where deleted is null
  order by henkilo, hakukohde, vastaanotot.id desc;

alter view newest_vastaanotto_events owner to oph;

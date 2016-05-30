create or replace view newest_vastaanotto_events as
    select
    henkilo, haku_oid, hakukohde, action, ilmoittaja, timestamp, selite, kk_tutkintoon_johtava, koulutuksen_alkamiskausi, yhden_paikan_saanto_voimassa, id
    from (
        select henkilo, haku_oid, hakukohde, action, ilmoittaja, timestamp, selite, kk_tutkintoon_johtava, koulutuksen_alkamiskausi, yhden_paikan_saanto_voimassa, id
        from vastaanotot
            join hakukohteet on hakukohde_oid = vastaanotot.hakukohde
        where deleted is NULL
    union
        select henkiloviitteet.person_oid as henkilo, haku_oid, hakukohde, action, ilmoittaja, timestamp, selite, kk_tutkintoon_johtava, koulutuksen_alkamiskausi, yhden_paikan_saanto_voimassa, id
        from vastaanotot
            join hakukohteet on hakukohde_oid = vastaanotot.hakukohde
            join henkiloviitteet on vastaanotot.henkilo in (henkiloviitteet.linked_oid, henkiloviitteet.person_oid)
        where deleted is NULL
    ) as t;

alter view newest_vastaanotto_events owner to oph;

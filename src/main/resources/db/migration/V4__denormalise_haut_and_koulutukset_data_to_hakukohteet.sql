alter table hakukohteet drop constraint hakukohteet_haku_fk;
drop table haut;

alter table hakukohteet add column koulutuksen_alkamiskausi varchar;
update hakukohteet hk set koulutuksen_alkamiskausi =
  (select distinct alkamiskausi from koulutukset k
    join koulutushakukohde khk on khk."koulutusOid" = k."koulutusOid" and khk."hakukohdeOid" = hk."hakukohdeOid");
alter table hakukohteet alter column koulutuksen_alkamiskausi set not null;
alter table hakukohteet add constraint kausi_format check (((koulutuksen_alkamiskausi)::text ~ similar_escape('[0-9]{4}[KS]'::text, NULL::text)));

drop table koulutushakukohde;
drop table koulutukset;

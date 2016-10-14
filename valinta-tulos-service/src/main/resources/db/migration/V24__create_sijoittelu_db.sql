create table sijoitteluajot (
  id bigint not null primary key,
  hakuOid character varying not null,
  "start" timestamp with time zone not null default now(),
  "end" timestamp with time zone not null default now(),
  erillissijoittelu boolean not null default false,
  valisijoittelu boolean not null default false
);
alter table sijoitteluajot owner to oph;

create sequence sijoitteluajonHakukohteet_id start 1;
alter sequence sijoitteluajonHakukohteet_id owner to oph;

create table sijoitteluajonHakukohteet (
  id bigint primary key default nextval('sijoitteluajonHakukohteet_id'),
  sijoitteluajoId bigint not null references sijoitteluajot(id),
  hakukohdeOid character varying not null references hakukohteet(hakukohde_oid),
  tarjoajaOid character varying not null, --TODO tähän vain hakukohteet-tauluun? Tarvitaanko, voiko muuttua?
  kaikkiJonotSijoiteltu boolean not null,
  unique(sijoitteluajoId, hakukohdeOid)
);
alter table sijoitteluajonHakukohteet owner to oph;

create table valintatapajonot(
  oid character varying not null,
  sijoitteluajonHakukohdeId bigint not null references sijoitteluajonHakukohteet(id),
  nimi character varying not null,
  prioriteetti integer,
  aloituspaikat integer,
  alkuperaisetAloituspaikat integer,
  kaikkiEhdonTayttavatHyvaksytaan boolean,
  poissaOlevaTaytto boolean,
  eiVarasijatayttoa boolean,
  varasijat integer,
  varasijaTayttoPaivat integer,
  varasijojaTaytetaanAsti boolean,
  hyvaksytty integer,
  varalla integer,
  alinHyvaksyttyPistemaara character varying,
  PRIMARY KEY (oid, sijoitteluajonHakukohdeId)
);
alter table valintatapajonot owner to oph;

create sequence jonosijat_id start 1;
alter sequence jonosijat_id owner to oph;

create table jonosijat (
  id bigint primary key default nextval('jonosijat_id'),
  valintatapajonoOid character varying not null,
  sijoitteluajonHakukohdeId bigint not null,
  hakemusOid character varying not null,
  hakijaOid character varying not null,
  etunimi character varying not null,
  sukunimi character varying not null,
  prioriteetti integer,
  jonosija integer,
-- onkoMuuttunutViimeSijoittelussa boolean,
  pisteet integer,
  tasasijaJonosija integer,
-- edellinenTila character varying,
  hyvaksyttyHarkinnanvaraisesti boolean,
  hyvaksyttyHakijaryhmasta boolean,
  siirtynytToisestaValintatapajonosta boolean,
  julkaistavissa boolean not null default false,
  unique(valintatapajonoOid, hakemusOid),
  constraint jonosijat_vaintatapajonot_fk foreign key (valintatapajonoOid, sijoitteluajonHakukohdeId) references valintatapajonot(oid, sijoitteluajonHakukohdeId)
);
alter table jonosijat owner to oph;

create type valinnantila as enum (
  'Hylatty',
  'Varalla',
  'Peruuntunut',
  'VarasijaltaHyvaksytty',
  'Hyvaksytty',
  'Perunut',
  'Peruutettu'
);

create sequence deleted_valinnantilat_id start 1;
alter sequence deleted_valinnantilat_id owner to oph;

create table deleted_valinnantilat(
  id bigint primary key default nextval('deleted_valinnantilat_id'),
  poistaja character varying not null,
  selite character varying not null,
  "timestamp" timestamp with time zone not null default now()
);
alter table deleted_valinnantilat owner to oph;

create sequence valinnantilat_id start 1;
alter sequence valinnantilat_id owner to oph;

create table valinnantilat(
  id bigint PRIMARY KEY default nextval('valinnantilat_id'),
  jonosijaId bigint not null constraint tilat_jonosijat_fk references jonosijat(id),
  tila valinnantila not null,
  ilmoittaja character varying not null,
  selite character varying not null,
  "timestamp" timestamp with time zone not null default now(),
  deleted bigint constraint vt_deleted_valinnantilat_fk references deleted_valinnantilat(id)
);
alter table valinnantilat owner to oph;

create table pistetiedot (
  jonosijaId bigint not null,
  tunniste character varying not null,
  arvo character varying,
  laskennallinenArvo character varying,
  osallistuminen character varying
);
alter table pistetiedot owner to oph;

create sequence hakijaryhmat_id start 1;
alter sequence hakijaryhmat_id owner to oph;

create table hakijaryhmat(
  id bigint PRIMARY KEY default nextval('hakijaryhmat_id'),
  oid character varying not null,
  sijoitteluajonHakukohdeId bigint not null references sijoitteluajonHakukohteet(id),
  nimi character varying not null,
  prioriteetti integer,
  paikat integer,
  kiintio integer,
  kaytaKaikki boolean,
  tarkkaKiintio boolean,
  kaytetaanRyhmaanKuuluvia boolean,
  alinHyvaksyttyPistemaara character varying,
  unique(oid, sijoitteluajonHakukohdeId)
);
alter table hakijaryhmat owner to oph;

create table hakijaryhmanHakemukset(
  hakijaryhmaId bigint not null constraint hakemukset_hakijaryhmat_fk references hakijaryhmat(id),
  hakemusOid character varying not null,
  PRIMARY KEY (hakijaryhmaId, hakemusOid)
);
alter table hakijaryhmanHakemukset owner to oph;

create sequence valintatulokset_id start 1;
alter sequence valintatulokset_id owner to oph;

create type ilmoittautumistila as enum (
  'EiTehty',
  'LasnaKokoLukuvuosi',
  'PoissaKokoLukuvuosi',
  'EiIlmoittautunut',
  'LasnaSyksy',
  'PoissaSyksy',
  'Lasna',
  'Poissa'
);

create sequence deleted_ilmoittautumiset_id start 1;
alter sequence deleted_ilmoittautumiset_id owner to oph;

create table deleted_ilmoittautumiset(
  id bigint primary key default nextval('deleted_ilmoittautumiset_id'),
  poistaja character varying not null,
  selite character varying not null,
  "timestamp" timestamp with time zone not null default now()
);
alter table deleted_ilmoittautumiset owner to oph;

create sequence ilmoittautumiset_id start 1;
alter sequence ilmoittautumiset_id owner to oph;

create table ilmoittautumiset(
  id bigint PRIMARY KEY default nextval('ilmoittautumiset_id'),
  henkilo character varying not null,
  hakukohde character varying not null constraint ilmoittautumiset_hakukohteet_fk references hakukohteet(hakukohde_oid),
  tila ilmoittautumistila not null default 'EiTehty',
  ilmoittaja character varying not null,
  selite character varying not null,
  "timestamp" timestamp with time zone not null default now(),
  deleted bigint constraint il_deleted_ilmoittautumiset_fk references deleted_ilmoittautumiset(id)
);
alter table ilmoittautumiset owner to oph;
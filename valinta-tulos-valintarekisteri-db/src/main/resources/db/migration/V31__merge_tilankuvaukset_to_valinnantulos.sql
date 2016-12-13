alter table valinnantulokset drop column tilankuvaus_id;

drop table tilankuvausten_tekstit;

alter table tilankuvaukset drop column id;
alter table tilankuvaukset drop column tilankuvauksen_tarkenne;

drop sequence tilankuvaukset_id;

drop table tilankuvaukset;

create table tilankuvaukset(
  hash bigint primary key,
  tilankuvauksen_tarkenne varchar(70) not null,
  text_fi varchar,
  text_sv varchar,
  text_en varchar
);

alter table tilankuvaukset owner to oph;

alter table valinnantulokset add column tilankuvaus_hash bigint references tilankuvaukset(hash);

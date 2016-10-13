CREATE TABLE sijoitteluajo (
  id bigint NOT NULL UNIQUE,
  hakuOid character varying NOT NULL,
  "start" timestamp with time zone NOT NULL DEFAULT now(),
  "end" timestamp with time zone NOT NULL DEFAULT now(),
  PRIMARY KEY (id)
);
ALTER TABLE sijoitteluajo OWNER TO oph;
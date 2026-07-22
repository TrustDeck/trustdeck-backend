--
-- PostgreSQL database dump
--

-- Dumped from database version 17.4 (Debian 17.4-1.pgdg120+2)
-- Dumped by pg_dump version 17.4 (Debian 17.4-1.pgdg120+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

\connect postgres

DROP DATABASE IF EXISTS trustdeck WITH (FORCE);

--
-- Name: trustdeck; Type: DATABASE; Schema: -; Owner: trustdeck-manager
--

CREATE DATABASE trustdeck WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'en_US.utf8';


ALTER DATABASE trustdeck OWNER TO "trustdeck-manager";

\connect trustdeck

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: algorithm; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.algorithm (
    id integer NOT NULL,
    name text NOT NULL,
    alphabet text NOT NULL,
    random_algorithm_desired_size bigint NOT NULL,
    random_algorithm_desired_success_probability double precision NOT NULL,
    consecutive_value_counter bigint NOT NULL,
    pseudonym_length integer NOT NULL,
    padding_character character(1) NOT NULL,
    add_check_digit boolean NOT NULL,
    length_includes_check_digit boolean NOT NULL,
    salt text NOT NULL,
    salt_length integer NOT NULL
);


ALTER TABLE public.algorithm OWNER TO "trustdeck-manager";

--
-- Name: algorithm_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.algorithm_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.algorithm_id_seq OWNER TO "trustdeck-manager";

--
-- Name: algorithm_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.algorithm_id_seq OWNED BY public.algorithm.id;


--
-- Name: audit_event; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.audit_event (
    id bigint NOT NULL,
    request_time timestamp without time zone NOT NULL,
    user_name text NOT NULL,
    requester_ip text,
    request_url text,
    request_body text,
    source_system text,
    topic text,
    partition integer,
    record_offset bigint
);


ALTER TABLE public.audit_event OWNER TO "trustdeck-manager";

--
-- Name: audit_event_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.audit_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.audit_event_id_seq OWNER TO "trustdeck-manager";

--
-- Name: audit_event_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.audit_event_id_seq OWNED BY public.audit_event.id;


--
-- Name: domain; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.domain (
    id integer NOT NULL,
    name text NOT NULL,
    prefix text NOT NULL,
    validfrom timestamp without time zone NOT NULL,
    validfrominherited boolean NOT NULL,
    validto timestamp without time zone NOT NULL,
    validtoinherited boolean NOT NULL,
    enforcestartdatevalidity boolean NOT NULL,
    enforcestartdatevalidityinherited boolean NOT NULL,
    enforceenddatevalidity boolean NOT NULL,
    enforceenddatevalidityinherited boolean NOT NULL,
    algorithm_id integer NOT NULL,
    algorithm_inherited boolean NOT NULL,
    multiplepsnallowed boolean NOT NULL,
    multiplepsnallowedinherited boolean NOT NULL,
    description text,
    superdomainid integer
);


ALTER TABLE public.domain OWNER TO "trustdeck-manager";

--
-- Name: domain_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.domain_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.domain_id_seq OWNER TO "trustdeck-manager";

--
-- Name: domain_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.domain_id_seq OWNED BY public.domain.id;


--
-- Name: entity_instance; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.entity_instance (
    id bigint NOT NULL,
    trustdeck_id uuid DEFAULT gen_random_uuid() NOT NULL,
    project_id integer NOT NULL,
    entity_type_id integer NOT NULL,
    data jsonb NOT NULL,
    full_text_search_vector tsvector GENERATED ALWAYS AS (jsonb_to_tsvector('simple'::regconfig, data, '["string"]'::jsonb)) STORED,
    data_text text GENERATED ALWAYS AS ((data)::text) STORED,
    data_sha256 bytea GENERATED ALWAYS AS (public.digest((data)::text, 'sha256'::text)) STORED,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
)
PARTITION BY LIST (entity_type_id);


ALTER TABLE public.entity_instance OWNER TO "trustdeck-manager";

--
-- Name: entity_instance_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.entity_instance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.entity_instance_id_seq OWNER TO "trustdeck-manager";

--
-- Name: entity_instance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.entity_instance_id_seq OWNED BY public.entity_instance.id;


--
-- Name: entity_type; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.entity_type (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    is_deprecated boolean DEFAULT false NOT NULL,
    is_base_type boolean NOT NULL,
    type_definition jsonb NOT NULL,
    base_type_id integer,
    associated_domain_id integer,
    project_id integer,
    full_text_search_vector tsvector GENERATED ALWAYS AS (jsonb_to_tsvector('simple'::regconfig, type_definition, '["string"]'::jsonb)) STORED,
    CONSTRAINT entity_type_check CHECK (((is_base_type AND (base_type_id IS NULL)) OR (NOT is_base_type))),
    CONSTRAINT entity_type_check1 CHECK (((base_type_id IS NULL) OR (base_type_id <> id)))
);


ALTER TABLE public.entity_type OWNER TO "trustdeck-manager";

--
-- Name: entity_type_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.entity_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.entity_type_id_seq OWNER TO "trustdeck-manager";

--
-- Name: entity_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.entity_type_id_seq OWNED BY public.entity_type.id;


--
-- Name: linkage_token; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.linkage_token (
    entity_instance_id bigint NOT NULL,
    entity_type_id integer NOT NULL,
    project_id integer NOT NULL,
    field_path text NOT NULL,
    tag text NOT NULL,
    token_type text NOT NULL,
    token_value text NOT NULL,
    weight double precision DEFAULT 1.0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.linkage_token OWNER TO "trustdeck-manager";

--
-- Name: permission_grant; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.permission_grant (
    id integer NOT NULL,
    subject_id character varying(128) NOT NULL,
    resource_type character varying(32) NOT NULL,
    resource_id integer NOT NULL,
    action character varying(128) NOT NULL,
    decision character varying(8) DEFAULT 'ALLOW'::character varying NOT NULL,
    valid_from timestamp with time zone,
    valid_to timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(64) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(64) NOT NULL
);


ALTER TABLE public.permission_grant OWNER TO "trustdeck-manager";

--
-- Name: permission_grant_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.permission_grant_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.permission_grant_id_seq OWNER TO "trustdeck-manager";

--
-- Name: permission_grant_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.permission_grant_id_seq OWNED BY public.permission_grant.id;


--
-- Name: project; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.project (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    abbreviation character varying(50) NOT NULL,
    start_date timestamp with time zone DEFAULT now() NOT NULL,
    end_date timestamp with time zone,
    store_entities boolean NOT NULL,
    store_pseudonyms boolean NOT NULL,
    description text,
    CONSTRAINT project_check CHECK (((end_date IS NULL) OR (end_date >= start_date)))
);


ALTER TABLE public.project OWNER TO "trustdeck-manager";

--
-- Name: project_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.project_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.project_id_seq OWNER TO "trustdeck-manager";

--
-- Name: project_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.project_id_seq OWNED BY public.project.id;


--
-- Name: project_image; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.project_image (
    id integer NOT NULL,
    project_id integer NOT NULL,
    image bytea,
    image_size_bytes integer GENERATED ALWAYS AS (octet_length(image)) STORED,
    mime_type text,
    CONSTRAINT project_image_image_check CHECK ((octet_length(image) <= ((5 * 1024) * 1024))),
    CONSTRAINT project_image_mime_type_check CHECK ((mime_type ~ '^[a-z0-9.+-]+/[a-z0-9.+-]+$'::text)),
    CONSTRAINT project_image_mime_type_check1 CHECK ((mime_type = ANY (ARRAY['image/jpeg'::text, 'image/png'::text, 'image/webp'::text, 'image/svg+xml'::text])))
);


ALTER TABLE public.project_image OWNER TO "trustdeck-manager";

--
-- Name: project_image_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.project_image_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.project_image_id_seq OWNER TO "trustdeck-manager";

--
-- Name: project_image_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.project_image_id_seq OWNED BY public.project_image.id;


--
-- Name: pseudonym; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.pseudonym (
    id bigint NOT NULL,
    identifier text NOT NULL,
    idtype text NOT NULL,
    pseudonym text NOT NULL,
    validfrom timestamp without time zone NOT NULL,
    validfrominherited boolean NOT NULL,
    validto timestamp without time zone NOT NULL,
    validtoinherited boolean NOT NULL,
    domainid integer
);


ALTER TABLE public.pseudonym OWNER TO "trustdeck-manager";

--
-- Name: pseudonym_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.pseudonym_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.pseudonym_id_seq OWNER TO "trustdeck-manager";

--
-- Name: pseudonym_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.pseudonym_id_seq OWNED BY public.pseudonym.id;


--
-- Name: algorithm id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.algorithm ALTER COLUMN id SET DEFAULT nextval('public.algorithm_id_seq'::regclass);


--
-- Name: audit_event id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.audit_event ALTER COLUMN id SET DEFAULT nextval('public.audit_event_id_seq'::regclass);


--
-- Name: domain id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.domain ALTER COLUMN id SET DEFAULT nextval('public.domain_id_seq'::regclass);


--
-- Name: entity_instance id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_instance ALTER COLUMN id SET DEFAULT nextval('public.entity_instance_id_seq'::regclass);


--
-- Name: entity_type id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_type ALTER COLUMN id SET DEFAULT nextval('public.entity_type_id_seq'::regclass);


--
-- Name: permission_grant id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.permission_grant ALTER COLUMN id SET DEFAULT nextval('public.permission_grant_id_seq'::regclass);


--
-- Name: project id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project ALTER COLUMN id SET DEFAULT nextval('public.project_id_seq'::regclass);


--
-- Name: project_image id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project_image ALTER COLUMN id SET DEFAULT nextval('public.project_image_id_seq'::regclass);


--
-- Name: pseudonym id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.pseudonym ALTER COLUMN id SET DEFAULT nextval('public.pseudonym_id_seq'::regclass);


--
-- Name: algorithm algorithm_configuration_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.algorithm
    ADD CONSTRAINT algorithm_configuration_key UNIQUE (name, alphabet, random_algorithm_desired_size, random_algorithm_desired_success_probability, pseudonym_length, padding_character, add_check_digit, length_includes_check_digit, salt, salt_length);


--
-- Name: algorithm algorithm_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.algorithm
    ADD CONSTRAINT algorithm_pkey PRIMARY KEY (id);


--
-- Name: audit_event audit_event_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.audit_event
    ADD CONSTRAINT audit_event_pkey PRIMARY KEY (id);


--
-- Name: domain domain_name_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.domain
    ADD CONSTRAINT domain_name_key UNIQUE (name);


--
-- Name: domain domain_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.domain
    ADD CONSTRAINT domain_pkey PRIMARY KEY (id);


--
-- Name: entity_instance entity_instance_entity_type_id_trustdeck_id_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_instance
    ADD CONSTRAINT entity_instance_entity_type_id_trustdeck_id_key UNIQUE (entity_type_id, trustdeck_id);


--
-- Name: entity_instance entity_instance_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_instance
    ADD CONSTRAINT entity_instance_pkey PRIMARY KEY (entity_type_id, id);


--
-- Name: entity_type entity_type_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_type
    ADD CONSTRAINT entity_type_pkey PRIMARY KEY (id);


--
-- Name: linkage_token linkage_token_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.linkage_token
    ADD CONSTRAINT linkage_token_pk PRIMARY KEY (entity_type_id, entity_instance_id, tag, token_type, token_value);


--
-- Name: permission_grant permission_grant_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.permission_grant
    ADD CONSTRAINT permission_grant_pkey PRIMARY KEY (id);


--
-- Name: project project_abbreviation_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_abbreviation_key UNIQUE (abbreviation);


--
-- Name: project_image project_image_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project_image
    ADD CONSTRAINT project_image_pkey PRIMARY KEY (id);


--
-- Name: project_image project_image_project_id_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project_image
    ADD CONSTRAINT project_image_project_id_key UNIQUE (project_id);


--
-- Name: project project_name_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_name_key UNIQUE (name);


--
-- Name: project project_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project
    ADD CONSTRAINT project_pkey PRIMARY KEY (id);


--
-- Name: pseudonym pseudonym_identifier_idtype_domainid_pseudonym_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.pseudonym
    ADD CONSTRAINT pseudonym_identifier_idtype_domainid_pseudonym_key UNIQUE (identifier, idtype, domainid, pseudonym);


--
-- Name: pseudonym pseudonym_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.pseudonym
    ADD CONSTRAINT pseudonym_pkey PRIMARY KEY (id);


--
-- Name: pseudonym pseudonym_psn_domainid_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.pseudonym
    ADD CONSTRAINT pseudonym_psn_domainid_key UNIQUE (domainid, pseudonym);


--
-- Name: algorithm_name_uindex; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX algorithm_name_uindex ON public.algorithm USING btree (name);


--
-- Name: auditusernameidx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX auditusernameidx ON public.audit_event USING btree (user_name);


--
-- Name: entity_instance_uq_type_sha256; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX entity_instance_uq_type_sha256 ON ONLY public.entity_instance USING btree (project_id, entity_type_id, public.digest((data)::text, 'sha256'::text)) WHERE (is_deleted = false);


--
-- Name: entity_type_fts_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX entity_type_fts_idx ON public.entity_type USING gin (full_text_search_vector);


--
-- Name: entity_type_name_global_uq; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX entity_type_name_global_uq ON public.entity_type USING btree (lower((name)::text)) WHERE (project_id IS NULL);


--
-- Name: entity_type_name_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX entity_type_name_idx ON public.entity_type USING btree (name);


--
-- Name: entity_type_name_proj_uq; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX entity_type_name_proj_uq ON public.entity_type USING btree (lower((name)::text), project_id);


--
-- Name: entity_type_type_definition_gin_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX entity_type_type_definition_gin_idx ON public.entity_type USING gin (type_definition);


--
-- Name: ididtypeidx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX ididtypeidx ON public.pseudonym USING btree (identifier, idtype);


--
-- Name: idpsnidx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX idpsnidx ON public.pseudonym USING btree (identifier, pseudonym);


--
-- Name: ix_permission_by_resource; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX ix_permission_by_resource ON public.permission_grant USING btree (resource_type, resource_id);


--
-- Name: ix_permission_by_subject; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX ix_permission_by_subject ON public.permission_grant USING btree (subject_id);


--
-- Name: ix_permission_check; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX ix_permission_check ON public.permission_grant USING btree (subject_id, resource_type, resource_id, action) WHERE ((decision)::text = 'ALLOW'::text);


--
-- Name: linkage_token_block_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX linkage_token_block_idx ON public.linkage_token USING btree (project_id, entity_type_id, token_type, token_value);


--
-- Name: linkage_token_entity_instance_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX linkage_token_entity_instance_idx ON public.linkage_token USING btree (entity_type_id, entity_instance_id);


--
-- Name: linkage_token_lookup_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX linkage_token_lookup_idx ON public.linkage_token USING btree (project_id, entity_type_id, tag, token_type, token_value);


--
-- Name: metadataidx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX metadataidx ON public.domain USING btree (name);


--
-- Name: uq_permission_grant; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX uq_permission_grant ON public.permission_grant USING btree (subject_id, resource_type, resource_id, action);


--
-- Name: domain domain_superdomainid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.domain
    ADD CONSTRAINT domain_superdomainid_fkey FOREIGN KEY (superdomainid) REFERENCES public.domain(id);

ALTER TABLE ONLY public.domain
    ADD CONSTRAINT domain_algorithm_id_fkey FOREIGN KEY (algorithm_id) REFERENCES public.algorithm(id);


--
-- Name: entity_instance entity_instance_entity_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE public.entity_instance
    ADD CONSTRAINT entity_instance_entity_type_id_fkey FOREIGN KEY (entity_type_id) REFERENCES public.entity_type(id) ON DELETE RESTRICT;


--
-- Name: entity_instance entity_instance_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE public.entity_instance
    ADD CONSTRAINT entity_instance_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE RESTRICT;


--
-- Name: entity_type entity_type_associated_domain_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_type
    ADD CONSTRAINT entity_type_associated_domain_id_fkey FOREIGN KEY (associated_domain_id) REFERENCES public.domain(id) ON DELETE SET NULL;


--
-- Name: entity_type entity_type_base_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_type
    ADD CONSTRAINT entity_type_base_type_id_fkey FOREIGN KEY (base_type_id) REFERENCES public.entity_type(id) ON DELETE RESTRICT;


--
-- Name: entity_type entity_type_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_type
    ADD CONSTRAINT entity_type_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE RESTRICT;


--
-- Name: project_image project_image_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.project_image
    ADD CONSTRAINT project_image_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE CASCADE;


--
-- Name: pseudonym pseudonym_domainid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.pseudonym
    ADD CONSTRAINT pseudonym_domainid_fkey FOREIGN KEY (domainid) REFERENCES public.domain(id);


--
-- PostgreSQL database dump complete
--

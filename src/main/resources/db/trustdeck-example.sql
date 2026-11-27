/*
 * Trust Deck Services
 * Copyright 2025-2026 Armin Müller
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    on_behalf_of text
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
    superdomainid integer,
    project_id integer NOT NULL
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
-- Name: entity; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.entity (
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


ALTER TABLE public.entity OWNER TO "trustdeck-manager";

--
-- Name: entity_id_seq; Type: SEQUENCE; Schema: public; Owner: trustdeck-manager
--

CREATE SEQUENCE public.entity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.entity_id_seq OWNER TO "trustdeck-manager";

--
-- Name: entity_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: trustdeck-manager
--

ALTER SEQUENCE public.entity_id_seq OWNED BY public.entity.id;


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
    entity_id bigint NOT NULL,
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
-- Name: entity id; Type: DEFAULT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity ALTER COLUMN id SET DEFAULT nextval('public.entity_id_seq'::regclass);


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
-- Name: entity entity_entity_type_id_trustdeck_id_key; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity
    ADD CONSTRAINT entity_entity_type_id_trustdeck_id_key UNIQUE (entity_type_id, trustdeck_id);


--
-- Name: entity entity_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity
    ADD CONSTRAINT entity_pkey PRIMARY KEY (entity_type_id, id);


--
-- Name: entity_type entity_type_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.entity_type
    ADD CONSTRAINT entity_type_pkey PRIMARY KEY (id);


--
-- Name: linkage_token linkage_token_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.linkage_token
    ADD CONSTRAINT linkage_token_pk PRIMARY KEY (entity_type_id, entity_id, tag, token_type, token_value);


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
-- Name: entity_uq_type_sha256; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX entity_uq_type_sha256 ON ONLY public.entity USING btree (project_id, entity_type_id, public.digest((data)::text, 'sha256'::text)) WHERE (is_deleted = false);


--
-- Name: entity_created_at_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX entity_created_at_idx ON public.entity USING btree (entity_type_id, created_at, id) WHERE (is_deleted = false);


--
-- Name: entity_data_text_trgm_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX entity_data_text_trgm_idx ON public.entity USING gin (data_text public.gin_trgm_ops) WHERE (is_deleted = false);


--
-- Name: entity_fts_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX entity_fts_idx ON public.entity USING gin (full_text_search_vector) WHERE (is_deleted = false);


--
-- Name: entity_updated_at_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX entity_updated_at_idx ON public.entity USING btree (entity_type_id, updated_at, id) WHERE (is_deleted = false);


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
-- Name: pseudonym_domain_identifier_prefix_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

--
-- Name: pseudonym_domain_identifier_order_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX pseudonym_domain_identifier_order_idx ON public.pseudonym USING btree (domainid, identifier, idtype, id);


--
-- Name: pseudonym_domain_idtype_order_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX pseudonym_domain_idtype_order_idx ON public.pseudonym USING btree (domainid, idtype, identifier, id);


CREATE INDEX pseudonym_domain_identifier_prefix_idx ON public.pseudonym USING btree (domainid, lower(identifier) text_pattern_ops);


--
-- Name: pseudonym_domain_idtype_prefix_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX pseudonym_domain_idtype_prefix_idx ON public.pseudonym USING btree (domainid, lower(idtype) text_pattern_ops);


--
-- Name: pseudonym_domain_pseudonym_prefix_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX pseudonym_domain_pseudonym_prefix_idx ON public.pseudonym USING btree (domainid, lower(pseudonym) text_pattern_ops);


--
-- Name: pseudonym_identifier_trgm_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX pseudonym_identifier_trgm_idx ON public.pseudonym USING gin (lower(identifier) public.gin_trgm_ops);


--
-- Name: pseudonym_idtype_trgm_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX pseudonym_idtype_trgm_idx ON public.pseudonym USING gin (lower(idtype) public.gin_trgm_ops);


--
-- Name: pseudonym_pseudonym_trgm_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX pseudonym_pseudonym_trgm_idx ON public.pseudonym USING gin (lower(pseudonym) public.gin_trgm_ops);


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
-- Name: linkage_token_entity_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX linkage_token_entity_idx ON public.linkage_token USING btree (entity_type_id, entity_id);


--
-- Name: linkage_token_lookup_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX linkage_token_lookup_idx ON public.linkage_token USING btree (project_id, entity_type_id, tag, token_type, token_value);


--
-- Name: metadataidx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE UNIQUE INDEX metadataidx ON public.domain USING btree (name);


--
-- Name: domain_project_id_idx; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX domain_project_id_idx ON public.domain USING btree (project_id);


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

ALTER TABLE ONLY public.domain
    ADD CONSTRAINT domain_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE RESTRICT;


--
-- Name: entity entity_entity_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE public.entity
    ADD CONSTRAINT entity_entity_type_id_fkey FOREIGN KEY (entity_type_id) REFERENCES public.entity_type(id) ON DELETE RESTRICT;


--
-- Name: entity entity_project_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE public.entity
    ADD CONSTRAINT entity_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.project(id) ON DELETE RESTRICT;


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
-- Counters for the project statistics: the number of pseudonyms per domain and of non-deleted
-- entities per project-owned entity type. Counting these on demand would scan tables with
-- hundreds of millions of rows, so triggers keep the counts up to date on every write,
-- including set-based statements, TRUNCATE, and direct SQL.
--
-- Triggers only append changes to the *_count_delta log tables, so concurrent writers never wait
-- for each other because of the counters. fold_count_deltas() moves the logged changes into the
-- *_count tables; the backend calls it regularly. The *_counts views add both up, so a read is
-- always exact. Writes must go through public.entity, not through its partitions, since statement
-- triggers only fire for the table named in the statement.
--

CREATE TABLE public.pseudonym_count (
    domain_id integer NOT NULL,
    record_count bigint NOT NULL
);


ALTER TABLE public.pseudonym_count OWNER TO "trustdeck-manager";

CREATE TABLE public.pseudonym_count_delta (
    id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    domain_id integer NOT NULL,
    delta bigint NOT NULL
);


ALTER TABLE public.pseudonym_count_delta OWNER TO "trustdeck-manager";

CREATE TABLE public.entity_count (
    entity_type_id integer NOT NULL,
    record_count bigint NOT NULL
);


ALTER TABLE public.entity_count OWNER TO "trustdeck-manager";

CREATE TABLE public.entity_count_delta (
    id bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
    entity_type_id integer NOT NULL,
    delta bigint NOT NULL
);


ALTER TABLE public.entity_count_delta OWNER TO "trustdeck-manager";

ALTER TABLE ONLY public.pseudonym_count
    ADD CONSTRAINT pseudonym_count_pkey PRIMARY KEY (domain_id);

ALTER TABLE ONLY public.pseudonym_count_delta
    ADD CONSTRAINT pseudonym_count_delta_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.entity_count
    ADD CONSTRAINT entity_count_pkey PRIMARY KEY (entity_type_id);

ALTER TABLE ONLY public.entity_count_delta
    ADD CONSTRAINT entity_count_delta_pkey PRIMARY KEY (id);

CREATE INDEX pseudonym_count_delta_domain_idx ON public.pseudonym_count_delta USING btree (domain_id);

CREATE INDEX entity_count_delta_type_idx ON public.entity_count_delta USING btree (entity_type_id);

--
-- The current number of pseudonyms per domain: the folded count plus the changes not yet folded.
-- The counter tables have no foreign keys, so that folding never waits for a domain deletion;
-- rows of deleted domains are not shown here and are removed by the next fold.
--

CREATE VIEW public.domain_pseudonym_counts AS
 SELECT d.id AS domain_id,
    d.project_id,
    (COALESCE(c.record_count, (0)::bigint) + COALESCE(( SELECT sum(x.delta) AS sum
           FROM public.pseudonym_count_delta x
          WHERE (x.domain_id = d.id)), (0)::numeric))::bigint AS record_count
   FROM (public.domain d
     LEFT JOIN public.pseudonym_count c ON ((c.domain_id = d.id)));


ALTER VIEW public.domain_pseudonym_counts OWNER TO "trustdeck-manager";

--
-- The current number of non-deleted entities per project-owned entity type. Entities whose type
-- is not owned by the entity's project (e.g. entities of a global base type) are not counted.
--

CREATE VIEW public.entity_type_entity_counts AS
 SELECT t.id AS entity_type_id,
    t.project_id,
    (COALESCE(c.record_count, (0)::bigint) + COALESCE(( SELECT sum(x.delta) AS sum
           FROM public.entity_count_delta x
          WHERE (x.entity_type_id = t.id)), (0)::numeric))::bigint AS record_count
   FROM (public.entity_type t
     LEFT JOIN public.entity_count c ON ((c.entity_type_id = t.id)))
  WHERE (t.project_id IS NOT NULL);


ALTER VIEW public.entity_type_entity_counts OWNER TO "trustdeck-manager";

--
-- Inserted and deleted pseudonyms, aggregated per statement; TRUNCATE empties the counters.
--

CREATE FUNCTION public.count_pseudonyms() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'public'
    AS $$
BEGIN
    IF TG_OP = 'TRUNCATE' THEN
        DELETE FROM public.pseudonym_count_delta;
        DELETE FROM public.pseudonym_count;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO public.pseudonym_count_delta (domain_id, delta)
        SELECT domainid, count(*) FROM new_rows WHERE domainid IS NOT NULL GROUP BY domainid;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO public.pseudonym_count_delta (domain_id, delta)
        SELECT domainid, -count(*) FROM old_rows WHERE domainid IS NOT NULL GROUP BY domainid;
    END IF;

    RETURN NULL;
END
$$;


ALTER FUNCTION public.count_pseudonyms() OWNER TO "trustdeck-manager";

--
-- A pseudonym moved to another domain. A row-level trigger, because PostgreSQL allows no column
-- list on triggers with transition tables, and a statement-level UPDATE trigger would collect
-- every updated row, e.g. all pseudonyms of a domain whose validity changes.
--

CREATE FUNCTION public.count_moved_pseudonym() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'public'
    AS $$
BEGIN
    INSERT INTO public.pseudonym_count_delta (domain_id, delta)
    SELECT m.domain_id, m.delta FROM (VALUES (OLD.domainid, -1::bigint), (NEW.domainid, 1::bigint)) AS m(domain_id, delta)
    WHERE m.domain_id IS NOT NULL;

    RETURN NULL;
END
$$;


ALTER FUNCTION public.count_moved_pseudonym() OWNER TO "trustdeck-manager";

--
-- Inserted, updated (soft-deleted, restored, moved), and deleted entities, aggregated per
-- statement. Only entities that are not soft-deleted and whose entity type is owned by the
-- entity's project are counted. Updates that change neither leave no log entry.
--

CREATE FUNCTION public.count_entities() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'public'
    AS $$
BEGIN
    IF TG_OP = 'TRUNCATE' THEN
        DELETE FROM public.entity_count_delta;
        DELETE FROM public.entity_count;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO public.entity_count_delta (entity_type_id, delta)
        SELECT r.entity_type_id, count(*) FROM new_rows r
        JOIN public.entity_type t ON t.id = r.entity_type_id AND t.project_id = r.project_id
        WHERE NOT r.is_deleted
        GROUP BY r.entity_type_id;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO public.entity_count_delta (entity_type_id, delta)
        SELECT r.entity_type_id, -count(*) FROM old_rows r
        JOIN public.entity_type t ON t.id = r.entity_type_id AND t.project_id = r.project_id
        WHERE NOT r.is_deleted
        GROUP BY r.entity_type_id;
    ELSE
        INSERT INTO public.entity_count_delta (entity_type_id, delta)
        SELECT d.entity_type_id, sum(d.delta) FROM (
            SELECT r.entity_type_id, 1 AS delta FROM new_rows r
            JOIN public.entity_type t ON t.id = r.entity_type_id AND t.project_id = r.project_id
            WHERE NOT r.is_deleted
            UNION ALL
            SELECT r.entity_type_id, -1 AS delta FROM old_rows r
            JOIN public.entity_type t ON t.id = r.entity_type_id AND t.project_id = r.project_id
            WHERE NOT r.is_deleted
        ) AS d
        GROUP BY d.entity_type_id
        HAVING sum(d.delta) <> 0;
    END IF;

    RETURN NULL;
END
$$;


ALTER FUNCTION public.count_entities() OWNER TO "trustdeck-manager";

--
-- Moves the logged changes into the counter tables and removes counters of deleted domains and
-- entity types. Only changes committed when the fold starts are moved; later ones stay in the log
-- for the next fold. Concurrent calls skip instead of waiting, so several backend instances can
-- call it.
--

CREATE FUNCTION public.fold_count_deltas() RETURNS void
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'public'
    AS $$
BEGIN
    IF NOT pg_try_advisory_xact_lock(hashtext('trustdeck.fold_count_deltas')) THEN
        RETURN;
    END IF;

    WITH moved AS (
        DELETE FROM public.pseudonym_count_delta RETURNING domain_id, delta
    )
    INSERT INTO public.pseudonym_count AS c (domain_id, record_count)
    SELECT domain_id, sum(delta) FROM moved GROUP BY domain_id
    ON CONFLICT (domain_id) DO UPDATE SET record_count = c.record_count + EXCLUDED.record_count;

    WITH moved AS (
        DELETE FROM public.entity_count_delta RETURNING entity_type_id, delta
    )
    INSERT INTO public.entity_count AS c (entity_type_id, record_count)
    SELECT entity_type_id, sum(delta) FROM moved GROUP BY entity_type_id
    ON CONFLICT (entity_type_id) DO UPDATE SET record_count = c.record_count + EXCLUDED.record_count;

    DELETE FROM public.pseudonym_count c WHERE NOT EXISTS (SELECT 1 FROM public.domain d WHERE d.id = c.domain_id);
    DELETE FROM public.entity_count c WHERE NOT EXISTS (SELECT 1 FROM public.entity_type t WHERE t.id = c.entity_type_id);
END
$$;


ALTER FUNCTION public.fold_count_deltas() OWNER TO "trustdeck-manager";

CREATE TRIGGER pseudonym_count_insert AFTER INSERT ON public.pseudonym REFERENCING NEW TABLE AS new_rows FOR EACH STATEMENT EXECUTE FUNCTION public.count_pseudonyms();

CREATE TRIGGER pseudonym_count_delete AFTER DELETE ON public.pseudonym REFERENCING OLD TABLE AS old_rows FOR EACH STATEMENT EXECUTE FUNCTION public.count_pseudonyms();

CREATE TRIGGER pseudonym_count_truncate AFTER TRUNCATE ON public.pseudonym FOR EACH STATEMENT EXECUTE FUNCTION public.count_pseudonyms();

CREATE TRIGGER pseudonym_count_move AFTER UPDATE OF domainid ON public.pseudonym FOR EACH ROW WHEN (OLD.domainid IS DISTINCT FROM NEW.domainid) EXECUTE FUNCTION public.count_moved_pseudonym();

CREATE TRIGGER entity_count_insert AFTER INSERT ON public.entity REFERENCING NEW TABLE AS new_rows FOR EACH STATEMENT EXECUTE FUNCTION public.count_entities();

CREATE TRIGGER entity_count_update AFTER UPDATE ON public.entity REFERENCING OLD TABLE AS old_rows NEW TABLE AS new_rows FOR EACH STATEMENT EXECUTE FUNCTION public.count_entities();

CREATE TRIGGER entity_count_delete AFTER DELETE ON public.entity REFERENCING OLD TABLE AS old_rows FOR EACH STATEMENT EXECUTE FUNCTION public.count_entities();

CREATE TRIGGER entity_count_truncate AFTER TRUNCATE ON public.entity FOR EACH STATEMENT EXECUTE FUNCTION public.count_entities();

--
-- PostgreSQL database dump complete
--

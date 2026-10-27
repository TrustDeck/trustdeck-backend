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

DROP DATABASE IF EXISTS keycloak WITH (FORCE);
--
-- Name: keycloak; Type: DATABASE; Schema: -; Owner: trustdeck-manager
--

CREATE DATABASE keycloak WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'en_US.utf8';


ALTER DATABASE keycloak OWNER TO "trustdeck-manager";

\connect keycloak

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin_event_entity; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.admin_event_entity (
    id character varying(36) NOT NULL,
    admin_event_time bigint,
    realm_id character varying(255),
    operation_type character varying(255),
    auth_realm_id character varying(255),
    auth_client_id character varying(255),
    auth_user_id character varying(255),
    ip_address character varying(255),
    resource_path character varying(2550),
    representation text,
    error character varying(255),
    resource_type character varying(64),
    details_json text
);


ALTER TABLE public.admin_event_entity OWNER TO "trustdeck-manager";

--
-- Name: associated_policy; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.associated_policy (
    policy_id character varying(36) NOT NULL,
    associated_policy_id character varying(36) NOT NULL
);


ALTER TABLE public.associated_policy OWNER TO "trustdeck-manager";

--
-- Name: authentication_execution; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.authentication_execution (
    id character varying(36) NOT NULL,
    alias character varying(255),
    authenticator character varying(36),
    realm_id character varying(36),
    flow_id character varying(36),
    requirement integer,
    priority integer,
    authenticator_flow boolean DEFAULT false NOT NULL,
    auth_flow_id character varying(36),
    auth_config character varying(36)
);


ALTER TABLE public.authentication_execution OWNER TO "trustdeck-manager";

--
-- Name: authentication_flow; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.authentication_flow (
    id character varying(36) NOT NULL,
    alias character varying(255),
    description character varying(255),
    realm_id character varying(36),
    provider_id character varying(36) DEFAULT 'basic-flow'::character varying NOT NULL,
    top_level boolean DEFAULT false NOT NULL,
    built_in boolean DEFAULT false NOT NULL
);


ALTER TABLE public.authentication_flow OWNER TO "trustdeck-manager";

--
-- Name: authenticator_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.authenticator_config (
    id character varying(36) NOT NULL,
    alias character varying(255),
    realm_id character varying(36)
);


ALTER TABLE public.authenticator_config OWNER TO "trustdeck-manager";

--
-- Name: authenticator_config_entry; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.authenticator_config_entry (
    authenticator_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.authenticator_config_entry OWNER TO "trustdeck-manager";

--
-- Name: broker_link; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.broker_link (
    identity_provider character varying(255) NOT NULL,
    storage_provider_id character varying(255),
    realm_id character varying(36) NOT NULL,
    broker_user_id character varying(255),
    broker_username character varying(255),
    token text,
    user_id character varying(255) NOT NULL
);


ALTER TABLE public.broker_link OWNER TO "trustdeck-manager";

--
-- Name: client; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client (
    id character varying(36) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    full_scope_allowed boolean DEFAULT false NOT NULL,
    client_id character varying(255),
    not_before integer,
    public_client boolean DEFAULT false NOT NULL,
    secret character varying(255),
    base_url character varying(255),
    bearer_only boolean DEFAULT false NOT NULL,
    management_url character varying(255),
    surrogate_auth_required boolean DEFAULT false NOT NULL,
    realm_id character varying(36),
    protocol character varying(255),
    node_rereg_timeout integer DEFAULT 0,
    frontchannel_logout boolean DEFAULT false NOT NULL,
    consent_required boolean DEFAULT false NOT NULL,
    name character varying(255),
    service_accounts_enabled boolean DEFAULT false NOT NULL,
    client_authenticator_type character varying(255),
    root_url character varying(255),
    description character varying(255),
    registration_token character varying(255),
    standard_flow_enabled boolean DEFAULT true NOT NULL,
    implicit_flow_enabled boolean DEFAULT false NOT NULL,
    direct_access_grants_enabled boolean DEFAULT false NOT NULL,
    always_display_in_console boolean DEFAULT false NOT NULL
);


ALTER TABLE public.client OWNER TO "trustdeck-manager";

--
-- Name: client_attributes; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_attributes (
    client_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


ALTER TABLE public.client_attributes OWNER TO "trustdeck-manager";

--
-- Name: client_auth_flow_bindings; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_auth_flow_bindings (
    client_id character varying(36) NOT NULL,
    flow_id character varying(36),
    binding_name character varying(255) NOT NULL
);


ALTER TABLE public.client_auth_flow_bindings OWNER TO "trustdeck-manager";

--
-- Name: client_initial_access; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_initial_access (
    id character varying(36) NOT NULL,
    realm_id character varying(36) NOT NULL,
    "timestamp" integer,
    expiration integer,
    count integer,
    remaining_count integer
);


ALTER TABLE public.client_initial_access OWNER TO "trustdeck-manager";

--
-- Name: client_node_registrations; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_node_registrations (
    client_id character varying(36) NOT NULL,
    value integer,
    name character varying(255) NOT NULL
);


ALTER TABLE public.client_node_registrations OWNER TO "trustdeck-manager";

--
-- Name: client_scope; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_scope (
    id character varying(36) NOT NULL,
    name character varying(255),
    realm_id character varying(36),
    description character varying(255),
    protocol character varying(255)
);


ALTER TABLE public.client_scope OWNER TO "trustdeck-manager";

--
-- Name: client_scope_attributes; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_scope_attributes (
    scope_id character varying(36) NOT NULL,
    value character varying(2048),
    name character varying(255) NOT NULL
);


ALTER TABLE public.client_scope_attributes OWNER TO "trustdeck-manager";

--
-- Name: client_scope_client; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_scope_client (
    client_id character varying(255) NOT NULL,
    scope_id character varying(255) NOT NULL,
    default_scope boolean DEFAULT false NOT NULL
);


ALTER TABLE public.client_scope_client OWNER TO "trustdeck-manager";

--
-- Name: client_scope_role_mapping; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.client_scope_role_mapping (
    scope_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


ALTER TABLE public.client_scope_role_mapping OWNER TO "trustdeck-manager";

--
-- Name: component; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.component (
    id character varying(36) NOT NULL,
    name character varying(255),
    parent_id character varying(36),
    provider_id character varying(36),
    provider_type character varying(255),
    realm_id character varying(36),
    sub_type character varying(255)
);


ALTER TABLE public.component OWNER TO "trustdeck-manager";

--
-- Name: component_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.component_config (
    id character varying(36) NOT NULL,
    component_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


ALTER TABLE public.component_config OWNER TO "trustdeck-manager";

--
-- Name: composite_role; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.composite_role (
    composite character varying(36) NOT NULL,
    child_role character varying(36) NOT NULL
);


ALTER TABLE public.composite_role OWNER TO "trustdeck-manager";

--
-- Name: credential; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.credential (
    id character varying(36) NOT NULL,
    salt bytea,
    type character varying(255),
    user_id character varying(36),
    created_date bigint,
    user_label character varying(255),
    secret_data text,
    credential_data text,
    priority integer,
    version integer DEFAULT 0
);


ALTER TABLE public.credential OWNER TO "trustdeck-manager";

--
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


ALTER TABLE public.databasechangelog OWNER TO "trustdeck-manager";

--
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


ALTER TABLE public.databasechangeloglock OWNER TO "trustdeck-manager";

--
-- Name: default_client_scope; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.default_client_scope (
    realm_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL,
    default_scope boolean DEFAULT false NOT NULL
);


ALTER TABLE public.default_client_scope OWNER TO "trustdeck-manager";

--
-- Name: event_entity; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.event_entity (
    id character varying(36) NOT NULL,
    client_id character varying(255),
    details_json character varying(2550),
    error character varying(255),
    ip_address character varying(255),
    realm_id character varying(255),
    session_id character varying(255),
    event_time bigint,
    type character varying(255),
    user_id character varying(255),
    details_json_long_value text
);


ALTER TABLE public.event_entity OWNER TO "trustdeck-manager";

--
-- Name: fed_user_attribute; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.fed_user_attribute (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    value character varying(2024),
    long_value_hash bytea,
    long_value_hash_lower_case bytea,
    long_value text
);


ALTER TABLE public.fed_user_attribute OWNER TO "trustdeck-manager";

--
-- Name: fed_user_consent; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.fed_user_consent (
    id character varying(36) NOT NULL,
    client_id character varying(255),
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    created_date bigint,
    last_updated_date bigint,
    client_storage_provider character varying(36),
    external_client_id character varying(255)
);


ALTER TABLE public.fed_user_consent OWNER TO "trustdeck-manager";

--
-- Name: fed_user_consent_cl_scope; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.fed_user_consent_cl_scope (
    user_consent_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


ALTER TABLE public.fed_user_consent_cl_scope OWNER TO "trustdeck-manager";

--
-- Name: fed_user_credential; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.fed_user_credential (
    id character varying(36) NOT NULL,
    salt bytea,
    type character varying(255),
    created_date bigint,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    user_label character varying(255),
    secret_data text,
    credential_data text,
    priority integer
);


ALTER TABLE public.fed_user_credential OWNER TO "trustdeck-manager";

--
-- Name: fed_user_group_membership; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.fed_user_group_membership (
    group_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


ALTER TABLE public.fed_user_group_membership OWNER TO "trustdeck-manager";

--
-- Name: fed_user_required_action; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.fed_user_required_action (
    required_action character varying(255) DEFAULT ' '::character varying NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


ALTER TABLE public.fed_user_required_action OWNER TO "trustdeck-manager";

--
-- Name: fed_user_role_mapping; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.fed_user_role_mapping (
    role_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


ALTER TABLE public.fed_user_role_mapping OWNER TO "trustdeck-manager";

--
-- Name: federated_identity; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.federated_identity (
    identity_provider character varying(255) NOT NULL,
    realm_id character varying(36),
    federated_user_id character varying(255),
    federated_username character varying(255),
    token text,
    user_id character varying(36) NOT NULL
);


ALTER TABLE public.federated_identity OWNER TO "trustdeck-manager";

--
-- Name: federated_user; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.federated_user (
    id character varying(255) NOT NULL,
    storage_provider_id character varying(255),
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.federated_user OWNER TO "trustdeck-manager";

--
-- Name: group_attribute; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.group_attribute (
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255),
    group_id character varying(36) NOT NULL
);


ALTER TABLE public.group_attribute OWNER TO "trustdeck-manager";

--
-- Name: group_role_mapping; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.group_role_mapping (
    role_id character varying(36) NOT NULL,
    group_id character varying(36) NOT NULL
);


ALTER TABLE public.group_role_mapping OWNER TO "trustdeck-manager";

--
-- Name: identity_provider; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.identity_provider (
    internal_id character varying(36) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    provider_alias character varying(255),
    provider_id character varying(255),
    store_token boolean DEFAULT false NOT NULL,
    authenticate_by_default boolean DEFAULT false NOT NULL,
    realm_id character varying(36),
    add_token_role boolean DEFAULT true NOT NULL,
    trust_email boolean DEFAULT false NOT NULL,
    first_broker_login_flow_id character varying(36),
    post_broker_login_flow_id character varying(36),
    provider_display_name character varying(255),
    link_only boolean DEFAULT false NOT NULL,
    organization_id character varying(255),
    hide_on_login boolean DEFAULT false
);


ALTER TABLE public.identity_provider OWNER TO "trustdeck-manager";

--
-- Name: identity_provider_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.identity_provider_config (
    identity_provider_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.identity_provider_config OWNER TO "trustdeck-manager";

--
-- Name: identity_provider_mapper; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.identity_provider_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    idp_alias character varying(255) NOT NULL,
    idp_mapper_name character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.identity_provider_mapper OWNER TO "trustdeck-manager";

--
-- Name: idp_mapper_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.idp_mapper_config (
    idp_mapper_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.idp_mapper_config OWNER TO "trustdeck-manager";

--
-- Name: jgroups_ping; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.jgroups_ping (
    address character varying(200) NOT NULL,
    name character varying(200),
    cluster_name character varying(200) NOT NULL,
    ip character varying(200) NOT NULL,
    coord boolean
);


ALTER TABLE public.jgroups_ping OWNER TO "trustdeck-manager";

--
-- Name: keycloak_group; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.keycloak_group (
    id character varying(36) NOT NULL,
    name character varying(255),
    parent_group character varying(36) NOT NULL,
    realm_id character varying(36),
    type integer DEFAULT 0 NOT NULL,
    description character varying(255)
);


ALTER TABLE public.keycloak_group OWNER TO "trustdeck-manager";

--
-- Name: keycloak_role; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.keycloak_role (
    id character varying(36) NOT NULL,
    client_realm_constraint character varying(255),
    client_role boolean DEFAULT false NOT NULL,
    description character varying(255),
    name character varying(255),
    realm_id character varying(255),
    client character varying(36),
    realm character varying(36)
);


ALTER TABLE public.keycloak_role OWNER TO "trustdeck-manager";

--
-- Name: migration_model; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.migration_model (
    id character varying(36) NOT NULL,
    version character varying(36),
    update_time bigint DEFAULT 0 NOT NULL
);


ALTER TABLE public.migration_model OWNER TO "trustdeck-manager";

--
-- Name: offline_client_session; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.offline_client_session (
    user_session_id character varying(36) NOT NULL,
    client_id character varying(255) NOT NULL,
    offline_flag character varying(4) NOT NULL,
    "timestamp" integer,
    data text,
    client_storage_provider character varying(36) DEFAULT 'local'::character varying NOT NULL,
    external_client_id character varying(255) DEFAULT 'local'::character varying NOT NULL,
    version integer DEFAULT 0
);


ALTER TABLE public.offline_client_session OWNER TO "trustdeck-manager";

--
-- Name: offline_user_session; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.offline_user_session (
    user_session_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    created_on integer NOT NULL,
    offline_flag character varying(4) NOT NULL,
    data text,
    last_session_refresh integer DEFAULT 0 NOT NULL,
    broker_session_id character varying(1024),
    version integer DEFAULT 0
);


ALTER TABLE public.offline_user_session OWNER TO "trustdeck-manager";

--
-- Name: org; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.org (
    id character varying(255) NOT NULL,
    enabled boolean NOT NULL,
    realm_id character varying(255) NOT NULL,
    group_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(4000),
    alias character varying(255) NOT NULL,
    redirect_url character varying(2048)
);


ALTER TABLE public.org OWNER TO "trustdeck-manager";

--
-- Name: org_domain; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.org_domain (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    verified boolean NOT NULL,
    org_id character varying(255) NOT NULL
);


ALTER TABLE public.org_domain OWNER TO "trustdeck-manager";

--
-- Name: policy_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.policy_config (
    policy_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


ALTER TABLE public.policy_config OWNER TO "trustdeck-manager";

--
-- Name: protocol_mapper; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.protocol_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    protocol character varying(255) NOT NULL,
    protocol_mapper_name character varying(255) NOT NULL,
    client_id character varying(36),
    client_scope_id character varying(36)
);


ALTER TABLE public.protocol_mapper OWNER TO "trustdeck-manager";

--
-- Name: protocol_mapper_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.protocol_mapper_config (
    protocol_mapper_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.protocol_mapper_config OWNER TO "trustdeck-manager";

--
-- Name: realm; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm (
    id character varying(36) NOT NULL,
    access_code_lifespan integer,
    user_action_lifespan integer,
    access_token_lifespan integer,
    account_theme character varying(255),
    admin_theme character varying(255),
    email_theme character varying(255),
    enabled boolean DEFAULT false NOT NULL,
    events_enabled boolean DEFAULT false NOT NULL,
    events_expiration bigint,
    login_theme character varying(255),
    name character varying(255),
    not_before integer,
    password_policy character varying(2550),
    registration_allowed boolean DEFAULT false NOT NULL,
    remember_me boolean DEFAULT false NOT NULL,
    reset_password_allowed boolean DEFAULT false NOT NULL,
    social boolean DEFAULT false NOT NULL,
    ssl_required character varying(255),
    sso_idle_timeout integer,
    sso_max_lifespan integer,
    update_profile_on_soc_login boolean DEFAULT false NOT NULL,
    verify_email boolean DEFAULT false NOT NULL,
    master_admin_client character varying(36),
    login_lifespan integer,
    internationalization_enabled boolean DEFAULT false NOT NULL,
    default_locale character varying(255),
    reg_email_as_username boolean DEFAULT false NOT NULL,
    admin_events_enabled boolean DEFAULT false NOT NULL,
    admin_events_details_enabled boolean DEFAULT false NOT NULL,
    edit_username_allowed boolean DEFAULT false NOT NULL,
    otp_policy_counter integer DEFAULT 0,
    otp_policy_window integer DEFAULT 1,
    otp_policy_period integer DEFAULT 30,
    otp_policy_digits integer DEFAULT 6,
    otp_policy_alg character varying(36) DEFAULT 'HmacSHA1'::character varying,
    otp_policy_type character varying(36) DEFAULT 'totp'::character varying,
    browser_flow character varying(36),
    registration_flow character varying(36),
    direct_grant_flow character varying(36),
    reset_credentials_flow character varying(36),
    client_auth_flow character varying(36),
    offline_session_idle_timeout integer DEFAULT 0,
    revoke_refresh_token boolean DEFAULT false NOT NULL,
    access_token_life_implicit integer DEFAULT 0,
    login_with_email_allowed boolean DEFAULT true NOT NULL,
    duplicate_emails_allowed boolean DEFAULT false NOT NULL,
    docker_auth_flow character varying(36),
    refresh_token_max_reuse integer DEFAULT 0,
    allow_user_managed_access boolean DEFAULT false NOT NULL,
    sso_max_lifespan_remember_me integer DEFAULT 0 NOT NULL,
    sso_idle_timeout_remember_me integer DEFAULT 0 NOT NULL,
    default_role character varying(255)
);


ALTER TABLE public.realm OWNER TO "trustdeck-manager";

--
-- Name: realm_attribute; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_attribute (
    name character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    value text
);


ALTER TABLE public.realm_attribute OWNER TO "trustdeck-manager";

--
-- Name: realm_default_groups; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_default_groups (
    realm_id character varying(36) NOT NULL,
    group_id character varying(36) NOT NULL
);


ALTER TABLE public.realm_default_groups OWNER TO "trustdeck-manager";

--
-- Name: realm_enabled_event_types; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_enabled_event_types (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.realm_enabled_event_types OWNER TO "trustdeck-manager";

--
-- Name: realm_events_listeners; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_events_listeners (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.realm_events_listeners OWNER TO "trustdeck-manager";

--
-- Name: realm_localizations; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_localizations (
    realm_id character varying(255) NOT NULL,
    locale character varying(255) NOT NULL,
    texts text NOT NULL
);


ALTER TABLE public.realm_localizations OWNER TO "trustdeck-manager";

--
-- Name: realm_required_credential; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_required_credential (
    type character varying(255) NOT NULL,
    form_label character varying(255),
    input boolean DEFAULT false NOT NULL,
    secret boolean DEFAULT false NOT NULL,
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.realm_required_credential OWNER TO "trustdeck-manager";

--
-- Name: realm_smtp_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_smtp_config (
    realm_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.realm_smtp_config OWNER TO "trustdeck-manager";

--
-- Name: realm_supported_locales; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.realm_supported_locales (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.realm_supported_locales OWNER TO "trustdeck-manager";

--
-- Name: redirect_uris; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.redirect_uris (
    client_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.redirect_uris OWNER TO "trustdeck-manager";

--
-- Name: required_action_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.required_action_config (
    required_action_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.required_action_config OWNER TO "trustdeck-manager";

--
-- Name: required_action_provider; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.required_action_provider (
    id character varying(36) NOT NULL,
    alias character varying(255),
    name character varying(255),
    realm_id character varying(36),
    enabled boolean DEFAULT false NOT NULL,
    default_action boolean DEFAULT false NOT NULL,
    provider_id character varying(255),
    priority integer
);


ALTER TABLE public.required_action_provider OWNER TO "trustdeck-manager";

--
-- Name: resource_attribute; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_attribute (
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255),
    resource_id character varying(36) NOT NULL
);


ALTER TABLE public.resource_attribute OWNER TO "trustdeck-manager";

--
-- Name: resource_policy; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_policy (
    resource_id character varying(36) NOT NULL,
    policy_id character varying(36) NOT NULL
);


ALTER TABLE public.resource_policy OWNER TO "trustdeck-manager";

--
-- Name: resource_scope; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_scope (
    resource_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


ALTER TABLE public.resource_scope OWNER TO "trustdeck-manager";

--
-- Name: resource_server; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_server (
    id character varying(36) NOT NULL,
    allow_rs_remote_mgmt boolean DEFAULT false NOT NULL,
    policy_enforce_mode smallint NOT NULL,
    decision_strategy smallint DEFAULT 1 NOT NULL
);


ALTER TABLE public.resource_server OWNER TO "trustdeck-manager";

--
-- Name: resource_server_perm_ticket; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_server_perm_ticket (
    id character varying(36) NOT NULL,
    owner character varying(255) NOT NULL,
    requester character varying(255) NOT NULL,
    created_timestamp bigint NOT NULL,
    granted_timestamp bigint,
    resource_id character varying(36) NOT NULL,
    scope_id character varying(36),
    resource_server_id character varying(36) NOT NULL,
    policy_id character varying(36)
);


ALTER TABLE public.resource_server_perm_ticket OWNER TO "trustdeck-manager";

--
-- Name: resource_server_policy; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_server_policy (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    type character varying(255) NOT NULL,
    decision_strategy smallint,
    logic smallint,
    resource_server_id character varying(36) NOT NULL,
    owner character varying(255)
);


ALTER TABLE public.resource_server_policy OWNER TO "trustdeck-manager";

--
-- Name: resource_server_resource; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_server_resource (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255),
    icon_uri character varying(255),
    owner character varying(255) NOT NULL,
    resource_server_id character varying(36) NOT NULL,
    owner_managed_access boolean DEFAULT false NOT NULL,
    display_name character varying(255)
);


ALTER TABLE public.resource_server_resource OWNER TO "trustdeck-manager";

--
-- Name: resource_server_scope; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_server_scope (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    icon_uri character varying(255),
    resource_server_id character varying(36) NOT NULL,
    display_name character varying(255)
);


ALTER TABLE public.resource_server_scope OWNER TO "trustdeck-manager";

--
-- Name: resource_uris; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.resource_uris (
    resource_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.resource_uris OWNER TO "trustdeck-manager";

--
-- Name: revoked_token; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.revoked_token (
    id character varying(255) NOT NULL,
    expire bigint NOT NULL
);


ALTER TABLE public.revoked_token OWNER TO "trustdeck-manager";

--
-- Name: role_attribute; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.role_attribute (
    id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255)
);


ALTER TABLE public.role_attribute OWNER TO "trustdeck-manager";

--
-- Name: scope_mapping; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.scope_mapping (
    client_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


ALTER TABLE public.scope_mapping OWNER TO "trustdeck-manager";

--
-- Name: scope_policy; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.scope_policy (
    scope_id character varying(36) NOT NULL,
    policy_id character varying(36) NOT NULL
);


ALTER TABLE public.scope_policy OWNER TO "trustdeck-manager";

--
-- Name: server_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.server_config (
    server_config_key character varying(255) NOT NULL,
    value text NOT NULL,
    version integer DEFAULT 0
);


ALTER TABLE public.server_config OWNER TO "trustdeck-manager";

--
-- Name: user_attribute; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_attribute (
    name character varying(255) NOT NULL,
    value character varying(255),
    user_id character varying(36) NOT NULL,
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL,
    long_value_hash bytea,
    long_value_hash_lower_case bytea,
    long_value text
);


ALTER TABLE public.user_attribute OWNER TO "trustdeck-manager";

--
-- Name: user_consent; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_consent (
    id character varying(36) NOT NULL,
    client_id character varying(255),
    user_id character varying(36) NOT NULL,
    created_date bigint,
    last_updated_date bigint,
    client_storage_provider character varying(36),
    external_client_id character varying(255)
);


ALTER TABLE public.user_consent OWNER TO "trustdeck-manager";

--
-- Name: user_consent_client_scope; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_consent_client_scope (
    user_consent_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


ALTER TABLE public.user_consent_client_scope OWNER TO "trustdeck-manager";

--
-- Name: user_entity; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_entity (
    id character varying(36) NOT NULL,
    email character varying(255),
    email_constraint character varying(255),
    email_verified boolean DEFAULT false NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    federation_link character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    realm_id character varying(255),
    username character varying(255),
    created_timestamp bigint,
    service_account_client_link character varying(255),
    not_before integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.user_entity OWNER TO "trustdeck-manager";

--
-- Name: user_federation_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_federation_config (
    user_federation_provider_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.user_federation_config OWNER TO "trustdeck-manager";

--
-- Name: user_federation_mapper; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_federation_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    federation_provider_id character varying(36) NOT NULL,
    federation_mapper_type character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.user_federation_mapper OWNER TO "trustdeck-manager";

--
-- Name: user_federation_mapper_config; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_federation_mapper_config (
    user_federation_mapper_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.user_federation_mapper_config OWNER TO "trustdeck-manager";

--
-- Name: user_federation_provider; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_federation_provider (
    id character varying(36) NOT NULL,
    changed_sync_period integer,
    display_name character varying(255),
    full_sync_period integer,
    last_sync integer,
    priority integer,
    provider_name character varying(255),
    realm_id character varying(36)
);


ALTER TABLE public.user_federation_provider OWNER TO "trustdeck-manager";

--
-- Name: user_group_membership; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_group_membership (
    group_id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL,
    membership_type character varying(255) NOT NULL
);


ALTER TABLE public.user_group_membership OWNER TO "trustdeck-manager";

--
-- Name: user_required_action; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_required_action (
    user_id character varying(36) NOT NULL,
    required_action character varying(255) DEFAULT ' '::character varying NOT NULL
);


ALTER TABLE public.user_required_action OWNER TO "trustdeck-manager";

--
-- Name: user_role_mapping; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.user_role_mapping (
    role_id character varying(255) NOT NULL,
    user_id character varying(36) NOT NULL
);


ALTER TABLE public.user_role_mapping OWNER TO "trustdeck-manager";

--
-- Name: web_origins; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.web_origins (
    client_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.web_origins OWNER TO "trustdeck-manager";

--
-- Name: workflow_state; Type: TABLE; Schema: public; Owner: trustdeck-manager
--

CREATE TABLE public.workflow_state (
    execution_id character varying(255) NOT NULL,
    resource_id character varying(255) NOT NULL,
    workflow_id character varying(255) NOT NULL,
    workflow_provider_id character varying(255),
    resource_type character varying(255),
    scheduled_step_id character varying(255),
    scheduled_step_timestamp bigint
);


ALTER TABLE public.workflow_state OWNER TO "trustdeck-manager";

--
-- Data for Name: admin_event_entity; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: associated_policy; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: authentication_execution; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('7eed02d8-9a68-44ba-a5ee-6b2c6c4ac3c8', NULL, 'auth-cookie', 'master', 'ce2d4e14-e0d7-4fb0-9ba1-388e3ec46b1a', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('59f8f223-85e2-43f1-ae04-531aa2bd68d8', NULL, 'auth-spnego', 'master', 'ce2d4e14-e0d7-4fb0-9ba1-388e3ec46b1a', 3, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('544f8b60-97c9-4e42-989d-6df8d205c814', NULL, 'identity-provider-redirector', 'master', 'ce2d4e14-e0d7-4fb0-9ba1-388e3ec46b1a', 2, 25, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('0a949018-3c90-48d5-a033-4bb6d929bb35', NULL, NULL, 'master', 'ce2d4e14-e0d7-4fb0-9ba1-388e3ec46b1a', 2, 30, true, '40ed377f-3be8-419d-9a88-5872c77d5056', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('508bbb2e-afae-41ec-9224-1ddf99a846c3', NULL, 'auth-username-password-form', 'master', '40ed377f-3be8-419d-9a88-5872c77d5056', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('d754be4b-f330-4ae3-9d4d-a186392ea8bf', NULL, NULL, 'master', '40ed377f-3be8-419d-9a88-5872c77d5056', 1, 20, true, '6bd78730-cb02-4a1b-a8f6-e729c710592f', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('b6ca293a-293f-4b1e-ab3b-a1e84ed7c8bd', NULL, 'conditional-user-configured', 'master', '6bd78730-cb02-4a1b-a8f6-e729c710592f', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('54b1f589-f746-481f-9401-0ffe51ff87c3', NULL, 'auth-otp-form', 'master', '6bd78730-cb02-4a1b-a8f6-e729c710592f', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('ed2e088b-325e-45d7-b92f-2b50d7c522cd', NULL, 'direct-grant-validate-username', 'master', 'ecff1be7-9d5e-4de0-9dc0-fb978f82dfbd', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('d5a49dfb-f076-4273-a1d9-fb86633fa563', NULL, 'direct-grant-validate-password', 'master', 'ecff1be7-9d5e-4de0-9dc0-fb978f82dfbd', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('1991d09f-81de-4827-9fd5-59d0959e8c6e', NULL, NULL, 'master', 'ecff1be7-9d5e-4de0-9dc0-fb978f82dfbd', 1, 30, true, '37fa5397-4669-46f9-b6ee-5392237b328e', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('a1283329-87ac-48c7-bdee-bcf7b81a2f1f', NULL, 'conditional-user-configured', 'master', '37fa5397-4669-46f9-b6ee-5392237b328e', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('29eee9bd-51a9-4086-9421-ec00fb67d0b5', NULL, 'direct-grant-validate-otp', 'master', '37fa5397-4669-46f9-b6ee-5392237b328e', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('659bb227-d1da-42b6-a3b4-425e6f1df009', NULL, 'registration-page-form', 'master', 'a0b128f9-cca8-4ff2-a2d1-50d3e92e87c5', 0, 10, true, 'ad4ec3e3-f3eb-4aa9-81ea-f371ae11c01a', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('19d8887d-0207-4d32-beb4-67220dec8139', NULL, 'registration-user-creation', 'master', 'ad4ec3e3-f3eb-4aa9-81ea-f371ae11c01a', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('ea2ab2b6-a1d8-45d9-975d-96640c1d559c', NULL, 'registration-password-action', 'master', 'ad4ec3e3-f3eb-4aa9-81ea-f371ae11c01a', 0, 50, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('09392b04-2f62-46ec-b24e-c59c2e6ea37e', NULL, 'registration-recaptcha-action', 'master', 'ad4ec3e3-f3eb-4aa9-81ea-f371ae11c01a', 3, 60, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('dda563fb-db41-4aef-8b1a-7e70a3e9b3b9', NULL, 'reset-credentials-choose-user', 'master', 'abb4c92e-d6d3-4618-bab7-8a458f5c9b16', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('8f2b1f13-89ff-4fa9-ba5c-c392a8eb6d73', NULL, 'reset-credential-email', 'master', 'abb4c92e-d6d3-4618-bab7-8a458f5c9b16', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('802d0ec1-7a09-4bfe-907c-130877011fe4', NULL, 'reset-password', 'master', 'abb4c92e-d6d3-4618-bab7-8a458f5c9b16', 0, 30, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('404f3efc-8120-4488-8397-cb843047bdbb', NULL, NULL, 'master', 'abb4c92e-d6d3-4618-bab7-8a458f5c9b16', 1, 40, true, '539a7746-dbc4-4143-ab7a-d02991959367', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('57cd4adc-6336-4a83-8a90-9559301b2834', NULL, 'conditional-user-configured', 'master', '539a7746-dbc4-4143-ab7a-d02991959367', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('588f99eb-6be0-4408-95b8-5c0603fc863b', NULL, 'reset-otp', 'master', '539a7746-dbc4-4143-ab7a-d02991959367', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('15b2983e-6965-42de-aba7-3cb0d99a4e07', NULL, 'client-secret', 'master', '4168c80d-15c5-43d3-8369-ccf0237f909a', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('247dc5bf-456a-4b38-82f3-1bcdc17259de', NULL, 'client-jwt', 'master', '4168c80d-15c5-43d3-8369-ccf0237f909a', 2, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('811070d6-466a-480f-bbb3-0020e1c4a5b9', NULL, 'client-secret-jwt', 'master', '4168c80d-15c5-43d3-8369-ccf0237f909a', 2, 30, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('2b1deec4-1626-482d-a26d-99e2b43d4c74', NULL, 'client-x509', 'master', '4168c80d-15c5-43d3-8369-ccf0237f909a', 2, 40, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('f405c53b-29a1-4066-b51c-31e54bc3bca6', NULL, 'idp-review-profile', 'master', '10feac09-fca5-4030-85ae-b0cc4c6ba38a', 0, 10, false, NULL, 'b93bda5f-adf2-43db-bc08-09c4c72be0c5');
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('d13e625a-5cd3-46f6-855b-5ffeca86a977', NULL, NULL, 'master', '10feac09-fca5-4030-85ae-b0cc4c6ba38a', 0, 20, true, 'bd58caf7-529a-4cd1-ad06-5ee2832e25d2', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('e2081ade-b885-44d3-8e2d-ec613215dcb6', NULL, 'idp-create-user-if-unique', 'master', 'bd58caf7-529a-4cd1-ad06-5ee2832e25d2', 2, 10, false, NULL, '209beb6d-231b-482e-845d-cb6582f2b320');
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('72d11887-083d-49f2-9649-cd4453143c60', NULL, NULL, 'master', 'bd58caf7-529a-4cd1-ad06-5ee2832e25d2', 2, 20, true, '86460394-8bc8-4f5c-98ab-059f18fcc315', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('3d77fb32-02eb-458a-a40e-cfe77be14568', NULL, 'idp-confirm-link', 'master', '86460394-8bc8-4f5c-98ab-059f18fcc315', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('baae2d18-7017-4142-8054-897dc984cd5a', NULL, NULL, 'master', '86460394-8bc8-4f5c-98ab-059f18fcc315', 0, 20, true, '439079a5-1973-4937-a134-f6ad8e83462f', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('4b45e2fc-e188-409c-9bb0-0b0f0aa6c252', NULL, 'idp-email-verification', 'master', '439079a5-1973-4937-a134-f6ad8e83462f', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('80df4cbc-2647-4614-a357-0d04057644b3', NULL, NULL, 'master', '439079a5-1973-4937-a134-f6ad8e83462f', 2, 20, true, '4412342e-3757-484d-af03-d7364c8f3548', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('a32b8aa3-bdf8-486c-ad00-6608aa0bfe76', NULL, 'idp-username-password-form', 'master', '4412342e-3757-484d-af03-d7364c8f3548', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('7281ec19-fa45-4fbd-8e50-43c0cd1478df', NULL, NULL, 'master', '4412342e-3757-484d-af03-d7364c8f3548', 1, 20, true, '37ab3b5f-5c1f-4bf4-adf1-1c8a27c41421', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('6e9a5630-a38f-4496-b385-96e36a054d45', NULL, 'conditional-user-configured', 'master', '37ab3b5f-5c1f-4bf4-adf1-1c8a27c41421', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('c312f0c1-a9a8-4c6a-ae4a-9790126ae42c', NULL, 'auth-otp-form', 'master', '37ab3b5f-5c1f-4bf4-adf1-1c8a27c41421', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('24f9fdd7-e620-4a22-8ded-3991994394b1', NULL, 'http-basic-authenticator', 'master', '6a77c3f7-c801-4bee-a315-dae4a299036c', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('f8378404-fa8c-48ce-ac79-4abd1e18983b', NULL, 'docker-http-basic-authenticator', 'master', '0367ff4e-3e8d-4b52-a7fa-1ec95778554f', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('9697a071-2098-400d-ae36-8ad6c0976126', NULL, 'auth-cookie', 'development', 'e2ddcf02-28b9-429d-a8e8-f06a3fe786a5', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('78785988-4dad-4346-9ed2-68db8a397c38', NULL, 'auth-spnego', 'development', 'e2ddcf02-28b9-429d-a8e8-f06a3fe786a5', 3, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('c5ad5f05-9339-4e90-bf6e-3aa6e9f94c52', NULL, 'identity-provider-redirector', 'development', 'e2ddcf02-28b9-429d-a8e8-f06a3fe786a5', 2, 25, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('9f1a55a1-78fd-4054-9efc-d4e9e0fb10d8', NULL, NULL, 'development', 'e2ddcf02-28b9-429d-a8e8-f06a3fe786a5', 2, 30, true, '3651611f-0bdf-4629-a92f-17649ca88506', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('1f200ca7-f272-4262-b107-a653e9afb836', NULL, 'auth-username-password-form', 'development', '3651611f-0bdf-4629-a92f-17649ca88506', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('b35010e7-ef4b-4baf-94d9-f8e26cb28084', NULL, NULL, 'development', '3651611f-0bdf-4629-a92f-17649ca88506', 1, 20, true, '3bca2e87-8233-4108-bd56-81f026d0c0ed', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('fcca9778-3b08-4902-a621-4f33c00bf18c', NULL, 'conditional-user-configured', 'development', '3bca2e87-8233-4108-bd56-81f026d0c0ed', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('88ac3f9b-2be6-4353-af1f-328d6f4267a6', NULL, 'auth-otp-form', 'development', '3bca2e87-8233-4108-bd56-81f026d0c0ed', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('5665e01e-a1f9-48ca-8869-bbfff6f76baf', NULL, 'direct-grant-validate-username', 'development', '0ecb5636-f5b7-4ff8-b47e-db1c9fc3b50d', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('16e12c52-5a48-488a-a873-8d9f02988db6', NULL, 'direct-grant-validate-password', 'development', '0ecb5636-f5b7-4ff8-b47e-db1c9fc3b50d', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('de39c8a9-c610-44b5-9c3f-29904b620730', NULL, NULL, 'development', '0ecb5636-f5b7-4ff8-b47e-db1c9fc3b50d', 1, 30, true, '27073d10-e393-4e9e-8462-ed6c6944ae5d', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('f71d4f5b-695b-4829-bbeb-7fabef093703', NULL, 'conditional-user-configured', 'development', '27073d10-e393-4e9e-8462-ed6c6944ae5d', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('ff99c574-8574-4fc8-a2df-97bfab3a939f', NULL, 'direct-grant-validate-otp', 'development', '27073d10-e393-4e9e-8462-ed6c6944ae5d', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('a5a33d91-6045-4640-b298-97ab9d95dcee', NULL, 'registration-page-form', 'development', 'c5976461-e400-4fe5-8253-18d4fc358c1a', 0, 10, true, '2afd4555-d4c8-4b37-986e-1aa20e6c331a', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('d3f1bf5b-c9d7-4673-a350-bb8adc910809', NULL, 'registration-user-creation', 'development', '2afd4555-d4c8-4b37-986e-1aa20e6c331a', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('5fab369e-be96-4d11-bcb0-6b7a99210c78', NULL, 'registration-password-action', 'development', '2afd4555-d4c8-4b37-986e-1aa20e6c331a', 0, 50, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('cabb6cf3-d7d5-41f6-bd2f-057d45e56c67', NULL, 'registration-recaptcha-action', 'development', '2afd4555-d4c8-4b37-986e-1aa20e6c331a', 3, 60, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('792054ad-046e-473b-8746-0e717aa52139', NULL, 'reset-credentials-choose-user', 'development', 'd2393001-e20d-45f2-ac91-0904d5c59539', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('a407da86-e794-4da5-9054-b0ce06099631', NULL, 'reset-credential-email', 'development', 'd2393001-e20d-45f2-ac91-0904d5c59539', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('7e7520d5-48d3-4c28-b5e6-8924d2ff57cb', NULL, 'reset-password', 'development', 'd2393001-e20d-45f2-ac91-0904d5c59539', 0, 30, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('87404848-1aef-4165-a0fa-42d8b2c63704', NULL, NULL, 'development', 'd2393001-e20d-45f2-ac91-0904d5c59539', 1, 40, true, '8311c9b2-a6ad-4a84-8212-bc3e440de265', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('5045de4c-f91f-4a51-bbc3-11982261c94a', NULL, 'conditional-user-configured', 'development', '8311c9b2-a6ad-4a84-8212-bc3e440de265', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('912337ca-6ff6-4bee-9464-0b2f8b78f7e7', NULL, 'reset-otp', 'development', '8311c9b2-a6ad-4a84-8212-bc3e440de265', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('baa9dd81-5e6f-4cde-868e-c61f3adfb152', NULL, 'client-secret', 'development', 'b952af4b-074e-4ed9-a4f2-b1be38a53edd', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('a7964860-5d04-4e6e-a3eb-4aab4b925ccf', NULL, 'client-jwt', 'development', 'b952af4b-074e-4ed9-a4f2-b1be38a53edd', 2, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('d191c7a6-3b4c-4bc0-b9ac-fbde41791cc3', NULL, 'client-secret-jwt', 'development', 'b952af4b-074e-4ed9-a4f2-b1be38a53edd', 2, 30, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('c81b299c-8ee4-4a5f-9706-ed9922512078', NULL, 'client-x509', 'development', 'b952af4b-074e-4ed9-a4f2-b1be38a53edd', 2, 40, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('1b115428-4411-4482-b467-677ac9fbbff9', NULL, 'idp-review-profile', 'development', '74132866-cda2-4a80-96f4-f2a6814bc4de', 0, 10, false, NULL, 'b075369c-2763-4462-bbbb-f8c5729e1cb9');
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('ae504026-9c60-4521-aab8-9cff4a96cb25', NULL, NULL, 'development', '74132866-cda2-4a80-96f4-f2a6814bc4de', 0, 20, true, '04764a48-d157-4021-9279-23413fbeaeaa', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('ed405d76-e4df-4941-93ab-3b1263bc8718', NULL, 'idp-create-user-if-unique', 'development', '04764a48-d157-4021-9279-23413fbeaeaa', 2, 10, false, NULL, 'a3b29b3d-c79e-4950-b072-1b50f2582372');
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('53696894-493d-4459-b8d5-40fb390a7a9e', NULL, NULL, 'development', '04764a48-d157-4021-9279-23413fbeaeaa', 2, 20, true, 'c53664c3-f0d1-43eb-b457-ddf04adf7163', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('25f1ad2e-2a94-41f6-86df-abcbe86e6634', NULL, 'idp-confirm-link', 'development', 'c53664c3-f0d1-43eb-b457-ddf04adf7163', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('f5214b3b-a2d6-4625-92bf-ae4620c3fe15', NULL, NULL, 'development', 'c53664c3-f0d1-43eb-b457-ddf04adf7163', 0, 20, true, 'd49b7a9c-9631-489b-beb8-6eac2d84d176', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('8005540c-b5d6-42fc-b6fe-743d4089a207', NULL, 'idp-email-verification', 'development', 'd49b7a9c-9631-489b-beb8-6eac2d84d176', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('b236554c-2358-4fc2-901f-1a7b8542a7d9', NULL, NULL, 'development', 'd49b7a9c-9631-489b-beb8-6eac2d84d176', 2, 20, true, 'fd309606-b3e0-42be-8bfb-d7e69ee05ee7', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('e3480425-7a6a-430d-8fde-5d7a25f776fe', NULL, 'idp-username-password-form', 'development', 'fd309606-b3e0-42be-8bfb-d7e69ee05ee7', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('5797c9c6-c9be-4e3e-81c5-d98a93c2aa0c', NULL, NULL, 'development', 'fd309606-b3e0-42be-8bfb-d7e69ee05ee7', 1, 20, true, 'c3ac8f54-8486-43f1-b6c7-84f3a98842f3', NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('ef3a7df4-34a0-452c-a2c8-c51c1602086f', NULL, 'conditional-user-configured', 'development', 'c3ac8f54-8486-43f1-b6c7-84f3a98842f3', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('e362db65-5246-48c3-bc31-c447e5029341', NULL, 'auth-otp-form', 'development', 'c3ac8f54-8486-43f1-b6c7-84f3a98842f3', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('473f57e9-4a33-486b-b9a4-578a06d4f99b', NULL, 'http-basic-authenticator', 'development', '065f7d95-f8fe-40ee-907e-3e3ad3e14140', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution (id, alias, authenticator, realm_id, flow_id, requirement, priority, authenticator_flow, auth_flow_id, auth_config) VALUES ('63d40f19-8161-4100-89c9-93b6ac7ddb39', NULL, 'docker-http-basic-authenticator', 'development', '957edc7c-0a36-4a5b-92b8-372d9dd7abc5', 0, 10, false, NULL, NULL);


--
-- Data for Name: authentication_flow; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('ce2d4e14-e0d7-4fb0-9ba1-388e3ec46b1a', 'browser', 'browser based authentication', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('40ed377f-3be8-419d-9a88-5872c77d5056', 'forms', 'Username, password, otp and other auth forms.', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('6bd78730-cb02-4a1b-a8f6-e729c710592f', 'Browser - Conditional OTP', 'Flow to determine if the OTP is required for the authentication', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('ecff1be7-9d5e-4de0-9dc0-fb978f82dfbd', 'direct grant', 'OpenID Connect Resource Owner Grant', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('37fa5397-4669-46f9-b6ee-5392237b328e', 'Direct Grant - Conditional OTP', 'Flow to determine if the OTP is required for the authentication', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('a0b128f9-cca8-4ff2-a2d1-50d3e92e87c5', 'registration', 'registration flow', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('ad4ec3e3-f3eb-4aa9-81ea-f371ae11c01a', 'registration form', 'registration form', 'master', 'form-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('abb4c92e-d6d3-4618-bab7-8a458f5c9b16', 'reset credentials', 'Reset credentials for a user if they forgot their password or something', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('539a7746-dbc4-4143-ab7a-d02991959367', 'Reset - Conditional OTP', 'Flow to determine if the OTP should be reset or not. Set to REQUIRED to force.', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('4168c80d-15c5-43d3-8369-ccf0237f909a', 'clients', 'Base authentication for clients', 'master', 'client-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('10feac09-fca5-4030-85ae-b0cc4c6ba38a', 'first broker login', 'Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('bd58caf7-529a-4cd1-ad06-5ee2832e25d2', 'User creation or linking', 'Flow for the existing/non-existing user alternatives', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('86460394-8bc8-4f5c-98ab-059f18fcc315', 'Handle Existing Account', 'Handle what to do if there is existing account with same email/username like authenticated identity provider', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('439079a5-1973-4937-a134-f6ad8e83462f', 'Account verification options', 'Method with which to verity the existing account', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('4412342e-3757-484d-af03-d7364c8f3548', 'Verify Existing Account by Re-authentication', 'Reauthentication of existing account', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('37ab3b5f-5c1f-4bf4-adf1-1c8a27c41421', 'First broker login - Conditional OTP', 'Flow to determine if the OTP is required for the authentication', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('6a77c3f7-c801-4bee-a315-dae4a299036c', 'saml ecp', 'SAML ECP Profile Authentication Flow', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('0367ff4e-3e8d-4b52-a7fa-1ec95778554f', 'docker auth', 'Used by Docker clients to authenticate against the IDP', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('e2ddcf02-28b9-429d-a8e8-f06a3fe786a5', 'browser', 'browser based authentication', 'development', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('3651611f-0bdf-4629-a92f-17649ca88506', 'forms', 'Username, password, otp and other auth forms.', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('3bca2e87-8233-4108-bd56-81f026d0c0ed', 'Browser - Conditional OTP', 'Flow to determine if the OTP is required for the authentication', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('0ecb5636-f5b7-4ff8-b47e-db1c9fc3b50d', 'direct grant', 'OpenID Connect Resource Owner Grant', 'development', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('27073d10-e393-4e9e-8462-ed6c6944ae5d', 'Direct Grant - Conditional OTP', 'Flow to determine if the OTP is required for the authentication', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('c5976461-e400-4fe5-8253-18d4fc358c1a', 'registration', 'registration flow', 'development', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('2afd4555-d4c8-4b37-986e-1aa20e6c331a', 'registration form', 'registration form', 'development', 'form-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('d2393001-e20d-45f2-ac91-0904d5c59539', 'reset credentials', 'Reset credentials for a user if they forgot their password or something', 'development', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('8311c9b2-a6ad-4a84-8212-bc3e440de265', 'Reset - Conditional OTP', 'Flow to determine if the OTP should be reset or not. Set to REQUIRED to force.', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('b952af4b-074e-4ed9-a4f2-b1be38a53edd', 'clients', 'Base authentication for clients', 'development', 'client-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('74132866-cda2-4a80-96f4-f2a6814bc4de', 'first broker login', 'Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account', 'development', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('04764a48-d157-4021-9279-23413fbeaeaa', 'User creation or linking', 'Flow for the existing/non-existing user alternatives', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('c53664c3-f0d1-43eb-b457-ddf04adf7163', 'Handle Existing Account', 'Handle what to do if there is existing account with same email/username like authenticated identity provider', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('d49b7a9c-9631-489b-beb8-6eac2d84d176', 'Account verification options', 'Method with which to verity the existing account', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('fd309606-b3e0-42be-8bfb-d7e69ee05ee7', 'Verify Existing Account by Re-authentication', 'Reauthentication of existing account', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('c3ac8f54-8486-43f1-b6c7-84f3a98842f3', 'First broker login - Conditional OTP', 'Flow to determine if the OTP is required for the authentication', 'development', 'basic-flow', false, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('065f7d95-f8fe-40ee-907e-3e3ad3e14140', 'saml ecp', 'SAML ECP Profile Authentication Flow', 'development', 'basic-flow', true, true);
INSERT INTO public.authentication_flow (id, alias, description, realm_id, provider_id, top_level, built_in) VALUES ('957edc7c-0a36-4a5b-92b8-372d9dd7abc5', 'docker auth', 'Used by Docker clients to authenticate against the IDP', 'development', 'basic-flow', true, true);


--
-- Data for Name: authenticator_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.authenticator_config (id, alias, realm_id) VALUES ('b93bda5f-adf2-43db-bc08-09c4c72be0c5', 'review profile config', 'master');
INSERT INTO public.authenticator_config (id, alias, realm_id) VALUES ('209beb6d-231b-482e-845d-cb6582f2b320', 'create unique user config', 'master');
INSERT INTO public.authenticator_config (id, alias, realm_id) VALUES ('b075369c-2763-4462-bbbb-f8c5729e1cb9', 'review profile config', 'development');
INSERT INTO public.authenticator_config (id, alias, realm_id) VALUES ('a3b29b3d-c79e-4950-b072-1b50f2582372', 'create unique user config', 'development');


--
-- Data for Name: authenticator_config_entry; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.authenticator_config_entry (authenticator_id, value, name) VALUES ('b93bda5f-adf2-43db-bc08-09c4c72be0c5', 'missing', 'update.profile.on.first.login');
INSERT INTO public.authenticator_config_entry (authenticator_id, value, name) VALUES ('209beb6d-231b-482e-845d-cb6582f2b320', 'false', 'require.password.update.after.registration');
INSERT INTO public.authenticator_config_entry (authenticator_id, value, name) VALUES ('b075369c-2763-4462-bbbb-f8c5729e1cb9', 'missing', 'update.profile.on.first.login');
INSERT INTO public.authenticator_config_entry (authenticator_id, value, name) VALUES ('a3b29b3d-c79e-4950-b072-1b50f2582372', 'false', 'require.password.update.after.registration');


--
-- Data for Name: broker_link; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: client; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', true, false, 'master-realm', 0, false, NULL, NULL, true, NULL, false, 'master', NULL, 0, false, false, 'master Realm', false, 'client-secret', NULL, NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, false, 'account', 0, true, NULL, '/realms/master/account/', false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_account}', false, 'client-secret', '${authBaseUrl}', NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', true, false, 'account-console', 0, true, NULL, '/realms/master/account/', false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_account-console}', false, 'client-secret', '${authBaseUrl}', NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', true, false, 'broker', 0, false, NULL, NULL, true, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_broker}', false, 'client-secret', NULL, NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('297eff51-12d4-43da-9069-f632024daf18', true, false, 'development-realm', 0, false, NULL, NULL, true, NULL, false, 'master', NULL, 0, false, false, 'development Realm', false, 'client-secret', NULL, NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', true, false, 'realm-management', 0, false, NULL, NULL, true, NULL, false, 'development', 'openid-connect', 0, false, false, '${client_realm-management}', false, 'client-secret', NULL, NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', true, false, 'account', 0, true, NULL, '/realms/development/account/', false, NULL, false, 'development', 'openid-connect', 0, false, false, '${client_account}', false, 'client-secret', '${authBaseUrl}', NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', true, false, 'account-console', 0, true, NULL, '/realms/development/account/', false, NULL, false, 'development', 'openid-connect', 0, false, false, '${client_account-console}', false, 'client-secret', '${authBaseUrl}', NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', true, false, 'broker', 0, false, NULL, NULL, true, NULL, false, 'development', 'openid-connect', 0, false, false, '${client_broker}', false, 'client-secret', NULL, NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', true, true, 'security-admin-console', 0, true, NULL, '/admin/master/console/', false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_security-admin-console}', false, 'client-secret', '${authAdminUrl}', NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', true, true, 'admin-cli', 0, true, NULL, NULL, false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_admin-cli}', false, 'client-secret', NULL, NULL, NULL, false, false, true, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', true, true, 'security-admin-console', 0, true, NULL, '/admin/development/console/', false, NULL, false, 'development', 'openid-connect', 0, false, false, '${client_security-admin-console}', false, 'client-secret', '${authAdminUrl}', NULL, NULL, true, false, false, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', true, true, 'admin-cli', 0, true, NULL, NULL, false, NULL, false, 'development', 'openid-connect', 0, false, false, '${client_admin-cli}', false, 'client-secret', NULL, NULL, NULL, false, false, true, false);
INSERT INTO public.client (id, enabled, full_scope_allowed, client_id, not_before, public_client, secret, base_url, bearer_only, management_url, surrogate_auth_required, realm_id, protocol, node_rereg_timeout, frontchannel_logout, consent_required, name, service_accounts_enabled, client_authenticator_type, root_url, description, registration_token, standard_flow_enabled, implicit_flow_enabled, direct_access_grants_enabled, always_display_in_console) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', true, true, 'trustdeck', 0, false, '1h6T3Dnx45hrd4pgv7YdcIfP9GRarbpN', '', false, '', false, 'development', 'openid-connect', -1, false, false, '', true, 'client-secret', '', '', NULL, true, false, true, false);


--
-- Data for Name: client_attributes; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.client_attributes (client_id, name, value) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', 'pkce.code.challenge.method', 'S256');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', 'pkce.code.challenge.method', 'S256');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', 'pkce.code.challenge.method', 'S256');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', 'pkce.code.challenge.method', 'S256');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'backchannel.logout.session.required', 'true');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'backchannel.logout.revoke.offline.tokens', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.artifact.binding', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.server.signature', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.server.signature.keyinfo.ext', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.assertion.signature', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.client.signature', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.encrypt', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.authnstatement', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.onetimeuse.condition', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml_force_name_id_format', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.multivalued.roles', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'saml.force.post.binding', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'exclude.session.state.from.auth.response', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'oauth2.device.authorization.grant.enabled', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'oidc.ciba.grant.enabled', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'use.refresh.tokens', 'true');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'id.token.as.detached.signature', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'tls.client.certificate.bound.access.tokens', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'require.pushed.authorization.requests', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'client_credentials.use_refresh_token', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'display.on.consent.screen', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', 'post.logout.redirect.uris', '+');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', 'post.logout.redirect.uris', '+');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', 'post.logout.redirect.uris', '+');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', 'post.logout.redirect.uris', '+');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'post.logout.redirect.uris', '+');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', 'post.logout.redirect.uris', '+');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', 'post.logout.redirect.uris', '+');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', 'client.use.lightweight.access.token.enabled', 'true');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', 'client.use.lightweight.access.token.enabled', 'true');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', 'client.use.lightweight.access.token.enabled', 'true');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', 'client.use.lightweight.access.token.enabled', 'true');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'realm_client', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'standard.token.exchange.enabled', 'false');
INSERT INTO public.client_attributes (client_id, name, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'dpop.bound.access.tokens', 'false');


--
-- Data for Name: client_auth_flow_bindings; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: client_initial_access; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: client_node_registrations; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: client_scope; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('9999bbc8-5f73-4a21-aa56-797628fa5b5f', 'offline_access', 'master', 'OpenID Connect built-in scope: offline_access', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('22487cb4-ae45-40c0-bf42-6cc8a0522d49', 'role_list', 'master', 'SAML role list', 'saml');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('7d3289fd-f5f7-4c73-a857-6506457c1e6c', 'profile', 'master', 'OpenID Connect built-in scope: profile', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('e8f953e9-1796-475d-bfca-e6d7b2c911c4', 'email', 'master', 'OpenID Connect built-in scope: email', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('cce07087-b8b0-4793-890f-5c84fefa9439', 'address', 'master', 'OpenID Connect built-in scope: address', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('b6f91cd7-ee91-49dd-8159-b595d810694b', 'phone', 'master', 'OpenID Connect built-in scope: phone', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('241020fc-78d9-4971-b9ec-a906bda7b14c', 'roles', 'master', 'OpenID Connect scope for add user roles to the access token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('e32926f4-d4af-44cf-99e9-4d5eceb55a35', 'web-origins', 'master', 'OpenID Connect scope for add allowed web origins to the access token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('2007079b-9593-4a89-905e-3a492a1a6135', 'microprofile-jwt', 'master', 'Microprofile - JWT built-in scope', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('b3b6dccf-46b0-4199-9d2e-a1908cb98623', 'offline_access', 'development', 'OpenID Connect built-in scope: offline_access', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('c1f5c722-af78-4138-b5e5-8b9c5a22fd58', 'role_list', 'development', 'SAML role list', 'saml');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('2838ffe1-c64d-49c1-8d36-6e6966f22b92', 'profile', 'development', 'OpenID Connect built-in scope: profile', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', 'email', 'development', 'OpenID Connect built-in scope: email', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('2a598f0a-e595-46b6-9641-7e305f447623', 'address', 'development', 'OpenID Connect built-in scope: address', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('053548c2-191c-44b2-82a5-1aa9f2804735', 'phone', 'development', 'OpenID Connect built-in scope: phone', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('5ef28d69-868b-4f07-80f9-d5284e4f82a9', 'roles', 'development', 'OpenID Connect scope for add user roles to the access token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('16915d69-1f21-416e-8194-04783e07e47c', 'web-origins', 'development', 'OpenID Connect scope for add allowed web origins to the access token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('80982e4f-2393-44de-8207-6ce9526f3fb3', 'microprofile-jwt', 'development', 'Microprofile - JWT built-in scope', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('3515bf6a-cb15-45db-b55c-6ec21ac99990', 'acr', 'master', 'OpenID Connect scope for add acr (authentication context class reference) to the token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('e22d2191-0ebe-40c6-9d8c-a3c6cc44aaf3', 'acr', 'development', 'OpenID Connect scope for add acr (authentication context class reference) to the token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('7ef392a1-6728-40ac-9bf6-381f1fbc3b20', 'basic', 'master', 'OpenID Connect scope for add all basic claims to the token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('25df115c-d573-4b3e-bf3b-db291bfbceff', 'basic', 'development', 'OpenID Connect scope for add all basic claims to the token', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('0530c3dd-3064-4438-b252-3aa26b879293', 'service_account', 'master', 'Specific scope for a client enabled for service accounts', 'openid-connect');
INSERT INTO public.client_scope (id, name, realm_id, description, protocol) VALUES ('b55992d7-358a-4282-8162-43f01e1da5ec', 'service_account', 'development', 'Specific scope for a client enabled for service accounts', 'openid-connect');


--
-- Data for Name: client_scope_attributes; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('9999bbc8-5f73-4a21-aa56-797628fa5b5f', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('9999bbc8-5f73-4a21-aa56-797628fa5b5f', '${offlineAccessScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('22487cb4-ae45-40c0-bf42-6cc8a0522d49', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('22487cb4-ae45-40c0-bf42-6cc8a0522d49', '${samlRoleListScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('7d3289fd-f5f7-4c73-a857-6506457c1e6c', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('7d3289fd-f5f7-4c73-a857-6506457c1e6c', '${profileScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('7d3289fd-f5f7-4c73-a857-6506457c1e6c', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e8f953e9-1796-475d-bfca-e6d7b2c911c4', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e8f953e9-1796-475d-bfca-e6d7b2c911c4', '${emailScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e8f953e9-1796-475d-bfca-e6d7b2c911c4', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('cce07087-b8b0-4793-890f-5c84fefa9439', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('cce07087-b8b0-4793-890f-5c84fefa9439', '${addressScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('cce07087-b8b0-4793-890f-5c84fefa9439', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('b6f91cd7-ee91-49dd-8159-b595d810694b', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('b6f91cd7-ee91-49dd-8159-b595d810694b', '${phoneScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('b6f91cd7-ee91-49dd-8159-b595d810694b', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('241020fc-78d9-4971-b9ec-a906bda7b14c', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('241020fc-78d9-4971-b9ec-a906bda7b14c', '${rolesScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('241020fc-78d9-4971-b9ec-a906bda7b14c', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e32926f4-d4af-44cf-99e9-4d5eceb55a35', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e32926f4-d4af-44cf-99e9-4d5eceb55a35', '', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e32926f4-d4af-44cf-99e9-4d5eceb55a35', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2007079b-9593-4a89-905e-3a492a1a6135', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2007079b-9593-4a89-905e-3a492a1a6135', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('b3b6dccf-46b0-4199-9d2e-a1908cb98623', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('b3b6dccf-46b0-4199-9d2e-a1908cb98623', '${offlineAccessScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('c1f5c722-af78-4138-b5e5-8b9c5a22fd58', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('c1f5c722-af78-4138-b5e5-8b9c5a22fd58', '${samlRoleListScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2838ffe1-c64d-49c1-8d36-6e6966f22b92', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2838ffe1-c64d-49c1-8d36-6e6966f22b92', '${profileScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2838ffe1-c64d-49c1-8d36-6e6966f22b92', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', '${emailScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2a598f0a-e595-46b6-9641-7e305f447623', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2a598f0a-e595-46b6-9641-7e305f447623', '${addressScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('2a598f0a-e595-46b6-9641-7e305f447623', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('053548c2-191c-44b2-82a5-1aa9f2804735', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('053548c2-191c-44b2-82a5-1aa9f2804735', '${phoneScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('053548c2-191c-44b2-82a5-1aa9f2804735', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('5ef28d69-868b-4f07-80f9-d5284e4f82a9', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('5ef28d69-868b-4f07-80f9-d5284e4f82a9', '${rolesScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('5ef28d69-868b-4f07-80f9-d5284e4f82a9', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('16915d69-1f21-416e-8194-04783e07e47c', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('16915d69-1f21-416e-8194-04783e07e47c', '', 'consent.screen.text');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('16915d69-1f21-416e-8194-04783e07e47c', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('80982e4f-2393-44de-8207-6ce9526f3fb3', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('80982e4f-2393-44de-8207-6ce9526f3fb3', 'true', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('3515bf6a-cb15-45db-b55c-6ec21ac99990', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('3515bf6a-cb15-45db-b55c-6ec21ac99990', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e22d2191-0ebe-40c6-9d8c-a3c6cc44aaf3', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('e22d2191-0ebe-40c6-9d8c-a3c6cc44aaf3', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('7ef392a1-6728-40ac-9bf6-381f1fbc3b20', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('7ef392a1-6728-40ac-9bf6-381f1fbc3b20', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('25df115c-d573-4b3e-bf3b-db291bfbceff', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('25df115c-d573-4b3e-bf3b-db291bfbceff', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('0530c3dd-3064-4438-b252-3aa26b879293', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('0530c3dd-3064-4438-b252-3aa26b879293', 'false', 'include.in.token.scope');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('b55992d7-358a-4282-8162-43f01e1da5ec', 'false', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes (scope_id, value, name) VALUES ('b55992d7-358a-4282-8162-43f01e1da5ec', 'false', 'include.in.token.scope');


--
-- Data for Name: client_scope_client; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', '7d3289fd-f5f7-4c73-a857-6506457c1e6c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', 'e32926f4-d4af-44cf-99e9-4d5eceb55a35', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', 'e8f953e9-1796-475d-bfca-e6d7b2c911c4', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', '241020fc-78d9-4971-b9ec-a906bda7b14c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', '2007079b-9593-4a89-905e-3a492a1a6135', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', 'b6f91cd7-ee91-49dd-8159-b595d810694b', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', 'cce07087-b8b0-4793-890f-5c84fefa9439', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', '9999bbc8-5f73-4a21-aa56-797628fa5b5f', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '7d3289fd-f5f7-4c73-a857-6506457c1e6c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', 'e32926f4-d4af-44cf-99e9-4d5eceb55a35', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', 'e8f953e9-1796-475d-bfca-e6d7b2c911c4', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '241020fc-78d9-4971-b9ec-a906bda7b14c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '2007079b-9593-4a89-905e-3a492a1a6135', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', 'b6f91cd7-ee91-49dd-8159-b595d810694b', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', 'cce07087-b8b0-4793-890f-5c84fefa9439', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '9999bbc8-5f73-4a21-aa56-797628fa5b5f', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', '7d3289fd-f5f7-4c73-a857-6506457c1e6c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', 'e32926f4-d4af-44cf-99e9-4d5eceb55a35', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', 'e8f953e9-1796-475d-bfca-e6d7b2c911c4', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', '241020fc-78d9-4971-b9ec-a906bda7b14c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', '2007079b-9593-4a89-905e-3a492a1a6135', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', 'b6f91cd7-ee91-49dd-8159-b595d810694b', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', 'cce07087-b8b0-4793-890f-5c84fefa9439', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', '9999bbc8-5f73-4a21-aa56-797628fa5b5f', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', '7d3289fd-f5f7-4c73-a857-6506457c1e6c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', 'e32926f4-d4af-44cf-99e9-4d5eceb55a35', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', 'e8f953e9-1796-475d-bfca-e6d7b2c911c4', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', '241020fc-78d9-4971-b9ec-a906bda7b14c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', '2007079b-9593-4a89-905e-3a492a1a6135', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', 'b6f91cd7-ee91-49dd-8159-b595d810694b', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', 'cce07087-b8b0-4793-890f-5c84fefa9439', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('082d9499-a226-4009-af33-5c10e7a18333', '9999bbc8-5f73-4a21-aa56-797628fa5b5f', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', '7d3289fd-f5f7-4c73-a857-6506457c1e6c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', 'e32926f4-d4af-44cf-99e9-4d5eceb55a35', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', 'e8f953e9-1796-475d-bfca-e6d7b2c911c4', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', '241020fc-78d9-4971-b9ec-a906bda7b14c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', '2007079b-9593-4a89-905e-3a492a1a6135', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', 'b6f91cd7-ee91-49dd-8159-b595d810694b', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', 'cce07087-b8b0-4793-890f-5c84fefa9439', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('d8a35e32-b522-425f-a314-77e6b313330c', '9999bbc8-5f73-4a21-aa56-797628fa5b5f', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', '7d3289fd-f5f7-4c73-a857-6506457c1e6c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', 'e32926f4-d4af-44cf-99e9-4d5eceb55a35', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', 'e8f953e9-1796-475d-bfca-e6d7b2c911c4', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', '241020fc-78d9-4971-b9ec-a906bda7b14c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', '2007079b-9593-4a89-905e-3a492a1a6135', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', 'b6f91cd7-ee91-49dd-8159-b595d810694b', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', 'cce07087-b8b0-4793-890f-5c84fefa9439', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', '9999bbc8-5f73-4a21-aa56-797628fa5b5f', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('51bba6d9-f73d-4f45-8b06-392303051f3c', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2090c53c-83d2-4087-b22a-88d4c12730e4', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', '7ef392a1-6728-40ac-9bf6-381f1fbc3b20', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '7ef392a1-6728-40ac-9bf6-381f1fbc3b20', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', '7ef392a1-6728-40ac-9bf6-381f1fbc3b20', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('a49877bc-ff43-4dcd-8a21-c423e5fb895f', '7ef392a1-6728-40ac-9bf6-381f1fbc3b20', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '25df115c-d573-4b3e-bf3b-db291bfbceff', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '25df115c-d573-4b3e-bf3b-db291bfbceff', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '25df115c-d573-4b3e-bf3b-db291bfbceff', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('5c7b1c4b-aff8-47d7-b732-4aba58db74b2', '25df115c-d573-4b3e-bf3b-db291bfbceff', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '25df115c-d573-4b3e-bf3b-db291bfbceff', true);
INSERT INTO public.client_scope_client (client_id, scope_id, default_scope) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', 'b55992d7-358a-4282-8162-43f01e1da5ec', true);


--
-- Data for Name: client_scope_role_mapping; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.client_scope_role_mapping (scope_id, role_id) VALUES ('9999bbc8-5f73-4a21-aa56-797628fa5b5f', 'f10a139c-29b3-4236-86d1-98baedb83512');
INSERT INTO public.client_scope_role_mapping (scope_id, role_id) VALUES ('b3b6dccf-46b0-4199-9d2e-a1908cb98623', 'd611163f-3327-43ee-a7ea-790110d80df3');


--
-- Data for Name: component; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('327c25f6-84d9-48a7-89d8-de7fa38bbd08', 'Trusted Hosts', 'master', 'trusted-hosts', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('ec8647f4-5fb9-48d3-9442-9aaa4d01f1a8', 'Consent Required', 'master', 'consent-required', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('886e2e28-dbb9-44cc-b6b2-0c1ace9801cf', 'Full Scope Disabled', 'master', 'scope', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('3bcc0b97-cb51-4073-af52-9e0d41759494', 'Max Clients Limit', 'master', 'max-clients', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'Allowed Protocol Mapper Types', 'master', 'allowed-protocol-mappers', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('c496dcb4-32ee-410d-9d4d-964d080731d3', 'Allowed Client Scopes', 'master', 'allowed-client-templates', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'Allowed Protocol Mapper Types', 'master', 'allowed-protocol-mappers', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'authenticated');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('607d325d-7f5a-48f6-bf52-c45617e65541', 'Allowed Client Scopes', 'master', 'allowed-client-templates', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'authenticated');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('a94d14fd-01ac-4224-b43b-c4115688295a', 'rsa-generated', 'master', 'rsa-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('74e8f05e-6c92-44f8-a262-c6155d867b84', 'rsa-enc-generated', 'master', 'rsa-enc-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('a107aceb-e7bd-464c-a595-2db924ec80ea', 'hmac-generated', 'master', 'hmac-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('a4adc65f-f9ea-450a-8324-a1af957e66f1', 'aes-generated', 'master', 'aes-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('21d0d3ff-9b05-439c-b91b-d7784f06a529', 'rsa-generated', 'development', 'rsa-generated', 'org.keycloak.keys.KeyProvider', 'development', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('1a2d6b81-7138-44c1-af8e-b3d53f60adc2', 'rsa-enc-generated', 'development', 'rsa-enc-generated', 'org.keycloak.keys.KeyProvider', 'development', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('83589cbd-0149-4039-aa3c-2966bea0c0e2', 'hmac-generated', 'development', 'hmac-generated', 'org.keycloak.keys.KeyProvider', 'development', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('08a41e5e-3a93-460b-aa41-98eb01a8d5ed', 'aes-generated', 'development', 'aes-generated', 'org.keycloak.keys.KeyProvider', 'development', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('3d270a11-d3a5-4eca-a752-a884dc3a34f8', 'Trusted Hosts', 'development', 'trusted-hosts', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('d0691ab1-ff9c-4299-9513-5f1762ff782a', 'Consent Required', 'development', 'consent-required', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('53fb02ab-713e-47b9-9be6-a5c6a6cf13a2', 'Full Scope Disabled', 'development', 'scope', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('96839694-4d29-4f82-9188-0eb8bf409b03', 'Max Clients Limit', 'development', 'max-clients', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('67081642-d074-44cb-bc38-06e5ad4db597', 'Allowed Protocol Mapper Types', 'development', 'allowed-protocol-mappers', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('e960b9d9-6f6e-4030-a460-3ba2102b56b1', 'Allowed Client Scopes', 'development', 'allowed-client-templates', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'anonymous');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('1878d1d1-a2ff-4dde-8de6-15274670e90e', 'Allowed Protocol Mapper Types', 'development', 'allowed-protocol-mappers', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'authenticated');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('00293f25-13f8-48fd-b1fb-3c706e94db05', 'Allowed Client Scopes', 'development', 'allowed-client-templates', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'development', 'authenticated');
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('a9536731-9ec0-45bf-bc51-509650ff3a48', NULL, 'master', 'declarative-user-profile', 'org.keycloak.userprofile.UserProfileProvider', 'master', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('8d502e71-fb01-4431-9c89-594a08043ebc', 'hmac-generated-hs512', 'master', 'hmac-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('946f6bb1-ddd6-4033-8454-47754e7bc026', NULL, 'development', 'declarative-user-profile', 'org.keycloak.userprofile.UserProfileProvider', 'development', NULL);
INSERT INTO public.component (id, name, parent_id, provider_id, provider_type, realm_id, sub_type) VALUES ('35b8caa2-f491-4589-b36c-0a510aee7c14', 'hmac-generated-hs512', 'development', 'hmac-generated', 'org.keycloak.keys.KeyProvider', 'development', NULL);


--
-- Data for Name: component_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.component_config (id, component_id, name, value) VALUES ('8d51535d-997a-4628-8c91-7699177d7c6f', 'c496dcb4-32ee-410d-9d4d-964d080731d3', 'allow-default-scopes', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('a3ce98d3-8916-49d7-8d20-a58d64894c04', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'saml-role-list-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('fd7aa267-41cc-40a8-8a76-5b6569c8391e', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'oidc-address-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('7d75807d-806d-4728-8878-5f4dac09effb', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'saml-user-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('32fd5c39-4f70-460f-8712-26eb6fe82215', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'saml-user-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('efda198d-e0ce-45b0-b550-d146a66d6c6d', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'saml-role-list-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('05219a2c-bd3c-4099-8e0a-92885726b851', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'saml-user-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('99095c5f-6be5-4aef-90c2-0ed6ecaf94ba', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'oidc-usermodel-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('ee0d7b02-acf2-4b4f-b78b-7b159d4e5222', '74e8f05e-6c92-44f8-a262-c6155d867b84', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('c8fada7b-2b09-443c-bbce-385740f5c3f7', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'oidc-full-name-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('af0358e7-037d-4acb-ba70-2b7fb1b18717', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'oidc-sha256-pairwise-sub-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('73973e32-0937-428a-abff-ad14762d677b', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'oidc-usermodel-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('266762ab-393a-4410-970e-575ce7e7081b', 'a7130712-f8b2-4ee7-ade6-a43f7aab8e7b', 'allowed-protocol-mapper-types', 'oidc-usermodel-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('3f74bb52-7334-4876-94d0-6b289190175b', '327c25f6-84d9-48a7-89d8-de7fa38bbd08', 'host-sending-registration-request-must-match', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('b1e2d061-54cd-49f6-b1ec-59cce8a4d6a3', '327c25f6-84d9-48a7-89d8-de7fa38bbd08', 'client-uris-must-match', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('1b93c3ff-7cdd-4243-aa8c-bdeb687eef81', '3bcc0b97-cb51-4073-af52-9e0d41759494', 'max-clients', '200');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('0b959c3f-c753-4c3e-9c92-2c83607454b2', '607d325d-7f5a-48f6-bf52-c45617e65541', 'allow-default-scopes', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('110bb5d4-74dc-46a8-85c0-d769c7b1ad49', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'oidc-full-name-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('eb8aae52-af6c-4ab0-9838-78734c366087', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'oidc-sha256-pairwise-sub-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('1c2d2ca7-fad3-4b5f-aa67-071eab7c4914', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'saml-user-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('af36d33f-9b6d-421e-a74a-b1dfe9db76c7', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'oidc-usermodel-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('3be8d71c-a8d3-4808-8bd7-d98899fe9176', '08a41e5e-3a93-460b-aa41-98eb01a8d5ed', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('e7f125d4-5325-4dd8-b69f-cec116e0eea3', 'c62919b8-1ab7-4cf8-a887-1ef460ead6e2', 'allowed-protocol-mapper-types', 'oidc-address-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('fb27611b-43f7-40a8-b727-6173f726935c', 'a94d14fd-01ac-4224-b43b-c4115688295a', 'certificate', 'MIICmzCCAYMCBgF/Pzd0+jANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjIwMjI4MDcyMjUzWhcNMzIwMjI4MDcyNDMzWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC8hDy654pWsDIZO7MVQrDRU5n8wxNYjbTKnSUw+EwPemN3GU2nmD/oEZISvilsXWR4SAtV67M25qoie1HINKHJdD7QBS8y6CkBe1SkC3hwuiSVqGw2BL4P3SKZzuwCWpkGaOERmmODUGlEAbKUZvoFZ8cbaWSu4uenWYsA1Ee4PB7yZMQXnXUkTQjFgyZu6jgQyMP9G86nWiDn38HTp4n2+Snbn0FevdNgf3um63KPST8pBidCcX0196Fy5jzeenhMeGZP0nY6+dB3xdPuIGs/wc2Ky8pBijyzDyINhzdBm1luE6aLQL3KHArv/psd8MdvahcqDGHKe+tBink4XpN3AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAA2PSbomy0LrWCkbvxgYYsCNLtWcN/kDXiNC/f7fnJyE3fJPoe0VOJ9g0xkZxl7BVXQAZZiNSbBYPyXRnWZkxnO62UIzI1xwQTO13PX1iUFefwdjxv11MbCyiTcwVaGohU5YcQxeDTX2hUap6A0L19JePsrOcxBT4OVFw5P2SV4FoVhSlAWOlSnDz5vE+1oYKjaV8gP3EpGYA2UhzgbN1wFfn3RFTrNio7CL7fQJl6SRsthCL8kaZtKSpP9+5mEtn3xD14OtQSKAYXzsNR7w6ZnFiwkheTllpeXma7NAwg5FePRgXacQaJJJp+di+Y46aQUGoFPsy9BImdRxq75vYCk=');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('4c20c582-3b70-4fb9-ac2e-68092fd9179b', 'a94d14fd-01ac-4224-b43b-c4115688295a', 'privateKey', 'MIIEpAIBAAKCAQEAvIQ8uueKVrAyGTuzFUKw0VOZ/MMTWI20yp0lMPhMD3pjdxlNp5g/6BGSEr4pbF1keEgLVeuzNuaqIntRyDShyXQ+0AUvMugpAXtUpAt4cLoklahsNgS+D90imc7sAlqZBmjhEZpjg1BpRAGylGb6BWfHG2lkruLnp1mLANRHuDwe8mTEF511JE0IxYMmbuo4EMjD/RvOp1og59/B06eJ9vkp259BXr3TYH97putyj0k/KQYnQnF9NfehcuY83np4THhmT9J2OvnQd8XT7iBrP8HNisvKQYo8sw8iDYc3QZtZbhOmi0C9yhwK7/6bHfDHb2oXKgxhynvrQYp5OF6TdwIDAQABAoIBADJxL87TJbEMdFtMHh8SOT3JoOqBSCvpvvZ4FfuEdNWvae30V1MA6hiumudo+qyEUN3eaSoUZ1/JWLzhQoDPlHrTH/uJZ5e3h0FePsemShPfuupQpqPNoQ2dniSJuZznoQqWBaMwLHB2k7QewCn1Y/EOU7iB+u7QyCt/SjJumsIw3kQCPq4OkTXoouV2uQ3kls1sBaxfUi94WrdqEEDtPq4nh4XDxd/pG3wNv+c+U3fJH8DuSdFxoswAppYZYfe3fkxvthVSuiB8bRqF61xQRfj/1THeHRPIIarr6Stz99GTt0JUSMkRdVP0Ftu4FrB1XEC9uWBorTsCv2zU25jjbAECgYEA6shsD7S3MaC9fVVswagr5zykFD5VgvpTbkZwJzlZrklI8VcMgtKxq4zXJHawYRfuLNFzyyzy5Si4J/2Y448LpS8ohURtQZ2RLQWgDSV1l2Pjoen/9EHCpM4x+qSG1GDk7rhVyYHLmcTAwubpWF75zSF/KEnoYP8rILAK44w5XW0CgYEAzY13uyfYQoOmikw1tu2NMjNmvlaGyKgzZVHzOq0G20dKiDT+BQiPBpVkMfoRGgTz9EwnsRwZ3EplCuC/czijSqX7wTRgTvZOR4x/YlA0mrsVFlkKN194VNAcZLu6u45TQFgkeZJq4wHy8shlzzv8ZeTlacOdQ4sVE68Zn0gwWfMCgYASCLcUNcNkkn47pFYoQeytZGEOFQOofeXusIZo3OTgmEx8DT2uxtRS4wybhmph6t6mnqgQUTGPHKOYnsghopk4ZPjt06W4xouiM65plBkGOewAQeMhNfPniNcZ841PvumW1J3yTn//HbfYwLfny9PQR+RmidbJrI/5gMw1Uk4NuQKBgQCAsbns651yx4pVvDFFPqXQcG47bWEl8Gl9Xjmy2vIYCCKZ9NO1ww4JSS9aOv7KE5/JrISNVtK6IbnxfZVgRm22JwXgiTJjPcL0+PooS58j4U3qmATVdmiYcVPVndPj6dAl0QnFM+7IAkhhySY1AZGdGhqohVjJGSMxF8gq+b+MEQKBgQCWf7vR88m7R0yqRH3bmybMJueQNmtKO+0zRmyLJ6n1HeQurRc7V27YhNdqtttoijZnnj71FtLGfQEo7g4BvwDaQG+RAcI92MdasTVXwyugd1YRVlUKOEq2ZKi2gidGzCUGIMjanZyUzcPSliZeXjvFhu4AjxgdH/Fvqi/RT5ibow==');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('5a90e23e-e868-4cb4-a4b7-033f6584053a', 'a94d14fd-01ac-4224-b43b-c4115688295a', 'keyUse', 'SIG');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('798b0cb4-2892-4e69-90f9-db098b2bae2c', 'a94d14fd-01ac-4224-b43b-c4115688295a', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('8738c04e-4675-4cec-9b7d-6ed47467da62', 'a107aceb-e7bd-464c-a595-2db924ec80ea', 'kid', '5f0ce6b5-2070-4c7f-bf6c-16c4acf2a807');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('4944c692-db1d-47c4-a818-67f42345afce', 'a107aceb-e7bd-464c-a595-2db924ec80ea', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('1cf15456-b504-4eec-bc78-bc0551381df1', 'a107aceb-e7bd-464c-a595-2db924ec80ea', 'algorithm', 'HS256');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('3c413c09-1e94-42a0-9ba7-a594ccb83727', 'a107aceb-e7bd-464c-a595-2db924ec80ea', 'secret', 'UF7GBx3KPP-fT1vjC9_Wk_GzlxWW91UxRhRKwVgAAb0TuEDf5m1xJ3QvDCpBkMV6SGyUJJxMxpmR0OsuPDh0rQ');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('8e060851-47ec-4393-9f95-faf7bafe45b3', 'a4adc65f-f9ea-450a-8324-a1af957e66f1', 'secret', 'F3r-97hgWonvzIvXehJgTA');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('c7190c6f-fdd1-433b-8b28-18949e38e34c', 'a4adc65f-f9ea-450a-8324-a1af957e66f1', 'kid', '3a957f2d-65f0-4cca-b250-d46418412cdb');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('f0de0cca-bcf1-42d7-b6bf-d4eec88d1f49', 'a4adc65f-f9ea-450a-8324-a1af957e66f1', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('593750fb-c9fe-4ae5-9858-d9b2d07a7b14', '8d502e71-fb01-4431-9c89-594a08043ebc', 'secret', 'F550gyrjwhYhQr3V30brTgUp-S-IEDC1QUgu2N1TdP8QA_1TdAwT63ArmE3z-1mQ1ClC38Kg9jbIhY5BX4qylLOIVNN54ryQaCb9_bxu3V2BKDjQJ82A2evO8uYH-fOv_dj1Jp3fwkjT3c7tGSHMxbXNdsN5YTPVO04XS5QjmhU');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('ad2f59b4-d76f-491a-a46b-a24e76d9e704', '8d502e71-fb01-4431-9c89-594a08043ebc', 'algorithm', 'HS512');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('27645848-10a4-4acc-b69f-766ff9cc1ea2', '35b8caa2-f491-4589-b36c-0a510aee7c14', 'kid', 'c6d2a9dc-d03e-4ab7-bbb5-6ef63a09578a');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('b5b30b00-c87c-4424-8feb-bd4fa1ac645a', '35b8caa2-f491-4589-b36c-0a510aee7c14', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('e8f05d25-e839-45b1-acbe-0ae5a6a3279d', '74e8f05e-6c92-44f8-a262-c6155d867b84', 'privateKey', 'MIIEpAIBAAKCAQEAp7kOFljIFGSxMQJULK2gN3leVP+2JWTOp2eGp3a3MziixqTJUefh8mTYs89EIadlLA2khnTn5m431MTbEtAPXcKr8s2GMuMOiqRDO+9hpv4VLnsJGfm7UGrsagTYmB/WAlktdSjrrjazQuDjQlsfDUdeit5++k5XI4Ejp9oCOvA43maF9ThcoDXYNB1cGfeci4Xz97xYm7cKO2U6GEwpLTyiBRZoyz6h2vops4QJKTo/YYs5uqH5SItQdyuv/opP1R/8pGzon4cXi13VXgvStzZZS8DX+d62npFsBpZDVF9ItBo/SrZpxFW+bUjtVu7iShrnjuQBU4wUD28SWxUCQQIDAQABAoIBABT8So7xPNeQYbtIF1AfyVQnk23/MMMtAc5gXbDGYdW+2F8Sbz4YTWUOyS52WIf9ceM5McTBC+MBF2s/1klcTw/kM6+Fl6z2S/N86gytvwIt+GiAbHsbjBxbxIjfYlpmYViZ394w6Iw7u+I6UTUUQaZfr6ygejLHao++sL9nv6+v+xJmJn9IcABHmEkL/f0LSRyX74sKfsAbDF/1PSZsak1Qzoqag6ciRXZuPv5sSNBY5h+0euTt/yU8Q67JYejXQ9uceWCIuemRGKvYyPCr1d/p5Q8NnBkBH6UAjoyHY+hh6++BFvKMsmf6bkJrAZrkWQnlGeX/elaVrgGoyNn7hiECgYEA0UJmYeqtPDTZ6/YOuEYVmwKCIzq9m6/utsGrIUl+9ld9PHAyyJJxo+NNboe5Yz+95t1/hOlMm1DBQxVufy1DI1kh62BhtZsyruem1OykuwNSRPWJuFGzcV3PquouFZiywyNMgPzf/6l2MgeTBCcuRqd+N0HTwP0ULPcCoLGFMIcCgYEAzS+ReInzy/q7T+AD7jj+iSgTq65yEcHEcJplBTZmxqLzOcSPQ0LbFawh8nidN2AQ/5VyhlGuJyuh0Bim8M9+OSHR9GMylZlXFhDQE+DlA9rcLogVTudxrnVy+GXmSrY/Plcz8SQDtyI3BKDc/zsOKM1g4OgeeheHfnfeQR+PUPcCgYEAvGPT+oklBtPp8RWncNyfrNrYQFEszFHaTiwTHbemFq7zL4svHQVCS7JiToTgMOr80zBMEmNatWVBaFyOCu67x8IB2H7/2Fhti6s/teeJ8lduJGkNYKQlMYWsZQnJDWZYYXeQ83s1SlzM7QOGprGDZtZ5udOIiPEOzeBfPD41U1cCgYA4gvtr2YLeEUZ5r1cly2i/WdxhEoC0R0vVP5DHKL6Rwtt+c5aJjeZNR8VpxLJ4R+smmYCbYe+3Nhsw9zkzZdJ75cavqoqKDVug5iyI7Q3mS+cMocpMmw4CYLKzm00cEqnEf7v8lFC5paDFk5CKczdpIXenOgb61XQp7tW11f1g7QKBgQCta4KhaKF3ge1ZZlFexfjZXAMWbxbcPRIGu/ohYks7+nYMLIZGu5yJjZEm0h8lKa3nINCPwXk+fR7+WOvV0i7bSegRSR5I38E33nK042Ltb/cka0g6ZBNzS0zsbZ6WaIdoqxSuoBHcc3GFPEJ4MUqRJ/N8t+nRbWl+uNEzKLG+KA==');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('6d1b2047-72e2-4da8-8281-43bc2e1a4ee0', '3d270a11-d3a5-4eca-a752-a884dc3a34f8', 'host-sending-registration-request-must-match', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('f7355ea0-6796-4008-be9d-606ac19806a6', '00293f25-13f8-48fd-b1fb-3c706e94db05', 'allow-default-scopes', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('61c17c3f-42fb-4992-9357-0c7a30191a5e', '8d502e71-fb01-4431-9c89-594a08043ebc', 'kid', '0824a445-cec6-494c-a959-87087e69b3eb');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('4dbc119b-ac47-42a3-8816-eed2c9b86ab1', '8d502e71-fb01-4431-9c89-594a08043ebc', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('eb0516d6-f07f-4b16-8906-9a09004ecf6f', '74e8f05e-6c92-44f8-a262-c6155d867b84', 'certificate', 'MIICmzCCAYMCBgF/Pzd12TANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMjIwMjI4MDcyMjUzWhcNMzIwMjI4MDcyNDMzWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCnuQ4WWMgUZLExAlQsraA3eV5U/7YlZM6nZ4andrczOKLGpMlR5+HyZNizz0Qhp2UsDaSGdOfmbjfUxNsS0A9dwqvyzYYy4w6KpEM772Gm/hUuewkZ+btQauxqBNiYH9YCWS11KOuuNrNC4ONCWx8NR16K3n76TlcjgSOn2gI68DjeZoX1OFygNdg0HVwZ95yLhfP3vFibtwo7ZToYTCktPKIFFmjLPqHa+imzhAkpOj9hizm6oflIi1B3K6/+ik/VH/ykbOifhxeLXdVeC9K3NllLwNf53raekWwGlkNUX0i0Gj9KtmnEVb5tSO1W7uJKGueO5AFTjBQPbxJbFQJBAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAGgaXhVQnOo/zaX/eKFVP3DKEMbig8I1/vYXbv55Q2gt7G4SEhjWuuJHddaEw84igMdgS8goYj6Lg8jkIHnGCjRM5fujpkOniZH2R/p/j+eGtdlLsJEM4PHxoMQjbKRd1bxc3JVQEte7wEt2s1xxxJWvc+OWsIjyf9EWsr77e9Jt6Hl3VFOyrVf3OxmNdQZdGLCp3dlRnBX3e0G2s/V3ZgWMdoR7rfDhQfnWfMeTT17+oAM5+A11oX39LMDJNz0gkJ8nU633qw1lyGzowWSF/c3q8CenEGbywZ2Be7cUSUPv+22DoK9jQCUaLZSI1jn8hW1NPcjFI15RZOAoCFECwfM=');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('cfb21859-633c-412f-8a96-a3b7f79d68bf', '74e8f05e-6c92-44f8-a262-c6155d867b84', 'algorithm', 'RSA-OAEP');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('6b9011b6-3e77-464a-a004-e0bdd7167d60', '74e8f05e-6c92-44f8-a262-c6155d867b84', 'keyUse', 'ENC');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('a2cb8dda-337c-4af2-afe8-1b92190076ce', '1a2d6b81-7138-44c1-af8e-b3d53f60adc2', 'privateKey', 'MIIEpAIBAAKCAQEAiByyFbTPQ7k6Kq6lx/jiDPZ7N+80/TEWceBSnR3TVhYjsdaz6tDrH2hbg4y1d6jhN9S9zz3Dd2v9MHNpmCpi1SEhHPkOXjFuZH1tmRrBuPOLoGHGJRQ0L1NrmhEy4pBwDyxSZUhcf2USf1kc696oMKYSSnmL156ZCxTN8qi84jPqsE57kPce/MRh3v0e94Ly0wXmE+1oFDN/hy1qpcy/LctzGuYwXnkP4ABZYgFFlkJ2njgaT8L+UpUQc6TH507SvgJKx0dqRzWkSdXuR65laOa833C6le3+ZpVtLo1DzKNZRW4dnUG5uCH95EKnicgXCcA3TS5CgVXG7h8r9QgRkwIDAQABAoIBAHrlng5nUXWOu2xqgRmMKV8W9q7Md4XdWqy7pRxkH7vtBZG/kHgG5yVWrrqaxAclHyWwGyoSVHlxsgybSM6yBsb1wNZOMqDt9QUbE2hYdm4uyPWpEqXMFkpdAhz8Rnu3etbYaBU3Pv1wH0GJDsoSIr66VG2WTaQEeZ89y668mL+MVvU243hjHIxlaqKJj0f3lgRsE0nAOzaV7GGqKM2gqvERB6WaBKuUU6uP61FIrqw5WRSZoJ1/pgZTFQ+8J9YUrfr2EEmJxrE9WARavPx3NtTH4BP5ibITYWiW34YMa+J34LbxGl7SeS6PQtuykUyR/bUthD+Og1ULxDK32tolJ1ECgYEA3TzW+qmySudYGlUIti2ut9Hvbg0GHgyyBVD0SunCiiDyRtyN0nuHuGVjdxSN8Ks/DpojPrfYNaBt8t5Rki0uwBSMz6Np+vBiCR7crGY8d5M/tdvvZmc4ZfIO5SFjd10fr38F/QMGN717TB9zjK+T91SHq+EipEliX69Rf+4coCcCgYEAnX+6Xd9tXqnWrJUW7vJYPa+8rwOXd1ayXArhdJBmMpXcJ6NAYYfqJ59sECWLoMLvYP/Aj3SmCtXZFyanqxRF3dTnFQ9rQZbaw9GBCwcObc3ny8DzxH4vo/r5haImaDgWdtNrutC9PhZoAos887rOk9g7ZwQ9GPkGgtyh6xyuOrUCgYEAhRAHmOGhu41jHwtP3wqg0CxzHjS1HjOyiqIDHF92Q0/HiDk1D4h+u7g3V7LEICFPHsA0PI+kebSRDIcMdqe4O4zgmC4IGwajYCrwvLwM2Dp3EjFCzst8T0yECunHAk6/NPVoK6G7BqFvW/x6qf4cN6DerNEO3dLJWRWAJjqXevcCgYEAlLBUEnFvQHJYv9+UograsYnqONPMOBb6FZkUFcaJJPKDwEwH6Mu2jKeeDnRTqHbAJTPt58D5TLts0sVkf0eJWg64BhLjcC1p5HoyOGX2AkXksmA8AmIaudWjobxeQ//HfXDFgL22GxpuIRCwf2z5v2vhULUYtS3MLtj6aAtQlQ0CgYBE+Kc6gwNBFtM+aUpBsOl2fgVG1zJmdCWNZM93ycD5IBkpb0YB4MYRSVC2mcO18jSoIicVTb5rWr/c2Gv2vFLcIBMbP7GzXWJMhAWGBIQrHwHX9mjc+ktFpg3TKPyDTlobFUtuqElp+A3kJ67qWwoGv2AeYyrwnzuysYBeqzmLog==');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('59b18860-1a98-4066-b089-e19f1aaef0ab', '1a2d6b81-7138-44c1-af8e-b3d53f60adc2', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('dabbd9f9-97b7-47cd-b784-37ae09a3a024', '1a2d6b81-7138-44c1-af8e-b3d53f60adc2', 'algorithm', 'RSA-OAEP');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('ff2bef69-b43b-4a80-bdbc-069cbe4fdc73', '1a2d6b81-7138-44c1-af8e-b3d53f60adc2', 'keyUse', 'ENC');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('628c1d40-530d-44d6-af95-b6d9ad1c82b2', '1a2d6b81-7138-44c1-af8e-b3d53f60adc2', 'certificate', 'MIICpTCCAY0CBgF/P0ZyrTANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDDAtkZXZlbG9wbWVudDAeFw0yMjAyMjgwNzM5MTVaFw0zMjAyMjgwNzQwNTVaMBYxFDASBgNVBAMMC2RldmVsb3BtZW50MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiByyFbTPQ7k6Kq6lx/jiDPZ7N+80/TEWceBSnR3TVhYjsdaz6tDrH2hbg4y1d6jhN9S9zz3Dd2v9MHNpmCpi1SEhHPkOXjFuZH1tmRrBuPOLoGHGJRQ0L1NrmhEy4pBwDyxSZUhcf2USf1kc696oMKYSSnmL156ZCxTN8qi84jPqsE57kPce/MRh3v0e94Ly0wXmE+1oFDN/hy1qpcy/LctzGuYwXnkP4ABZYgFFlkJ2njgaT8L+UpUQc6TH507SvgJKx0dqRzWkSdXuR65laOa833C6le3+ZpVtLo1DzKNZRW4dnUG5uCH95EKnicgXCcA3TS5CgVXG7h8r9QgRkwIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQACHjp9lipLQvrRMLSLJmFY8TUL1JSh2pKSjEbXLCeN09P++ZpAVo0ujV/pcVLaW84K3l5UOUcPNa6rUjZJsG3AGMSlINMFl9dTLomXE9R+v3wtXRxdgluEIMyiAITX9fEgFDOdyCMeFztdmqrWefSf22mvdj8OBZlsNo9GzdfSzLG8OKhWiapsJjw9zfYkgiD5jBq0tGK2bNVr3gAlXUIwCrou0jhoTpUNt62dUfoFQRD/OwdtGB3P8JLM4QWlVLXuCP9yklbleimXOYkWi6MASYMoC0tBREOfQPlzEJ1PCP/qL/QuxjJnIcg02vcyr4MjWBKrQUJJxchxOuV2zkRe');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('9fa68088-034a-452e-8438-6bdf686af3d3', '21d0d3ff-9b05-439c-b91b-d7784f06a529', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('e86512b1-4190-4792-89b2-bce359c9a096', '21d0d3ff-9b05-439c-b91b-d7784f06a529', 'keyUse', 'SIG');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('7bc6be69-6ead-4f8b-a395-3f2fa866ff6f', 'a9536731-9ec0-45bf-bc51-509650ff3a48', 'kc.user.profile.config', '{"attributes":[{"name":"username","displayName":"${username}","validations":{"length":{"min":3,"max":255},"username-prohibited-characters":{},"up-username-not-idn-homograph":{}},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false},{"name":"email","displayName":"${email}","validations":{"email":{},"length":{"max":255}},"required":{"roles":["user"]},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false},{"name":"firstName","displayName":"${firstName}","validations":{"length":{"max":255},"person-name-prohibited-characters":{}},"required":{"roles":["user"]},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false},{"name":"lastName","displayName":"${lastName}","validations":{"length":{"max":255},"person-name-prohibited-characters":{}},"required":{"roles":["user"]},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false}],"groups":[{"name":"user-metadata","displayHeader":"User metadata","displayDescription":"Attributes, which refer to user metadata"}],"unmanagedAttributePolicy":"ENABLED"}');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('6e7bd710-04ef-4690-84df-15c1597010ee', '21d0d3ff-9b05-439c-b91b-d7784f06a529', 'certificate', 'MIICpTCCAY0CBgF/P0ZxwTANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDDAtkZXZlbG9wbWVudDAeFw0yMjAyMjgwNzM5MTVaFw0zMjAyMjgwNzQwNTVaMBYxFDASBgNVBAMMC2RldmVsb3BtZW50MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoFms0zrz+TnA7ikWhjCn1o9XC2hpamlxRMkhIHYDjmZKqh3+KshrB1YfNVoszkw0cGL2wH85+mrNQZg6UO9ElEHfKZY1cPB1JaKJAGF7x471bvGtkTRt7uS+6yax9f0wu6U4AjSDkZhqmGubaJKF1BPCj/EulzdvJEjdbkHdGwVejTWfEcZJwEm/Ryl95HxaCTuvq5FT9cnevAV/259dwVcizZ9b/z/yut4G9SGwU/Cs3OmncW+5JXg9mRfOuyeQbJW3tEODT7HgN4WawirFJQWDWn5a+xjewtMyytW5v3y8hKP8UaI3W72VK9mr3jjLZgVZjROQVidMX0D7macf6QIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQBvIzQGCOS4Auv1cmGUd49yUqmypXErpAsuhuGZaYO2H4d26osgMKOOvd6fSPB1aJsSwlhsPhy8e6P/uEuV5FoIcSqxocc7AWb58a8Cr0FImSMk4OwhhvPY2IU4/7U8alOAhl/1b/Z6Gf2oBIQWFcBQ9zaetsddSRA6KdOLNfXijTTSZzPcnOpk5OQXewk5gYIrZYPcryrQaGOdi1+bLPjs7J89sDUi0B5XurLTMJciyWnXXp5eWfyDbyUJAWY3rg8b09Om8nD/NRCQbevrE8Tt/WdJ3KYoLg0pK3MbEzjQyJbZaW1FikLBsiyVqdCxw1mr9hR1ZejJe5QeUK07L5NA');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('60336002-d5d3-4d50-a4b3-e87a01b40371', '21d0d3ff-9b05-439c-b91b-d7784f06a529', 'privateKey', 'MIIEowIBAAKCAQEAoFms0zrz+TnA7ikWhjCn1o9XC2hpamlxRMkhIHYDjmZKqh3+KshrB1YfNVoszkw0cGL2wH85+mrNQZg6UO9ElEHfKZY1cPB1JaKJAGF7x471bvGtkTRt7uS+6yax9f0wu6U4AjSDkZhqmGubaJKF1BPCj/EulzdvJEjdbkHdGwVejTWfEcZJwEm/Ryl95HxaCTuvq5FT9cnevAV/259dwVcizZ9b/z/yut4G9SGwU/Cs3OmncW+5JXg9mRfOuyeQbJW3tEODT7HgN4WawirFJQWDWn5a+xjewtMyytW5v3y8hKP8UaI3W72VK9mr3jjLZgVZjROQVidMX0D7macf6QIDAQABAoIBABzOHzLO61cBHfxqmyY+BViQ5WUiO5WnHXbq3q2TJa5mB9Gpk6gh2sA1o2OVMGla3Sy3SnZoJIUsvHSlIQy6/snXT3yMBEumlpTNLulHmdHOh56HgdOM5Ja+tuFNUTjMY/3Bf18+CciU5ck9w2w0rh/b8N6kx+bYSXnuMQEGie65bmV0Ga9KEUJAUPkn8i00iYcf/fyeVVGRr4WutMLmixb9YdNto0L6wID0sSyCMb8YJnptfJuPhxnLi8yjr6+W7ZsyLgMI0VgRsKpNRVPJGweapglQgqlPm6zY7X2QQcu+Fjc2ibqUu+fWda4/WEeeUpKshqNSsTA+/tlDDHkRbQECgYEAzm/hsGSMJErogfb9Yw+mRFu3Tx8zfMLf/+h5WL1wUEezkvE7UHYHTedHDw9pA2NN97EnXtKIFdwYrPx/XlC2Su/fgEns+lWsxTPXoK3sBxHdSMStoItZckJ4eazXxgxLw6yQFb7GcsLqzT2QoSuF/rZhnlGw3wGwsxL89Z5wD8kCgYEAxtkw5g6rnq2ZW4y7oldEW5KQa9eeLzkymrbLyjCSuvxsP13C8g9BTzlfTJamVti3+7hGE81IhIhagdfRQDk+H6ukNRWECdaIKpsAjKifDbtb8gu5jImTFjVovbdiZ446WEvwTlCdOD6TNcT92+eAzez1F9iG7hBrknx501BN3yECgYEApObscpou9EK+LisdR4dVup9E87gpycxlYKfUXxWcZRSRlpyKVACkXw+TTK1zplgqDOT/XRhxynxxXaWpHK7+nyez8mebHHFZvIFoCptDwnX7vMgXJfqKbeI5GIEAL9zcnKb4xdYJ8dxP8bjvEj+RHuGTYwNAdTOFcdaGKLHyNDkCgYAKkvAbu7n6lSarjJ9JsbRL3k5ZvbyGrFb5jTDnDpAu609iSz0bnB4P9XAQ5Y3w3lNiSllHoknx6kJN10kkTib0ji8MN5mu4D6bbWSpFipnE4h/JYp3Y7Vzn+Fy/TZ5ZUHLAjbEI1tI62LuoAwTzoq16Jzb6MK8FBl0T7Q2vXV2oQKBgB9olSU1iRY3/CGDTjl1Rt4wmmVRFWOWqMjsSm802iEfVqTr/nsQdzDuV/G6hhRGkb42RT7mzQrACSFMEEkFECnzoSZWvC7ffS5SSn5PV7qUAYGp8rryXML62CjTBCRvYNDmytBrfLXhK4wuVdKexT4DNDQM0XI+MN2Q0T2fBBEx');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('f54eafeb-bc38-4aa2-b93d-536f8043ef60', '08a41e5e-3a93-460b-aa41-98eb01a8d5ed', 'kid', '0d6fcba2-2da3-4bac-b5b6-62a65010e15b');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('88f50a5b-aa33-46fe-9e5b-122566e88b9a', '08a41e5e-3a93-460b-aa41-98eb01a8d5ed', 'secret', 'k4OXepRGU3GvAzBdhRTfDQ');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('7c3bf77a-872f-49ee-a160-f00b68a306a0', '83589cbd-0149-4039-aa3c-2966bea0c0e2', 'kid', '3c769bbe-6eec-496f-8c19-0a94580ba110');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('c14e23c1-38b8-43d7-84a0-9013dd5d2fb0', '83589cbd-0149-4039-aa3c-2966bea0c0e2', 'secret', 'EbSP4aNPVD0u09YNZMxaej7PQRavZlSnOgJXVrDodb89TJVj073AOELugjMothlgOfAwnYb9OXZiVBWDfg8wfw');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('2f7a8922-6ff3-4f94-8f50-dc772f66a915', '83589cbd-0149-4039-aa3c-2966bea0c0e2', 'algorithm', 'HS256');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('a1f39829-ea50-448e-ba6f-723a8d6e7dc0', '83589cbd-0149-4039-aa3c-2966bea0c0e2', 'priority', '100');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('d03239a2-f69b-4448-940a-fbe8117e5e7e', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'saml-user-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('b7f0f6b0-c9ff-46cf-9d03-ff838f1f57e1', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'saml-user-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('b157214a-6a94-42b7-aa13-28b1c7ca23c1', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'oidc-usermodel-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('585f5ede-5b67-408a-8f5c-a45d8a66f55f', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'oidc-sha256-pairwise-sub-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('049e25f7-5df7-4dd1-a94e-e1cb01ec4632', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'oidc-address-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('bb620ee3-5402-46fe-8617-347992e8b106', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'oidc-usermodel-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('00d15048-ffc4-432b-979a-9faea61e27b7', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'saml-role-list-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('c1368b80-c70b-4ef7-9874-5789fc82ca25', '67081642-d074-44cb-bc38-06e5ad4db597', 'allowed-protocol-mapper-types', 'oidc-full-name-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('0a484739-abe8-4f41-a5af-cf1be730deb2', 'e960b9d9-6f6e-4030-a460-3ba2102b56b1', 'allow-default-scopes', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('6b5c21a7-1e18-448f-bd5b-eef2d04e2328', '96839694-4d29-4f82-9188-0eb8bf409b03', 'max-clients', '200');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('d4454b3b-0a72-44a6-ab8c-5592f6e31c8d', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'oidc-full-name-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('8de5db62-da66-49b8-9e6b-b6bf73a37804', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'saml-role-list-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('57dc7ff5-4500-498e-8b2e-3a252ac4e1fd', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'saml-user-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('e69be9fa-73d1-4de5-a234-53391e5481c4', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'oidc-sha256-pairwise-sub-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('ecdb76aa-d3f7-47de-8d38-abfeb69994d2', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'oidc-usermodel-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('933e6f49-ed11-440e-a14d-5db2858f0f33', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'oidc-usermodel-property-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('f5113353-ffe6-4c11-b268-d30d54c3259c', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'saml-user-attribute-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('b01f09ae-6141-4e35-9f25-08fb49a71675', '1878d1d1-a2ff-4dde-8de6-15274670e90e', 'allowed-protocol-mapper-types', 'oidc-address-mapper');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('4d8cdb6d-7e1e-45b9-9280-4acbe41bf5b0', '3d270a11-d3a5-4eca-a752-a884dc3a34f8', 'client-uris-must-match', 'true');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('f42b2737-2878-49b6-bb01-f8af6e4eec93', '946f6bb1-ddd6-4033-8454-47754e7bc026', 'kc.user.profile.config', '{"attributes":[{"name":"username","displayName":"${username}","validations":{"length":{"min":3,"max":255},"username-prohibited-characters":{},"up-username-not-idn-homograph":{}},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false},{"name":"email","displayName":"${email}","validations":{"email":{},"length":{"max":255}},"required":{"roles":["user"]},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false},{"name":"firstName","displayName":"${firstName}","validations":{"length":{"max":255},"person-name-prohibited-characters":{}},"required":{"roles":["user"]},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false},{"name":"lastName","displayName":"${lastName}","validations":{"length":{"max":255},"person-name-prohibited-characters":{}},"required":{"roles":["user"]},"permissions":{"view":["admin","user"],"edit":["admin","user"]},"multivalued":false}],"groups":[{"name":"user-metadata","displayHeader":"User metadata","displayDescription":"Attributes, which refer to user metadata"}],"unmanagedAttributePolicy":"ENABLED"}');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('1768b224-bb8e-4c8e-aafd-1ecac60e1494', '35b8caa2-f491-4589-b36c-0a510aee7c14', 'secret', 'I_5YBWpzfOL6r57h2AnJyU-jp0zItXNnAuyDZ68keUGkjuwQzx5ZlaPzUACyrsIWDLex4W8XcDc92qh02mXlP91exGgNQgNxVRMgDIqLYmRy-krU2NsjQ7P1CF0iUcCWiU6VcIpZNel8_qYj3wRQbh2B-onJhioF7pg1Bp9Eh4A');
INSERT INTO public.component_config (id, component_id, name, value) VALUES ('fc745c81-4ade-4270-8e1f-cea38d3c853f', '35b8caa2-f491-4589-b36c-0a510aee7c14', 'algorithm', 'HS512');


--
-- Data for Name: composite_role; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'e0d0806a-b318-4b7a-81ac-21a250011133');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'eab5d470-1f6c-4a3b-b695-2991f25bad1b');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '230f61d0-fb05-4c05-8146-6db93b13acf4');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '83d629c9-f726-43d0-9b61-19bf53c55a63');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'dab0de20-8ea9-4832-89de-f4d5504fb9db');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '8974458a-8aa7-49a1-99ff-b37d8a7c7692');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '1e7a2f1c-be44-453e-9d2a-dd269734585d');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '4ce8f947-ee72-43bf-8317-244d1ea60606');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'c4b35d41-4136-4570-96bd-653354add90f');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'd871f132-78e6-4403-af4c-a8b828af732c');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'f379ea05-5388-477e-afbe-70d7342350e8');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'da866b13-59bd-4774-b0e1-982d8e3b5750');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'c09a1a15-48f5-4131-a657-2696a698a63e');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '4c52c5e7-861f-4833-886a-8f25685dec9b');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '269f6cb5-2c16-4724-9069-65d3fb736675');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '1b2e57b8-2621-4e2c-8fb8-cadcbe755852');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '778070c7-349e-483c-9fff-0457eadfbcd9');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'fddaaae9-36f4-4732-8e34-57c27aecc3b4');
INSERT INTO public.composite_role (composite, child_role) VALUES ('dab0de20-8ea9-4832-89de-f4d5504fb9db', '1b2e57b8-2621-4e2c-8fb8-cadcbe755852');
INSERT INTO public.composite_role (composite, child_role) VALUES ('83d629c9-f726-43d0-9b61-19bf53c55a63', '269f6cb5-2c16-4724-9069-65d3fb736675');
INSERT INTO public.composite_role (composite, child_role) VALUES ('83d629c9-f726-43d0-9b61-19bf53c55a63', 'fddaaae9-36f4-4732-8e34-57c27aecc3b4');
INSERT INTO public.composite_role (composite, child_role) VALUES ('8abade97-996e-4b25-a889-e4bc2d1fc387', '2fc3f427-2d06-4b3e-9ac3-3d9ba3b191b1');
INSERT INTO public.composite_role (composite, child_role) VALUES ('8abade97-996e-4b25-a889-e4bc2d1fc387', '412e3737-b214-48e3-9a53-e5eeff2d222e');
INSERT INTO public.composite_role (composite, child_role) VALUES ('412e3737-b214-48e3-9a53-e5eeff2d222e', '4483f956-e449-4adc-90bd-f6d67bb4c555');
INSERT INTO public.composite_role (composite, child_role) VALUES ('34a5646f-1726-4adc-9904-b2ca3566efe2', '6e8c8293-b03f-4d31-893c-8e33d4c6c507');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '8909e49f-c025-4988-9aa4-7e9a40ffc14f');
INSERT INTO public.composite_role (composite, child_role) VALUES ('8abade97-996e-4b25-a889-e4bc2d1fc387', 'f10a139c-29b3-4236-86d1-98baedb83512');
INSERT INTO public.composite_role (composite, child_role) VALUES ('8abade97-996e-4b25-a889-e4bc2d1fc387', 'd947ccfe-6754-4a0b-bb57-3a520a70144c');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'aafba5bd-8890-4c4c-b178-a7ca86104278');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '5470f70b-c2cd-49b6-90bc-6d20fb403b66');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '34cc8105-3714-4b58-9f7c-4d0f02169b43');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '3803571c-5eb3-4714-b9c4-eb5e41203d43');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'b9190dcf-d7ee-4fd6-ac2b-1e311a48f840');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'fe5ad8b5-406e-41d7-9a2e-42385f1925a6');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '3b2a3190-037c-4017-ac90-0f06e7699345');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '16d4e279-a059-4b3d-8eca-cc886d3b00cc');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '095d2494-5f71-45b4-9e41-4fc9b3538628');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'e98d79ce-f38f-4f7e-a247-1f835e3a5f7b');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '99a9bc2e-472d-4ee9-84d1-79c77288c8bf');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '1b5f94f4-443b-451a-9e10-2d582d72c52d');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'ea6aa2ac-489d-44de-a353-0fd663eadedf');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '8a8f1618-4dcd-4a62-86b6-f83077d1ce5d');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '590d2771-11cd-49d9-afd9-3d843ea0fa7f');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '55a0caae-ea26-40b8-b739-4e4cd008ae40');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'c20d3730-b91c-49cf-b9de-b9cd7ad66eb7');
INSERT INTO public.composite_role (composite, child_role) VALUES ('3803571c-5eb3-4714-b9c4-eb5e41203d43', '590d2771-11cd-49d9-afd9-3d843ea0fa7f');
INSERT INTO public.composite_role (composite, child_role) VALUES ('34cc8105-3714-4b58-9f7c-4d0f02169b43', '8a8f1618-4dcd-4a62-86b6-f83077d1ce5d');
INSERT INTO public.composite_role (composite, child_role) VALUES ('34cc8105-3714-4b58-9f7c-4d0f02169b43', 'c20d3730-b91c-49cf-b9de-b9cd7ad66eb7');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', 'b795e084-dc0a-4903-8e4d-06f553bb6fd9');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '3e73b15e-331c-481d-b925-babec65d356b');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '8bc3c47d-126a-49b7-94d7-541ff2ff7804');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '448e437a-b999-419d-81b9-462d3b9faa47');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', 'f65abf79-96d7-46bb-898e-ca591060d130');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '158c3f81-fbe3-438f-b355-780f26b2a962');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '2a1d3fe3-7043-498c-a7b9-fdd88610c5cb');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '46981d80-d5f3-477e-8859-ff0384df5ebc');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '4ba6f226-e44d-4601-8dfd-64c21b052b17');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '4fdcfef1-c38b-4ca0-8182-b1d664709520');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '2f8deaf3-3c26-4901-ad18-4c7a9ca0699a');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '031c3ed2-1559-491b-800a-ca9aef6f923b');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '097a6bd3-7ad7-4c32-b736-1cd6181ec505');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', 'b1e0272c-5f10-45ad-9ec5-1fa8dd8f6a07');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', 'd380a439-4eb0-4f64-b365-184a87dbf1ae');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', 'ba27d6f5-65f7-48a0-8d1c-2d50a96ea936');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '6c7ef695-de97-4ffe-bef7-1607ed2c1b37');
INSERT INTO public.composite_role (composite, child_role) VALUES ('448e437a-b999-419d-81b9-462d3b9faa47', 'd380a439-4eb0-4f64-b365-184a87dbf1ae');
INSERT INTO public.composite_role (composite, child_role) VALUES ('8bc3c47d-126a-49b7-94d7-541ff2ff7804', 'b1e0272c-5f10-45ad-9ec5-1fa8dd8f6a07');
INSERT INTO public.composite_role (composite, child_role) VALUES ('8bc3c47d-126a-49b7-94d7-541ff2ff7804', '6c7ef695-de97-4ffe-bef7-1607ed2c1b37');
INSERT INTO public.composite_role (composite, child_role) VALUES ('03eb0760-ea2d-490f-8608-402662997979', '6b56c8e8-5853-4bc5-a1b9-684867617d18');
INSERT INTO public.composite_role (composite, child_role) VALUES ('03eb0760-ea2d-490f-8608-402662997979', '78750051-2e54-4c61-9f29-380d8fd8d7c1');
INSERT INTO public.composite_role (composite, child_role) VALUES ('78750051-2e54-4c61-9f29-380d8fd8d7c1', '36460f40-0577-4fa4-8ff3-3b3fd8d0fa85');
INSERT INTO public.composite_role (composite, child_role) VALUES ('e11cfec5-3824-45a3-a42c-fcd5058a0e63', '632a9199-7c61-4594-b05b-91b628f59333');
INSERT INTO public.composite_role (composite, child_role) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '30838f74-ad97-4640-b9cb-d5c997518bfa');
INSERT INTO public.composite_role (composite, child_role) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '5e757fff-6d31-4597-b37c-c3cdd4775066');
INSERT INTO public.composite_role (composite, child_role) VALUES ('03eb0760-ea2d-490f-8608-402662997979', 'd611163f-3327-43ee-a7ea-790110d80df3');
INSERT INTO public.composite_role (composite, child_role) VALUES ('03eb0760-ea2d-490f-8608-402662997979', '666ed87a-22da-43e8-8cf0-08cffd1e0b8a');


--
-- Data for Name: credential; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.credential (id, salt, type, user_id, created_date, user_label, secret_data, credential_data, priority, version) VALUES ('8707bee4-8c52-4349-8762-d524ea60ef63', NULL, 'password', '6d478587-a790-46aa-ac3a-133226549795', 1740149478833, NULL, '{"value":"i6xNiSt5h+vJZPnptTIuffiF+X9rvg8vKMwy+Tjk+1s=","salt":"PcMG+DmGMIW0BWO+Jw6Z4A==","additionalParameters":{}}', '{"hashIterations":5,"algorithm":"argon2","additionalParameters":{"hashLength":["32"],"memory":["7168"],"type":["id"],"version":["1.3"],"parallelism":["1"]}}', 10, 0);
INSERT INTO public.credential (id, salt, type, user_id, created_date, user_label, secret_data, credential_data, priority, version) VALUES ('35c26377-fd9a-4db1-a1c8-32355a417357', NULL, 'password', '25003dd9-9c30-4da5-a4cd-79c2a7dab915', 1740149642577, 'My password', '{"value":"tTmFRpl11l758HEkVVicrZRLkWMgaITxMh9iz64fluU=","salt":"YCdaNvUcsnWyrWVevkBdeg==","additionalParameters":{}}', '{"hashIterations":5,"algorithm":"argon2","additionalParameters":{"hashLength":["32"],"memory":["7168"],"type":["id"],"version":["1.3"],"parallelism":["1"]}}', 10, 0);
INSERT INTO public.credential (id, salt, type, user_id, created_date, user_label, secret_data, credential_data, priority, version) VALUES ('6be8d32b-928f-42e2-8fe6-38757e8285a3', NULL, 'password', '3dfb6717-3def-493b-a237-b7345fc42718', 1646034188771, NULL, '{"value":"7H1aCqkJj9g72mSW/+qAEp3msfq9DCb4kJgB3V3nKbQ=","salt":"/sVEhNpSqlJVw/3R25Veig==","additionalParameters":{}}', '{"hashIterations":5,"algorithm":"argon2","additionalParameters":{"hashLength":["32"],"memory":["7168"],"type":["id"],"version":["1.3"],"parallelism":["1"]}}', 10, 0);
INSERT INTO public.credential (id, salt, type, user_id, created_date, user_label, secret_data, credential_data, priority, version) VALUES ('ab6dd9e1-0f23-45ae-8e0d-707fe5a80e10', NULL, 'password', '2719369c-4354-4303-aab3-a5f43a9aa4d6', 1776441425578, 'My password', '{"value":"fky16Ydjg2wgRviCRdrrfaA1hMdd1UqDgLLZtwJeOQw=","salt":"piKF5ygarUwReCloiA4b3Q==","additionalParameters":{}}', '{"hashIterations":5,"algorithm":"argon2","additionalParameters":{"hashLength":["32"],"memory":["7168"],"type":["id"],"version":["1.3"],"parallelism":["1"]}}', 10, 1);


--
-- Data for Name: databasechangelog; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-7.0.0-KEYCLOAK-10443', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-7.0.0.xml', '2022-02-28 08:24:26.639458', 71, 'EXECUTED', '9:fd4ade7b90c3b67fae0bfcfcb42dfb5f', 'addColumn tableName=RESOURCE_SERVER', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('22.0.0-17484', 'keycloak', 'META-INF/jpa-changelog-22.0.0.xml', '2023-08-04 15:15:28.772879', 114, 'EXECUTED', '8:4c3d4e8b142a66fcdf21b89a4dd33301', 'customChange', '', NULL, '4.20.0', NULL, NULL, '1154928737');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.0.0.Final-KEYCLOAK-5461', 'sthorger@redhat.com', 'META-INF/jpa-changelog-1.0.0.Final.xml', '2022-02-28 08:24:22.785912', 1, 'EXECUTED', '9:6f1016664e21e16d26517a4418f5e3df', 'createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.0.0.Final-KEYCLOAK-5461', 'sthorger@redhat.com', 'META-INF/db2-jpa-changelog-1.0.0.Final.xml', '2022-02-28 08:24:22.804134', 2, 'MARK_RAN', '9:828775b1596a07d1200ba1d49e5e3941', 'createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.1.0.Beta1', 'sthorger@redhat.com', 'META-INF/jpa-changelog-1.1.0.Beta1.xml', '2022-02-28 08:24:22.87275', 3, 'EXECUTED', '9:5f090e44a7d595883c1fb61f4b41fd38', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=CLIENT_ATTRIBUTES; createTable tableName=CLIENT_SESSION_NOTE; createTable tableName=APP_NODE_REGISTRATIONS; addColumn table...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.1.0.Final', 'sthorger@redhat.com', 'META-INF/jpa-changelog-1.1.0.Final.xml', '2022-02-28 08:24:22.879514', 4, 'EXECUTED', '9:c07e577387a3d2c04d1adc9aaad8730e', 'renameColumn newColumnName=EVENT_TIME, oldColumnName=TIME, tableName=EVENT_ENTITY', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.2.0.Beta1', 'psilva@redhat.com', 'META-INF/jpa-changelog-1.2.0.Beta1.xml', '2022-02-28 08:24:23.055073', 5, 'EXECUTED', '9:b68ce996c655922dbcd2fe6b6ae72686', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.2.0.Beta1', 'psilva@redhat.com', 'META-INF/db2-jpa-changelog-1.2.0.Beta1.xml', '2022-02-28 08:24:23.062066', 6, 'MARK_RAN', '9:543b5c9989f024fe35c6f6c5a97de88e', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.2.0.RC1', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.2.0.CR1.xml', '2022-02-28 08:24:23.238353', 7, 'EXECUTED', '9:765afebbe21cf5bbca048e632df38336', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.2.0.RC1', 'bburke@redhat.com', 'META-INF/db2-jpa-changelog-1.2.0.CR1.xml', '2022-02-28 08:24:23.243581', 8, 'MARK_RAN', '9:db4a145ba11a6fdaefb397f6dbf829a1', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.2.0.Final', 'keycloak', 'META-INF/jpa-changelog-1.2.0.Final.xml', '2022-02-28 08:24:23.250274', 9, 'EXECUTED', '9:9d05c7be10cdb873f8bcb41bc3a8ab23', 'update tableName=CLIENT; update tableName=CLIENT; update tableName=CLIENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.3.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.3.0.xml', '2022-02-28 08:24:23.400602', 10, 'EXECUTED', '9:18593702353128d53111f9b1ff0b82b8', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=ADMI...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.4.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.4.0.xml', '2022-02-28 08:24:23.475969', 11, 'EXECUTED', '9:6122efe5f090e41a85c0f1c9e52cbb62', 'delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.4.0', 'bburke@redhat.com', 'META-INF/db2-jpa-changelog-1.4.0.xml', '2022-02-28 08:24:23.479523', 12, 'MARK_RAN', '9:e1ff28bf7568451453f844c5d54bb0b5', 'delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.5.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.5.0.xml', '2022-02-28 08:24:23.504833', 13, 'EXECUTED', '9:7af32cd8957fbc069f796b61217483fd', 'delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.6.1_from15', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2022-02-28 08:24:23.534235', 14, 'EXECUTED', '9:6005e15e84714cd83226bf7879f54190', 'addColumn tableName=REALM; addColumn tableName=KEYCLOAK_ROLE; addColumn tableName=CLIENT; createTable tableName=OFFLINE_USER_SESSION; createTable tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_US_SES_PK2, tableName=...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.6.1_from16-pre', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2022-02-28 08:24:23.537311', 15, 'MARK_RAN', '9:bf656f5a2b055d07f314431cae76f06c', 'delete tableName=OFFLINE_CLIENT_SESSION; delete tableName=OFFLINE_USER_SESSION', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.6.1_from16', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2022-02-28 08:24:23.540144', 16, 'MARK_RAN', '9:f8dadc9284440469dcf71e25ca6ab99b', 'dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_US_SES_PK, tableName=OFFLINE_USER_SESSION; dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_CL_SES_PK, tableName=OFFLINE_CLIENT_SESSION; addColumn tableName=OFFLINE_USER_SESSION; update tableName=OF...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.6.1', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2022-02-28 08:24:23.542689', 17, 'EXECUTED', '9:d41d8cd98f00b204e9800998ecf8427e', 'empty', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.7.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.7.0.xml', '2022-02-28 08:24:23.616346', 18, 'EXECUTED', '9:3368ff0be4c2855ee2dd9ca813b38d8e', 'createTable tableName=KEYCLOAK_GROUP; createTable tableName=GROUP_ROLE_MAPPING; createTable tableName=GROUP_ATTRIBUTE; createTable tableName=USER_GROUP_MEMBERSHIP; createTable tableName=REALM_DEFAULT_GROUPS; addColumn tableName=IDENTITY_PROVIDER; ...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.8.0', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.8.0.xml', '2022-02-28 08:24:23.677458', 19, 'EXECUTED', '9:8ac2fb5dd030b24c0570a763ed75ed20', 'addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.8.0-2', 'keycloak', 'META-INF/jpa-changelog-1.8.0.xml', '2022-02-28 08:24:23.683327', 20, 'EXECUTED', '9:f91ddca9b19743db60e3057679810e6c', 'dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.8.0', 'mposolda@redhat.com', 'META-INF/db2-jpa-changelog-1.8.0.xml', '2022-02-28 08:24:23.685994', 21, 'MARK_RAN', '9:831e82914316dc8a57dc09d755f23c51', 'addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.8.0-2', 'keycloak', 'META-INF/db2-jpa-changelog-1.8.0.xml', '2022-02-28 08:24:23.689029', 22, 'MARK_RAN', '9:f91ddca9b19743db60e3057679810e6c', 'dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.9.0', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.9.0.xml', '2022-02-28 08:24:23.743293', 23, 'EXECUTED', '9:bc3d0f9e823a69dc21e23e94c7a94bb1', 'update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=REALM; update tableName=REALM; customChange; dr...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.9.1', 'keycloak', 'META-INF/jpa-changelog-1.9.1.xml', '2022-02-28 08:24:23.74984', 24, 'EXECUTED', '9:c9999da42f543575ab790e76439a2679', 'modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=PUBLIC_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.9.1', 'keycloak', 'META-INF/db2-jpa-changelog-1.9.1.xml', '2022-02-28 08:24:23.752268', 25, 'MARK_RAN', '9:0d6c65c6f58732d81569e77b10ba301d', 'modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('1.9.2', 'keycloak', 'META-INF/jpa-changelog-1.9.2.xml', '2022-02-28 08:24:24.017468', 26, 'EXECUTED', '9:fc576660fc016ae53d2d4778d84d86d0', 'createIndex indexName=IDX_USER_EMAIL, tableName=USER_ENTITY; createIndex indexName=IDX_USER_ROLE_MAPPING, tableName=USER_ROLE_MAPPING; createIndex indexName=IDX_USER_GROUP_MAPPING, tableName=USER_GROUP_MEMBERSHIP; createIndex indexName=IDX_USER_CO...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-2.0.0', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-2.0.0.xml', '2022-02-28 08:24:24.131833', 27, 'EXECUTED', '9:43ed6b0da89ff77206289e87eaa9c024', 'createTable tableName=RESOURCE_SERVER; addPrimaryKey constraintName=CONSTRAINT_FARS, tableName=RESOURCE_SERVER; addUniqueConstraint constraintName=UK_AU8TT6T700S9V50BU18WS5HA6, tableName=RESOURCE_SERVER; createTable tableName=RESOURCE_SERVER_RESOU...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-2.5.1', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-2.5.1.xml', '2022-02-28 08:24:24.136659', 28, 'EXECUTED', '9:44bae577f551b3738740281eceb4ea70', 'update tableName=RESOURCE_SERVER_POLICY', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.1.0-KEYCLOAK-5461', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.1.0.xml', '2022-02-28 08:24:24.248219', 29, 'EXECUTED', '9:bd88e1f833df0420b01e114533aee5e8', 'createTable tableName=BROKER_LINK; createTable tableName=FED_USER_ATTRIBUTE; createTable tableName=FED_USER_CONSENT; createTable tableName=FED_USER_CONSENT_ROLE; createTable tableName=FED_USER_CONSENT_PROT_MAPPER; createTable tableName=FED_USER_CR...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.2.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.2.0.xml', '2022-02-28 08:24:24.275813', 30, 'EXECUTED', '9:a7022af5267f019d020edfe316ef4371', 'addColumn tableName=ADMIN_EVENT_ENTITY; createTable tableName=CREDENTIAL_ATTRIBUTE; createTable tableName=FED_CREDENTIAL_ATTRIBUTE; modifyDataType columnName=VALUE, tableName=CREDENTIAL; addForeignKeyConstraint baseTableName=FED_CREDENTIAL_ATTRIBU...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.3.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.3.0.xml', '2022-02-28 08:24:24.308216', 31, 'EXECUTED', '9:fc155c394040654d6a79227e56f5e25a', 'createTable tableName=FEDERATED_USER; addPrimaryKey constraintName=CONSTR_FEDERATED_USER, tableName=FEDERATED_USER; dropDefaultValue columnName=TOTP, tableName=USER_ENTITY; dropColumn columnName=TOTP, tableName=USER_ENTITY; addColumn tableName=IDE...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.4.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.4.0.xml', '2022-02-28 08:24:24.315073', 32, 'EXECUTED', '9:eac4ffb2a14795e5dc7b426063e54d88', 'customChange', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.5.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2022-02-28 08:24:24.324942', 33, 'EXECUTED', '9:54937c05672568c4c64fc9524c1e9462', 'customChange; modifyDataType columnName=USER_ID, tableName=OFFLINE_USER_SESSION', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.5.0-unicode-oracle', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2022-02-28 08:24:24.328503', 34, 'MARK_RAN', '9:3a32bace77c84d7678d035a7f5a8084e', 'modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.5.0-unicode-other-dbs', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2022-02-28 08:24:24.378216', 35, 'EXECUTED', '9:33d72168746f81f98ae3a1e8e0ca3554', 'modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.5.0-duplicate-email-support', 'slawomir@dabek.name', 'META-INF/jpa-changelog-2.5.0.xml', '2022-02-28 08:24:24.385968', 36, 'EXECUTED', '9:61b6d3d7a4c0e0024b0c839da283da0c', 'addColumn tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.5.0-unique-group-names', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2022-02-28 08:24:24.397041', 37, 'EXECUTED', '9:8dcac7bdf7378e7d823cdfddebf72fda', 'addUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('2.5.1', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.5.1.xml', '2022-02-28 08:24:24.403456', 38, 'EXECUTED', '9:a2b870802540cb3faa72098db5388af3', 'addColumn tableName=FED_USER_CONSENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.0.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-3.0.0.xml', '2022-02-28 08:24:24.409886', 39, 'EXECUTED', '9:132a67499ba24bcc54fb5cbdcfe7e4c0', 'addColumn tableName=IDENTITY_PROVIDER', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.2.0-fix', 'keycloak', 'META-INF/jpa-changelog-3.2.0.xml', '2022-02-28 08:24:24.412918', 40, 'MARK_RAN', '9:938f894c032f5430f2b0fafb1a243462', 'addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.2.0-fix-with-keycloak-5416', 'keycloak', 'META-INF/jpa-changelog-3.2.0.xml', '2022-02-28 08:24:24.415818', 41, 'MARK_RAN', '9:845c332ff1874dc5d35974b0babf3006', 'dropIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS; addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS; createIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.2.0-fix-offline-sessions', 'hmlnarik', 'META-INF/jpa-changelog-3.2.0.xml', '2022-02-28 08:24:24.422646', 42, 'EXECUTED', '9:fc86359c079781adc577c5a217e4d04c', 'customChange', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.2.0-fixed', 'keycloak', 'META-INF/jpa-changelog-3.2.0.xml', '2022-02-28 08:24:25.49052', 43, 'EXECUTED', '9:59a64800e3c0d09b825f8a3b444fa8f4', 'addColumn tableName=REALM; dropPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_PK2, tableName=OFFLINE_CLIENT_SESSION; dropColumn columnName=CLIENT_SESSION_ID, tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_P...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.3.0', 'keycloak', 'META-INF/jpa-changelog-3.3.0.xml', '2022-02-28 08:24:25.497469', 44, 'EXECUTED', '9:d48d6da5c6ccf667807f633fe489ce88', 'addColumn tableName=USER_ENTITY', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part1', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2022-02-28 08:24:25.504327', 45, 'EXECUTED', '9:dde36f7973e80d71fceee683bc5d2951', 'addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_RESOURCE; addColumn tableName=RESOURCE_SERVER_SCOPE', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part2-KEYCLOAK-6095', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2022-02-28 08:24:25.51191', 46, 'EXECUTED', '9:b855e9b0a406b34fa323235a0cf4f640', 'customChange', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part3-fixed', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2022-02-28 08:24:25.514821', 47, 'MARK_RAN', '9:51abbacd7b416c50c4421a8cabf7927e', 'dropIndex indexName=IDX_RES_SERV_POL_RES_SERV, tableName=RESOURCE_SERVER_POLICY; dropIndex indexName=IDX_RES_SRV_RES_RES_SRV, tableName=RESOURCE_SERVER_RESOURCE; dropIndex indexName=IDX_RES_SRV_SCOPE_RES_SRV, tableName=RESOURCE_SERVER_SCOPE', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part3-fixed-nodropindex', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2022-02-28 08:24:25.633904', 48, 'EXECUTED', '9:bdc99e567b3398bac83263d375aad143', 'addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_POLICY; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_RESOURCE; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, ...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authn-3.4.0.CR1-refresh-token-max-reuse', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2022-02-28 08:24:25.640283', 49, 'EXECUTED', '9:d198654156881c46bfba39abd7769e69', 'addColumn tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.4.0', 'keycloak', 'META-INF/jpa-changelog-3.4.0.xml', '2022-02-28 08:24:25.706669', 50, 'EXECUTED', '9:cfdd8736332ccdd72c5256ccb42335db', 'addPrimaryKey constraintName=CONSTRAINT_REALM_DEFAULT_ROLES, tableName=REALM_DEFAULT_ROLES; addPrimaryKey constraintName=CONSTRAINT_COMPOSITE_ROLE, tableName=COMPOSITE_ROLE; addPrimaryKey constraintName=CONSTR_REALM_DEFAULT_GROUPS, tableName=REALM...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.4.0-KEYCLOAK-5230', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-3.4.0.xml', '2022-02-28 08:24:25.96845', 51, 'EXECUTED', '9:7c84de3d9bd84d7f077607c1a4dcb714', 'createIndex indexName=IDX_FU_ATTRIBUTE, tableName=FED_USER_ATTRIBUTE; createIndex indexName=IDX_FU_CONSENT, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CONSENT_RU, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CREDENTIAL, t...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.4.1', 'psilva@redhat.com', 'META-INF/jpa-changelog-3.4.1.xml', '2022-02-28 08:24:25.97393', 52, 'EXECUTED', '9:5a6bb36cbefb6a9d6928452c0852af2d', 'modifyDataType columnName=VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.4.2', 'keycloak', 'META-INF/jpa-changelog-3.4.2.xml', '2022-02-28 08:24:25.97811', 53, 'EXECUTED', '9:8f23e334dbc59f82e0a328373ca6ced0', 'update tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('3.4.2-KEYCLOAK-5172', 'mkanis@redhat.com', 'META-INF/jpa-changelog-3.4.2.xml', '2022-02-28 08:24:25.982062', 54, 'EXECUTED', '9:9156214268f09d970cdf0e1564d866af', 'update tableName=CLIENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.0.0-KEYCLOAK-6335', 'bburke@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2022-02-28 08:24:25.992544', 55, 'EXECUTED', '9:db806613b1ed154826c02610b7dbdf74', 'createTable tableName=CLIENT_AUTH_FLOW_BINDINGS; addPrimaryKey constraintName=C_CLI_FLOW_BIND, tableName=CLIENT_AUTH_FLOW_BINDINGS', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.0.0-CLEANUP-UNUSED-TABLE', 'bburke@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2022-02-28 08:24:26.003669', 56, 'EXECUTED', '9:229a041fb72d5beac76bb94a5fa709de', 'dropTable tableName=CLIENT_IDENTITY_PROV_MAPPING', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.0.0-KEYCLOAK-6228', 'bburke@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2022-02-28 08:24:26.054439', 57, 'EXECUTED', '9:079899dade9c1e683f26b2aa9ca6ff04', 'dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; dropNotNullConstraint columnName=CLIENT_ID, tableName=USER_CONSENT; addColumn tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHO...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.0.0-KEYCLOAK-5579-fixed', 'mposolda@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2022-02-28 08:24:26.412423', 58, 'EXECUTED', '9:139b79bcbbfe903bb1c2d2a4dbf001d9', 'dropForeignKeyConstraint baseTableName=CLIENT_TEMPLATE_ATTRIBUTES, constraintName=FK_CL_TEMPL_ATTR_TEMPL; renameTable newTableName=CLIENT_SCOPE_ATTRIBUTES, oldTableName=CLIENT_TEMPLATE_ATTRIBUTES; renameColumn newColumnName=SCOPE_ID, oldColumnName...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-4.0.0.CR1', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-4.0.0.CR1.xml', '2022-02-28 08:24:26.466486', 59, 'EXECUTED', '9:b55738ad889860c625ba2bf483495a04', 'createTable tableName=RESOURCE_SERVER_PERM_TICKET; addPrimaryKey constraintName=CONSTRAINT_FAPMT, tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRHO213XCX4WNKOG82SSPMT...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-4.0.0.Beta3', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-4.0.0.Beta3.xml', '2022-02-28 08:24:26.477849', 60, 'EXECUTED', '9:e0057eac39aa8fc8e09ac6cfa4ae15fe', 'addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRPO2128CX4WNKOG82SSRFY, referencedTableName=RESOURCE_SERVER_POLICY', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-4.2.0.Final', 'mhajas@redhat.com', 'META-INF/jpa-changelog-authz-4.2.0.Final.xml', '2022-02-28 08:24:26.491283', 61, 'EXECUTED', '9:42a33806f3a0443fe0e7feeec821326c', 'createTable tableName=RESOURCE_URIS; addForeignKeyConstraint baseTableName=RESOURCE_URIS, constraintName=FK_RESOURCE_SERVER_URIS, referencedTableName=RESOURCE_SERVER_RESOURCE; customChange; dropColumn columnName=URI, tableName=RESOURCE_SERVER_RESO...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('authz-4.2.0.Final-KEYCLOAK-9944', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-authz-4.2.0.Final.xml', '2022-02-28 08:24:26.500912', 62, 'EXECUTED', '9:9968206fca46eecc1f51db9c024bfe56', 'addPrimaryKey constraintName=CONSTRAINT_RESOUR_URIS_PK, tableName=RESOURCE_URIS', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.2.0-KEYCLOAK-6313', 'wadahiro@gmail.com', 'META-INF/jpa-changelog-4.2.0.xml', '2022-02-28 08:24:26.505796', 63, 'EXECUTED', '9:92143a6daea0a3f3b8f598c97ce55c3d', 'addColumn tableName=REQUIRED_ACTION_PROVIDER', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.3.0-KEYCLOAK-7984', 'wadahiro@gmail.com', 'META-INF/jpa-changelog-4.3.0.xml', '2022-02-28 08:24:26.510375', 64, 'EXECUTED', '9:82bab26a27195d889fb0429003b18f40', 'update tableName=REQUIRED_ACTION_PROVIDER', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.6.0-KEYCLOAK-7950', 'psilva@redhat.com', 'META-INF/jpa-changelog-4.6.0.xml', '2022-02-28 08:24:26.514408', 65, 'EXECUTED', '9:e590c88ddc0b38b0ae4249bbfcb5abc3', 'update tableName=RESOURCE_SERVER_RESOURCE', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.6.0-KEYCLOAK-8377', 'keycloak', 'META-INF/jpa-changelog-4.6.0.xml', '2022-02-28 08:24:26.552206', 66, 'EXECUTED', '9:5c1f475536118dbdc38d5d7977950cc0', 'createTable tableName=ROLE_ATTRIBUTE; addPrimaryKey constraintName=CONSTRAINT_ROLE_ATTRIBUTE_PK, tableName=ROLE_ATTRIBUTE; addForeignKeyConstraint baseTableName=ROLE_ATTRIBUTE, constraintName=FK_ROLE_ATTRIBUTE_ID, referencedTableName=KEYCLOAK_ROLE...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.6.0-KEYCLOAK-8555', 'gideonray@gmail.com', 'META-INF/jpa-changelog-4.6.0.xml', '2022-02-28 08:24:26.577645', 67, 'EXECUTED', '9:e7c9f5f9c4d67ccbbcc215440c718a17', 'createIndex indexName=IDX_COMPONENT_PROVIDER_TYPE, tableName=COMPONENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.7.0-KEYCLOAK-1267', 'sguilhen@redhat.com', 'META-INF/jpa-changelog-4.7.0.xml', '2022-02-28 08:24:26.585585', 68, 'EXECUTED', '9:88e0bfdda924690d6f4e430c53447dd5', 'addColumn tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.7.0-KEYCLOAK-7275', 'keycloak', 'META-INF/jpa-changelog-4.7.0.xml', '2022-02-28 08:24:26.622258', 69, 'EXECUTED', '9:f53177f137e1c46b6a88c59ec1cb5218', 'renameColumn newColumnName=CREATED_ON, oldColumnName=LAST_SESSION_REFRESH, tableName=OFFLINE_USER_SESSION; addNotNullConstraint columnName=CREATED_ON, tableName=OFFLINE_USER_SESSION; addColumn tableName=OFFLINE_USER_SESSION; customChange; createIn...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('4.8.0-KEYCLOAK-8835', 'sguilhen@redhat.com', 'META-INF/jpa-changelog-4.8.0.xml', '2022-02-28 08:24:26.631788', 70, 'EXECUTED', '9:a74d33da4dc42a37ec27121580d1459f', 'addNotNullConstraint columnName=SSO_MAX_LIFESPAN_REMEMBER_ME, tableName=REALM; addNotNullConstraint columnName=SSO_IDLE_TIMEOUT_REMEMBER_ME, tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('8.0.0-adding-credential-columns', 'keycloak', 'META-INF/jpa-changelog-8.0.0.xml', '2022-02-28 08:24:26.653964', 72, 'EXECUTED', '9:aa072ad090bbba210d8f18781b8cebf4', 'addColumn tableName=CREDENTIAL; addColumn tableName=FED_USER_CREDENTIAL', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('8.0.0-updating-credential-data-not-oracle-fixed', 'keycloak', 'META-INF/jpa-changelog-8.0.0.xml', '2022-02-28 08:24:26.665015', 73, 'EXECUTED', '9:1ae6be29bab7c2aa376f6983b932be37', 'update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('8.0.0-updating-credential-data-oracle-fixed', 'keycloak', 'META-INF/jpa-changelog-8.0.0.xml', '2022-02-28 08:24:26.668596', 74, 'MARK_RAN', '9:14706f286953fc9a25286dbd8fb30d97', 'update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL; update tableName=FED_USER_CREDENTIAL', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('8.0.0-credential-cleanup-fixed', 'keycloak', 'META-INF/jpa-changelog-8.0.0.xml', '2022-02-28 08:24:26.702374', 75, 'EXECUTED', '9:2b9cc12779be32c5b40e2e67711a218b', 'dropDefaultValue columnName=COUNTER, tableName=CREDENTIAL; dropDefaultValue columnName=DIGITS, tableName=CREDENTIAL; dropDefaultValue columnName=PERIOD, tableName=CREDENTIAL; dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; dropColumn ...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('8.0.0-resource-tag-support', 'keycloak', 'META-INF/jpa-changelog-8.0.0.xml', '2022-02-28 08:24:26.727079', 76, 'EXECUTED', '9:91fa186ce7a5af127a2d7a91ee083cc5', 'addColumn tableName=MIGRATION_MODEL; createIndex indexName=IDX_UPDATE_TIME, tableName=MIGRATION_MODEL', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.0-always-display-client', 'keycloak', 'META-INF/jpa-changelog-9.0.0.xml', '2022-02-28 08:24:26.733598', 77, 'EXECUTED', '9:6335e5c94e83a2639ccd68dd24e2e5ad', 'addColumn tableName=CLIENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.0-drop-constraints-for-column-increase', 'keycloak', 'META-INF/jpa-changelog-9.0.0.xml', '2022-02-28 08:24:26.736243', 78, 'MARK_RAN', '9:6bdb5658951e028bfe16fa0a8228b530', 'dropUniqueConstraint constraintName=UK_FRSR6T700S9V50BU18WS5PMT, tableName=RESOURCE_SERVER_PERM_TICKET; dropUniqueConstraint constraintName=UK_FRSR6T700S9V50BU18WS5HA6, tableName=RESOURCE_SERVER_RESOURCE; dropPrimaryKey constraintName=CONSTRAINT_O...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.0-increase-column-size-federated-fk', 'keycloak', 'META-INF/jpa-changelog-9.0.0.xml', '2022-02-28 08:24:26.767864', 79, 'EXECUTED', '9:d5bc15a64117ccad481ce8792d4c608f', 'modifyDataType columnName=CLIENT_ID, tableName=FED_USER_CONSENT; modifyDataType columnName=CLIENT_REALM_CONSTRAINT, tableName=KEYCLOAK_ROLE; modifyDataType columnName=OWNER, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=CLIENT_ID, ta...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.0-recreate-constraints-after-column-increase', 'keycloak', 'META-INF/jpa-changelog-9.0.0.xml', '2022-02-28 08:24:26.770848', 80, 'MARK_RAN', '9:077cba51999515f4d3e7ad5619ab592c', 'addNotNullConstraint columnName=CLIENT_ID, tableName=OFFLINE_CLIENT_SESSION; addNotNullConstraint columnName=OWNER, tableName=RESOURCE_SERVER_PERM_TICKET; addNotNullConstraint columnName=REQUESTER, tableName=RESOURCE_SERVER_PERM_TICKET; addNotNull...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.1-add-index-to-client.client_id', 'keycloak', 'META-INF/jpa-changelog-9.0.1.xml', '2022-02-28 08:24:26.791848', 81, 'EXECUTED', '9:be969f08a163bf47c6b9e9ead8ac2afb', 'createIndex indexName=IDX_CLIENT_ID, tableName=CLIENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.1-KEYCLOAK-12579-drop-constraints', 'keycloak', 'META-INF/jpa-changelog-9.0.1.xml', '2022-02-28 08:24:26.794044', 82, 'MARK_RAN', '9:6d3bb4408ba5a72f39bd8a0b301ec6e3', 'dropUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.1-KEYCLOAK-12579-add-not-null-constraint', 'keycloak', 'META-INF/jpa-changelog-9.0.1.xml', '2022-02-28 08:24:26.798976', 83, 'EXECUTED', '9:966bda61e46bebf3cc39518fbed52fa7', 'addNotNullConstraint columnName=PARENT_GROUP, tableName=KEYCLOAK_GROUP', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.1-KEYCLOAK-12579-recreate-constraints', 'keycloak', 'META-INF/jpa-changelog-9.0.1.xml', '2022-02-28 08:24:26.801397', 84, 'MARK_RAN', '9:8dcac7bdf7378e7d823cdfddebf72fda', 'addUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('9.0.1-add-index-to-events', 'keycloak', 'META-INF/jpa-changelog-9.0.1.xml', '2022-02-28 08:24:26.820087', 85, 'EXECUTED', '9:7d93d602352a30c0c317e6a609b56599', 'createIndex indexName=IDX_EVENT_TIME, tableName=EVENT_ENTITY', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('map-remove-ri', 'keycloak', 'META-INF/jpa-changelog-11.0.0.xml', '2022-02-28 08:24:26.827456', 86, 'EXECUTED', '9:71c5969e6cdd8d7b6f47cebc86d37627', 'dropForeignKeyConstraint baseTableName=REALM, constraintName=FK_TRAF444KK6QRKMS7N56AIWQ5Y; dropForeignKeyConstraint baseTableName=KEYCLOAK_ROLE, constraintName=FK_KJHO5LE2C0RAL09FL8CM9WFW9', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('map-remove-ri', 'keycloak', 'META-INF/jpa-changelog-12.0.0.xml', '2022-02-28 08:24:26.841259', 87, 'EXECUTED', '9:a9ba7d47f065f041b7da856a81762021', 'dropForeignKeyConstraint baseTableName=REALM_DEFAULT_GROUPS, constraintName=FK_DEF_GROUPS_GROUP; dropForeignKeyConstraint baseTableName=REALM_DEFAULT_ROLES, constraintName=FK_H4WPD7W4HSOOLNI3H0SW7BTJE; dropForeignKeyConstraint baseTableName=CLIENT...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('12.1.0-add-realm-localization-table', 'keycloak', 'META-INF/jpa-changelog-12.0.0.xml', '2022-02-28 08:24:26.853156', 88, 'EXECUTED', '9:fffabce2bc01e1a8f5110d5278500065', 'createTable tableName=REALM_LOCALIZATIONS; addPrimaryKey tableName=REALM_LOCALIZATIONS', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('default-roles', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.85968', 89, 'EXECUTED', '9:fa8a5b5445e3857f4b010bafb5009957', 'addColumn tableName=REALM; customChange', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('default-roles-cleanup', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.87098', 90, 'EXECUTED', '9:67ac3241df9a8582d591c5ed87125f39', 'dropTable tableName=REALM_DEFAULT_ROLES; dropTable tableName=CLIENT_DEFAULT_ROLES', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('13.0.0-KEYCLOAK-16844', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.894388', 91, 'EXECUTED', '9:ad1194d66c937e3ffc82386c050ba089', 'createIndex indexName=IDX_OFFLINE_USS_PRELOAD, tableName=OFFLINE_USER_SESSION', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('map-remove-ri-13.0.0', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.908821', 92, 'EXECUTED', '9:d9be619d94af5a2f5d07b9f003543b91', 'dropForeignKeyConstraint baseTableName=DEFAULT_CLIENT_SCOPE, constraintName=FK_R_DEF_CLI_SCOPE_SCOPE; dropForeignKeyConstraint baseTableName=CLIENT_SCOPE_CLIENT, constraintName=FK_C_CLI_SCOPE_SCOPE; dropForeignKeyConstraint baseTableName=CLIENT_SC...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('13.0.0-KEYCLOAK-17992-drop-constraints', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.911157', 93, 'MARK_RAN', '9:544d201116a0fcc5a5da0925fbbc3bde', 'dropPrimaryKey constraintName=C_CLI_SCOPE_BIND, tableName=CLIENT_SCOPE_CLIENT; dropIndex indexName=IDX_CLSCOPE_CL, tableName=CLIENT_SCOPE_CLIENT; dropIndex indexName=IDX_CL_CLSCOPE, tableName=CLIENT_SCOPE_CLIENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('13.0.0-increase-column-size-federated', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.925633', 94, 'EXECUTED', '9:43c0c1055b6761b4b3e89de76d612ccf', 'modifyDataType columnName=CLIENT_ID, tableName=CLIENT_SCOPE_CLIENT; modifyDataType columnName=SCOPE_ID, tableName=CLIENT_SCOPE_CLIENT', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('13.0.0-KEYCLOAK-17992-recreate-constraints', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.927973', 95, 'MARK_RAN', '9:8bd711fd0330f4fe980494ca43ab1139', 'addNotNullConstraint columnName=CLIENT_ID, tableName=CLIENT_SCOPE_CLIENT; addNotNullConstraint columnName=SCOPE_ID, tableName=CLIENT_SCOPE_CLIENT; addPrimaryKey constraintName=C_CLI_SCOPE_BIND, tableName=CLIENT_SCOPE_CLIENT; createIndex indexName=...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('json-string-accomodation-fixed', 'keycloak', 'META-INF/jpa-changelog-13.0.0.xml', '2022-02-28 08:24:26.934211', 96, 'EXECUTED', '9:e07d2bc0970c348bb06fb63b1f82ddbf', 'addColumn tableName=REALM_ATTRIBUTE; update tableName=REALM_ATTRIBUTE; dropColumn columnName=VALUE, tableName=REALM_ATTRIBUTE; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=REALM_ATTRIBUTE', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('14.0.0-KEYCLOAK-11019', 'keycloak', 'META-INF/jpa-changelog-14.0.0.xml', '2022-02-28 08:24:26.997078', 97, 'EXECUTED', '9:24fb8611e97f29989bea412aa38d12b7', 'createIndex indexName=IDX_OFFLINE_CSS_PRELOAD, tableName=OFFLINE_CLIENT_SESSION; createIndex indexName=IDX_OFFLINE_USS_BY_USER, tableName=OFFLINE_USER_SESSION; createIndex indexName=IDX_OFFLINE_USS_BY_USERSESS, tableName=OFFLINE_USER_SESSION', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('14.0.0-KEYCLOAK-18286', 'keycloak', 'META-INF/jpa-changelog-14.0.0.xml', '2022-02-28 08:24:26.999668', 98, 'MARK_RAN', '9:259f89014ce2506ee84740cbf7163aa7', 'createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('14.0.0-KEYCLOAK-18286-revert', 'keycloak', 'META-INF/jpa-changelog-14.0.0.xml', '2022-02-28 08:24:27.018406', 99, 'MARK_RAN', '9:04baaf56c116ed19951cbc2cca584022', 'dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('14.0.0-KEYCLOAK-18286-supported-dbs', 'keycloak', 'META-INF/jpa-changelog-14.0.0.xml', '2022-02-28 08:24:27.039293', 100, 'EXECUTED', '9:60ca84a0f8c94ec8c3504a5a3bc88ee8', 'createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('14.0.0-KEYCLOAK-18286-unsupported-dbs', 'keycloak', 'META-INF/jpa-changelog-14.0.0.xml', '2022-02-28 08:24:27.041687', 101, 'MARK_RAN', '9:d3d977031d431db16e2c181ce49d73e9', 'createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('KEYCLOAK-17267-add-index-to-user-attributes', 'keycloak', 'META-INF/jpa-changelog-14.0.0.xml', '2022-02-28 08:24:27.062157', 102, 'EXECUTED', '9:0b305d8d1277f3a89a0a53a659ad274c', 'createIndex indexName=IDX_USER_ATTRIBUTE_NAME, tableName=USER_ATTRIBUTE', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('KEYCLOAK-18146-add-saml-art-binding-identifier', 'keycloak', 'META-INF/jpa-changelog-14.0.0.xml', '2022-02-28 08:24:27.067692', 103, 'EXECUTED', '9:2c374ad2cdfe20e2905a84c8fac48460', 'customChange', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('15.0.0-KEYCLOAK-18467', 'keycloak', 'META-INF/jpa-changelog-15.0.0.xml', '2022-02-28 08:24:27.074295', 104, 'EXECUTED', '9:47a760639ac597360a8219f5b768b4de', 'addColumn tableName=REALM_LOCALIZATIONS; update tableName=REALM_LOCALIZATIONS; dropColumn columnName=TEXTS, tableName=REALM_LOCALIZATIONS; renameColumn newColumnName=TEXTS, oldColumnName=TEXTS_NEW, tableName=REALM_LOCALIZATIONS; addNotNullConstrai...', '', NULL, '3.5.4', NULL, NULL, '6033062234');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('17.0.0-9562', 'keycloak', 'META-INF/jpa-changelog-17.0.0.xml', '2023-05-09 13:20:01.351586', 105, 'EXECUTED', '9:a6272f0576727dd8cad2522335f5d99e', 'createIndex indexName=IDX_USER_SERVICE_ACCOUNT, tableName=USER_ENTITY', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('18.0.0-10625-IDX_ADMIN_EVENT_TIME', 'keycloak', 'META-INF/jpa-changelog-18.0.0.xml', '2023-05-09 13:20:01.380769', 106, 'EXECUTED', '9:015479dbd691d9cc8669282f4828c41d', 'createIndex indexName=IDX_ADMIN_EVENT_TIME, tableName=ADMIN_EVENT_ENTITY', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('19.0.0-10135', 'keycloak', 'META-INF/jpa-changelog-19.0.0.xml', '2023-05-09 13:20:01.482841', 107, 'EXECUTED', '9:9518e495fdd22f78ad6425cc30630221', 'customChange', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('20.0.0-12964-supported-dbs', 'keycloak', 'META-INF/jpa-changelog-20.0.0.xml', '2023-05-09 13:20:01.501435', 108, 'EXECUTED', '9:e5f243877199fd96bcc842f27a1656ac', 'createIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('20.0.0-12964-unsupported-dbs', 'keycloak', 'META-INF/jpa-changelog-20.0.0.xml', '2023-05-09 13:20:01.510843', 109, 'MARK_RAN', '9:1a6fcaa85e20bdeae0a9ce49b41946a5', 'createIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('client-attributes-string-accomodation-fixed', 'keycloak', 'META-INF/jpa-changelog-20.0.0.xml', '2023-05-09 13:20:01.532955', 110, 'EXECUTED', '9:3f332e13e90739ed0c35b0b25b7822ca', 'addColumn tableName=CLIENT_ATTRIBUTES; update tableName=CLIENT_ATTRIBUTES; dropColumn columnName=VALUE, tableName=CLIENT_ATTRIBUTES; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('21.0.2-17277', 'keycloak', 'META-INF/jpa-changelog-21.0.2.xml', '2023-05-09 13:20:01.555659', 111, 'EXECUTED', '9:7ee1f7a3fb8f5588f171fb9a6ab623c0', 'customChange', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('21.1.0-19404', 'keycloak', 'META-INF/jpa-changelog-21.1.0.xml', '2023-05-09 13:20:01.630399', 112, 'EXECUTED', '9:3d7e830b52f33676b9d64f7f2b2ea634', 'modifyDataType columnName=DECISION_STRATEGY, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=LOGIC, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=POLICY_ENFORCE_MODE, tableName=RESOURCE_SERVER', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('21.1.0-19404-2', 'keycloak', 'META-INF/jpa-changelog-21.1.0.xml', '2023-05-09 13:20:01.63486', 113, 'MARK_RAN', '9:627d032e3ef2c06c0e1f73d2ae25c26c', 'addColumn tableName=RESOURCE_SERVER_POLICY; update tableName=RESOURCE_SERVER_POLICY; dropColumn columnName=DECISION_STRATEGY, tableName=RESOURCE_SERVER_POLICY; renameColumn newColumnName=DECISION_STRATEGY, oldColumnName=DECISION_STRATEGY_NEW, tabl...', '', NULL, '4.16.1', NULL, NULL, '3631201171');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('22.0.0-17484-updated', 'keycloak', 'META-INF/jpa-changelog-22.0.0.xml', '2024-03-19 09:50:20.729109', 115, 'MARK_RAN', '9:90af0bfd30cafc17b9f4d6eccd92b8b3', 'customChange', '', NULL, '4.23.2', NULL, NULL, '0838220490');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('22.0.5-24031', 'keycloak', 'META-INF/jpa-changelog-22.0.0.xml', '2024-03-19 09:50:20.73591', 116, 'EXECUTED', '9:a60d2d7b315ec2d3eba9e2f145f9df28', 'customChange', '', NULL, '4.23.2', NULL, NULL, '0838220490');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('18.0.15-30992-index-consent', 'keycloak', 'META-INF/jpa-changelog-18.0.15.xml', '2025-02-19 11:59:20.681361', 117, 'EXECUTED', '9:80071ede7a05604b1f4906f3bf3b00f0', 'createIndex indexName=IDX_USCONSENT_SCOPE_ID, tableName=USER_CONSENT_CLIENT_SCOPE', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('23.0.0-12062', 'keycloak', 'META-INF/jpa-changelog-23.0.0.xml', '2025-02-19 11:59:20.713171', 118, 'EXECUTED', '9:2168fbe728fec46ae9baf15bf80927b8', 'addColumn tableName=COMPONENT_CONFIG; update tableName=COMPONENT_CONFIG; dropColumn columnName=VALUE, tableName=COMPONENT_CONFIG; renameColumn newColumnName=VALUE, oldColumnName=VALUE_NEW, tableName=COMPONENT_CONFIG', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('23.0.0-17258', 'keycloak', 'META-INF/jpa-changelog-23.0.0.xml', '2025-02-19 11:59:20.723885', 119, 'EXECUTED', '9:36506d679a83bbfda85a27ea1864dca8', 'addColumn tableName=EVENT_ENTITY', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.0-9758', 'keycloak', 'META-INF/jpa-changelog-24.0.0.xml', '2025-02-19 11:59:20.982702', 120, 'EXECUTED', '9:502c557a5189f600f0f445a9b49ebbce', 'addColumn tableName=USER_ATTRIBUTE; addColumn tableName=FED_USER_ATTRIBUTE; createIndex indexName=USER_ATTR_LONG_VALUES, tableName=USER_ATTRIBUTE; createIndex indexName=FED_USER_ATTR_LONG_VALUES, tableName=FED_USER_ATTRIBUTE; createIndex indexName...', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.0-9758-2', 'keycloak', 'META-INF/jpa-changelog-24.0.0.xml', '2025-02-19 11:59:20.991883', 121, 'EXECUTED', '9:bf0fdee10afdf597a987adbf291db7b2', 'customChange', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.0-26618-drop-index-if-present', 'keycloak', 'META-INF/jpa-changelog-24.0.0.xml', '2025-02-19 11:59:21.001902', 122, 'MARK_RAN', '9:04baaf56c116ed19951cbc2cca584022', 'dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.0-26618-reindex', 'keycloak', 'META-INF/jpa-changelog-24.0.0.xml', '2025-02-19 11:59:21.068108', 123, 'EXECUTED', '9:08707c0f0db1cef6b352db03a60edc7f', 'createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.2-27228', 'keycloak', 'META-INF/jpa-changelog-24.0.2.xml', '2025-02-19 11:59:21.079161', 124, 'EXECUTED', '9:eaee11f6b8aa25d2cc6a84fb86fc6238', 'customChange', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.2-27967-drop-index-if-present', 'keycloak', 'META-INF/jpa-changelog-24.0.2.xml', '2025-02-19 11:59:21.083191', 125, 'MARK_RAN', '9:04baaf56c116ed19951cbc2cca584022', 'dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.2-27967-reindex', 'keycloak', 'META-INF/jpa-changelog-24.0.2.xml', '2025-02-19 11:59:21.088927', 126, 'MARK_RAN', '9:d3d977031d431db16e2c181ce49d73e9', 'createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-tables', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.103001', 127, 'EXECUTED', '9:deda2df035df23388af95bbd36c17cef', 'addColumn tableName=OFFLINE_USER_SESSION; addColumn tableName=OFFLINE_CLIENT_SESSION', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-creation', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.161305', 128, 'EXECUTED', '9:3e96709818458ae49f3c679ae58d263a', 'createIndex indexName=IDX_OFFLINE_USS_BY_LAST_SESSION_REFRESH, tableName=OFFLINE_USER_SESSION', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-cleanup', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.175361', 129, 'EXECUTED', '9:8c0cfa341a0474385b324f5c4b2dfcc1', 'dropIndex indexName=IDX_OFFLINE_USS_CREATEDON, tableName=OFFLINE_USER_SESSION; dropIndex indexName=IDX_OFFLINE_USS_PRELOAD, tableName=OFFLINE_USER_SESSION; dropIndex indexName=IDX_OFFLINE_USS_BY_USERSESS, tableName=OFFLINE_USER_SESSION; dropIndex ...', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-2-mysql', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.179819', 130, 'MARK_RAN', '9:b7ef76036d3126bb83c2423bf4d449d6', 'createIndex indexName=IDX_OFFLINE_USS_BY_BROKER_SESSION_ID, tableName=OFFLINE_USER_SESSION', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-2-not-mysql', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.243308', 131, 'EXECUTED', '9:23396cf51ab8bc1ae6f0cac7f9f6fcf7', 'createIndex indexName=IDX_OFFLINE_USS_BY_BROKER_SESSION_ID, tableName=OFFLINE_USER_SESSION', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-org', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.301301', 132, 'EXECUTED', '9:5c859965c2c9b9c72136c360649af157', 'createTable tableName=ORG; addUniqueConstraint constraintName=UK_ORG_NAME, tableName=ORG; addUniqueConstraint constraintName=UK_ORG_GROUP, tableName=ORG; createTable tableName=ORG_DOMAIN', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('unique-consentuser', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.32858', 133, 'EXECUTED', '9:5857626a2ea8767e9a6c66bf3a2cb32f', 'customChange; dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_LOCAL_CONSENT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_EXTERNAL_CONSENT, tableName=...', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('unique-consentuser-mysql', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.338173', 134, 'MARK_RAN', '9:b79478aad5adaa1bc428e31563f55e8e', 'customChange; dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_LOCAL_CONSENT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_EXTERNAL_CONSENT, tableName=...', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28861-index-creation', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-19 11:59:21.446551', 135, 'EXECUTED', '9:b9acb58ac958d9ada0fe12a5d4794ab1', 'createIndex indexName=IDX_PERM_TICKET_REQUESTER, tableName=RESOURCE_SERVER_PERM_TICKET; createIndex indexName=IDX_PERM_TICKET_OWNER, tableName=RESOURCE_SERVER_PERM_TICKET', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0-org-alias', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.470632', 136, 'EXECUTED', '9:6ef7d63e4412b3c2d66ed179159886a4', 'addColumn tableName=ORG; update tableName=ORG; addNotNullConstraint columnName=ALIAS, tableName=ORG; addUniqueConstraint constraintName=UK_ORG_ALIAS, tableName=ORG', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0-org-group', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.49296', 137, 'EXECUTED', '9:da8e8087d80ef2ace4f89d8c5b9ca223', 'addColumn tableName=KEYCLOAK_GROUP; update tableName=KEYCLOAK_GROUP; addNotNullConstraint columnName=TYPE, tableName=KEYCLOAK_GROUP; customChange', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0-org-indexes', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.55551', 138, 'EXECUTED', '9:79b05dcd610a8c7f25ec05135eec0857', 'createIndex indexName=IDX_ORG_DOMAIN_ORG_ID, tableName=ORG_DOMAIN', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0-org-group-membership', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.567385', 139, 'EXECUTED', '9:a6ace2ce583a421d89b01ba2a28dc2d4', 'addColumn tableName=USER_GROUP_MEMBERSHIP; update tableName=USER_GROUP_MEMBERSHIP; addNotNullConstraint columnName=MEMBERSHIP_TYPE, tableName=USER_GROUP_MEMBERSHIP', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('31296-persist-revoked-access-tokens', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.58486', 140, 'EXECUTED', '9:64ef94489d42a358e8304b0e245f0ed4', 'createTable tableName=REVOKED_TOKEN; addPrimaryKey constraintName=CONSTRAINT_RT, tableName=REVOKED_TOKEN', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('31725-index-persist-revoked-access-tokens', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.640872', 141, 'EXECUTED', '9:b994246ec2bf7c94da881e1d28782c7b', 'createIndex indexName=IDX_REV_TOKEN_ON_EXPIRE, tableName=REVOKED_TOKEN', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0-idps-for-login', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.758184', 142, 'EXECUTED', '9:51f5fffadf986983d4bd59582c6c1604', 'addColumn tableName=IDENTITY_PROVIDER; createIndex indexName=IDX_IDP_REALM_ORG, tableName=IDENTITY_PROVIDER; createIndex indexName=IDX_IDP_FOR_LOGIN, tableName=IDENTITY_PROVIDER; customChange', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0-32583-drop-redundant-index-on-client-session', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.815143', 143, 'EXECUTED', '9:24972d83bf27317a055d234187bb4af9', 'dropIndex indexName=IDX_US_SESS_ID_ON_CL_SESS, tableName=OFFLINE_CLIENT_SESSION', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0.32582-remove-tables-user-session-user-session-note-and-client-session', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.848173', 144, 'EXECUTED', '9:febdc0f47f2ed241c59e60f58c3ceea5', 'dropTable tableName=CLIENT_SESSION_ROLE; dropTable tableName=CLIENT_SESSION_NOTE; dropTable tableName=CLIENT_SESSION_PROT_MAPPER; dropTable tableName=CLIENT_SESSION_AUTH_STATUS; dropTable tableName=CLIENT_USER_SESSION_NOTE; dropTable tableName=CLI...', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.0-33201-org-redirect-url', 'keycloak', 'META-INF/jpa-changelog-26.0.0.xml', '2025-02-19 11:59:21.856562', 145, 'EXECUTED', '9:4d0e22b0ac68ebe9794fa9cb752ea660', 'addColumn tableName=ORG', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.0.6-34013', 'keycloak', 'META-INF/jpa-changelog-26.0.6.xml', '2025-02-19 11:59:21.864575', 146, 'EXECUTED', '9:e6b686a15759aef99a6d758a5c4c6a26', 'addColumn tableName=ADMIN_EVENT_ENTITY', '', NULL, '4.29.1', NULL, NULL, '9962760527');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-cleanup-uss-createdon', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-21 15:48:20.244638', 147, 'MARK_RAN', '9:78ab4fc129ed5e8265dbcc3485fba92f', 'dropIndex indexName=IDX_OFFLINE_USS_CREATEDON, tableName=OFFLINE_USER_SESSION', '', NULL, '4.29.1', NULL, NULL, '0149300183');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-cleanup-uss-preload', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-21 15:48:20.296378', 148, 'MARK_RAN', '9:de5f7c1f7e10994ed8b62e621d20eaab', 'dropIndex indexName=IDX_OFFLINE_USS_PRELOAD, tableName=OFFLINE_USER_SESSION', '', NULL, '4.29.1', NULL, NULL, '0149300183');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-cleanup-uss-by-usersess', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-21 15:48:20.316068', 149, 'MARK_RAN', '9:6eee220d024e38e89c799417ec33667f', 'dropIndex indexName=IDX_OFFLINE_USS_BY_USERSESS, tableName=OFFLINE_USER_SESSION', '', NULL, '4.29.1', NULL, NULL, '0149300183');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('25.0.0-28265-index-cleanup-css-preload', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-02-21 15:48:20.334668', 150, 'MARK_RAN', '9:5411d2fb2891d3e8d63ddb55dfa3c0c9', 'dropIndex indexName=IDX_OFFLINE_CSS_PRELOAD, tableName=OFFLINE_CLIENT_SESSION', '', NULL, '4.29.1', NULL, NULL, '0149300183');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('29399-jdbc-ping-default', 'keycloak', 'META-INF/jpa-changelog-26.1.0.xml', '2025-03-21 16:31:00.205066', 151, 'EXECUTED', '9:007dbe99d7203fca403b89d4edfdf21e', 'createTable tableName=JGROUPS_PING; addPrimaryKey constraintName=CONSTRAINT_JGROUPS_PING, tableName=JGROUPS_PING', '', NULL, '4.29.1', NULL, NULL, '2571060156');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.1.0-34013', 'keycloak', 'META-INF/jpa-changelog-26.1.0.xml', '2025-03-21 16:31:00.220523', 152, 'MARK_RAN', '9:e6b686a15759aef99a6d758a5c4c6a26', 'addColumn tableName=ADMIN_EVENT_ENTITY', '', NULL, '4.29.1', NULL, NULL, '2571060156');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.1.0-34380', 'keycloak', 'META-INF/jpa-changelog-26.1.0.xml', '2025-03-21 16:31:00.235473', 153, 'EXECUTED', '9:ac8b9edb7c2b6c17a1c7a11fcf5ccf01', 'dropTable tableName=USERNAME_LOGIN_FAILURE', '', NULL, '4.29.1', NULL, NULL, '2571060156');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('20.0.0-12964-supported-dbs-edb-migration', 'keycloak', 'META-INF/jpa-changelog-20.0.0.xml', '2025-12-05 13:17:26.538621', 154, 'EXECUTED', '9:a6b18a8e38062df5793edbe064f4aecd', 'dropIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE; createIndex indexName=IDX_GROUP_ATT_BY_NAME_VALUE, tableName=GROUP_ATTRIBUTE', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('client-attributes-string-accomodation-fixed-pre-drop-index', 'keycloak', 'META-INF/jpa-changelog-20.0.0.xml', '2025-12-05 13:17:26.549859', 155, 'MARK_RAN', '9:04baaf56c116ed19951cbc2cca584022', 'dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('client-attributes-string-accomodation-fixed-post-create-index', 'keycloak', 'META-INF/jpa-changelog-20.0.0.xml', '2025-12-05 13:17:26.557424', 156, 'MARK_RAN', '9:bd2bd0fc7768cf0845ac96a8786fa735', 'createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('24.0.0-26618-edb-migration', 'keycloak', 'META-INF/jpa-changelog-24.0.0.xml', '2025-12-05 13:17:26.662811', 157, 'EXECUTED', '9:2f684b29d414cd47efe3a3599f390741', 'dropIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES; createIndex indexName=IDX_CLIENT_ATT_BY_NAME_VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('unique-consentuser-edb-migration', 'keycloak', 'META-INF/jpa-changelog-25.0.0.xml', '2025-12-05 13:17:26.684084', 158, 'MARK_RAN', '9:5857626a2ea8767e9a6c66bf3a2cb32f', 'customChange; dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_LOCAL_CONSENT, tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_EXTERNAL_CONSENT, tableName=...', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.2.0-36750', 'keycloak', 'META-INF/jpa-changelog-26.2.0.xml', '2025-12-05 13:17:26.709485', 159, 'EXECUTED', '9:b49ce951c22f7eb16480ff085640a33a', 'createTable tableName=SERVER_CONFIG', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.2.0-26106', 'keycloak', 'META-INF/jpa-changelog-26.2.0.xml', '2025-12-05 13:17:26.722256', 160, 'EXECUTED', '9:b5877d5dab7d10ff3a9d209d7beb6680', 'addColumn tableName=CREDENTIAL', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.2.6-39866-duplicate', 'keycloak', 'META-INF/jpa-changelog-26.2.6.xml', '2025-12-05 13:17:26.740558', 161, 'EXECUTED', '9:1dc67ccee24f30331db2cba4f372e40e', 'customChange', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.2.6-39866-uk', 'keycloak', 'META-INF/jpa-changelog-26.2.6.xml', '2025-12-05 13:17:26.754339', 162, 'EXECUTED', '9:b70b76f47210cf0a5f4ef0e219eac7cd', 'addUniqueConstraint constraintName=UK_MIGRATION_VERSION, tableName=MIGRATION_MODEL', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.2.6-40088-duplicate', 'keycloak', 'META-INF/jpa-changelog-26.2.6.xml', '2025-12-05 13:17:26.772213', 163, 'EXECUTED', '9:cc7e02ed69ab31979afb1982f9670e8f', 'customChange', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.2.6-40088-uk', 'keycloak', 'META-INF/jpa-changelog-26.2.6.xml', '2025-12-05 13:17:26.783181', 164, 'EXECUTED', '9:5bb848128da7bc4595cc507383325241', 'addUniqueConstraint constraintName=UK_MIGRATION_UPDATE_TIME, tableName=MIGRATION_MODEL', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.3.0-groups-description', 'keycloak', 'META-INF/jpa-changelog-26.3.0.xml', '2025-12-05 13:17:26.793837', 165, 'EXECUTED', '9:e1a3c05574326fb5b246b73b9a4c4d49', 'addColumn tableName=KEYCLOAK_GROUP', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.4.0-40933-saml-encryption-attributes', 'keycloak', 'META-INF/jpa-changelog-26.4.0.xml', '2025-12-05 13:17:26.809159', 166, 'EXECUTED', '9:7e9eaba362ca105efdda202303a4fe49', 'customChange', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.4.0-51321', 'keycloak', 'META-INF/jpa-changelog-26.4.0.xml', '2025-12-05 13:17:26.895273', 167, 'EXECUTED', '9:34bab2bc56f75ffd7e347c580874e306', 'createIndex indexName=IDX_EVENT_ENTITY_USER_ID_TYPE, tableName=EVENT_ENTITY', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('40343-workflow-state-table', 'keycloak', 'META-INF/jpa-changelog-26.4.0.xml', '2025-12-05 13:17:27.080804', 168, 'EXECUTED', '9:ed3ab4723ceed210e5b5e60ac4562106', 'createTable tableName=WORKFLOW_STATE; addPrimaryKey constraintName=PK_WORKFLOW_STATE, tableName=WORKFLOW_STATE; addUniqueConstraint constraintName=UQ_WORKFLOW_RESOURCE, tableName=WORKFLOW_STATE; createIndex indexName=IDX_WORKFLOW_STATE_STEP, table...', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.5.0-index-offline-css-by-client', 'keycloak', 'META-INF/jpa-changelog-26.5.0.xml', '2025-12-05 13:17:27.187892', 169, 'EXECUTED', '9:383e981ce95d16e32af757b7998820f7', 'createIndex indexName=IDX_OFFLINE_CSS_BY_CLIENT, tableName=OFFLINE_CLIENT_SESSION', '', NULL, '4.33.0', NULL, NULL, '4937038731');
INSERT INTO public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('26.5.0-index-offline-css-by-client-storage-provider', 'keycloak', 'META-INF/jpa-changelog-26.5.0.xml', '2025-12-05 13:17:27.291175', 170, 'EXECUTED', '9:f5bc200e6fa7d7e483854dee535ca425', 'createIndex indexName=IDX_OFFLINE_CSS_BY_CLIENT_STORAGE_PROVIDER, tableName=OFFLINE_CLIENT_SESSION', '', NULL, '4.33.0', NULL, NULL, '4937038731');


--
-- Data for Name: databasechangeloglock; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.databasechangeloglock (id, locked, lockgranted, lockedby) VALUES (1, false, NULL, NULL);
INSERT INTO public.databasechangeloglock (id, locked, lockgranted, lockedby) VALUES (1000, false, NULL, NULL);
INSERT INTO public.databasechangeloglock (id, locked, lockgranted, lockedby) VALUES (1001, false, NULL, NULL);


--
-- Data for Name: default_client_scope; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', '9999bbc8-5f73-4a21-aa56-797628fa5b5f', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', '22487cb4-ae45-40c0-bf42-6cc8a0522d49', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', '7d3289fd-f5f7-4c73-a857-6506457c1e6c', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', 'e8f953e9-1796-475d-bfca-e6d7b2c911c4', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', 'cce07087-b8b0-4793-890f-5c84fefa9439', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', 'b6f91cd7-ee91-49dd-8159-b595d810694b', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', '241020fc-78d9-4971-b9ec-a906bda7b14c', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', 'e32926f4-d4af-44cf-99e9-4d5eceb55a35', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', '2007079b-9593-4a89-905e-3a492a1a6135', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', 'b3b6dccf-46b0-4199-9d2e-a1908cb98623', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', 'c1f5c722-af78-4138-b5e5-8b9c5a22fd58', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', '2838ffe1-c64d-49c1-8d36-6e6966f22b92', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', '2a598f0a-e595-46b6-9641-7e305f447623', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', '053548c2-191c-44b2-82a5-1aa9f2804735', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', '5ef28d69-868b-4f07-80f9-d5284e4f82a9', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', '16915d69-1f21-416e-8194-04783e07e47c', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', '80982e4f-2393-44de-8207-6ce9526f3fb3', false);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', '3515bf6a-cb15-45db-b55c-6ec21ac99990', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', 'e22d2191-0ebe-40c6-9d8c-a3c6cc44aaf3', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('master', '7ef392a1-6728-40ac-9bf6-381f1fbc3b20', true);
INSERT INTO public.default_client_scope (realm_id, scope_id, default_scope) VALUES ('development', '25df115c-d573-4b3e-bf3b-db291bfbceff', true);


--
-- Data for Name: event_entity; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: fed_user_attribute; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: fed_user_consent; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: fed_user_consent_cl_scope; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: fed_user_credential; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: fed_user_group_membership; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: fed_user_required_action; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: fed_user_role_mapping; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: federated_identity; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: federated_user; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: group_attribute; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: group_role_mapping; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: identity_provider; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: identity_provider_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: identity_provider_mapper; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: idp_mapper_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: jgroups_ping; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: keycloak_group; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.keycloak_group (id, name, parent_group, realm_id, type, description) VALUES ('321da70e-bc39-4c6c-8cf6-579dc6e95bab', 'Human', ' ', 'development', 0, NULL);
INSERT INTO public.keycloak_group (id, name, parent_group, realm_id, type, description) VALUES ('71463f7d-68b9-4a2e-80aa-d1835bbb1736', 'Technical', ' ', 'development', 0, NULL);
INSERT INTO public.keycloak_group (id, name, parent_group, realm_id, type, description) VALUES ('6db1981a-209e-4687-a8fb-3be35974111e', 'Unaudited', ' ', 'development', 0, NULL);
INSERT INTO public.keycloak_group (id, name, parent_group, realm_id, type, description) VALUES ('34ba8933-03d7-43a0-8e36-eddd0d6b7537', 'AuditEverything', ' ', 'development', 0, NULL);


--
-- Data for Name: keycloak_role; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('8abade97-996e-4b25-a889-e4bc2d1fc387', 'master', false, '${role_default-roles}', 'default-roles-master', 'master', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', 'master', false, '${role_admin}', 'admin', 'master', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('e0d0806a-b318-4b7a-81ac-21a250011133', 'master', false, '${role_create-realm}', 'create-realm', 'master', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('eab5d470-1f6c-4a3b-b695-2991f25bad1b', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_create-client}', 'create-client', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('230f61d0-fb05-4c05-8146-6db93b13acf4', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_view-realm}', 'view-realm', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('83d629c9-f726-43d0-9b61-19bf53c55a63', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_view-users}', 'view-users', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('dab0de20-8ea9-4832-89de-f4d5504fb9db', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_view-clients}', 'view-clients', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('8974458a-8aa7-49a1-99ff-b37d8a7c7692', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_view-events}', 'view-events', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('1e7a2f1c-be44-453e-9d2a-dd269734585d', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_view-identity-providers}', 'view-identity-providers', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('4ce8f947-ee72-43bf-8317-244d1ea60606', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_view-authorization}', 'view-authorization', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('c4b35d41-4136-4570-96bd-653354add90f', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_manage-realm}', 'manage-realm', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('d871f132-78e6-4403-af4c-a8b828af732c', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_manage-users}', 'manage-users', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('f379ea05-5388-477e-afbe-70d7342350e8', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_manage-clients}', 'manage-clients', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('da866b13-59bd-4774-b0e1-982d8e3b5750', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_manage-events}', 'manage-events', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('c09a1a15-48f5-4131-a657-2696a698a63e', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_manage-identity-providers}', 'manage-identity-providers', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('4c52c5e7-861f-4833-886a-8f25685dec9b', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_manage-authorization}', 'manage-authorization', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('269f6cb5-2c16-4724-9069-65d3fb736675', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_query-users}', 'query-users', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('1b2e57b8-2621-4e2c-8fb8-cadcbe755852', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_query-clients}', 'query-clients', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('778070c7-349e-483c-9fff-0457eadfbcd9', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_query-realms}', 'query-realms', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('fddaaae9-36f4-4732-8e34-57c27aecc3b4', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_query-groups}', 'query-groups', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('2fc3f427-2d06-4b3e-9ac3-3d9ba3b191b1', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_view-profile}', 'view-profile', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('412e3737-b214-48e3-9a53-e5eeff2d222e', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_manage-account}', 'manage-account', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('4483f956-e449-4adc-90bd-f6d67bb4c555', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_manage-account-links}', 'manage-account-links', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('4652d8dc-a13a-4181-9965-4db7b91d10bd', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_view-applications}', 'view-applications', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('6e8c8293-b03f-4d31-893c-8e33d4c6c507', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_view-consent}', 'view-consent', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('34a5646f-1726-4adc-9904-b2ca3566efe2', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_manage-consent}', 'manage-consent', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('d05e5d5b-32b5-4d25-ae20-404c245fc01f', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_delete-account}', 'delete-account', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('f0fa5dc8-305e-4897-b668-1f5218fa5545', '082d9499-a226-4009-af33-5c10e7a18333', true, '${role_read-token}', 'read-token', 'master', '082d9499-a226-4009-af33-5c10e7a18333', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('8909e49f-c025-4988-9aa4-7e9a40ffc14f', 'd8a35e32-b522-425f-a314-77e6b313330c', true, '${role_impersonation}', 'impersonation', 'master', 'd8a35e32-b522-425f-a314-77e6b313330c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('f10a139c-29b3-4236-86d1-98baedb83512', 'master', false, '${role_offline-access}', 'offline_access', 'master', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('d947ccfe-6754-4a0b-bb57-3a520a70144c', 'master', false, '${role_uma_authorization}', 'uma_authorization', 'master', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('03eb0760-ea2d-490f-8608-402662997979', 'development', false, '${role_default-roles}', 'default-roles-development', 'development', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('aafba5bd-8890-4c4c-b178-a7ca86104278', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_create-client}', 'create-client', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('5470f70b-c2cd-49b6-90bc-6d20fb403b66', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_view-realm}', 'view-realm', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('34cc8105-3714-4b58-9f7c-4d0f02169b43', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_view-users}', 'view-users', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('3803571c-5eb3-4714-b9c4-eb5e41203d43', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_view-clients}', 'view-clients', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('b9190dcf-d7ee-4fd6-ac2b-1e311a48f840', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_view-events}', 'view-events', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('fe5ad8b5-406e-41d7-9a2e-42385f1925a6', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_view-identity-providers}', 'view-identity-providers', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('3b2a3190-037c-4017-ac90-0f06e7699345', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_view-authorization}', 'view-authorization', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('16d4e279-a059-4b3d-8eca-cc886d3b00cc', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_manage-realm}', 'manage-realm', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('095d2494-5f71-45b4-9e41-4fc9b3538628', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_manage-users}', 'manage-users', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('e98d79ce-f38f-4f7e-a247-1f835e3a5f7b', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_manage-clients}', 'manage-clients', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('99a9bc2e-472d-4ee9-84d1-79c77288c8bf', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_manage-events}', 'manage-events', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('1b5f94f4-443b-451a-9e10-2d582d72c52d', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_manage-identity-providers}', 'manage-identity-providers', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('ea6aa2ac-489d-44de-a353-0fd663eadedf', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_manage-authorization}', 'manage-authorization', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('8a8f1618-4dcd-4a62-86b6-f83077d1ce5d', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_query-users}', 'query-users', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('590d2771-11cd-49d9-afd9-3d843ea0fa7f', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_query-clients}', 'query-clients', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('55a0caae-ea26-40b8-b739-4e4cd008ae40', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_query-realms}', 'query-realms', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('c20d3730-b91c-49cf-b9de-b9cd7ad66eb7', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_query-groups}', 'query-groups', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('c72c9718-fa5e-4ec2-9b02-8989639dcece', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_realm-admin}', 'realm-admin', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('b795e084-dc0a-4903-8e4d-06f553bb6fd9', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_create-client}', 'create-client', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('3e73b15e-331c-481d-b925-babec65d356b', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_view-realm}', 'view-realm', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('8bc3c47d-126a-49b7-94d7-541ff2ff7804', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_view-users}', 'view-users', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('448e437a-b999-419d-81b9-462d3b9faa47', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_view-clients}', 'view-clients', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('f65abf79-96d7-46bb-898e-ca591060d130', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_view-events}', 'view-events', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('158c3f81-fbe3-438f-b355-780f26b2a962', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_view-identity-providers}', 'view-identity-providers', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('2a1d3fe3-7043-498c-a7b9-fdd88610c5cb', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_view-authorization}', 'view-authorization', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('46981d80-d5f3-477e-8859-ff0384df5ebc', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_manage-realm}', 'manage-realm', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('4ba6f226-e44d-4601-8dfd-64c21b052b17', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_manage-users}', 'manage-users', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('4fdcfef1-c38b-4ca0-8182-b1d664709520', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_manage-clients}', 'manage-clients', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('2f8deaf3-3c26-4901-ad18-4c7a9ca0699a', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_manage-events}', 'manage-events', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('031c3ed2-1559-491b-800a-ca9aef6f923b', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_manage-identity-providers}', 'manage-identity-providers', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('097a6bd3-7ad7-4c32-b736-1cd6181ec505', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_manage-authorization}', 'manage-authorization', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('b1e0272c-5f10-45ad-9ec5-1fa8dd8f6a07', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_query-users}', 'query-users', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('d380a439-4eb0-4f64-b365-184a87dbf1ae', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_query-clients}', 'query-clients', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('ba27d6f5-65f7-48a0-8d1c-2d50a96ea936', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_query-realms}', 'query-realms', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('6c7ef695-de97-4ffe-bef7-1607ed2c1b37', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_query-groups}', 'query-groups', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('6b56c8e8-5853-4bc5-a1b9-684867617d18', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_view-profile}', 'view-profile', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('78750051-2e54-4c61-9f29-380d8fd8d7c1', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_manage-account}', 'manage-account', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('36460f40-0577-4fa4-8ff3-3b3fd8d0fa85', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_manage-account-links}', 'manage-account-links', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('08c608dc-b490-499d-8e5d-99aae83d0c37', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_view-applications}', 'view-applications', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('632a9199-7c61-4594-b05b-91b628f59333', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_view-consent}', 'view-consent', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('e11cfec5-3824-45a3-a42c-fcd5058a0e63', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_manage-consent}', 'manage-consent', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('3b29c92a-19dd-4185-8e45-26e5e751cfb2', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_delete-account}', 'delete-account', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('30838f74-ad97-4640-b9cb-d5c997518bfa', '297eff51-12d4-43da-9069-f632024daf18', true, '${role_impersonation}', 'impersonation', 'master', '297eff51-12d4-43da-9069-f632024daf18', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('5e757fff-6d31-4597-b37c-c3cdd4775066', '2090c53c-83d2-4087-b22a-88d4c12730e4', true, '${role_impersonation}', 'impersonation', 'development', '2090c53c-83d2-4087-b22a-88d4c12730e4', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('947dd62a-190e-4b63-b787-b3d65c14e0b9', '51bba6d9-f73d-4f45-8b06-392303051f3c', true, '${role_read-token}', 'read-token', 'development', '51bba6d9-f73d-4f45-8b06-392303051f3c', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('d611163f-3327-43ee-a7ea-790110d80df3', 'development', false, '${role_offline-access}', 'offline_access', 'development', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('666ed87a-22da-43e8-8cf0-08cffd1e0b8a', 'development', false, '${role_uma_authorization}', 'uma_authorization', 'development', NULL, NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('124eaa2c-407d-457e-b011-43de0447c790', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', true, '${role_view-groups}', 'view-groups', 'master', 'c8afb027-0c8c-4bdc-bf0f-be4a2c172439', NULL);
INSERT INTO public.keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client, realm) VALUES ('4acedae6-e297-4029-a9a0-4c87ff235666', '15b080ba-7783-49c9-b155-86ac5e1855b1', true, '${role_view-groups}', 'view-groups', 'development', '15b080ba-7783-49c9-b155-86ac5e1855b1', NULL);


--
-- Data for Name: migration_model; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.migration_model (id, version, update_time) VALUES ('mi42b', '16.1.1', 1646033070);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('vpkin', '21.1.1', 1683631203);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('avej5', '21.1.2', 1691059042);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('pojcj', '22.0.1', 1691154929);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('m28j7', '22.0.5', 1710838220);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('vgqdp', '26.0.6', 1739962763);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('6c2df', '26.0.8', 1740149300);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('jknmx', '26.1.4', 1742571061);
INSERT INTO public.migration_model (id, version, update_time) VALUES ('5rh32', '26.4.7', 1764937050);


--
-- Data for Name: offline_client_session; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: offline_user_session; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: org; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: org_domain; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: policy_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: protocol_mapper; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('40b33f72-e076-45a0-9e9a-78ba0b77743e', 'audience resolve', 'openid-connect', 'oidc-audience-resolve-mapper', '34c90cb1-59ee-4c3d-b280-035eb751690a', NULL);
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('537dc4a8-319a-45b3-8625-0b6f46376529', 'locale', 'openid-connect', 'oidc-usermodel-attribute-mapper', '19f8a8d4-4eee-413b-8430-1779b7cd1bec', NULL);
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('d48c53e0-d218-4ace-a934-85ae05c88c75', 'role list', 'saml', 'saml-role-list-mapper', NULL, '22487cb4-ae45-40c0-bf42-6cc8a0522d49');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('f1f8bdb1-65fe-4ee1-930d-3a6672cda70b', 'full name', 'openid-connect', 'oidc-full-name-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('4949773e-f6ed-450e-bb1d-a736ecb0cb7a', 'family name', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('1b17f5cb-4b85-4486-9c27-56f1922b46d9', 'given name', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('8e37b39e-aaba-46cc-94b7-c821e7ee1b52', 'middle name', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('21ed3c62-15f7-4adf-b8d2-6fe9ea8ec9d3', 'nickname', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('f108e47f-739e-40c7-b5a8-ae33303214f6', 'username', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('b76fbc73-9421-4c26-90a7-9aee9fb84d32', 'profile', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('58a799c1-7656-43bc-823d-5d00975525c1', 'picture', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('2aadad1b-acaa-43a4-953e-783fe5ee41d0', 'website', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('9b4bd544-ac57-4682-ba8a-d1b541164595', 'gender', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('bef89f2a-21ff-4ee4-bb8a-044ed77717de', 'birthdate', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('786da749-5da7-4014-bb27-c0c34891bd33', 'zoneinfo', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('55f5468d-5fdd-4a84-b68d-0f120a99bbb3', 'locale', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('56de78a9-4e0c-4247-a08b-b1d39fec815e', 'updated at', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '7d3289fd-f5f7-4c73-a857-6506457c1e6c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('736eb3d3-b09a-4cdf-90aa-0417c6102c0b', 'email', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, 'e8f953e9-1796-475d-bfca-e6d7b2c911c4');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('d565550c-11e9-462e-a5d1-415cfa62d5fe', 'email verified', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, 'e8f953e9-1796-475d-bfca-e6d7b2c911c4');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'address', 'openid-connect', 'oidc-address-mapper', NULL, 'cce07087-b8b0-4793-890f-5c84fefa9439');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('6fee272b-15a2-4042-bcc8-fbcc64a3c71a', 'phone number', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, 'b6f91cd7-ee91-49dd-8159-b595d810694b');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('76c1ebff-9137-481e-ae4c-86292e5dddba', 'phone number verified', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, 'b6f91cd7-ee91-49dd-8159-b595d810694b');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('e09332c5-238c-4195-aef1-848b456574d2', 'realm roles', 'openid-connect', 'oidc-usermodel-realm-role-mapper', NULL, '241020fc-78d9-4971-b9ec-a906bda7b14c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('6d278c6b-2bb5-45b8-8eda-1dee64fbea9e', 'client roles', 'openid-connect', 'oidc-usermodel-client-role-mapper', NULL, '241020fc-78d9-4971-b9ec-a906bda7b14c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('c3df2599-0774-46b8-a6b4-8470a7f33ee5', 'audience resolve', 'openid-connect', 'oidc-audience-resolve-mapper', NULL, '241020fc-78d9-4971-b9ec-a906bda7b14c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('17e0ccef-888c-488f-8bfb-f1bbb44a4bc3', 'allowed web origins', 'openid-connect', 'oidc-allowed-origins-mapper', NULL, 'e32926f4-d4af-44cf-99e9-4d5eceb55a35');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('138f19d0-e478-489a-a4a8-6a49b64ea134', 'upn', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '2007079b-9593-4a89-905e-3a492a1a6135');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('1e9a0ad0-bb76-4c23-875f-3e466bf365c7', 'groups', 'openid-connect', 'oidc-usermodel-realm-role-mapper', NULL, '2007079b-9593-4a89-905e-3a492a1a6135');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('885fbaa6-21ec-441e-8b72-7ada2255eeab', 'audience resolve', 'openid-connect', 'oidc-audience-resolve-mapper', '2c539eab-1781-4d43-94da-033984eea7f6', NULL);
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('ac121897-105f-4107-9998-e9b3ea6f2052', 'role list', 'saml', 'saml-role-list-mapper', NULL, 'c1f5c722-af78-4138-b5e5-8b9c5a22fd58');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('2bddd448-39d2-48ca-b48c-8d8752cbeed2', 'full name', 'openid-connect', 'oidc-full-name-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('1d3dbbdc-8253-4dbf-b361-eca7b882f24a', 'family name', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('b7384fb3-6f4a-44e8-b8f7-800b70f6d0b4', 'given name', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('b0d0bacd-5c73-40d0-8e68-958912bf32c1', 'middle name', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('1745b133-6ef3-46c1-ad0a-83ba5318f85e', 'nickname', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('9c4f86a7-0c7c-4f11-ae05-8bf79fb29ecd', 'username', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('0e02ae25-af22-4d3f-8339-84226f59efeb', 'profile', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('c380c5f4-71a0-4fda-85bc-a6d457117e7c', 'picture', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('80f4c45b-8c40-4b6e-ba78-2ee41da9d2c7', 'website', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('4ed39ad0-bffa-435f-873e-fe1e5df82804', 'gender', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('c35abd6c-1366-476b-b911-3c2a16ebbc66', 'birthdate', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('dbb0edad-89ee-4ffd-a9ff-46bde1937501', 'zoneinfo', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('b59c940d-dcdf-44e9-a4ed-88d23e37b46d', 'locale', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('a7511fbd-c6e7-49dd-afca-afd25fc96e2f', 'updated at', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '2838ffe1-c64d-49c1-8d36-6e6966f22b92');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('dc9e0066-ae75-450f-a29e-327f62a03ae8', 'email', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('8561d60e-af93-485e-bd8d-d4493cae1bdb', 'email verified', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, 'a048e0b8-7cfd-46ca-af8b-7790dbcb69fd');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'address', 'openid-connect', 'oidc-address-mapper', NULL, '2a598f0a-e595-46b6-9641-7e305f447623');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('0a751081-e95c-4340-a7b2-bf35739c8a65', 'phone number', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '053548c2-191c-44b2-82a5-1aa9f2804735');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('cce0896f-a126-4b89-8756-f750b0a076b8', 'phone number verified', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '053548c2-191c-44b2-82a5-1aa9f2804735');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('e21b93cb-13f1-4ba5-a373-7d41c892a9c5', 'realm roles', 'openid-connect', 'oidc-usermodel-realm-role-mapper', NULL, '5ef28d69-868b-4f07-80f9-d5284e4f82a9');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'client roles', 'openid-connect', 'oidc-usermodel-client-role-mapper', NULL, '5ef28d69-868b-4f07-80f9-d5284e4f82a9');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('20aa8f3d-675b-4f62-8388-daaa163274f9', 'audience resolve', 'openid-connect', 'oidc-audience-resolve-mapper', NULL, '5ef28d69-868b-4f07-80f9-d5284e4f82a9');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('02239292-0cc4-4689-a540-078e115642be', 'allowed web origins', 'openid-connect', 'oidc-allowed-origins-mapper', NULL, '16915d69-1f21-416e-8194-04783e07e47c');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('e2a7c68f-19ed-4536-8303-c14eac258710', 'upn', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '80982e4f-2393-44de-8207-6ce9526f3fb3');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('0cf91558-aaa6-4d67-bc7e-4ab382771048', 'groups', 'openid-connect', 'oidc-usermodel-realm-role-mapper', NULL, '80982e4f-2393-44de-8207-6ce9526f3fb3');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('3c934d4b-bc0a-4e58-a0be-79cda19eabe5', 'locale', 'openid-connect', 'oidc-usermodel-attribute-mapper', '723bc5a7-52cb-4063-b8f4-b45327f5db02', NULL);
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('dd8f1754-723c-47ed-b19e-1dad52006743', 'audience trustdeck', 'openid-connect', 'oidc-audience-mapper', '2be34fd2-d092-457d-b56b-9535ff5ea02a', NULL);
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('8663d46a-7643-4724-9d15-4c670dc00d8e', 'client roles', 'openid-connect', 'oidc-usermodel-client-role-mapper', '2be34fd2-d092-457d-b56b-9535ff5ea02a', NULL);
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'GroupMapper', 'openid-connect', 'oidc-group-membership-mapper', '2be34fd2-d092-457d-b56b-9535ff5ea02a', NULL);
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('f2f1d931-0a0a-48b8-99ba-6342ca70dc51', 'acr loa level', 'openid-connect', 'oidc-acr-mapper', NULL, '3515bf6a-cb15-45db-b55c-6ec21ac99990');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('4527bbbe-cf23-4c2a-9b34-9fa94bb680e0', 'acr loa level', 'openid-connect', 'oidc-acr-mapper', NULL, 'e22d2191-0ebe-40c6-9d8c-a3c6cc44aaf3');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('d9119f30-a33e-4a14-a874-72452b269c56', 'auth_time', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, '7ef392a1-6728-40ac-9bf6-381f1fbc3b20');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('0d9cea58-2032-4916-81b3-7bc4123d1a95', 'sub', 'openid-connect', 'oidc-sub-mapper', NULL, '7ef392a1-6728-40ac-9bf6-381f1fbc3b20');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('08ba371e-e26f-4fec-ae61-af8652dd7461', 'auth_time', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, '25df115c-d573-4b3e-bf3b-db291bfbceff');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('ca6d5979-186c-4b5f-885c-1e949f4f9cdf', 'sub', 'openid-connect', 'oidc-sub-mapper', NULL, '25df115c-d573-4b3e-bf3b-db291bfbceff');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('4e3492f3-e576-4381-a744-5216db43a8ce', 'Client ID', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, '0530c3dd-3064-4438-b252-3aa26b879293');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('efd4e08a-c198-4d02-944b-55743faff236', 'Client Host', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, '0530c3dd-3064-4438-b252-3aa26b879293');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('b55da32b-ff87-450d-8e17-ce8778db6e19', 'Client IP Address', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, '0530c3dd-3064-4438-b252-3aa26b879293');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('2fddd973-fc1b-4bff-ba31-b0ee3b4a3307', 'Client ID', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, 'b55992d7-358a-4282-8162-43f01e1da5ec');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('0e4436d1-f5ae-4834-a06c-dd064f6feb7c', 'Client Host', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, 'b55992d7-358a-4282-8162-43f01e1da5ec');
INSERT INTO public.protocol_mapper (id, name, protocol, protocol_mapper_name, client_id, client_scope_id) VALUES ('9a5b84f9-0f88-4575-ac41-d7e951fa4ab8', 'Client IP Address', 'openid-connect', 'oidc-usersessionmodel-note-mapper', NULL, 'b55992d7-358a-4282-8162-43f01e1da5ec');


--
-- Data for Name: protocol_mapper_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('537dc4a8-319a-45b3-8625-0b6f46376529', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('537dc4a8-319a-45b3-8625-0b6f46376529', 'locale', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('537dc4a8-319a-45b3-8625-0b6f46376529', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('537dc4a8-319a-45b3-8625-0b6f46376529', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('537dc4a8-319a-45b3-8625-0b6f46376529', 'locale', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('537dc4a8-319a-45b3-8625-0b6f46376529', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d48c53e0-d218-4ace-a934-85ae05c88c75', 'false', 'single');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d48c53e0-d218-4ace-a934-85ae05c88c75', 'Basic', 'attribute.nameformat');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d48c53e0-d218-4ace-a934-85ae05c88c75', 'Role', 'attribute.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f1f8bdb1-65fe-4ee1-930d-3a6672cda70b', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f1f8bdb1-65fe-4ee1-930d-3a6672cda70b', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f1f8bdb1-65fe-4ee1-930d-3a6672cda70b', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4949773e-f6ed-450e-bb1d-a736ecb0cb7a', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4949773e-f6ed-450e-bb1d-a736ecb0cb7a', 'lastName', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4949773e-f6ed-450e-bb1d-a736ecb0cb7a', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4949773e-f6ed-450e-bb1d-a736ecb0cb7a', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4949773e-f6ed-450e-bb1d-a736ecb0cb7a', 'family_name', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4949773e-f6ed-450e-bb1d-a736ecb0cb7a', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1b17f5cb-4b85-4486-9c27-56f1922b46d9', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1b17f5cb-4b85-4486-9c27-56f1922b46d9', 'firstName', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1b17f5cb-4b85-4486-9c27-56f1922b46d9', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1b17f5cb-4b85-4486-9c27-56f1922b46d9', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1b17f5cb-4b85-4486-9c27-56f1922b46d9', 'given_name', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1b17f5cb-4b85-4486-9c27-56f1922b46d9', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8e37b39e-aaba-46cc-94b7-c821e7ee1b52', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8e37b39e-aaba-46cc-94b7-c821e7ee1b52', 'middleName', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8e37b39e-aaba-46cc-94b7-c821e7ee1b52', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8e37b39e-aaba-46cc-94b7-c821e7ee1b52', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8e37b39e-aaba-46cc-94b7-c821e7ee1b52', 'middle_name', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8e37b39e-aaba-46cc-94b7-c821e7ee1b52', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('21ed3c62-15f7-4adf-b8d2-6fe9ea8ec9d3', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('21ed3c62-15f7-4adf-b8d2-6fe9ea8ec9d3', 'nickname', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('21ed3c62-15f7-4adf-b8d2-6fe9ea8ec9d3', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('21ed3c62-15f7-4adf-b8d2-6fe9ea8ec9d3', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('21ed3c62-15f7-4adf-b8d2-6fe9ea8ec9d3', 'nickname', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('21ed3c62-15f7-4adf-b8d2-6fe9ea8ec9d3', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f108e47f-739e-40c7-b5a8-ae33303214f6', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f108e47f-739e-40c7-b5a8-ae33303214f6', 'username', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f108e47f-739e-40c7-b5a8-ae33303214f6', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f108e47f-739e-40c7-b5a8-ae33303214f6', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f108e47f-739e-40c7-b5a8-ae33303214f6', 'preferred_username', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f108e47f-739e-40c7-b5a8-ae33303214f6', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b76fbc73-9421-4c26-90a7-9aee9fb84d32', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b76fbc73-9421-4c26-90a7-9aee9fb84d32', 'profile', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b76fbc73-9421-4c26-90a7-9aee9fb84d32', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b76fbc73-9421-4c26-90a7-9aee9fb84d32', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b76fbc73-9421-4c26-90a7-9aee9fb84d32', 'profile', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b76fbc73-9421-4c26-90a7-9aee9fb84d32', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('58a799c1-7656-43bc-823d-5d00975525c1', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('58a799c1-7656-43bc-823d-5d00975525c1', 'picture', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('58a799c1-7656-43bc-823d-5d00975525c1', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('58a799c1-7656-43bc-823d-5d00975525c1', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('58a799c1-7656-43bc-823d-5d00975525c1', 'picture', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('58a799c1-7656-43bc-823d-5d00975525c1', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2aadad1b-acaa-43a4-953e-783fe5ee41d0', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2aadad1b-acaa-43a4-953e-783fe5ee41d0', 'website', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2aadad1b-acaa-43a4-953e-783fe5ee41d0', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2aadad1b-acaa-43a4-953e-783fe5ee41d0', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2aadad1b-acaa-43a4-953e-783fe5ee41d0', 'website', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2aadad1b-acaa-43a4-953e-783fe5ee41d0', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9b4bd544-ac57-4682-ba8a-d1b541164595', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9b4bd544-ac57-4682-ba8a-d1b541164595', 'gender', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9b4bd544-ac57-4682-ba8a-d1b541164595', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9b4bd544-ac57-4682-ba8a-d1b541164595', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9b4bd544-ac57-4682-ba8a-d1b541164595', 'gender', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9b4bd544-ac57-4682-ba8a-d1b541164595', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('bef89f2a-21ff-4ee4-bb8a-044ed77717de', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('bef89f2a-21ff-4ee4-bb8a-044ed77717de', 'birthdate', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('bef89f2a-21ff-4ee4-bb8a-044ed77717de', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('bef89f2a-21ff-4ee4-bb8a-044ed77717de', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('bef89f2a-21ff-4ee4-bb8a-044ed77717de', 'birthdate', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('bef89f2a-21ff-4ee4-bb8a-044ed77717de', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('786da749-5da7-4014-bb27-c0c34891bd33', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('786da749-5da7-4014-bb27-c0c34891bd33', 'zoneinfo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('786da749-5da7-4014-bb27-c0c34891bd33', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('786da749-5da7-4014-bb27-c0c34891bd33', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('786da749-5da7-4014-bb27-c0c34891bd33', 'zoneinfo', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('786da749-5da7-4014-bb27-c0c34891bd33', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('55f5468d-5fdd-4a84-b68d-0f120a99bbb3', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('55f5468d-5fdd-4a84-b68d-0f120a99bbb3', 'locale', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('55f5468d-5fdd-4a84-b68d-0f120a99bbb3', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('55f5468d-5fdd-4a84-b68d-0f120a99bbb3', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('55f5468d-5fdd-4a84-b68d-0f120a99bbb3', 'locale', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('55f5468d-5fdd-4a84-b68d-0f120a99bbb3', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('56de78a9-4e0c-4247-a08b-b1d39fec815e', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('56de78a9-4e0c-4247-a08b-b1d39fec815e', 'updatedAt', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('56de78a9-4e0c-4247-a08b-b1d39fec815e', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('56de78a9-4e0c-4247-a08b-b1d39fec815e', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('56de78a9-4e0c-4247-a08b-b1d39fec815e', 'updated_at', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('56de78a9-4e0c-4247-a08b-b1d39fec815e', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('736eb3d3-b09a-4cdf-90aa-0417c6102c0b', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('736eb3d3-b09a-4cdf-90aa-0417c6102c0b', 'email', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('736eb3d3-b09a-4cdf-90aa-0417c6102c0b', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('736eb3d3-b09a-4cdf-90aa-0417c6102c0b', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('736eb3d3-b09a-4cdf-90aa-0417c6102c0b', 'email', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('736eb3d3-b09a-4cdf-90aa-0417c6102c0b', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d565550c-11e9-462e-a5d1-415cfa62d5fe', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d565550c-11e9-462e-a5d1-415cfa62d5fe', 'emailVerified', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d565550c-11e9-462e-a5d1-415cfa62d5fe', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d565550c-11e9-462e-a5d1-415cfa62d5fe', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d565550c-11e9-462e-a5d1-415cfa62d5fe', 'email_verified', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d565550c-11e9-462e-a5d1-415cfa62d5fe', 'boolean', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'formatted', 'user.attribute.formatted');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'country', 'user.attribute.country');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'postal_code', 'user.attribute.postal_code');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'street', 'user.attribute.street');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'region', 'user.attribute.region');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e0f23c36-d522-45a8-939a-f610357da89f', 'locality', 'user.attribute.locality');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6fee272b-15a2-4042-bcc8-fbcc64a3c71a', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6fee272b-15a2-4042-bcc8-fbcc64a3c71a', 'phoneNumber', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6fee272b-15a2-4042-bcc8-fbcc64a3c71a', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6fee272b-15a2-4042-bcc8-fbcc64a3c71a', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6fee272b-15a2-4042-bcc8-fbcc64a3c71a', 'phone_number', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6fee272b-15a2-4042-bcc8-fbcc64a3c71a', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('76c1ebff-9137-481e-ae4c-86292e5dddba', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('76c1ebff-9137-481e-ae4c-86292e5dddba', 'phoneNumberVerified', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('76c1ebff-9137-481e-ae4c-86292e5dddba', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('76c1ebff-9137-481e-ae4c-86292e5dddba', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('76c1ebff-9137-481e-ae4c-86292e5dddba', 'phone_number_verified', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('76c1ebff-9137-481e-ae4c-86292e5dddba', 'boolean', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e09332c5-238c-4195-aef1-848b456574d2', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e09332c5-238c-4195-aef1-848b456574d2', 'foo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e09332c5-238c-4195-aef1-848b456574d2', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e09332c5-238c-4195-aef1-848b456574d2', 'realm_access.roles', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e09332c5-238c-4195-aef1-848b456574d2', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6d278c6b-2bb5-45b8-8eda-1dee64fbea9e', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6d278c6b-2bb5-45b8-8eda-1dee64fbea9e', 'foo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6d278c6b-2bb5-45b8-8eda-1dee64fbea9e', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6d278c6b-2bb5-45b8-8eda-1dee64fbea9e', 'resource_access.${client_id}.roles', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('6d278c6b-2bb5-45b8-8eda-1dee64fbea9e', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('138f19d0-e478-489a-a4a8-6a49b64ea134', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('138f19d0-e478-489a-a4a8-6a49b64ea134', 'username', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('138f19d0-e478-489a-a4a8-6a49b64ea134', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('138f19d0-e478-489a-a4a8-6a49b64ea134', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('138f19d0-e478-489a-a4a8-6a49b64ea134', 'upn', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('138f19d0-e478-489a-a4a8-6a49b64ea134', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1e9a0ad0-bb76-4c23-875f-3e466bf365c7', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1e9a0ad0-bb76-4c23-875f-3e466bf365c7', 'foo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1e9a0ad0-bb76-4c23-875f-3e466bf365c7', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1e9a0ad0-bb76-4c23-875f-3e466bf365c7', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1e9a0ad0-bb76-4c23-875f-3e466bf365c7', 'groups', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1e9a0ad0-bb76-4c23-875f-3e466bf365c7', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac121897-105f-4107-9998-e9b3ea6f2052', 'false', 'single');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac121897-105f-4107-9998-e9b3ea6f2052', 'Basic', 'attribute.nameformat');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac121897-105f-4107-9998-e9b3ea6f2052', 'Role', 'attribute.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2bddd448-39d2-48ca-b48c-8d8752cbeed2', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2bddd448-39d2-48ca-b48c-8d8752cbeed2', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2bddd448-39d2-48ca-b48c-8d8752cbeed2', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1d3dbbdc-8253-4dbf-b361-eca7b882f24a', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1d3dbbdc-8253-4dbf-b361-eca7b882f24a', 'lastName', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1d3dbbdc-8253-4dbf-b361-eca7b882f24a', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1d3dbbdc-8253-4dbf-b361-eca7b882f24a', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1d3dbbdc-8253-4dbf-b361-eca7b882f24a', 'family_name', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1d3dbbdc-8253-4dbf-b361-eca7b882f24a', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b7384fb3-6f4a-44e8-b8f7-800b70f6d0b4', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b7384fb3-6f4a-44e8-b8f7-800b70f6d0b4', 'firstName', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b7384fb3-6f4a-44e8-b8f7-800b70f6d0b4', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b7384fb3-6f4a-44e8-b8f7-800b70f6d0b4', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b7384fb3-6f4a-44e8-b8f7-800b70f6d0b4', 'given_name', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b7384fb3-6f4a-44e8-b8f7-800b70f6d0b4', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b0d0bacd-5c73-40d0-8e68-958912bf32c1', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b0d0bacd-5c73-40d0-8e68-958912bf32c1', 'middleName', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b0d0bacd-5c73-40d0-8e68-958912bf32c1', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b0d0bacd-5c73-40d0-8e68-958912bf32c1', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b0d0bacd-5c73-40d0-8e68-958912bf32c1', 'middle_name', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b0d0bacd-5c73-40d0-8e68-958912bf32c1', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1745b133-6ef3-46c1-ad0a-83ba5318f85e', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1745b133-6ef3-46c1-ad0a-83ba5318f85e', 'nickname', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1745b133-6ef3-46c1-ad0a-83ba5318f85e', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1745b133-6ef3-46c1-ad0a-83ba5318f85e', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1745b133-6ef3-46c1-ad0a-83ba5318f85e', 'nickname', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('1745b133-6ef3-46c1-ad0a-83ba5318f85e', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9c4f86a7-0c7c-4f11-ae05-8bf79fb29ecd', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9c4f86a7-0c7c-4f11-ae05-8bf79fb29ecd', 'username', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9c4f86a7-0c7c-4f11-ae05-8bf79fb29ecd', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9c4f86a7-0c7c-4f11-ae05-8bf79fb29ecd', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9c4f86a7-0c7c-4f11-ae05-8bf79fb29ecd', 'preferred_username', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9c4f86a7-0c7c-4f11-ae05-8bf79fb29ecd', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e02ae25-af22-4d3f-8339-84226f59efeb', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e02ae25-af22-4d3f-8339-84226f59efeb', 'profile', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e02ae25-af22-4d3f-8339-84226f59efeb', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e02ae25-af22-4d3f-8339-84226f59efeb', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e02ae25-af22-4d3f-8339-84226f59efeb', 'profile', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e02ae25-af22-4d3f-8339-84226f59efeb', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c380c5f4-71a0-4fda-85bc-a6d457117e7c', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c380c5f4-71a0-4fda-85bc-a6d457117e7c', 'picture', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c380c5f4-71a0-4fda-85bc-a6d457117e7c', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c380c5f4-71a0-4fda-85bc-a6d457117e7c', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c380c5f4-71a0-4fda-85bc-a6d457117e7c', 'picture', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c380c5f4-71a0-4fda-85bc-a6d457117e7c', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('80f4c45b-8c40-4b6e-ba78-2ee41da9d2c7', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('80f4c45b-8c40-4b6e-ba78-2ee41da9d2c7', 'website', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('80f4c45b-8c40-4b6e-ba78-2ee41da9d2c7', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('80f4c45b-8c40-4b6e-ba78-2ee41da9d2c7', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('80f4c45b-8c40-4b6e-ba78-2ee41da9d2c7', 'website', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('80f4c45b-8c40-4b6e-ba78-2ee41da9d2c7', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4ed39ad0-bffa-435f-873e-fe1e5df82804', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4ed39ad0-bffa-435f-873e-fe1e5df82804', 'gender', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4ed39ad0-bffa-435f-873e-fe1e5df82804', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4ed39ad0-bffa-435f-873e-fe1e5df82804', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4ed39ad0-bffa-435f-873e-fe1e5df82804', 'gender', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4ed39ad0-bffa-435f-873e-fe1e5df82804', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c35abd6c-1366-476b-b911-3c2a16ebbc66', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c35abd6c-1366-476b-b911-3c2a16ebbc66', 'birthdate', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c35abd6c-1366-476b-b911-3c2a16ebbc66', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c35abd6c-1366-476b-b911-3c2a16ebbc66', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c35abd6c-1366-476b-b911-3c2a16ebbc66', 'birthdate', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('c35abd6c-1366-476b-b911-3c2a16ebbc66', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dbb0edad-89ee-4ffd-a9ff-46bde1937501', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dbb0edad-89ee-4ffd-a9ff-46bde1937501', 'zoneinfo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dbb0edad-89ee-4ffd-a9ff-46bde1937501', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dbb0edad-89ee-4ffd-a9ff-46bde1937501', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dbb0edad-89ee-4ffd-a9ff-46bde1937501', 'zoneinfo', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dbb0edad-89ee-4ffd-a9ff-46bde1937501', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b59c940d-dcdf-44e9-a4ed-88d23e37b46d', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b59c940d-dcdf-44e9-a4ed-88d23e37b46d', 'locale', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b59c940d-dcdf-44e9-a4ed-88d23e37b46d', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b59c940d-dcdf-44e9-a4ed-88d23e37b46d', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b59c940d-dcdf-44e9-a4ed-88d23e37b46d', 'locale', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b59c940d-dcdf-44e9-a4ed-88d23e37b46d', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('a7511fbd-c6e7-49dd-afca-afd25fc96e2f', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('a7511fbd-c6e7-49dd-afca-afd25fc96e2f', 'updatedAt', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('a7511fbd-c6e7-49dd-afca-afd25fc96e2f', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('a7511fbd-c6e7-49dd-afca-afd25fc96e2f', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('a7511fbd-c6e7-49dd-afca-afd25fc96e2f', 'updated_at', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('a7511fbd-c6e7-49dd-afca-afd25fc96e2f', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dc9e0066-ae75-450f-a29e-327f62a03ae8', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dc9e0066-ae75-450f-a29e-327f62a03ae8', 'email', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dc9e0066-ae75-450f-a29e-327f62a03ae8', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dc9e0066-ae75-450f-a29e-327f62a03ae8', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dc9e0066-ae75-450f-a29e-327f62a03ae8', 'email', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dc9e0066-ae75-450f-a29e-327f62a03ae8', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8561d60e-af93-485e-bd8d-d4493cae1bdb', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8561d60e-af93-485e-bd8d-d4493cae1bdb', 'emailVerified', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8561d60e-af93-485e-bd8d-d4493cae1bdb', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8561d60e-af93-485e-bd8d-d4493cae1bdb', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8561d60e-af93-485e-bd8d-d4493cae1bdb', 'email_verified', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8561d60e-af93-485e-bd8d-d4493cae1bdb', 'boolean', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'formatted', 'user.attribute.formatted');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'country', 'user.attribute.country');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'postal_code', 'user.attribute.postal_code');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'street', 'user.attribute.street');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'region', 'user.attribute.region');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('aa4b2c5e-1d8f-4187-bbfb-4471d0cb66a1', 'locality', 'user.attribute.locality');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0a751081-e95c-4340-a7b2-bf35739c8a65', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0a751081-e95c-4340-a7b2-bf35739c8a65', 'phoneNumber', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0a751081-e95c-4340-a7b2-bf35739c8a65', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0a751081-e95c-4340-a7b2-bf35739c8a65', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0a751081-e95c-4340-a7b2-bf35739c8a65', 'phone_number', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0a751081-e95c-4340-a7b2-bf35739c8a65', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('cce0896f-a126-4b89-8756-f750b0a076b8', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('cce0896f-a126-4b89-8756-f750b0a076b8', 'phoneNumberVerified', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('cce0896f-a126-4b89-8756-f750b0a076b8', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('cce0896f-a126-4b89-8756-f750b0a076b8', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('cce0896f-a126-4b89-8756-f750b0a076b8', 'phone_number_verified', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('cce0896f-a126-4b89-8756-f750b0a076b8', 'boolean', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e21b93cb-13f1-4ba5-a373-7d41c892a9c5', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e21b93cb-13f1-4ba5-a373-7d41c892a9c5', 'foo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e21b93cb-13f1-4ba5-a373-7d41c892a9c5', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e21b93cb-13f1-4ba5-a373-7d41c892a9c5', 'realm_access.roles', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e21b93cb-13f1-4ba5-a373-7d41c892a9c5', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'foo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'resource_access.${client_id}.roles', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e2a7c68f-19ed-4536-8303-c14eac258710', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e2a7c68f-19ed-4536-8303-c14eac258710', 'username', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e2a7c68f-19ed-4536-8303-c14eac258710', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e2a7c68f-19ed-4536-8303-c14eac258710', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e2a7c68f-19ed-4536-8303-c14eac258710', 'upn', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e2a7c68f-19ed-4536-8303-c14eac258710', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0cf91558-aaa6-4d67-bc7e-4ab382771048', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0cf91558-aaa6-4d67-bc7e-4ab382771048', 'foo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0cf91558-aaa6-4d67-bc7e-4ab382771048', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0cf91558-aaa6-4d67-bc7e-4ab382771048', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0cf91558-aaa6-4d67-bc7e-4ab382771048', 'groups', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0cf91558-aaa6-4d67-bc7e-4ab382771048', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('3c934d4b-bc0a-4e58-a0be-79cda19eabe5', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('3c934d4b-bc0a-4e58-a0be-79cda19eabe5', 'locale', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('3c934d4b-bc0a-4e58-a0be-79cda19eabe5', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('3c934d4b-bc0a-4e58-a0be-79cda19eabe5', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('3c934d4b-bc0a-4e58-a0be-79cda19eabe5', 'locale', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('3c934d4b-bc0a-4e58-a0be-79cda19eabe5', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dd8f1754-723c-47ed-b19e-1dad52006743', 'trustdeck', 'included.client.audience');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dd8f1754-723c-47ed-b19e-1dad52006743', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('dd8f1754-723c-47ed-b19e-1dad52006743', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('e831139c-a82c-4a6b-9890-44d689e0e271', 'trustdeck', 'usermodel.clientRoleMapping.clientId');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8663d46a-7643-4724-9d15-4c670dc00d8e', 'foo', 'user.attribute');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8663d46a-7643-4724-9d15-4c670dc00d8e', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8663d46a-7643-4724-9d15-4c670dc00d8e', 'resource_access.${client_id}.roles', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8663d46a-7643-4724-9d15-4c670dc00d8e', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('8663d46a-7643-4724-9d15-4c670dc00d8e', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f2f1d931-0a0a-48b8-99ba-6342ca70dc51', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('f2f1d931-0a0a-48b8-99ba-6342ca70dc51', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4527bbbe-cf23-4c2a-9b34-9fa94bb680e0', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4527bbbe-cf23-4c2a-9b34-9fa94bb680e0', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'true', 'multivalued');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0d9cea58-2032-4916-81b3-7bc4123d1a95', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0d9cea58-2032-4916-81b3-7bc4123d1a95', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d9119f30-a33e-4a14-a874-72452b269c56', 'AUTH_TIME', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d9119f30-a33e-4a14-a874-72452b269c56', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d9119f30-a33e-4a14-a874-72452b269c56', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d9119f30-a33e-4a14-a874-72452b269c56', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d9119f30-a33e-4a14-a874-72452b269c56', 'auth_time', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('d9119f30-a33e-4a14-a874-72452b269c56', 'long', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('08ba371e-e26f-4fec-ae61-af8652dd7461', 'AUTH_TIME', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('08ba371e-e26f-4fec-ae61-af8652dd7461', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('08ba371e-e26f-4fec-ae61-af8652dd7461', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('08ba371e-e26f-4fec-ae61-af8652dd7461', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('08ba371e-e26f-4fec-ae61-af8652dd7461', 'auth_time', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('08ba371e-e26f-4fec-ae61-af8652dd7461', 'long', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ca6d5979-186c-4b5f-885c-1e949f4f9cdf', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ca6d5979-186c-4b5f-885c-1e949f4f9cdf', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4e3492f3-e576-4381-a744-5216db43a8ce', 'client_id', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4e3492f3-e576-4381-a744-5216db43a8ce', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4e3492f3-e576-4381-a744-5216db43a8ce', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4e3492f3-e576-4381-a744-5216db43a8ce', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4e3492f3-e576-4381-a744-5216db43a8ce', 'client_id', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('4e3492f3-e576-4381-a744-5216db43a8ce', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b55da32b-ff87-450d-8e17-ce8778db6e19', 'clientAddress', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b55da32b-ff87-450d-8e17-ce8778db6e19', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b55da32b-ff87-450d-8e17-ce8778db6e19', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b55da32b-ff87-450d-8e17-ce8778db6e19', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b55da32b-ff87-450d-8e17-ce8778db6e19', 'clientAddress', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('b55da32b-ff87-450d-8e17-ce8778db6e19', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('efd4e08a-c198-4d02-944b-55743faff236', 'clientHost', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('efd4e08a-c198-4d02-944b-55743faff236', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('efd4e08a-c198-4d02-944b-55743faff236', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('efd4e08a-c198-4d02-944b-55743faff236', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('efd4e08a-c198-4d02-944b-55743faff236', 'clientHost', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('efd4e08a-c198-4d02-944b-55743faff236', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e4436d1-f5ae-4834-a06c-dd064f6feb7c', 'clientHost', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e4436d1-f5ae-4834-a06c-dd064f6feb7c', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e4436d1-f5ae-4834-a06c-dd064f6feb7c', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e4436d1-f5ae-4834-a06c-dd064f6feb7c', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e4436d1-f5ae-4834-a06c-dd064f6feb7c', 'clientHost', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('0e4436d1-f5ae-4834-a06c-dd064f6feb7c', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2fddd973-fc1b-4bff-ba31-b0ee3b4a3307', 'client_id', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2fddd973-fc1b-4bff-ba31-b0ee3b4a3307', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'trustdeck_groups', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'false', 'full.path');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2fddd973-fc1b-4bff-ba31-b0ee3b4a3307', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2fddd973-fc1b-4bff-ba31-b0ee3b4a3307', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2fddd973-fc1b-4bff-ba31-b0ee3b4a3307', 'client_id', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('2fddd973-fc1b-4bff-ba31-b0ee3b4a3307', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9a5b84f9-0f88-4575-ac41-d7e951fa4ab8', 'clientAddress', 'user.session.note');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9a5b84f9-0f88-4575-ac41-d7e951fa4ab8', 'true', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9a5b84f9-0f88-4575-ac41-d7e951fa4ab8', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9a5b84f9-0f88-4575-ac41-d7e951fa4ab8', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9a5b84f9-0f88-4575-ac41-d7e951fa4ab8', 'clientAddress', 'claim.name');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('9a5b84f9-0f88-4575-ac41-d7e951fa4ab8', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'false', 'introspection.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'false', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'true', 'lightweight.claim');
INSERT INTO public.protocol_mapper_config (protocol_mapper_id, value, name) VALUES ('ac41b127-b22e-4cdf-a590-e4343cc5daad', 'true', 'access.token.claim');


--
-- Data for Name: realm; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.realm (id, access_code_lifespan, user_action_lifespan, access_token_lifespan, account_theme, admin_theme, email_theme, enabled, events_enabled, events_expiration, login_theme, name, not_before, password_policy, registration_allowed, remember_me, reset_password_allowed, social, ssl_required, sso_idle_timeout, sso_max_lifespan, update_profile_on_soc_login, verify_email, master_admin_client, login_lifespan, internationalization_enabled, default_locale, reg_email_as_username, admin_events_enabled, admin_events_details_enabled, edit_username_allowed, otp_policy_counter, otp_policy_window, otp_policy_period, otp_policy_digits, otp_policy_alg, otp_policy_type, browser_flow, registration_flow, direct_grant_flow, reset_credentials_flow, client_auth_flow, offline_session_idle_timeout, revoke_refresh_token, access_token_life_implicit, login_with_email_allowed, duplicate_emails_allowed, docker_auth_flow, refresh_token_max_reuse, allow_user_managed_access, sso_max_lifespan_remember_me, sso_idle_timeout_remember_me, default_role) VALUES ('master', 60, 300, 60, NULL, NULL, NULL, true, false, 0, NULL, 'master', 0, NULL, false, false, false, false, 'EXTERNAL', 1800, 36000, false, false, 'd8a35e32-b522-425f-a314-77e6b313330c', 1800, false, NULL, false, false, false, false, 0, 1, 30, 6, 'HmacSHA1', 'totp', 'ce2d4e14-e0d7-4fb0-9ba1-388e3ec46b1a', 'a0b128f9-cca8-4ff2-a2d1-50d3e92e87c5', 'ecff1be7-9d5e-4de0-9dc0-fb978f82dfbd', 'abb4c92e-d6d3-4618-bab7-8a458f5c9b16', '4168c80d-15c5-43d3-8369-ccf0237f909a', 2592000, false, 900, true, false, '0367ff4e-3e8d-4b52-a7fa-1ec95778554f', 0, false, 0, 0, '8abade97-996e-4b25-a889-e4bc2d1fc387');
INSERT INTO public.realm (id, access_code_lifespan, user_action_lifespan, access_token_lifespan, account_theme, admin_theme, email_theme, enabled, events_enabled, events_expiration, login_theme, name, not_before, password_policy, registration_allowed, remember_me, reset_password_allowed, social, ssl_required, sso_idle_timeout, sso_max_lifespan, update_profile_on_soc_login, verify_email, master_admin_client, login_lifespan, internationalization_enabled, default_locale, reg_email_as_username, admin_events_enabled, admin_events_details_enabled, edit_username_allowed, otp_policy_counter, otp_policy_window, otp_policy_period, otp_policy_digits, otp_policy_alg, otp_policy_type, browser_flow, registration_flow, direct_grant_flow, reset_credentials_flow, client_auth_flow, offline_session_idle_timeout, revoke_refresh_token, access_token_life_implicit, login_with_email_allowed, duplicate_emails_allowed, docker_auth_flow, refresh_token_max_reuse, allow_user_managed_access, sso_max_lifespan_remember_me, sso_idle_timeout_remember_me, default_role) VALUES ('development', 60, 300, 900, NULL, NULL, NULL, true, false, 0, NULL, 'development', 0, NULL, false, false, false, false, 'EXTERNAL', 1800, 36000, false, false, '297eff51-12d4-43da-9069-f632024daf18', 1800, true, 'en', false, false, false, false, 0, 1, 30, 6, 'HmacSHA1', 'totp', 'e2ddcf02-28b9-429d-a8e8-f06a3fe786a5', 'c5976461-e400-4fe5-8253-18d4fc358c1a', '0ecb5636-f5b7-4ff8-b47e-db1c9fc3b50d', 'd2393001-e20d-45f2-ac91-0904d5c59539', 'b952af4b-074e-4ed9-a4f2-b1be38a53edd', 2592000, false, 900, true, false, '957edc7c-0a36-4a5b-92b8-372d9dd7abc5', 0, false, 0, 0, '03eb0760-ea2d-490f-8608-402662997979');


--
-- Data for Name: realm_attribute; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.contentSecurityPolicyReportOnly', 'master', '');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xContentTypeOptions', 'master', 'nosniff');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xRobotsTag', 'master', 'none');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xFrameOptions', 'master', 'SAMEORIGIN');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.contentSecurityPolicy', 'master', 'frame-src ''self''; frame-ancestors ''self''; object-src ''none'';');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xXSSProtection', 'master', '1; mode=block');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.strictTransportSecurity', 'master', 'max-age=31536000; includeSubDomains');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('bruteForceProtected', 'master', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('permanentLockout', 'master', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('maxFailureWaitSeconds', 'master', '900');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('minimumQuickLoginWaitSeconds', 'master', '60');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('waitIncrementSeconds', 'master', '60');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('quickLoginCheckMilliSeconds', 'master', '1000');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('maxDeltaTimeSeconds', 'master', '43200');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('failureFactor', 'master', '30');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('displayName', 'master', 'Keycloak');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('displayNameHtml', 'master', '<div class="kc-logo-text"><span>Keycloak</span></div>');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('defaultSignatureAlgorithm', 'master', 'RS256');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('offlineSessionMaxLifespanEnabled', 'master', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('offlineSessionMaxLifespan', 'master', '5184000');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('oauth2DeviceCodeLifespan', 'development', '600');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('oauth2DevicePollingInterval', 'development', '5');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('cibaBackchannelTokenDeliveryMode', 'development', 'poll');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('cibaExpiresIn', 'development', '120');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('cibaInterval', 'development', '5');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('cibaAuthRequestedUserHint', 'development', 'login_hint');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('parRequestUriLifespan', 'development', '60');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('clientSessionIdleTimeout', 'development', '0');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('clientSessionMaxLifespan', 'development', '0');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('clientOfflineSessionIdleTimeout', 'development', '0');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('clientOfflineSessionMaxLifespan', 'development', '0');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('displayName', 'development', 'TRUSTDECK');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('bruteForceProtected', 'development', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('permanentLockout', 'development', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('maxFailureWaitSeconds', 'development', '900');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('minimumQuickLoginWaitSeconds', 'development', '60');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('waitIncrementSeconds', 'development', '60');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('quickLoginCheckMilliSeconds', 'development', '1000');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('maxDeltaTimeSeconds', 'development', '43200');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('failureFactor', 'development', '30');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('actionTokenGeneratedByAdminLifespan', 'development', '43200');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('actionTokenGeneratedByUserLifespan', 'development', '1500');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('defaultSignatureAlgorithm', 'development', 'RS256');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('offlineSessionMaxLifespanEnabled', 'development', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('offlineSessionMaxLifespan', 'development', '5184000');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyRpEntityName', 'development', 'keycloak');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicySignatureAlgorithms', 'development', 'ES256');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyRpId', 'development', '');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyAttestationConveyancePreference', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyAuthenticatorAttachment', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyRequireResidentKey', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyUserVerificationRequirement', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyCreateTimeout', 'development', '0');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyAvoidSameAuthenticatorRegister', 'development', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyRpEntityNamePasswordless', 'development', 'keycloak');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicySignatureAlgorithmsPasswordless', 'development', 'ES256');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyRpIdPasswordless', 'development', '');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyAttestationConveyancePreferencePasswordless', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyAuthenticatorAttachmentPasswordless', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyRequireResidentKeyPasswordless', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyUserVerificationRequirementPasswordless', 'development', 'not specified');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyCreateTimeoutPasswordless', 'development', '0');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('webAuthnPolicyAvoidSameAuthenticatorRegisterPasswordless', 'development', 'false');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('client-policies.profiles', 'development', '{"profiles":[]}');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('client-policies.policies', 'development', '{"policies":[]}');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.contentSecurityPolicyReportOnly', 'development', '');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xContentTypeOptions', 'development', 'nosniff');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xRobotsTag', 'development', 'none');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xFrameOptions', 'development', 'SAMEORIGIN');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.contentSecurityPolicy', 'development', 'frame-src ''self''; frame-ancestors ''self''; object-src ''none'';');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.xXSSProtection', 'development', '1; mode=block');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('_browser_header.strictTransportSecurity', 'development', 'max-age=31536000; includeSubDomains');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('firstBrokerLoginFlowId', 'master', '10feac09-fca5-4030-85ae-b0cc4c6ba38a');
INSERT INTO public.realm_attribute (name, realm_id, value) VALUES ('firstBrokerLoginFlowId', 'development', '74132866-cda2-4a80-96f4-f2a6814bc4de');


--
-- Data for Name: realm_default_groups; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: realm_enabled_event_types; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: realm_events_listeners; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.realm_events_listeners (realm_id, value) VALUES ('master', 'jboss-logging');
INSERT INTO public.realm_events_listeners (realm_id, value) VALUES ('development', 'jboss-logging');


--
-- Data for Name: realm_localizations; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: realm_required_credential; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.realm_required_credential (type, form_label, input, secret, realm_id) VALUES ('password', 'password', true, true, 'master');
INSERT INTO public.realm_required_credential (type, form_label, input, secret, realm_id) VALUES ('password', 'password', true, true, 'development');


--
-- Data for Name: realm_smtp_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: realm_supported_locales; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.realm_supported_locales (realm_id, value) VALUES ('development', 'de');
INSERT INTO public.realm_supported_locales (realm_id, value) VALUES ('development', 'en');


--
-- Data for Name: redirect_uris; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.redirect_uris (client_id, value) VALUES ('c8afb027-0c8c-4bdc-bf0f-be4a2c172439', '/realms/master/account/*');
INSERT INTO public.redirect_uris (client_id, value) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '/realms/master/account/*');
INSERT INTO public.redirect_uris (client_id, value) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', '/admin/master/console/*');
INSERT INTO public.redirect_uris (client_id, value) VALUES ('15b080ba-7783-49c9-b155-86ac5e1855b1', '/realms/development/account/*');
INSERT INTO public.redirect_uris (client_id, value) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '/realms/development/account/*');
INSERT INTO public.redirect_uris (client_id, value) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '/admin/development/console/*');
INSERT INTO public.redirect_uris (client_id, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '/*');


--
-- Data for Name: required_action_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: required_action_provider; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('a4a1f2ee-cce5-48d7-bfc1-abebf61a4f47', 'VERIFY_EMAIL', 'Verify Email', 'master', true, false, 'VERIFY_EMAIL', 50);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('bef4f233-f19d-4d49-8029-d204b96be1cd', 'UPDATE_PROFILE', 'Update Profile', 'master', true, false, 'UPDATE_PROFILE', 40);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('bfc7a109-0948-45bc-b12e-43a786f380f9', 'CONFIGURE_TOTP', 'Configure OTP', 'master', true, false, 'CONFIGURE_TOTP', 10);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('d47af276-c05c-44a4-9407-3422b8aa2c01', 'UPDATE_PASSWORD', 'Update Password', 'master', true, false, 'UPDATE_PASSWORD', 30);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('56f8f8ae-8694-421f-8b65-3f164711f85e', 'update_user_locale', 'Update User Locale', 'master', true, false, 'update_user_locale', 1000);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('f00f1d52-93bd-4ec9-ad50-f8698916f203', 'delete_account', 'Delete Account', 'master', false, false, 'delete_account', 60);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('d5b863b8-252b-4e1e-8375-95afbeacb395', 'VERIFY_EMAIL', 'Verify Email', 'development', true, false, 'VERIFY_EMAIL', 50);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('b5a497d4-2375-4e2b-bb24-d12824a9ee7c', 'UPDATE_PROFILE', 'Update Profile', 'development', true, false, 'UPDATE_PROFILE', 40);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('bbde7345-627a-41c4-8e0b-8cf4987814ea', 'CONFIGURE_TOTP', 'Configure OTP', 'development', true, false, 'CONFIGURE_TOTP', 10);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('6d2b7b73-3c00-4967-bce7-c97667fbaf16', 'UPDATE_PASSWORD', 'Update Password', 'development', true, false, 'UPDATE_PASSWORD', 30);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('f95f1314-8ba8-417d-8fdd-970030a6f5a1', 'update_user_locale', 'Update User Locale', 'development', true, false, 'update_user_locale', 1000);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('8cb11261-f224-47bb-960e-1fb5b6e6cda5', 'delete_account', 'Delete Account', 'development', false, false, 'delete_account', 60);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('ce1824db-6c92-401c-98bb-053b3302b213', 'TERMS_AND_CONDITIONS', 'Terms and Conditions', 'master', false, false, 'TERMS_AND_CONDITIONS', 20);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('e2340e0a-d968-4519-9d47-6a799b50366f', 'TERMS_AND_CONDITIONS', 'Terms and Conditions', 'development', false, false, 'TERMS_AND_CONDITIONS', 20);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('e803079e-d56b-4492-af29-356af1c45abd', 'delete_credential', 'Delete Credential', 'master', true, false, 'delete_credential', 100);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('9948a0e3-4e8f-4568-8288-288104ef4eec', 'delete_credential', 'Delete Credential', 'development', true, false, 'delete_credential', 100);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('10e9e211-5e6f-40d5-9584-abcc3ed27b48', 'idp_link', 'Linking Identity Provider', 'master', true, false, 'idp_link', 120);
INSERT INTO public.required_action_provider (id, alias, name, realm_id, enabled, default_action, provider_id, priority) VALUES ('c379c0ee-fd5c-4e97-917f-2a710b7ad8a7', 'idp_link', 'Linking Identity Provider', 'development', true, false, 'idp_link', 120);


--
-- Data for Name: resource_attribute; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_policy; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_scope; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_server; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_server_perm_ticket; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_server_policy; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_server_resource; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_server_scope; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: resource_uris; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: revoked_token; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: role_attribute; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: scope_mapping; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.scope_mapping (client_id, role_id) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '412e3737-b214-48e3-9a53-e5eeff2d222e');
INSERT INTO public.scope_mapping (client_id, role_id) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '78750051-2e54-4c61-9f29-380d8fd8d7c1');
INSERT INTO public.scope_mapping (client_id, role_id) VALUES ('34c90cb1-59ee-4c3d-b280-035eb751690a', '124eaa2c-407d-457e-b011-43de0447c790');
INSERT INTO public.scope_mapping (client_id, role_id) VALUES ('2c539eab-1781-4d43-94da-033984eea7f6', '4acedae6-e297-4029-a9a0-4c87ff235666');


--
-- Data for Name: scope_policy; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: server_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_attribute; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.user_attribute (name, value, user_id, id, long_value_hash, long_value_hash_lower_case, long_value) VALUES ('locale', 'en', '25003dd9-9c30-4da5-a4cd-79c2a7dab915', '1469741b-8853-4283-8bca-9d4f96b0146c', NULL, NULL, NULL);


--
-- Data for Name: user_consent; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_consent_client_scope; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_entity; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.user_entity (id, email, email_constraint, email_verified, enabled, federation_link, first_name, last_name, realm_id, username, created_timestamp, service_account_client_link, not_before) VALUES ('6d478587-a790-46aa-ac3a-133226549795', NULL, 'ad864bc0-b66c-4aba-8f76-a6c7d22859e8', false, true, NULL, NULL, NULL, 'master', 'admin', 1646033074205, NULL, 0);
INSERT INTO public.user_entity (id, email, email_constraint, email_verified, enabled, federation_link, first_name, last_name, realm_id, username, created_timestamp, service_account_client_link, not_before) VALUES ('3dfb6717-3def-493b-a237-b7345fc42718', NULL, '40bf643c-2b40-4e6c-a419-5ac18a860376', false, true, NULL, NULL, NULL, 'development', 'test', 1646034179626, NULL, 1682506166);
INSERT INTO public.user_entity (id, email, email_constraint, email_verified, enabled, federation_link, first_name, last_name, realm_id, username, created_timestamp, service_account_client_link, not_before) VALUES ('25003dd9-9c30-4da5-a4cd-79c2a7dab915', NULL, '9223fd98-e411-446c-8109-4c534d36dbe4', true, true, NULL, NULL, NULL, 'development', 'cache-admin', 1740149602355, NULL, 0);
INSERT INTO public.user_entity (id, email, email_constraint, email_verified, enabled, federation_link, first_name, last_name, realm_id, username, created_timestamp, service_account_client_link, not_before) VALUES ('224572f6-e49b-4955-9b9c-658f385c9492', NULL, '2a5ec895-c6a9-4aea-9bd8-ff38c702bf98', false, true, NULL, NULL, NULL, 'development', 'service-account-trustdeck', 1770118508297, '2be34fd2-d092-457d-b56b-9535ff5ea02a', 0);
INSERT INTO public.user_entity (id, email, email_constraint, email_verified, enabled, federation_link, first_name, last_name, realm_id, username, created_timestamp, service_account_client_link, not_before) VALUES ('2719369c-4354-4303-aab3-a5f43a9aa4d6', 'testtest@test.com', 'testtest@test.com', true, true, NULL, 'Brandon', 'Sanderson', 'development', 'test2', 1776441404515, NULL, 0);


--
-- Data for Name: user_federation_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_federation_mapper; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_federation_mapper_config; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_federation_provider; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_group_membership; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.user_group_membership (group_id, user_id, membership_type) VALUES ('321da70e-bc39-4c6c-8cf6-579dc6e95bab', '3dfb6717-3def-493b-a237-b7345fc42718', 'UNMANAGED');


--
-- Data for Name: user_required_action; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Data for Name: user_role_mapping; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('8abade97-996e-4b25-a889-e4bc2d1fc387', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('30b61dbb-6aac-4a07-8d10-42771ade8537', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('aafba5bd-8890-4c4c-b178-a7ca86104278', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('5470f70b-c2cd-49b6-90bc-6d20fb403b66', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('34cc8105-3714-4b58-9f7c-4d0f02169b43', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('3803571c-5eb3-4714-b9c4-eb5e41203d43', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('b9190dcf-d7ee-4fd6-ac2b-1e311a48f840', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('fe5ad8b5-406e-41d7-9a2e-42385f1925a6', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('3b2a3190-037c-4017-ac90-0f06e7699345', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('16d4e279-a059-4b3d-8eca-cc886d3b00cc', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('095d2494-5f71-45b4-9e41-4fc9b3538628', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('e98d79ce-f38f-4f7e-a247-1f835e3a5f7b', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('99a9bc2e-472d-4ee9-84d1-79c77288c8bf', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('1b5f94f4-443b-451a-9e10-2d582d72c52d', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('ea6aa2ac-489d-44de-a353-0fd663eadedf', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('8a8f1618-4dcd-4a62-86b6-f83077d1ce5d', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('590d2771-11cd-49d9-afd9-3d843ea0fa7f', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('55a0caae-ea26-40b8-b739-4e4cd008ae40', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('c20d3730-b91c-49cf-b9de-b9cd7ad66eb7', '6d478587-a790-46aa-ac3a-133226549795');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('03eb0760-ea2d-490f-8608-402662997979', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('b1e0272c-5f10-45ad-9ec5-1fa8dd8f6a07', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('3e73b15e-331c-481d-b925-babec65d356b', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('8bc3c47d-126a-49b7-94d7-541ff2ff7804', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('6c7ef695-de97-4ffe-bef7-1607ed2c1b37', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('d380a439-4eb0-4f64-b365-184a87dbf1ae', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('448e437a-b999-419d-81b9-462d3b9faa47', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('4ba6f226-e44d-4601-8dfd-64c21b052b17', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('4fdcfef1-c38b-4ca0-8182-b1d664709520', '25003dd9-9c30-4da5-a4cd-79c2a7dab915');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('03eb0760-ea2d-490f-8608-402662997979', '224572f6-e49b-4955-9b9c-658f385c9492');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('d380a439-4eb0-4f64-b365-184a87dbf1ae', '224572f6-e49b-4955-9b9c-658f385c9492');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('6c7ef695-de97-4ffe-bef7-1607ed2c1b37', '224572f6-e49b-4955-9b9c-658f385c9492');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('3e73b15e-331c-481d-b925-babec65d356b', '224572f6-e49b-4955-9b9c-658f385c9492');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('8bc3c47d-126a-49b7-94d7-541ff2ff7804', '224572f6-e49b-4955-9b9c-658f385c9492');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('448e437a-b999-419d-81b9-462d3b9faa47', '224572f6-e49b-4955-9b9c-658f385c9492');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('b1e0272c-5f10-45ad-9ec5-1fa8dd8f6a07', '224572f6-e49b-4955-9b9c-658f385c9492');
INSERT INTO public.user_role_mapping (role_id, user_id) VALUES ('03eb0760-ea2d-490f-8608-402662997979', '2719369c-4354-4303-aab3-a5f43a9aa4d6');


--
-- Data for Name: web_origins; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--

INSERT INTO public.web_origins (client_id, value) VALUES ('19f8a8d4-4eee-413b-8430-1779b7cd1bec', '+');
INSERT INTO public.web_origins (client_id, value) VALUES ('723bc5a7-52cb-4063-b8f4-b45327f5db02', '+');
INSERT INTO public.web_origins (client_id, value) VALUES ('2be34fd2-d092-457d-b56b-9535ff5ea02a', '/*');


--
-- Data for Name: workflow_state; Type: TABLE DATA; Schema: public; Owner: trustdeck-manager
--



--
-- Name: org_domain ORG_DOMAIN_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.org_domain
    ADD CONSTRAINT "ORG_DOMAIN_pkey" PRIMARY KEY (id, name);


--
-- Name: org ORG_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.org
    ADD CONSTRAINT "ORG_pkey" PRIMARY KEY (id);


--
-- Name: server_config SERVER_CONFIG_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.server_config
    ADD CONSTRAINT "SERVER_CONFIG_pkey" PRIMARY KEY (server_config_key);


--
-- Name: keycloak_role UK_J3RWUVD56ONTGSUHOGM184WW2-2; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT "UK_J3RWUVD56ONTGSUHOGM184WW2-2" UNIQUE (name, client_realm_constraint);


--
-- Name: client_auth_flow_bindings c_cli_flow_bind; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_auth_flow_bindings
    ADD CONSTRAINT c_cli_flow_bind PRIMARY KEY (client_id, binding_name);


--
-- Name: client_scope_client c_cli_scope_bind; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_scope_client
    ADD CONSTRAINT c_cli_scope_bind PRIMARY KEY (client_id, scope_id);


--
-- Name: client_initial_access cnstr_client_init_acc_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_initial_access
    ADD CONSTRAINT cnstr_client_init_acc_pk PRIMARY KEY (id);


--
-- Name: realm_default_groups con_group_id_def_groups; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT con_group_id_def_groups UNIQUE (group_id);


--
-- Name: broker_link constr_broker_link_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.broker_link
    ADD CONSTRAINT constr_broker_link_pk PRIMARY KEY (identity_provider, user_id);


--
-- Name: component_config constr_component_config_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.component_config
    ADD CONSTRAINT constr_component_config_pk PRIMARY KEY (id);


--
-- Name: component constr_component_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.component
    ADD CONSTRAINT constr_component_pk PRIMARY KEY (id);


--
-- Name: fed_user_required_action constr_fed_required_action; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.fed_user_required_action
    ADD CONSTRAINT constr_fed_required_action PRIMARY KEY (required_action, user_id);


--
-- Name: fed_user_attribute constr_fed_user_attr_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.fed_user_attribute
    ADD CONSTRAINT constr_fed_user_attr_pk PRIMARY KEY (id);


--
-- Name: fed_user_consent constr_fed_user_consent_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.fed_user_consent
    ADD CONSTRAINT constr_fed_user_consent_pk PRIMARY KEY (id);


--
-- Name: fed_user_credential constr_fed_user_cred_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.fed_user_credential
    ADD CONSTRAINT constr_fed_user_cred_pk PRIMARY KEY (id);


--
-- Name: fed_user_group_membership constr_fed_user_group; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.fed_user_group_membership
    ADD CONSTRAINT constr_fed_user_group PRIMARY KEY (group_id, user_id);


--
-- Name: fed_user_role_mapping constr_fed_user_role; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.fed_user_role_mapping
    ADD CONSTRAINT constr_fed_user_role PRIMARY KEY (role_id, user_id);


--
-- Name: federated_user constr_federated_user; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.federated_user
    ADD CONSTRAINT constr_federated_user PRIMARY KEY (id);


--
-- Name: realm_default_groups constr_realm_default_groups; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT constr_realm_default_groups PRIMARY KEY (realm_id, group_id);


--
-- Name: realm_enabled_event_types constr_realm_enabl_event_types; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_enabled_event_types
    ADD CONSTRAINT constr_realm_enabl_event_types PRIMARY KEY (realm_id, value);


--
-- Name: realm_events_listeners constr_realm_events_listeners; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_events_listeners
    ADD CONSTRAINT constr_realm_events_listeners PRIMARY KEY (realm_id, value);


--
-- Name: realm_supported_locales constr_realm_supported_locales; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_supported_locales
    ADD CONSTRAINT constr_realm_supported_locales PRIMARY KEY (realm_id, value);


--
-- Name: identity_provider constraint_2b; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT constraint_2b PRIMARY KEY (internal_id);


--
-- Name: client_attributes constraint_3c; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_attributes
    ADD CONSTRAINT constraint_3c PRIMARY KEY (client_id, name);


--
-- Name: event_entity constraint_4; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.event_entity
    ADD CONSTRAINT constraint_4 PRIMARY KEY (id);


--
-- Name: federated_identity constraint_40; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.federated_identity
    ADD CONSTRAINT constraint_40 PRIMARY KEY (identity_provider, user_id);


--
-- Name: realm constraint_4a; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT constraint_4a PRIMARY KEY (id);


--
-- Name: user_federation_provider constraint_5c; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_provider
    ADD CONSTRAINT constraint_5c PRIMARY KEY (id);


--
-- Name: client constraint_7; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT constraint_7 PRIMARY KEY (id);


--
-- Name: scope_mapping constraint_81; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.scope_mapping
    ADD CONSTRAINT constraint_81 PRIMARY KEY (client_id, role_id);


--
-- Name: client_node_registrations constraint_84; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_node_registrations
    ADD CONSTRAINT constraint_84 PRIMARY KEY (client_id, name);


--
-- Name: realm_attribute constraint_9; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_attribute
    ADD CONSTRAINT constraint_9 PRIMARY KEY (name, realm_id);


--
-- Name: realm_required_credential constraint_92; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_required_credential
    ADD CONSTRAINT constraint_92 PRIMARY KEY (realm_id, type);


--
-- Name: keycloak_role constraint_a; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT constraint_a PRIMARY KEY (id);


--
-- Name: admin_event_entity constraint_admin_event_entity; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.admin_event_entity
    ADD CONSTRAINT constraint_admin_event_entity PRIMARY KEY (id);


--
-- Name: authenticator_config_entry constraint_auth_cfg_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authenticator_config_entry
    ADD CONSTRAINT constraint_auth_cfg_pk PRIMARY KEY (authenticator_id, name);


--
-- Name: authentication_execution constraint_auth_exec_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT constraint_auth_exec_pk PRIMARY KEY (id);


--
-- Name: authentication_flow constraint_auth_flow_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authentication_flow
    ADD CONSTRAINT constraint_auth_flow_pk PRIMARY KEY (id);


--
-- Name: authenticator_config constraint_auth_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authenticator_config
    ADD CONSTRAINT constraint_auth_pk PRIMARY KEY (id);


--
-- Name: user_role_mapping constraint_c; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_role_mapping
    ADD CONSTRAINT constraint_c PRIMARY KEY (role_id, user_id);


--
-- Name: composite_role constraint_composite_role; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT constraint_composite_role PRIMARY KEY (composite, child_role);


--
-- Name: identity_provider_config constraint_d; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.identity_provider_config
    ADD CONSTRAINT constraint_d PRIMARY KEY (identity_provider_id, name);


--
-- Name: policy_config constraint_dpc; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.policy_config
    ADD CONSTRAINT constraint_dpc PRIMARY KEY (policy_id, name);


--
-- Name: realm_smtp_config constraint_e; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_smtp_config
    ADD CONSTRAINT constraint_e PRIMARY KEY (realm_id, name);


--
-- Name: credential constraint_f; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT constraint_f PRIMARY KEY (id);


--
-- Name: user_federation_config constraint_f9; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_config
    ADD CONSTRAINT constraint_f9 PRIMARY KEY (user_federation_provider_id, name);


--
-- Name: resource_server_perm_ticket constraint_fapmt; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT constraint_fapmt PRIMARY KEY (id);


--
-- Name: resource_server_resource constraint_farsr; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT constraint_farsr PRIMARY KEY (id);


--
-- Name: resource_server_policy constraint_farsrp; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT constraint_farsrp PRIMARY KEY (id);


--
-- Name: associated_policy constraint_farsrpap; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT constraint_farsrpap PRIMARY KEY (policy_id, associated_policy_id);


--
-- Name: resource_policy constraint_farsrpp; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT constraint_farsrpp PRIMARY KEY (resource_id, policy_id);


--
-- Name: resource_server_scope constraint_farsrs; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT constraint_farsrs PRIMARY KEY (id);


--
-- Name: resource_scope constraint_farsrsp; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT constraint_farsrsp PRIMARY KEY (resource_id, scope_id);


--
-- Name: scope_policy constraint_farsrsps; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT constraint_farsrsps PRIMARY KEY (scope_id, policy_id);


--
-- Name: user_entity constraint_fb; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT constraint_fb PRIMARY KEY (id);


--
-- Name: user_federation_mapper_config constraint_fedmapper_cfg_pm; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_mapper_config
    ADD CONSTRAINT constraint_fedmapper_cfg_pm PRIMARY KEY (user_federation_mapper_id, name);


--
-- Name: user_federation_mapper constraint_fedmapperpm; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT constraint_fedmapperpm PRIMARY KEY (id);


--
-- Name: fed_user_consent_cl_scope constraint_fgrntcsnt_clsc_pm; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.fed_user_consent_cl_scope
    ADD CONSTRAINT constraint_fgrntcsnt_clsc_pm PRIMARY KEY (user_consent_id, scope_id);


--
-- Name: user_consent_client_scope constraint_grntcsnt_clsc_pm; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_consent_client_scope
    ADD CONSTRAINT constraint_grntcsnt_clsc_pm PRIMARY KEY (user_consent_id, scope_id);


--
-- Name: user_consent constraint_grntcsnt_pm; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT constraint_grntcsnt_pm PRIMARY KEY (id);


--
-- Name: keycloak_group constraint_group; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.keycloak_group
    ADD CONSTRAINT constraint_group PRIMARY KEY (id);


--
-- Name: group_attribute constraint_group_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.group_attribute
    ADD CONSTRAINT constraint_group_attribute_pk PRIMARY KEY (id);


--
-- Name: group_role_mapping constraint_group_role; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.group_role_mapping
    ADD CONSTRAINT constraint_group_role PRIMARY KEY (role_id, group_id);


--
-- Name: identity_provider_mapper constraint_idpm; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.identity_provider_mapper
    ADD CONSTRAINT constraint_idpm PRIMARY KEY (id);


--
-- Name: idp_mapper_config constraint_idpmconfig; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.idp_mapper_config
    ADD CONSTRAINT constraint_idpmconfig PRIMARY KEY (idp_mapper_id, name);


--
-- Name: jgroups_ping constraint_jgroups_ping; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.jgroups_ping
    ADD CONSTRAINT constraint_jgroups_ping PRIMARY KEY (address);


--
-- Name: migration_model constraint_migmod; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.migration_model
    ADD CONSTRAINT constraint_migmod PRIMARY KEY (id);


--
-- Name: offline_client_session constraint_offl_cl_ses_pk3; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.offline_client_session
    ADD CONSTRAINT constraint_offl_cl_ses_pk3 PRIMARY KEY (user_session_id, client_id, client_storage_provider, external_client_id, offline_flag);


--
-- Name: offline_user_session constraint_offl_us_ses_pk2; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.offline_user_session
    ADD CONSTRAINT constraint_offl_us_ses_pk2 PRIMARY KEY (user_session_id, offline_flag);


--
-- Name: protocol_mapper constraint_pcm; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT constraint_pcm PRIMARY KEY (id);


--
-- Name: protocol_mapper_config constraint_pmconfig; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.protocol_mapper_config
    ADD CONSTRAINT constraint_pmconfig PRIMARY KEY (protocol_mapper_id, name);


--
-- Name: redirect_uris constraint_redirect_uris; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.redirect_uris
    ADD CONSTRAINT constraint_redirect_uris PRIMARY KEY (client_id, value);


--
-- Name: required_action_config constraint_req_act_cfg_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.required_action_config
    ADD CONSTRAINT constraint_req_act_cfg_pk PRIMARY KEY (required_action_id, name);


--
-- Name: required_action_provider constraint_req_act_prv_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.required_action_provider
    ADD CONSTRAINT constraint_req_act_prv_pk PRIMARY KEY (id);


--
-- Name: user_required_action constraint_required_action; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_required_action
    ADD CONSTRAINT constraint_required_action PRIMARY KEY (required_action, user_id);


--
-- Name: resource_uris constraint_resour_uris_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_uris
    ADD CONSTRAINT constraint_resour_uris_pk PRIMARY KEY (resource_id, value);


--
-- Name: role_attribute constraint_role_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.role_attribute
    ADD CONSTRAINT constraint_role_attribute_pk PRIMARY KEY (id);


--
-- Name: revoked_token constraint_rt; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.revoked_token
    ADD CONSTRAINT constraint_rt PRIMARY KEY (id);


--
-- Name: user_attribute constraint_user_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT constraint_user_attribute_pk PRIMARY KEY (id);


--
-- Name: user_group_membership constraint_user_group; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_group_membership
    ADD CONSTRAINT constraint_user_group PRIMARY KEY (group_id, user_id);


--
-- Name: web_origins constraint_web_origins; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.web_origins
    ADD CONSTRAINT constraint_web_origins PRIMARY KEY (client_id, value);


--
-- Name: client_scope_attributes pk_cl_tmpl_attr; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_scope_attributes
    ADD CONSTRAINT pk_cl_tmpl_attr PRIMARY KEY (scope_id, name);


--
-- Name: client_scope pk_cli_template; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_scope
    ADD CONSTRAINT pk_cli_template PRIMARY KEY (id);


--
-- Name: databasechangeloglock pk_databasechangeloglock; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT pk_databasechangeloglock PRIMARY KEY (id);


--
-- Name: resource_server pk_resource_server; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server
    ADD CONSTRAINT pk_resource_server PRIMARY KEY (id);


--
-- Name: client_scope_role_mapping pk_template_scope; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_scope_role_mapping
    ADD CONSTRAINT pk_template_scope PRIMARY KEY (scope_id, role_id);


--
-- Name: workflow_state pk_workflow_state; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.workflow_state
    ADD CONSTRAINT pk_workflow_state PRIMARY KEY (execution_id);


--
-- Name: default_client_scope r_def_cli_scope_bind; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.default_client_scope
    ADD CONSTRAINT r_def_cli_scope_bind PRIMARY KEY (realm_id, scope_id);


--
-- Name: realm_localizations realm_localizations_pkey; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_localizations
    ADD CONSTRAINT realm_localizations_pkey PRIMARY KEY (realm_id, locale);


--
-- Name: resource_attribute res_attr_pk; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_attribute
    ADD CONSTRAINT res_attr_pk PRIMARY KEY (id);


--
-- Name: keycloak_group sibling_names; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.keycloak_group
    ADD CONSTRAINT sibling_names UNIQUE (realm_id, parent_group, name);


--
-- Name: identity_provider uk_2daelwnibji49avxsrtuf6xj33; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT uk_2daelwnibji49avxsrtuf6xj33 UNIQUE (provider_alias, realm_id);


--
-- Name: client uk_b71cjlbenv945rb6gcon438at; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT uk_b71cjlbenv945rb6gcon438at UNIQUE (realm_id, client_id);


--
-- Name: client_scope uk_cli_scope; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_scope
    ADD CONSTRAINT uk_cli_scope UNIQUE (realm_id, name);


--
-- Name: user_entity uk_dykn684sl8up1crfei6eckhd7; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT uk_dykn684sl8up1crfei6eckhd7 UNIQUE (realm_id, email_constraint);


--
-- Name: user_consent uk_external_consent; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT uk_external_consent UNIQUE (client_storage_provider, external_client_id, user_id);


--
-- Name: resource_server_resource uk_frsr6t700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT uk_frsr6t700s9v50bu18ws5ha6 UNIQUE (name, owner, resource_server_id);


--
-- Name: resource_server_perm_ticket uk_frsr6t700s9v50bu18ws5pmt; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT uk_frsr6t700s9v50bu18ws5pmt UNIQUE (owner, requester, resource_server_id, resource_id, scope_id);


--
-- Name: resource_server_policy uk_frsrpt700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT uk_frsrpt700s9v50bu18ws5ha6 UNIQUE (name, resource_server_id);


--
-- Name: resource_server_scope uk_frsrst700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT uk_frsrst700s9v50bu18ws5ha6 UNIQUE (name, resource_server_id);


--
-- Name: user_consent uk_local_consent; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT uk_local_consent UNIQUE (client_id, user_id);


--
-- Name: migration_model uk_migration_update_time; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.migration_model
    ADD CONSTRAINT uk_migration_update_time UNIQUE (update_time);


--
-- Name: migration_model uk_migration_version; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.migration_model
    ADD CONSTRAINT uk_migration_version UNIQUE (version);


--
-- Name: org uk_org_alias; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.org
    ADD CONSTRAINT uk_org_alias UNIQUE (realm_id, alias);


--
-- Name: org uk_org_group; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.org
    ADD CONSTRAINT uk_org_group UNIQUE (group_id);


--
-- Name: org uk_org_name; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.org
    ADD CONSTRAINT uk_org_name UNIQUE (realm_id, name);


--
-- Name: realm uk_orvsdmla56612eaefiq6wl5oi; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT uk_orvsdmla56612eaefiq6wl5oi UNIQUE (name);


--
-- Name: user_entity uk_ru8tt6t700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT uk_ru8tt6t700s9v50bu18ws5ha6 UNIQUE (realm_id, username);


--
-- Name: workflow_state uq_workflow_resource; Type: CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.workflow_state
    ADD CONSTRAINT uq_workflow_resource UNIQUE (workflow_id, resource_id);


--
-- Name: fed_user_attr_long_values; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX fed_user_attr_long_values ON public.fed_user_attribute USING btree (long_value_hash, name);


--
-- Name: fed_user_attr_long_values_lower_case; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX fed_user_attr_long_values_lower_case ON public.fed_user_attribute USING btree (long_value_hash_lower_case, name);


--
-- Name: idx_admin_event_time; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_admin_event_time ON public.admin_event_entity USING btree (realm_id, admin_event_time);


--
-- Name: idx_assoc_pol_assoc_pol_id; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_assoc_pol_assoc_pol_id ON public.associated_policy USING btree (associated_policy_id);


--
-- Name: idx_auth_config_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_auth_config_realm ON public.authenticator_config USING btree (realm_id);


--
-- Name: idx_auth_exec_flow; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_auth_exec_flow ON public.authentication_execution USING btree (flow_id);


--
-- Name: idx_auth_exec_realm_flow; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_auth_exec_realm_flow ON public.authentication_execution USING btree (realm_id, flow_id);


--
-- Name: idx_auth_flow_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_auth_flow_realm ON public.authentication_flow USING btree (realm_id);


--
-- Name: idx_cl_clscope; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_cl_clscope ON public.client_scope_client USING btree (scope_id);


--
-- Name: idx_client_att_by_name_value; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_client_att_by_name_value ON public.client_attributes USING btree (name, substr(value, 1, 255));


--
-- Name: idx_client_id; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_client_id ON public.client USING btree (client_id);


--
-- Name: idx_client_init_acc_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_client_init_acc_realm ON public.client_initial_access USING btree (realm_id);


--
-- Name: idx_clscope_attrs; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_clscope_attrs ON public.client_scope_attributes USING btree (scope_id);


--
-- Name: idx_clscope_cl; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_clscope_cl ON public.client_scope_client USING btree (client_id);


--
-- Name: idx_clscope_protmap; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_clscope_protmap ON public.protocol_mapper USING btree (client_scope_id);


--
-- Name: idx_clscope_role; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_clscope_role ON public.client_scope_role_mapping USING btree (scope_id);


--
-- Name: idx_compo_config_compo; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_compo_config_compo ON public.component_config USING btree (component_id);


--
-- Name: idx_component_provider_type; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_component_provider_type ON public.component USING btree (provider_type);


--
-- Name: idx_component_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_component_realm ON public.component USING btree (realm_id);


--
-- Name: idx_composite; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_composite ON public.composite_role USING btree (composite);


--
-- Name: idx_composite_child; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_composite_child ON public.composite_role USING btree (child_role);


--
-- Name: idx_defcls_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_defcls_realm ON public.default_client_scope USING btree (realm_id);


--
-- Name: idx_defcls_scope; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_defcls_scope ON public.default_client_scope USING btree (scope_id);


--
-- Name: idx_event_entity_user_id_type; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_event_entity_user_id_type ON public.event_entity USING btree (user_id, type, event_time);


--
-- Name: idx_event_time; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_event_time ON public.event_entity USING btree (realm_id, event_time);


--
-- Name: idx_fedidentity_feduser; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fedidentity_feduser ON public.federated_identity USING btree (federated_user_id);


--
-- Name: idx_fedidentity_user; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fedidentity_user ON public.federated_identity USING btree (user_id);


--
-- Name: idx_fu_attribute; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_attribute ON public.fed_user_attribute USING btree (user_id, realm_id, name);


--
-- Name: idx_fu_cnsnt_ext; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_cnsnt_ext ON public.fed_user_consent USING btree (user_id, client_storage_provider, external_client_id);


--
-- Name: idx_fu_consent; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_consent ON public.fed_user_consent USING btree (user_id, client_id);


--
-- Name: idx_fu_consent_ru; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_consent_ru ON public.fed_user_consent USING btree (realm_id, user_id);


--
-- Name: idx_fu_credential; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_credential ON public.fed_user_credential USING btree (user_id, type);


--
-- Name: idx_fu_credential_ru; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_credential_ru ON public.fed_user_credential USING btree (realm_id, user_id);


--
-- Name: idx_fu_group_membership; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_group_membership ON public.fed_user_group_membership USING btree (user_id, group_id);


--
-- Name: idx_fu_group_membership_ru; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_group_membership_ru ON public.fed_user_group_membership USING btree (realm_id, user_id);


--
-- Name: idx_fu_required_action; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_required_action ON public.fed_user_required_action USING btree (user_id, required_action);


--
-- Name: idx_fu_required_action_ru; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_required_action_ru ON public.fed_user_required_action USING btree (realm_id, user_id);


--
-- Name: idx_fu_role_mapping; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_role_mapping ON public.fed_user_role_mapping USING btree (user_id, role_id);


--
-- Name: idx_fu_role_mapping_ru; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_fu_role_mapping_ru ON public.fed_user_role_mapping USING btree (realm_id, user_id);


--
-- Name: idx_group_att_by_name_value; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_group_att_by_name_value ON public.group_attribute USING btree (name, ((value)::character varying(250)));


--
-- Name: idx_group_attr_group; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_group_attr_group ON public.group_attribute USING btree (group_id);


--
-- Name: idx_group_role_mapp_group; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_group_role_mapp_group ON public.group_role_mapping USING btree (group_id);


--
-- Name: idx_id_prov_mapp_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_id_prov_mapp_realm ON public.identity_provider_mapper USING btree (realm_id);


--
-- Name: idx_ident_prov_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_ident_prov_realm ON public.identity_provider USING btree (realm_id);


--
-- Name: idx_idp_for_login; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_idp_for_login ON public.identity_provider USING btree (realm_id, enabled, link_only, hide_on_login, organization_id);


--
-- Name: idx_idp_realm_org; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_idp_realm_org ON public.identity_provider USING btree (realm_id, organization_id);


--
-- Name: idx_keycloak_role_client; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_keycloak_role_client ON public.keycloak_role USING btree (client);


--
-- Name: idx_keycloak_role_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_keycloak_role_realm ON public.keycloak_role USING btree (realm);


--
-- Name: idx_offline_css_by_client; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_offline_css_by_client ON public.offline_client_session USING btree (client_id, offline_flag) WHERE ((client_id)::text <> 'external'::text);


--
-- Name: idx_offline_css_by_client_storage_provider; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_offline_css_by_client_storage_provider ON public.offline_client_session USING btree (client_storage_provider, external_client_id, offline_flag) WHERE ((client_storage_provider)::text <> 'internal'::text);


--
-- Name: idx_offline_uss_by_broker_session_id; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_offline_uss_by_broker_session_id ON public.offline_user_session USING btree (broker_session_id, realm_id);


--
-- Name: idx_offline_uss_by_last_session_refresh; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_offline_uss_by_last_session_refresh ON public.offline_user_session USING btree (realm_id, offline_flag, last_session_refresh);


--
-- Name: idx_offline_uss_by_user; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_offline_uss_by_user ON public.offline_user_session USING btree (user_id, realm_id, offline_flag);


--
-- Name: idx_org_domain_org_id; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_org_domain_org_id ON public.org_domain USING btree (org_id);


--
-- Name: idx_perm_ticket_owner; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_perm_ticket_owner ON public.resource_server_perm_ticket USING btree (owner);


--
-- Name: idx_perm_ticket_requester; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_perm_ticket_requester ON public.resource_server_perm_ticket USING btree (requester);


--
-- Name: idx_protocol_mapper_client; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_protocol_mapper_client ON public.protocol_mapper USING btree (client_id);


--
-- Name: idx_realm_attr_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_realm_attr_realm ON public.realm_attribute USING btree (realm_id);


--
-- Name: idx_realm_clscope; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_realm_clscope ON public.client_scope USING btree (realm_id);


--
-- Name: idx_realm_def_grp_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_realm_def_grp_realm ON public.realm_default_groups USING btree (realm_id);


--
-- Name: idx_realm_evt_list_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_realm_evt_list_realm ON public.realm_events_listeners USING btree (realm_id);


--
-- Name: idx_realm_evt_types_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_realm_evt_types_realm ON public.realm_enabled_event_types USING btree (realm_id);


--
-- Name: idx_realm_master_adm_cli; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_realm_master_adm_cli ON public.realm USING btree (master_admin_client);


--
-- Name: idx_realm_supp_local_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_realm_supp_local_realm ON public.realm_supported_locales USING btree (realm_id);


--
-- Name: idx_redir_uri_client; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_redir_uri_client ON public.redirect_uris USING btree (client_id);


--
-- Name: idx_req_act_prov_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_req_act_prov_realm ON public.required_action_provider USING btree (realm_id);


--
-- Name: idx_res_policy_policy; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_res_policy_policy ON public.resource_policy USING btree (policy_id);


--
-- Name: idx_res_scope_scope; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_res_scope_scope ON public.resource_scope USING btree (scope_id);


--
-- Name: idx_res_serv_pol_res_serv; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_res_serv_pol_res_serv ON public.resource_server_policy USING btree (resource_server_id);


--
-- Name: idx_res_srv_res_res_srv; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_res_srv_res_res_srv ON public.resource_server_resource USING btree (resource_server_id);


--
-- Name: idx_res_srv_scope_res_srv; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_res_srv_scope_res_srv ON public.resource_server_scope USING btree (resource_server_id);


--
-- Name: idx_rev_token_on_expire; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_rev_token_on_expire ON public.revoked_token USING btree (expire);


--
-- Name: idx_role_attribute; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_role_attribute ON public.role_attribute USING btree (role_id);


--
-- Name: idx_role_clscope; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_role_clscope ON public.client_scope_role_mapping USING btree (role_id);


--
-- Name: idx_scope_mapping_role; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_scope_mapping_role ON public.scope_mapping USING btree (role_id);


--
-- Name: idx_scope_policy_policy; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_scope_policy_policy ON public.scope_policy USING btree (policy_id);


--
-- Name: idx_update_time; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_update_time ON public.migration_model USING btree (update_time);


--
-- Name: idx_usconsent_clscope; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_usconsent_clscope ON public.user_consent_client_scope USING btree (user_consent_id);


--
-- Name: idx_usconsent_scope_id; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_usconsent_scope_id ON public.user_consent_client_scope USING btree (scope_id);


--
-- Name: idx_user_attribute; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_attribute ON public.user_attribute USING btree (user_id);


--
-- Name: idx_user_attribute_name; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_attribute_name ON public.user_attribute USING btree (name, value);


--
-- Name: idx_user_consent; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_consent ON public.user_consent USING btree (user_id);


--
-- Name: idx_user_credential; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_credential ON public.credential USING btree (user_id);


--
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_email ON public.user_entity USING btree (email);


--
-- Name: idx_user_group_mapping; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_group_mapping ON public.user_group_membership USING btree (user_id);


--
-- Name: idx_user_reqactions; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_reqactions ON public.user_required_action USING btree (user_id);


--
-- Name: idx_user_role_mapping; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_role_mapping ON public.user_role_mapping USING btree (user_id);


--
-- Name: idx_user_service_account; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_user_service_account ON public.user_entity USING btree (realm_id, service_account_client_link);


--
-- Name: idx_usr_fed_map_fed_prv; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_usr_fed_map_fed_prv ON public.user_federation_mapper USING btree (federation_provider_id);


--
-- Name: idx_usr_fed_map_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_usr_fed_map_realm ON public.user_federation_mapper USING btree (realm_id);


--
-- Name: idx_usr_fed_prv_realm; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_usr_fed_prv_realm ON public.user_federation_provider USING btree (realm_id);


--
-- Name: idx_web_orig_client; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_web_orig_client ON public.web_origins USING btree (client_id);


--
-- Name: idx_workflow_state_provider; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_workflow_state_provider ON public.workflow_state USING btree (resource_id, workflow_provider_id);


--
-- Name: idx_workflow_state_step; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX idx_workflow_state_step ON public.workflow_state USING btree (workflow_id, scheduled_step_id);


--
-- Name: user_attr_long_values; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX user_attr_long_values ON public.user_attribute USING btree (long_value_hash, name);


--
-- Name: user_attr_long_values_lower_case; Type: INDEX; Schema: public; Owner: trustdeck-manager
--

CREATE INDEX user_attr_long_values_lower_case ON public.user_attribute USING btree (long_value_hash_lower_case, name);


--
-- Name: identity_provider fk2b4ebc52ae5c3b34; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT fk2b4ebc52ae5c3b34 FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: client_attributes fk3c47c64beacca966; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_attributes
    ADD CONSTRAINT fk3c47c64beacca966 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: federated_identity fk404288b92ef007a6; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.federated_identity
    ADD CONSTRAINT fk404288b92ef007a6 FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- Name: client_node_registrations fk4129723ba992f594; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_node_registrations
    ADD CONSTRAINT fk4129723ba992f594 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: redirect_uris fk_1burs8pb4ouj97h5wuppahv9f; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.redirect_uris
    ADD CONSTRAINT fk_1burs8pb4ouj97h5wuppahv9f FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: user_federation_provider fk_1fj32f6ptolw2qy60cd8n01e8; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_provider
    ADD CONSTRAINT fk_1fj32f6ptolw2qy60cd8n01e8 FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: realm_required_credential fk_5hg65lybevavkqfki3kponh9v; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_required_credential
    ADD CONSTRAINT fk_5hg65lybevavkqfki3kponh9v FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: resource_attribute fk_5hrm2vlf9ql5fu022kqepovbr; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_attribute
    ADD CONSTRAINT fk_5hrm2vlf9ql5fu022kqepovbr FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- Name: user_attribute fk_5hrm2vlf9ql5fu043kqepovbr; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT fk_5hrm2vlf9ql5fu043kqepovbr FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- Name: user_required_action fk_6qj3w1jw9cvafhe19bwsiuvmd; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_required_action
    ADD CONSTRAINT fk_6qj3w1jw9cvafhe19bwsiuvmd FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- Name: keycloak_role fk_6vyqfe4cn4wlq8r6kt5vdsj5c; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT fk_6vyqfe4cn4wlq8r6kt5vdsj5c FOREIGN KEY (realm) REFERENCES public.realm(id);


--
-- Name: realm_smtp_config fk_70ej8xdxgxd0b9hh6180irr0o; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_smtp_config
    ADD CONSTRAINT fk_70ej8xdxgxd0b9hh6180irr0o FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: realm_attribute fk_8shxd6l3e9atqukacxgpffptw; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_attribute
    ADD CONSTRAINT fk_8shxd6l3e9atqukacxgpffptw FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: composite_role fk_a63wvekftu8jo1pnj81e7mce2; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT fk_a63wvekftu8jo1pnj81e7mce2 FOREIGN KEY (composite) REFERENCES public.keycloak_role(id);


--
-- Name: authentication_execution fk_auth_exec_flow; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT fk_auth_exec_flow FOREIGN KEY (flow_id) REFERENCES public.authentication_flow(id);


--
-- Name: authentication_execution fk_auth_exec_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT fk_auth_exec_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: authentication_flow fk_auth_flow_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authentication_flow
    ADD CONSTRAINT fk_auth_flow_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: authenticator_config fk_auth_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.authenticator_config
    ADD CONSTRAINT fk_auth_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: user_role_mapping fk_c4fqv34p1mbylloxang7b1q3l; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_role_mapping
    ADD CONSTRAINT fk_c4fqv34p1mbylloxang7b1q3l FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- Name: client_scope_attributes fk_cl_scope_attr_scope; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_scope_attributes
    ADD CONSTRAINT fk_cl_scope_attr_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- Name: client_scope_role_mapping fk_cl_scope_rm_scope; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_scope_role_mapping
    ADD CONSTRAINT fk_cl_scope_rm_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- Name: protocol_mapper fk_cli_scope_mapper; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT fk_cli_scope_mapper FOREIGN KEY (client_scope_id) REFERENCES public.client_scope(id);


--
-- Name: client_initial_access fk_client_init_acc_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.client_initial_access
    ADD CONSTRAINT fk_client_init_acc_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: component_config fk_component_config; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.component_config
    ADD CONSTRAINT fk_component_config FOREIGN KEY (component_id) REFERENCES public.component(id);


--
-- Name: component fk_component_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.component
    ADD CONSTRAINT fk_component_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: realm_default_groups fk_def_groups_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT fk_def_groups_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: user_federation_mapper_config fk_fedmapper_cfg; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_mapper_config
    ADD CONSTRAINT fk_fedmapper_cfg FOREIGN KEY (user_federation_mapper_id) REFERENCES public.user_federation_mapper(id);


--
-- Name: user_federation_mapper fk_fedmapperpm_fedprv; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT fk_fedmapperpm_fedprv FOREIGN KEY (federation_provider_id) REFERENCES public.user_federation_provider(id);


--
-- Name: user_federation_mapper fk_fedmapperpm_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT fk_fedmapperpm_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: associated_policy fk_frsr5s213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT fk_frsr5s213xcx4wnkog82ssrfy FOREIGN KEY (associated_policy_id) REFERENCES public.resource_server_policy(id);


--
-- Name: scope_policy fk_frsrasp13xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT fk_frsrasp13xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog82sspmt; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog82sspmt FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- Name: resource_server_resource fk_frsrho213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT fk_frsrho213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog83sspmt; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog83sspmt FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog84sspmt; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog84sspmt FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- Name: associated_policy fk_frsrpas14xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT fk_frsrpas14xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- Name: scope_policy fk_frsrpass3xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT fk_frsrpass3xcx4wnkog82ssrfy FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- Name: resource_server_perm_ticket fk_frsrpo2128cx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrpo2128cx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- Name: resource_server_policy fk_frsrpo213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT fk_frsrpo213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- Name: resource_scope fk_frsrpos13xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT fk_frsrpos13xcx4wnkog82ssrfy FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- Name: resource_policy fk_frsrpos53xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT fk_frsrpos53xcx4wnkog82ssrfy FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- Name: resource_policy fk_frsrpp213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT fk_frsrpp213xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- Name: resource_scope fk_frsrps213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT fk_frsrps213xcx4wnkog82ssrfy FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- Name: resource_server_scope fk_frsrso213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT fk_frsrso213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- Name: composite_role fk_gr7thllb9lu8q4vqa4524jjy8; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT fk_gr7thllb9lu8q4vqa4524jjy8 FOREIGN KEY (child_role) REFERENCES public.keycloak_role(id);


--
-- Name: user_consent_client_scope fk_grntcsnt_clsc_usc; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_consent_client_scope
    ADD CONSTRAINT fk_grntcsnt_clsc_usc FOREIGN KEY (user_consent_id) REFERENCES public.user_consent(id);


--
-- Name: user_consent fk_grntcsnt_user; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT fk_grntcsnt_user FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- Name: group_attribute fk_group_attribute_group; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.group_attribute
    ADD CONSTRAINT fk_group_attribute_group FOREIGN KEY (group_id) REFERENCES public.keycloak_group(id);


--
-- Name: group_role_mapping fk_group_role_group; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.group_role_mapping
    ADD CONSTRAINT fk_group_role_group FOREIGN KEY (group_id) REFERENCES public.keycloak_group(id);


--
-- Name: realm_enabled_event_types fk_h846o4h0w8epx5nwedrf5y69j; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_enabled_event_types
    ADD CONSTRAINT fk_h846o4h0w8epx5nwedrf5y69j FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: realm_events_listeners fk_h846o4h0w8epx5nxev9f5y69j; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_events_listeners
    ADD CONSTRAINT fk_h846o4h0w8epx5nxev9f5y69j FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: identity_provider_mapper fk_idpm_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.identity_provider_mapper
    ADD CONSTRAINT fk_idpm_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: idp_mapper_config fk_idpmconfig; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.idp_mapper_config
    ADD CONSTRAINT fk_idpmconfig FOREIGN KEY (idp_mapper_id) REFERENCES public.identity_provider_mapper(id);


--
-- Name: web_origins fk_lojpho213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.web_origins
    ADD CONSTRAINT fk_lojpho213xcx4wnkog82ssrfy FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: scope_mapping fk_ouse064plmlr732lxjcn1q5f1; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.scope_mapping
    ADD CONSTRAINT fk_ouse064plmlr732lxjcn1q5f1 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: protocol_mapper fk_pcm_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT fk_pcm_realm FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- Name: credential fk_pfyr0glasqyl0dei3kl69r6v0; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT fk_pfyr0glasqyl0dei3kl69r6v0 FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- Name: protocol_mapper_config fk_pmconfig; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.protocol_mapper_config
    ADD CONSTRAINT fk_pmconfig FOREIGN KEY (protocol_mapper_id) REFERENCES public.protocol_mapper(id);


--
-- Name: default_client_scope fk_r_def_cli_scope_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.default_client_scope
    ADD CONSTRAINT fk_r_def_cli_scope_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: required_action_provider fk_req_act_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.required_action_provider
    ADD CONSTRAINT fk_req_act_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: resource_uris fk_resource_server_uris; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.resource_uris
    ADD CONSTRAINT fk_resource_server_uris FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- Name: role_attribute fk_role_attribute_id; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.role_attribute
    ADD CONSTRAINT fk_role_attribute_id FOREIGN KEY (role_id) REFERENCES public.keycloak_role(id);


--
-- Name: realm_supported_locales fk_supported_locales_realm; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.realm_supported_locales
    ADD CONSTRAINT fk_supported_locales_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- Name: user_federation_config fk_t13hpu1j94r2ebpekr39x5eu5; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_federation_config
    ADD CONSTRAINT fk_t13hpu1j94r2ebpekr39x5eu5 FOREIGN KEY (user_federation_provider_id) REFERENCES public.user_federation_provider(id);


--
-- Name: user_group_membership fk_user_group_user; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.user_group_membership
    ADD CONSTRAINT fk_user_group_user FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- Name: policy_config fkdc34197cf864c4e43; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.policy_config
    ADD CONSTRAINT fkdc34197cf864c4e43 FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- Name: identity_provider_config fkdc4897cf864c4e43; Type: FK CONSTRAINT; Schema: public; Owner: trustdeck-manager
--

ALTER TABLE ONLY public.identity_provider_config
    ADD CONSTRAINT fkdc4897cf864c4e43 FOREIGN KEY (identity_provider_id) REFERENCES public.identity_provider(internal_id);


--
-- PostgreSQL database dump complete
--


#!/usr/bin/env bash
set -Eeuo pipefail

usage() {
  cat <<'USAGE'
Usage:
  addInitialAdminUser.sh --username <username> [--db-mode local|docker]
                         [--env-file <path/to/trustdeck.env>]
                         [--application-yml <path/to/application.yml>]
                         [--docker-container <postgres_container_name>]
                         [--keycloak-container <keycloak_container_name>]
                         [--db-host <host>] [--db-port <port>] [--db-name <db>]
                         [--db-user <user>] [--db-password <password>]
                         [--keycloak-url <url>] [--realm <realm>]
                         [--client-id <client_id>] [--client-secret <secret>]
                         [--created-by <marker>] [--dry-run]

Description:
  Resolves a Keycloak user by username, reads the ACE/KING/global permission
  lists from application.yml, and inserts all currently defined permissions
  for that user into the TrustDeck PostgreSQL database.

The script is independent of the current working directory. It locates the
repository root by walking upwards from the script file itself.

Defaults:
  --db-mode docker
  --docker-container trustdeck-postgresql
  --keycloak-container trustdeck-keycloak
  --db-name trustdeck
  --created-by bootstrap-script

Examples:
  ./src/main/resources/scripts/addInitialAdminUser.sh --username alice
  ./src/main/resources/scripts/addInitialAdminUser.sh --username alice --db-mode local
  ./src/main/resources/scripts/addInitialAdminUser.sh --username alice --dry-run
USAGE
}

err() {
  printf '[ERROR] %s\n' "$*" >&2
}

info() {
  printf '[INFO] %s\n' "$*" >&2
}

warn() {
  printf '[WARN] %s\n' "$*" >&2
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    err "Missing required command: $1"
    exit 1
  }
}

sq() {
  printf '%s' "$1" | sed "s/'/''/g"
}

make_absolute_path() {
  local path="$1"

  if [[ "$path" == /* ]]; then
    printf '%s' "$path"
  else
    printf '%s/%s' "$PWD" "$path"
  fi
}

SCRIPT_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

find_repo_root() {
  local current="$SCRIPT_DIR"

  while [[ "$current" != "/" ]]; do
    if [[ -f "$current/pom.xml" &&
          -f "$current/src/main/resources/application.yml" ]]; then
      printf '%s' "$current"
      return 0
    fi

    current="$(dirname -- "$current")"
  done

  err "Could not locate the TrustDeck repository root from: $SCRIPT_DIR"
  return 1
}

REPO_ROOT="$(find_repo_root)" || exit 1
DEFAULT_ENV_FILE="$REPO_ROOT/trustdeck.env"
DEFAULT_APP_YML="$REPO_ROOT/src/main/resources/application.yml"

USERNAME=""
DB_MODE="docker"
ENV_FILE_ARG=""
APPLICATION_YML_ARG=""
DOCKER_CONTAINER="trustdeck-postgresql"
KEYCLOAK_CONTAINER="trustdeck-keycloak"
DB_HOST_ARG=""
DB_PORT_ARG=""
DB_NAME_ARG=""
DB_USER_ARG=""
DB_PASSWORD_ARG=""
KEYCLOAK_URL_ARG=""
KEYCLOAK_REALM_ARG=""
KEYCLOAK_CLIENT_ID_ARG=""
KEYCLOAK_CLIENT_SECRET_ARG=""
CREATED_BY="bootstrap-script"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --username)
      USERNAME="${2:-}"
      shift 2
      ;;
    --db-mode)
      DB_MODE="${2:-}"
      shift 2
      ;;
    --env-file)
      ENV_FILE_ARG="${2:-}"
      shift 2
      ;;
    --application-yml)
      APPLICATION_YML_ARG="${2:-}"
      shift 2
      ;;
    --docker-container)
      DOCKER_CONTAINER="${2:-}"
      shift 2
      ;;
    --keycloak-container)
      KEYCLOAK_CONTAINER="${2:-}"
      shift 2
      ;;
    --db-host)
      DB_HOST_ARG="${2:-}"
      shift 2
      ;;
    --db-port)
      DB_PORT_ARG="${2:-}"
      shift 2
      ;;
    --db-name)
      DB_NAME_ARG="${2:-}"
      shift 2
      ;;
    --db-user)
      DB_USER_ARG="${2:-}"
      shift 2
      ;;
    --db-password)
      DB_PASSWORD_ARG="${2:-}"
      shift 2
      ;;
    --keycloak-url)
      KEYCLOAK_URL_ARG="${2:-}"
      shift 2
      ;;
    --realm)
      KEYCLOAK_REALM_ARG="${2:-}"
      shift 2
      ;;
    --client-id)
      KEYCLOAK_CLIENT_ID_ARG="${2:-}"
      shift 2
      ;;
    --client-secret)
      KEYCLOAK_CLIENT_SECRET_ARG="${2:-}"
      shift 2
      ;;
    --created-by)
      CREATED_BY="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      err "Unknown argument: $1"
      usage
      exit 1
      ;;
  esac
done

[[ -n "$USERNAME" ]] || {
  err "--username is required"
  usage
  exit 1
}

[[ "$DB_MODE" == "local" || "$DB_MODE" == "docker" ]] || {
  err "--db-mode must be local or docker"
  exit 1
}

ENV_FILE="${ENV_FILE_ARG:-$DEFAULT_ENV_FILE}"
APPLICATION_YML="${APPLICATION_YML_ARG:-$DEFAULT_APP_YML}"

ENV_FILE="$(make_absolute_path "$ENV_FILE")"
APPLICATION_YML="$(make_absolute_path "$APPLICATION_YML")"

require_cmd curl
require_cmd python3
require_cmd awk
require_cmd sed

if [[ -f "$ENV_FILE" ]]; then
  info "Loading environment from $ENV_FILE"

  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  err "Environment file not found: $ENV_FILE"
  exit 1
fi

[[ -f "$APPLICATION_YML" ]] || {
  err "application.yml not found: $APPLICATION_YML"
  exit 1
}

# curl ignores uppercase HTTP_PROXY for security reasons. Export lowercase
# equivalents while retaining the existing uppercase variables.
export http_proxy="${http_proxy:-${HTTP_PROXY:-}}"
export https_proxy="${https_proxy:-${HTTPS_PROXY:-}}"
export no_proxy="${no_proxy:-${NO_PROXY:-}}"

DB_HOST="${DB_HOST_ARG:-${DATABASE_TRUSTDECK_HOST:-localhost}}"
DB_PORT="${DB_PORT_ARG:-${DATABASE_TRUSTDECK_PORT:-5432}}"
DB_NAME="${DB_NAME_ARG:-trustdeck}"
DB_USER="${DB_USER_ARG:-${DATABASE_TRUSTDECK_USER:-}}"
DB_PASSWORD="${DB_PASSWORD_ARG:-${DATABASE_TRUSTDECK_PASSWORD:-}}"

KEYCLOAK_CONFIGURED_URL="${KEYCLOAK_URL_ARG:-${KEYCLOAK_SERVER_URI:-}}"
KEYCLOAK_REALM="${KEYCLOAK_REALM_ARG:-${KEYCLOAK_REALM_NAME:-}}"
KEYCLOAK_CLIENT_ID_EFFECTIVE="${KEYCLOAK_CLIENT_ID_ARG:-${KEYCLOAK_CLIENT_ID:-}}"
KEYCLOAK_CLIENT_SECRET_EFFECTIVE="${KEYCLOAK_CLIENT_SECRET_ARG:-${KEYCLOAK_CLIENT_SECRET:-}}"

[[ -n "$DB_USER" ]] || {
  err "Database user is missing. Set DATABASE_TRUSTDECK_USER or use --db-user."
  exit 1
}

[[ -n "$DB_PASSWORD" ]] || {
  err "Database password is missing. Set DATABASE_TRUSTDECK_PASSWORD or use --db-password."
  exit 1
}

[[ -n "$KEYCLOAK_CONFIGURED_URL" ]] || {
  err "Keycloak URL is missing. Set KEYCLOAK_SERVER_URI or use --keycloak-url."
  exit 1
}

[[ -n "$KEYCLOAK_REALM" ]] || {
  err "Keycloak realm is missing. Set KEYCLOAK_REALM_NAME or use --realm."
  exit 1
}

[[ -n "$KEYCLOAK_CLIENT_ID_EFFECTIVE" ]] || {
  err "Keycloak client ID is missing. Set KEYCLOAK_CLIENT_ID or use --client-id."
  exit 1
}

[[ -n "$KEYCLOAK_CLIENT_SECRET_EFFECTIVE" ]] || {
  err "Keycloak client secret is missing. Set KEYCLOAK_CLIENT_SECRET or use --client-secret."
  exit 1
}

KEYCLOAK_CONFIGURED_URL="${KEYCLOAK_CONFIGURED_URL%/}"

url_host() {
  python3 -c '
import sys
from urllib.parse import urlsplit

print(urlsplit(sys.argv[1]).hostname or "")
' "$1"
}

url_origin() {
  python3 -c '
import sys
from urllib.parse import urlsplit

url = urlsplit(sys.argv[1])
host = url.hostname or ""

if ":" in host and not host.startswith("["):
    host = f"[{host}]"

port = f":{url.port}" if url.port else ""
print(f"{url.scheme}://{host}{port}")
' "$1"
}

url_path() {
  python3 -c '
import sys
from urllib.parse import urlsplit

print((urlsplit(sys.argv[1]).path or "").rstrip("/"))
' "$1"
}

detect_keycloak_host_port() {
  local mapping=""

  if command -v docker >/dev/null 2>&1 &&
     docker inspect "$KEYCLOAK_CONTAINER" >/dev/null 2>&1; then

    mapping="$(
      docker port "$KEYCLOAK_CONTAINER" 8081/tcp 2>/dev/null |
        head -n 1 || true
    )"

    if [[ -z "$mapping" ]]; then
      mapping="$(
        docker port "$KEYCLOAK_CONTAINER" 8080/tcp 2>/dev/null |
          head -n 1 || true
      )"
    fi
  fi

  if [[ -n "$mapping" ]]; then
    printf '%s' "${mapping##*:}"
  else
    printf '8081'
  fi
}

candidate_exists() {
  local wanted="$1"
  local current

  for current in "${KEYCLOAK_CANDIDATES[@]:-}"; do
    [[ "$current" == "$wanted" ]] && return 0
  done

  return 1
}

add_keycloak_candidate() {
  local candidate="${1%/}"

  [[ -n "$candidate" ]] || return 0

  if ! candidate_exists "$candidate"; then
    KEYCLOAK_CANDIDATES+=("$candidate")
  fi
}

is_local_url() {
  local host
  host="$(url_host "$1")"

  [[ "$host" == "localhost" ||
     "$host" == "127.0.0.1" ||
     "$host" == "::1" ]]
}

probe_keycloak_url() {
  local base_url="$1"
  local discovery_url
  local http_code=""
  local -a proxy_args=()

  discovery_url="$base_url/realms/$KEYCLOAK_REALM/.well-known/openid-configuration"

  if is_local_url "$base_url"; then
    proxy_args=(--noproxy '*')
  fi

  http_code="$(
    curl -sS \
      --connect-timeout 4 \
      --max-time 12 \
      -o /dev/null \
      -w '%{http_code}' \
      "${proxy_args[@]}" \
      "$discovery_url" \
      2>/dev/null || true
  )"

  [[ "$http_code" =~ ^2[0-9][0-9]$ ]]
}

select_keycloak_url() {
  local configured_origin
  local configured_path
  local host_port
  local relative_path
  local candidate

  configured_origin="$(url_origin "$KEYCLOAK_CONFIGURED_URL")"
  configured_path="$(url_path "$KEYCLOAK_CONFIGURED_URL")"

  relative_path="${KEYCLOAK_RELATIVE_PATH:-}"
  relative_path="${relative_path%/}"

  if [[ -n "$relative_path" && "$relative_path" != /* ]]; then
    relative_path="/$relative_path"
  fi

  KEYCLOAK_CANDIDATES=()

  # Prefer the locally published Docker port. This avoids Docker-only DNS
  # names and avoids routing a local request through Nginx or a proxy.
  if command -v docker >/dev/null 2>&1 &&
     docker inspect "$KEYCLOAK_CONTAINER" >/dev/null 2>&1; then

    host_port="$(detect_keycloak_host_port)"

    # Preserve the path from KEYCLOAK_SERVER_URI first.
    add_keycloak_candidate \
      "http://127.0.0.1:${host_port}${configured_path}"

    # Then try KEYCLOAK_RELATIVE_PATH from trustdeck.env.
    add_keycloak_candidate \
      "http://127.0.0.1:${host_port}${relative_path}"

    # Finally try Keycloak without a relative path.
    add_keycloak_candidate \
      "http://127.0.0.1:${host_port}"
  fi

  # Try the configured URL exactly as supplied.
  add_keycloak_candidate "$KEYCLOAK_CONFIGURED_URL"

  # Try the configured origin with KEYCLOAK_RELATIVE_PATH.
  add_keycloak_candidate \
    "${configured_origin}${relative_path}"

  # Try the configured origin without a relative path.
  add_keycloak_candidate "$configured_origin"

  for candidate in "${KEYCLOAK_CANDIDATES[@]}"; do
    if probe_keycloak_url "$candidate"; then
      KEYCLOAK_URL="$candidate"

      if is_local_url "$KEYCLOAK_URL"; then
        KEYCLOAK_BYPASS_PROXY="true"
      else
        KEYCLOAK_BYPASS_PROXY="false"
      fi

      return 0
    fi
  done

  KEYCLOAK_URL="$KEYCLOAK_CONFIGURED_URL"
  KEYCLOAK_BYPASS_PROXY="false"

  warn "Could not verify a reachable Keycloak discovery endpoint."
  warn "Continuing with configured URL: $KEYCLOAK_URL"
}

select_keycloak_url

info "Repository root: $REPO_ROOT"
info "application.yml: $APPLICATION_YML"

if [[ "$KEYCLOAK_URL" != "$KEYCLOAK_CONFIGURED_URL" ]]; then
  info "Configured Keycloak URL: $KEYCLOAK_CONFIGURED_URL"
  info "Using reachable Keycloak URL: $KEYCLOAK_URL"
else
  info "Using Keycloak URL: $KEYCLOAK_URL"
fi

parse_actions() {
  local group="$1"

  awk -v target_group="$group" '
    BEGIN {
      in_app=0
      in_roles=0
      current_group=""
    }

    /^[[:space:]]*app:[[:space:]]*$/ {
      in_app=1
      next
    }

    in_app && /^[[:space:]]{2}roles:[[:space:]]*$/ {
      in_roles=1
      next
    }

    in_roles && /^[[:space:]]{4}[A-Za-z0-9_]+:[[:space:]]*$/ {
      line=$0
      sub(/^[[:space:]]+/, "", line)
      sub(/:.*/, "", line)
      current_group=line
      next
    }

    in_roles &&
    current_group == target_group &&
    /^[[:space:]]{6}-[[:space:]]*/ {
      line=$0
      sub(/^[[:space:]]{6}-[[:space:]]*/, "", line)
      gsub(/[[:space:]]+$/, "", line)

      if (length(line) > 0) {
        print line
      }

      next
    }

    in_roles && /^[^[:space:]]/ {
      in_roles=0
      in_app=0
    }
  ' "$APPLICATION_YML"
}

mapfile -t ACE_ACTIONS < <(parse_actions "ACE")
mapfile -t KING_ACTIONS < <(parse_actions "KING")
mapfile -t GLOBAL_ACTIONS < <(parse_actions "global")

[[ ${#ACE_ACTIONS[@]} -gt 0 ]] || {
  err "No ACE actions could be parsed from app.roles in $APPLICATION_YML"
  exit 1
}

[[ ${#KING_ACTIONS[@]} -gt 0 ]] || {
  err "No KING actions could be parsed from app.roles in $APPLICATION_YML"
  exit 1
}

[[ ${#GLOBAL_ACTIONS[@]} -gt 0 ]] || {
  err "No global actions could be parsed from app.roles in $APPLICATION_YML"
  exit 1
}

info "Parsed ${#ACE_ACTIONS[@]} ACE actions, ${#KING_ACTIONS[@]} KING actions, ${#GLOBAL_ACTIONS[@]} global actions from application.yml"

kc_curl() {
  local description="$1"
  shift

  local response_file
  local http_code
  local curl_status
  local -a proxy_args=()

  response_file="$(mktemp)"

  if [[ "$KEYCLOAK_BYPASS_PROXY" == "true" ]]; then
    proxy_args=(--noproxy '*')
  fi

  set +e

  http_code="$(
    curl -sS \
      --connect-timeout 10 \
      --max-time 60 \
      -o "$response_file" \
      -w '%{http_code}' \
      "${proxy_args[@]}" \
      "$@"
  )"

  curl_status=$?

  set -e

  if [[ $curl_status -ne 0 ]]; then
    err "$description request failed (curl exit code $curl_status)."

    if [[ -s "$response_file" ]]; then
      sed 's/^/[ERROR] Response body: /' "$response_file" >&2
    fi

    rm -f "$response_file"
    return "$curl_status"
  fi

  if [[ ! "$http_code" =~ ^2[0-9][0-9]$ ]]; then
    err "$description request failed with HTTP $http_code."

    if [[ -s "$response_file" ]]; then
      sed 's/^/[ERROR] Response body: /' "$response_file" >&2
    fi

    rm -f "$response_file"
    return 1
  fi

  cat "$response_file"
  rm -f "$response_file"
}

json_access_token() {
  python3 -c '
import json
import sys

try:
    obj = json.load(sys.stdin)
except json.JSONDecodeError as exc:
    print(
        f"Invalid JSON from Keycloak token endpoint: {exc}",
        file=sys.stderr
    )
    raise SystemExit(1)

token = obj.get("access_token", "")

if not token:
    print(
        "Keycloak token response did not contain an access_token",
        file=sys.stderr
    )
    raise SystemExit(1)

print(token)
'
}

json_subject_id() {
  local lookup_username="$1"

  LOOKUP_USERNAME="$lookup_username" python3 -c '
import json
import os
import sys

username = os.environ.get("LOOKUP_USERNAME", "")

try:
    users = json.load(sys.stdin)
except json.JSONDecodeError as exc:
    print(
        f"Invalid JSON from Keycloak users endpoint: {exc}",
        file=sys.stderr
    )
    raise SystemExit(1)

if not isinstance(users, list):
    print(
        "Keycloak users response was not a JSON array",
        file=sys.stderr
    )
    raise SystemExit(1)

exact = [
    user
    for user in users
    if str(user.get("username", "")).casefold() == username.casefold()
]

if len(exact) == 1:
    print(exact[0].get("id", ""))
elif len(exact) > 1:
    print(
        f"Multiple exact Keycloak users found for username {username!r}",
        file=sys.stderr
    )
    raise SystemExit(1)
elif len(users) == 1:
    print(users[0].get("id", ""))
'
}

urlencode() {
  python3 -c '
import sys
import urllib.parse

print(urllib.parse.quote(sys.argv[1]))
' "$1"
}

resolve_keycloak_user() {
  local token_response
  local access_token
  local users_response
  local subject_id
  local admin_users_url
  local encoded_username
  local token_url

  token_url="$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token"

  token_response="$(
    kc_curl "Keycloak token" \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      --data-urlencode "client_id=$KEYCLOAK_CLIENT_ID_EFFECTIVE" \
      --data-urlencode "client_secret=$KEYCLOAK_CLIENT_SECRET_EFFECTIVE" \
      --data-urlencode 'grant_type=client_credentials' \
      "$token_url"
  )" || return 1

  access_token="$(
    printf '%s' "$token_response" |
      json_access_token
  )" || {
    err "Failed to parse Keycloak admin access token response."
    return 1
  }

  encoded_username="$(urlencode "$USERNAME")"

  admin_users_url="$KEYCLOAK_URL/admin/realms/$KEYCLOAK_REALM/users?username=$encoded_username&exact=true"

  users_response="$(
    kc_curl "Keycloak user lookup" \
      -H "Authorization: Bearer $access_token" \
      "$admin_users_url"
  )" || return 1

  subject_id="$(
    printf '%s' "$users_response" |
      json_subject_id "$USERNAME"
  )" || {
    err "Failed to parse Keycloak user lookup response."
    return 1
  }

  [[ -n "$subject_id" ]] || {
    err "Could not resolve Keycloak user '$USERNAME' in realm '$KEYCLOAK_REALM'."
    return 1
  }

  printf '%s' "$subject_id"
}

SUBJECT_ID="$(resolve_keycloak_user)" || {
  err "Failed to resolve Keycloak user. See the Keycloak/curl error above."
  exit 1
}

info "Resolved username '$USERNAME' to subject ID '$SUBJECT_ID'"

build_values_cte() {
  local -n arr_ref=$1
  local out=""
  local first=1
  local item
  local escaped

  for item in "${arr_ref[@]}"; do
    escaped="$(sq "$item")"

    if [[ $first -eq 1 ]]; then
      out="('$escaped')"
      first=0
    else
      out+=$',\n        ('"'$escaped'"')'
    fi
  done

  printf '%s' "$out"
}

GLOBAL_VALUES="$(build_values_cte GLOBAL_ACTIONS)"
ACE_VALUES="$(build_values_cte ACE_ACTIONS)"
KING_VALUES="$(build_values_cte KING_ACTIONS)"
SUBJECT_ID_SQL="$(sq "$SUBJECT_ID")"
CREATED_BY_SQL="$(sq "$CREATED_BY")"

SQL_FILE="$(mktemp)"

cleanup() {
  rm -f "$SQL_FILE"
}

trap cleanup EXIT

cat > "$SQL_FILE" <<SQL
BEGIN;

-- Global permissions
WITH actions(action) AS (
    VALUES
        $GLOBAL_VALUES
)
INSERT INTO permission_grant
(
    subject_id,
    resource_type,
    resource_id,
    action,
    decision,
    valid_from,
    valid_to,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    '$SUBJECT_ID_SQL',
    'GLOBAL',
    0,
    a.action,
    'ALLOW',
    NOW(),
    NOW() + INTERVAL '3650 days',
    NOW(),
    '$CREATED_BY_SQL',
    NOW(),
    '$CREATED_BY_SQL'
FROM actions a
WHERE NOT EXISTS (
    SELECT 1
    FROM permission_grant pg
    WHERE pg.subject_id = '$SUBJECT_ID_SQL'
      AND pg.resource_type = 'GLOBAL'
      AND pg.resource_id = 0
      AND pg.action = a.action
);

-- Domain permissions for all existing domains
WITH actions(action) AS (
    VALUES
        $ACE_VALUES
),
domains(id) AS (
    SELECT id
    FROM domain
)
INSERT INTO permission_grant
(
    subject_id,
    resource_type,
    resource_id,
    action,
    decision,
    valid_from,
    valid_to,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    '$SUBJECT_ID_SQL',
    'DOMAIN',
    d.id,
    a.action,
    'ALLOW',
    NOW(),
    NOW() + INTERVAL '3650 days',
    NOW(),
    '$CREATED_BY_SQL',
    NOW(),
    '$CREATED_BY_SQL'
FROM domains d
CROSS JOIN actions a
WHERE NOT EXISTS (
    SELECT 1
    FROM permission_grant pg
    WHERE pg.subject_id = '$SUBJECT_ID_SQL'
      AND pg.resource_type = 'DOMAIN'
      AND pg.resource_id = d.id
      AND pg.action = a.action
);

-- Project permissions for all existing projects
WITH actions(action) AS (
    VALUES
        $KING_VALUES
),
projects(id) AS (
    SELECT id
    FROM project
)
INSERT INTO permission_grant
(
    subject_id,
    resource_type,
    resource_id,
    action,
    decision,
    valid_from,
    valid_to,
    created_at,
    created_by,
    updated_at,
    updated_by
)
SELECT
    '$SUBJECT_ID_SQL',
    'PROJECT',
    p.id,
    a.action,
    'ALLOW',
    NOW(),
    NOW() + INTERVAL '3650 days',
    NOW(),
    '$CREATED_BY_SQL',
    NOW(),
    '$CREATED_BY_SQL'
FROM projects p
CROSS JOIN actions a
WHERE NOT EXISTS (
    SELECT 1
    FROM permission_grant pg
    WHERE pg.subject_id = '$SUBJECT_ID_SQL'
      AND pg.resource_type = 'PROJECT'
      AND pg.resource_id = p.id
      AND pg.action = a.action
);

COMMIT;
SQL

if [[ "$DRY_RUN" == "true" ]]; then
  info "Dry run enabled. Generated SQL:"
  cat "$SQL_FILE"
  exit 0
fi

if [[ "$DB_MODE" == "local" ]]; then
  require_cmd psql

  info "Applying bootstrap permissions via local PostgreSQL connection to $DB_HOST:$DB_PORT/$DB_NAME"

  PGPASSWORD="$DB_PASSWORD" psql \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 \
    -f "$SQL_FILE"
else
  require_cmd docker

  docker inspect "$DOCKER_CONTAINER" >/dev/null 2>&1 || {
    err "PostgreSQL container not found: $DOCKER_CONTAINER"
    exit 1
  }

  info "Applying bootstrap permissions via Docker container '$DOCKER_CONTAINER'"

  docker exec -i \
    -e PGPASSWORD="$DB_PASSWORD" \
    "$DOCKER_CONTAINER" \
    psql \
      -U "$DB_USER" \
      -d "$DB_NAME" \
      -v ON_ERROR_STOP=1 \
    < "$SQL_FILE"
fi

info "Bootstrap permissions inserted successfully for user '$USERNAME' (subject ID: $SUBJECT_ID)."


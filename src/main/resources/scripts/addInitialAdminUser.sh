#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  addInitialAdminUser.sh --username <username> [--db-mode local|docker]
                         [--env-file <path/to/trustdeck.env>]
                         [--application-yml <path/to/application.yml>]
                         [--docker-container <postgres_container_name>]
                         [--db-host <host>] [--db-port <port>] [--db-name <db>]
                         [--db-user <user>] [--db-password <password>]
                         [--keycloak-url <url>] [--realm <realm>]
                         [--client-id <client_id>] [--client-secret <secret>]
                         [--created-by <marker>] [--dry-run]

Description:
  Resolves a Keycloak user by username, reads the ACE/KING/global permission lists from
  src/main/resources/application.yml, and inserts all currently defined permissions for that user
  into the TrustDeck PostgreSQL database.

What gets inserted:
  - all GLOBAL actions for resource_type=GLOBAL, resource_id=0
  - all ACE actions for every existing row in table "domain"
  - all KING actions for every existing row in table "project"

Defaults:
  --db-mode docker
  --docker-container trustdeck-postgresql
  --db-name trustdeck
  --created-by bootstrap-script

Environment / config discovery:
  The script first tries to source ./trustdeck.env from the repository root. Any command-line
  options override values from that file.

Examples:
  ./src/main/resources/scripts/addInitialAdminUser.sh --username alice
  ./src/main/resources/scripts/addInitialAdminUser.sh --username alice --db-mode local
  ./src/main/resources/scripts/addInitialAdminUser.sh --username alice --dry-run
USAGE
}

err() { echo "[ERROR] $*" >&2; }
info() { echo "[INFO] $*" >&2; }
warn() { echo "[WARN] $*" >&2; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { err "Missing required command: $1"; exit 1; }
}

sq() {
  printf "%s" "$1" | sed "s/'/''/g"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
DEFAULT_ENV_FILE="$REPO_ROOT/trustdeck.env"
DEFAULT_APP_YML="$REPO_ROOT/src/main/resources/application.yml"

USERNAME=""
DB_MODE="docker"
ENV_FILE="$DEFAULT_ENV_FILE"
APPLICATION_YML="$DEFAULT_APP_YML"
DOCKER_CONTAINER="trustdeck-postgresql"
DB_HOST=""
DB_PORT=""
DB_NAME="trustdeck"
DB_USER=""
DB_PASSWORD=""
KEYCLOAK_URL=""
KEYCLOAK_REALM=""
KEYCLOAK_CLIENT_ID=""
KEYCLOAK_CLIENT_SECRET=""
CREATED_BY="bootstrap-script"
DRY_RUN="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --username) USERNAME="${2:-}"; shift 2 ;;
    --db-mode) DB_MODE="${2:-}"; shift 2 ;;
    --env-file) ENV_FILE="${2:-}"; shift 2 ;;
    --application-yml) APPLICATION_YML="${2:-}"; shift 2 ;;
    --docker-container) DOCKER_CONTAINER="${2:-}"; shift 2 ;;
    --db-host) DB_HOST="${2:-}"; shift 2 ;;
    --db-port) DB_PORT="${2:-}"; shift 2 ;;
    --db-name) DB_NAME="${2:-}"; shift 2 ;;
    --db-user) DB_USER="${2:-}"; shift 2 ;;
    --db-password) DB_PASSWORD="${2:-}"; shift 2 ;;
    --keycloak-url) KEYCLOAK_URL="${2:-}"; shift 2 ;;
    --realm) KEYCLOAK_REALM="${2:-}"; shift 2 ;;
    --client-id) KEYCLOAK_CLIENT_ID="${2:-}"; shift 2 ;;
    --client-secret) KEYCLOAK_CLIENT_SECRET="${2:-}"; shift 2 ;;
    --created-by) CREATED_BY="${2:-}"; shift 2 ;;
    --dry-run) DRY_RUN="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) err "Unknown argument: $1"; usage; exit 1 ;;
  esac
done

[[ -n "$USERNAME" ]] || { err "--username is required"; usage; exit 1; }
[[ "$DB_MODE" == "local" || "$DB_MODE" == "docker" ]] || { err "--db-mode must be local or docker"; exit 1; }
[[ -f "$APPLICATION_YML" ]] || { err "application.yml not found: $APPLICATION_YML"; exit 1; }

require_cmd curl
require_cmd python3
require_cmd awk

if [[ -f "$ENV_FILE" ]]; then
  info "Loading environment from $ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  warn "Env file not found: $ENV_FILE"
fi

DB_HOST="${DB_HOST:-${DATABASE_TRUSTDECK_HOST:-localhost}}"
DB_PORT="${DB_PORT:-${DATABASE_TRUSTDECK_PORT:-5432}}"
DB_USER="${DB_USER:-${DATABASE_TRUSTDECK_USER:-}}"
DB_PASSWORD="${DB_PASSWORD:-${DATABASE_TRUSTDECK_PASSWORD:-}}"
KEYCLOAK_URL="${KEYCLOAK_URL:-${KEYCLOAK_SERVER_URI:-}}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-${KEYCLOAK_REALM_NAME:-}}"
KEYCLOAK_CLIENT_ID="${KEYCLOAK_CLIENT_ID:-${KEYCLOAK_CLIENT_ID:-}}"
KEYCLOAK_CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET:-${KEYCLOAK_CLIENT_SECRET:-}}"

[[ -n "$DB_USER" ]] || { err "Database user is missing. Set DATABASE_TRUSTDECK_USER or --db-user."; exit 1; }
[[ -n "$DB_PASSWORD" ]] || { err "Database password is missing. Set DATABASE_TRUSTDECK_PASSWORD or --db-password."; exit 1; }
[[ -n "$KEYCLOAK_URL" ]] || { err "Keycloak URL is missing. Set KEYCLOAK_SERVER_URI or --keycloak-url."; exit 1; }
[[ -n "$KEYCLOAK_REALM" ]] || { err "Keycloak realm is missing. Set KEYCLOAK_REALM_NAME or --realm."; exit 1; }
[[ -n "$KEYCLOAK_CLIENT_ID" ]] || { err "Keycloak client ID is missing. Set KEYCLOAK_CLIENT_ID or --client-id."; exit 1; }
[[ -n "$KEYCLOAK_CLIENT_SECRET" ]] || { err "Keycloak client secret is missing. Set KEYCLOAK_CLIENT_SECRET or --client-secret."; exit 1; }

parse_actions() {
  local group="$1"
  awk -v target_group="$group" '
    BEGIN { in_app=0; in_roles=0; current_group="" }
    /^[[:space:]]*app:[[:space:]]*$/ { in_app=1; next }
    in_app && /^[[:space:]]{2}roles:[[:space:]]*$/ { in_roles=1; next }
    in_roles && /^[[:space:]]{4}[A-Za-z0-9_]+:[[:space:]]*$/ {
      line=$0
      sub(/^[[:space:]]+/, "", line)
      sub(/:.*/, "", line)
      current_group=line
      next
    }
    in_roles && current_group == target_group && /^[[:space:]]{6}-[[:space:]]*/ {
      line=$0
      sub(/^[[:space:]]{6}-[[:space:]]*/, "", line)
      gsub(/[[:space:]]+$/, "", line)
      if (length(line) > 0) print line
      next
    }
    in_roles && /^[^[:space:]]/ { in_roles=0; in_app=0 }
  ' "$APPLICATION_YML"
}

mapfile -t ACE_ACTIONS < <(parse_actions "ACE")
mapfile -t KING_ACTIONS < <(parse_actions "KING")
mapfile -t GLOBAL_ACTIONS < <(parse_actions "global")

if [[ ${#ACE_ACTIONS[@]} -eq 0 && ${#KING_ACTIONS[@]} -eq 0 && ${#GLOBAL_ACTIONS[@]} -eq 0 ]]; then
  err "No actions could be parsed from app.roles in $APPLICATION_YML"
  exit 1
fi

info "Parsed ${#ACE_ACTIONS[@]} ACE actions, ${#KING_ACTIONS[@]} KING actions, ${#GLOBAL_ACTIONS[@]} global actions from application.yml"

resolve_keycloak_user() {
  local token_response access_token users_response subject_id admin_users_url

  token_response="$(curl -fsS \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "client_id=$KEYCLOAK_CLIENT_ID" \
    --data-urlencode "client_secret=$KEYCLOAK_CLIENT_SECRET" \
    --data-urlencode 'grant_type=client_credentials' \
    "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token")"

  access_token="$(python3 - <<'PY' "$token_response"
import json,sys
obj=json.loads(sys.argv[1])
print(obj.get('access_token',''))
PY
)"

  [[ -n "$access_token" ]] || { err "Failed to obtain Keycloak admin access token."; exit 1; }

  admin_users_url="$KEYCLOAK_URL/admin/realms/$KEYCLOAK_REALM/users?username=$(python3 - <<'PY' "$USERNAME"
import sys, urllib.parse
print(urllib.parse.quote(sys.argv[1]))
PY
)&exact=true"

  users_response="$(curl -fsS -H "Authorization: Bearer $access_token" "$admin_users_url")"

  subject_id="$(python3 - <<'PY' "$users_response" "$USERNAME"
import json,sys
users=json.loads(sys.argv[1])
username=sys.argv[2]
if not isinstance(users, list):
    print("")
    raise SystemExit(0)
for u in users:
    if u.get('username') == username:
        print(u.get('id',''))
        raise SystemExit(0)
if len(users) == 1:
    print(users[0].get('id',''))
else:
    print("")
PY
)"

  [[ -n "$subject_id" ]] || { err "Could not resolve Keycloak user '$USERNAME' in realm '$KEYCLOAK_REALM'."; exit 1; }
  printf "%s" "$subject_id"
}

SUBJECT_ID="$(resolve_keycloak_user)"
info "Resolved username '$USERNAME' to subject ID '$SUBJECT_ID'"

build_values_cte() {
  local -n arr_ref=$1
  local out=""
  local first=1
  local item escaped
  for item in "${arr_ref[@]}"; do
    escaped="$(sq "$item")"
    if [[ $first -eq 1 ]]; then
      out="('$escaped')"
      first=0
    else
      out+=$',\n        ('"'$escaped'"')'
    fi
  done
  printf "%s" "$out"
}

GLOBAL_VALUES="$(build_values_cte GLOBAL_ACTIONS)"
ACE_VALUES="$(build_values_cte ACE_ACTIONS)"
KING_VALUES="$(build_values_cte KING_ACTIONS)"
SUBJECT_ID_SQL="$(sq "$SUBJECT_ID")"
CREATED_BY_SQL="$(sq "$CREATED_BY")"

SQL_FILE="$(mktemp)"
trap 'rm -f "$SQL_FILE"' EXIT

cat > "$SQL_FILE" <<SQL
BEGIN;

-- Global permissions
WITH actions(action) AS (
    VALUES
        $GLOBAL_VALUES
)
INSERT INTO permission_grant
(subject_id, resource_type, resource_id, action, decision, valid_from, valid_to, created_at, created_by, updated_at, updated_by)
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
), domains(id) AS (
    SELECT id FROM domain
)
INSERT INTO permission_grant
(subject_id, resource_type, resource_id, action, decision, valid_from, valid_to, created_at, created_by, updated_at, updated_by)
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
), projects(id) AS (
    SELECT id FROM project
)
INSERT INTO permission_grant
(subject_id, resource_type, resource_id, action, decision, valid_from, valid_to, created_at, created_by, updated_at, updated_by)
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
  info "Applying bootstrap permissions via Docker container '$DOCKER_CONTAINER'"
  docker exec -i \
    -e PGPASSWORD="$DB_PASSWORD" \
    "$DOCKER_CONTAINER" \
    psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 \
    < "$SQL_FILE"
fi

info "Bootstrap permissions inserted successfully for user '$USERNAME' (subject ID: $SUBJECT_ID)."

#!/usr/bin/env bash
set -euo pipefail

# Get absolute path of the directory where this script is in
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$ROOT_DIR/trustdeck.env"
DEV_COMPOSE="$ROOT_DIR/docker/docker-compose.dev.yml"
PROD_COMPOSE="$ROOT_DIR/docker/docker-compose.prod.yml"
APP_COMPOSE="$ROOT_DIR/docker/docker-compose.app.yml"

# Container_names (same as in the compose files)
POSTGRES_CONTAINER="trustdeck-postgresql"
KEYCLOAK_CONTAINER="trustdeck-keycloak"

# Helper for printing how to use
usage() {
  cat <<EOF
Usage: $0 <dev|prod> <start|stop>

  dev   - Run PostgreSQL and Keycloak in Docker, backend via Maven
  prod  - Run PostgreSQL, Keycloak, and backend in Docker

Examples:
  $0 dev start
  $0 dev stop
  $0 prod start
  $0 prod stop
EOF
  exit 1
}

# Decide whether to prefix commands with sudo for Docker.
# If your user is not in the docker group, run "sudo -v" before this script so sudo can prompt once.
if docker info >/dev/null 2>&1; then
  SUDO_DOCKER=""
elif command -v sudo >/dev/null 2>&1; then
  SUDO_DOCKER="sudo"
else
  echo "Cannot talk to Docker and sudo is not available."
  echo "   - Is the Docker daemon running?"
  echo "   - Do you need to be in the 'docker' group or use sudo?"
  exit 1
fi

# Export trustdeck.env for local checks that need DATABASE_* variables.
load_env_for_local_checks() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Environment file not found: $ENV_FILE"
    exit 1
  fi

  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
}

ensure_trustdeck_network() {
  if ! $SUDO_DOCKER docker network inspect trustdeck >/dev/null 2>&1; then
    echo "Creating external Docker network 'trustdeck' ..."
    $SUDO_DOCKER docker network create trustdeck >/dev/null
  fi
}

ensure_kafka_ssl_dir() {
  local default_kafka_ssl_dir="/opt/kafka-ssl"
  local configured_kafka_ssl_dir="${KAFKA_SSL_DIR:-}"

  if [[ -n "$configured_kafka_ssl_dir" && -d "$configured_kafka_ssl_dir" && -r "$configured_kafka_ssl_dir" && -x "$configured_kafka_ssl_dir" ]]; then
    export KAFKA_SSL_DIR="$configured_kafka_ssl_dir"
    echo "Using Kafka SSL directory: $KAFKA_SSL_DIR"
    return 0
  fi

  if [[ -n "$configured_kafka_ssl_dir" ]]; then
    echo "KAFKA_SSL_DIR is set to '$configured_kafka_ssl_dir', but it is not readable/searchable."
    echo "Falling back to default Kafka SSL directory: $default_kafka_ssl_dir"
  else
    echo "KAFKA_SSL_DIR is not set. Using default Kafka SSL directory: $default_kafka_ssl_dir"
  fi

  if [[ ! -d "$default_kafka_ssl_dir" ]]; then
    echo "Creating Kafka SSL directory: $default_kafka_ssl_dir"

    if [[ -w "$(dirname "$default_kafka_ssl_dir")" ]]; then
      mkdir -p "$default_kafka_ssl_dir"
    elif command -v sudo >/dev/null 2>&1; then
      sudo mkdir -p "$default_kafka_ssl_dir"
    else
      echo "Cannot create '$default_kafka_ssl_dir'. Create it manually or run with sufficient permissions."
      exit 1
    fi
  fi

  if [[ ! -r "$default_kafka_ssl_dir" || ! -x "$default_kafka_ssl_dir" ]]; then
    echo "Making Kafka SSL directory readable/searchable: $default_kafka_ssl_dir"

    if command -v sudo >/dev/null 2>&1; then
      sudo chmod 0755 "$default_kafka_ssl_dir"
    else
      echo "'$default_kafka_ssl_dir' exists but is not readable/searchable. Fix permissions manually."
      exit 1
    fi
  fi

  export KAFKA_SSL_DIR="$default_kafka_ssl_dir"
}

# Method for checking docker container health status
wait_for_healthy() {
  local container="$1"
  local retries="${2:-60}"
  local delay="${3:-5}"

  echo "Waiting for container '$container' to become healthy ..."

  for i in $(seq 1 "$retries"); do
    # Read health status (or 'unknown' if no healthcheck / container not ready)
    local status
    status="$($SUDO_DOCKER docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}unknown{{end}}' "$container" 2>/dev/null || echo "unknown")"

    if [[ "$status" == "healthy" ]]; then
      echo "$container is healthy."
      return 0
    elif [[ "$status" == "unhealthy" ]]; then
      echo "$container is UNHEALTHY. Aborting."
      $SUDO_DOCKER docker logs --tail=100 "$container" || true
      return 1
    fi

    echo "  [$i/$retries] $container status: $status (retrying in ${delay}s...)"
    sleep "$delay"
  done

  echo "Timed out waiting for $container to become healthy."
  $SUDO_DOCKER docker logs --tail=100 "$container" || true
  return 1
}

wait_for_host_port() {
  local host="$1"
  local port="$2"
  local retries="${3:-60}"
  local delay="${4:-2}"

  echo "Waiting until ${host}:${port} accepts TCP connections from the host ..."

  for i in $(seq 1 "$retries"); do
    if (echo >"/dev/tcp/${host}/${port}") >/dev/null 2>&1; then
      echo "${host}:${port} is reachable from the host."
      return 0
    fi

    echo "  [$i/$retries] ${host}:${port} not reachable yet (retrying in ${delay}s...)"
    sleep "$delay"
  done

  echo "Timed out waiting for ${host}:${port}."
  echo "Check that docker-compose.prod.yml publishes PostgreSQL, for example:"
  echo '  ports:'
  echo '    - "127.0.0.1:5432:5432"'
  return 1
}

wait_for_trustdeck_db_schema() {
  local retries="${1:-90}"
  local delay="${2:-2}"

  echo "Waiting until the trustdeck database is reachable and the public schema is initialized ..."

  for i in $(seq 1 "$retries"); do
    local state
    state="$($SUDO_DOCKER docker exec "$POSTGRES_CONTAINER" \
      psql -v ON_ERROR_STOP=1 \
        -U "${DATABASE_TRUSTDECK_USER}" \
        -d trustdeck \
        -tAc "select case when count(*) > 0 then 'ready' else 'not_ready' end from information_schema.tables where table_schema = 'public';" \
      2>/dev/null | tr -d '[:space:]' || true)"

    if [[ "$state" == "ready" ]]; then
      echo "trustdeck database is reachable and schema is initialized."
      return 0
    fi

    echo "  [$i/$retries] trustdeck DB state: ${state:-not_ready} (retrying in ${delay}s...)"
    sleep "$delay"
  done

  echo "Timed out waiting for the trustdeck database schema."
  $SUDO_DOCKER docker logs --tail=100 "$POSTGRES_CONTAINER" || true
  return 1
}

start_postgres_for_codegen() {
  echo "Starting PostgreSQL first for jOOQ code generation ..."

  ensure_trustdeck_network

  $SUDO_DOCKER docker compose \
    --project-name "trustdeck" \
    --env-file "$ENV_FILE" \
    -f "$PROD_COMPOSE" \
    up -d --build "$POSTGRES_CONTAINER"

  wait_for_healthy "$POSTGRES_CONTAINER"

  # The Maven/jOOQ configuration connects to jdbc:postgresql://localhost:5432/trustdeck.
  # Therefore we must verify the host-side port, not only Docker-internal connectivity.
  wait_for_host_port "127.0.0.1" "5432"

  # Docker health only checks pg_isready. This additionally verifies the initialized trustdeck schema.
  wait_for_trustdeck_db_schema
}

# Method for building the backend jar that Dockerfile_TrustDeck copies into the image
validate_keycloak_themes() {
  local validator="$ROOT_DIR/src/main/resources/scripts/validateKeycloakThemes.sh"
  local themes_root="$ROOT_DIR/src/main/resources/keycloak-themes"

  if [[ ! -x "$validator" ]]; then
    echo "Keycloak theme validator is missing or not executable: $validator" >&2
    exit 1
  fi

  echo "Validating repository-provided Keycloak themes ..."
  "$validator" "$themes_root"
}

build_backend_jar() {
  echo "Building latest TrustDeck backend JAR with Maven ..."

  if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven (mvn) is required to build the backend JAR but was not found."
    echo "Install Maven first, or build the JAR on another machine and copy target/trustdeck*.jar here."
    exit 1
  fi

  # mvn clean removes any stale target/ JAR, so Docker can only copy the freshly built artifact.
  mvn -f "$ROOT_DIR/pom.xml" clean package -DskipTests

  local jar_count
  jar_count="$(find "$ROOT_DIR/target" -maxdepth 1 -type f -name 'trustdeck*.jar' | wc -l | tr -d ' ')"
  if [[ "$jar_count" -ne 1 ]]; then
    echo "Expected exactly one target/trustdeck*.jar after the Maven build, found $jar_count."
    find "$ROOT_DIR/target" -maxdepth 1 -type f -name 'trustdeck*.jar' -print || true
    exit 1
  fi

  echo "Using backend JAR:"
  find "$ROOT_DIR/target" -maxdepth 1 -type f -name 'trustdeck*.jar' -print -exec ls -lh {} \;
}

# Method that encapsulates the development startup commands
start_dev() {
  echo "Starting Keycloak and PostgreSQL in dev mode ..."

  # Export all variables from trustdeck.env to current shell
  load_env_for_local_checks

  # Ensure KAFKA_SSL_DIR is set and points to a readable/searchable directory.
  # Falls back to /opt/kafka-ssl if missing or unusable.
  ensure_kafka_ssl_dir

  validate_keycloak_themes

  $SUDO_DOCKER docker compose --project-name "trustdeck-dev" --env-file "$ENV_FILE" -f "$DEV_COMPOSE" up -d --build

  # Wait for DB + Keycloak to be ready
  wait_for_healthy "$POSTGRES_CONTAINER"
  wait_for_healthy "$KEYCLOAK_CONTAINER"

  echo "Running backend via Maven ..."

  mvn clean compile -f "$ROOT_DIR/pom.xml" -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector org.springframework.boot:spring-boot-maven-plugin:run -Dorg.jooq.no-logo=true -Dorg.jooq.no-tips=true
}

# Method that encapsulates the development stop commands
stop_dev() {
  echo "Stopping dev containers (PostgreSQL and Keycloak) ..."
  $SUDO_DOCKER docker compose --project-name trustdeck --env-file "$ENV_FILE" -f "$DEV_COMPOSE" down

  echo "Dev stack stopped."
}

# Method that encapsulates the production startup commands
start_prod() {
  echo "Building and starting full production stack (PostgreSQL, Keycloak, TrustDeck backend) ..."
  
  # Export all variables from trustdeck.env to current shell
  load_env_for_local_checks

  # Ensure KAFKA_SSL_DIR is set and points to a readable/searchable directory.
  # Falls back to /opt/kafka-ssl if missing or unusable.
  ensure_kafka_ssl_dir

  validate_keycloak_themes

  # jOOQ code generation in pom.xml connects to localhost:5432/trustdeck,
  # so PostgreSQL must be running and initialized before Maven builds the JAR.
  start_postgres_for_codegen
  build_backend_jar

  echo "Starting Keycloak ..."
  $SUDO_DOCKER docker compose \
    --project-name "trustdeck" \
    --env-file "$ENV_FILE" \
    -f "$PROD_COMPOSE" \
    up -d --build "$KEYCLOAK_CONTAINER"

  wait_for_healthy "$KEYCLOAK_CONTAINER"

  echo "Building/recreating TrustDeck backend container with the freshly generated JAR ..."
  $SUDO_DOCKER env KAFKA_SSL_DIR="$KAFKA_SSL_DIR" docker compose \
    --project-name "trustdeck" \
    --env-file "$ENV_FILE" \
    -f "$PROD_COMPOSE" \
    -f "$APP_COMPOSE" \
    up -d --build --force-recreate trustdeck-backend

  echo "Production stack is up (containers running in background)."
}

# Method that encapsulates the production stop commands
stop_prod() {
  echo "Stopping production stack ..."
  $SUDO_DOCKER docker compose \
    --project-name "trustdeck" \
    --env-file "$ENV_FILE" \
    -f "$PROD_COMPOSE" \
    -f "$APP_COMPOSE" \
    down

  echo "Production stack stopped."
}

# --- Main script ---
# Get command line args
MODE="${1:-}"
ACTION="${2:-}"

if [[ -z "$MODE" || -z "$ACTION" ]]; then
  usage
fi

# Decide on what to do or print "how to use"
case "$MODE" in
  dev)
    case "$ACTION" in
      start) start_dev ;;
      stop)  stop_dev ;;
      *) usage ;;
    esac
    ;;
  prod)
    case "$ACTION" in
      start) start_prod ;;
      stop)  stop_prod ;;
      *) usage ;;
    esac
    ;;
  *)
    usage
    ;;
esac


#!/bin/bash
# Trust Deck Services
# Copyright 2026 Armin Müller
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Sets the secret of the TrustDeck client in the example Keycloak realm to the value of
# KEYCLOAK_CLIENT_SECRET, so that installations don't keep the publicly known example secret.
# The PostgreSQL container runs this script once after keycloak-example.sql was imported.
#
set -euo pipefail

if [ -z "${KEYCLOAK_CLIENT_SECRET:-}" ]; then
	echo "KEYCLOAK_CLIENT_SECRET is not set; the secret of the example realm's client stays unchanged." >&2
	exit 0
fi

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname keycloak \
	-v secret="${KEYCLOAK_CLIENT_SECRET}" \
	-v client_id="${KEYCLOAK_CLIENT_ID:-trustdeck}" \
	-v realm="${KEYCLOAK_REALM_NAME:-development}" <<'EOSQL'
UPDATE client SET secret = :'secret'
WHERE client_id = :'client_id' AND realm_id = (SELECT id FROM realm WHERE name = :'realm');
EOSQL

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_THEMES_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)/keycloak-login-themes"
THEMES_ROOT="${1:-$DEFAULT_THEMES_ROOT}"

if [[ ! -d "$THEMES_ROOT" ]]; then
  echo "Keycloak login theme directory not found: $THEMES_ROOT" >&2
  exit 1
fi

shopt -s nullglob
THEME_DIRS=("$THEMES_ROOT"/*)
shopt -u nullglob

if [[ ${#THEME_DIRS[@]} -eq 0 ]]; then
  echo "No Keycloak login themes found below: $THEMES_ROOT" >&2
  exit 1
fi

validated=0
for theme_dir in "${THEME_DIRS[@]}"; do
  [[ -d "$theme_dir" ]] || continue

  theme_name="$(basename "$theme_dir")"
  login_dir="$theme_dir/login"
  properties_file="$login_dir/theme.properties"

  if [[ ! -f "$properties_file" ]]; then
    echo "Theme '$theme_name' is missing login/theme.properties" >&2
    exit 1
  fi

  styles_line="$(sed -n 's/^[[:space:]]*styles[[:space:]]*=[[:space:]]*//p' "$properties_file" | tail -n 1)"
  if [[ -z "$styles_line" ]]; then
    echo "Theme '$theme_name' does not declare a styles= entry in login/theme.properties" >&2
    exit 1
  fi

  for stylesheet in $styles_line; do
    if [[ ! -f "$login_dir/resources/$stylesheet" ]]; then
      echo "Theme '$theme_name' references missing stylesheet: login/resources/$stylesheet" >&2
      exit 1
    fi
  done

  echo "Validated Keycloak login theme: $theme_name"
  validated=$((validated + 1))
done

if [[ $validated -eq 0 ]]; then
  echo "No theme directories were validated below: $THEMES_ROOT" >&2
  exit 1
fi

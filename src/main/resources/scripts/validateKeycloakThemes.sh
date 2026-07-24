#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_THEMES_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)/keycloak-themes"
THEMES_ROOT="${1:-$DEFAULT_THEMES_ROOT}"
SUPPORTED_TYPES=(login account admin email welcome)

if [[ ! -d "$THEMES_ROOT" ]]; then
  echo "Keycloak theme directory not found: $THEMES_ROOT" >&2
  exit 1
fi

shopt -s nullglob
THEME_DIRS=("$THEMES_ROOT"/*)
shopt -u nullglob

if [[ ${#THEME_DIRS[@]} -eq 0 ]]; then
  echo "No Keycloak themes found below: $THEMES_ROOT" >&2
  exit 1
fi

validate_resource_list() {
  local theme_name="$1"
  local type_name="$2"
  local type_dir="$3"
  local properties_file="$4"
  local property_name="$5"
  local resource_prefix="$6"
  local property_value

  property_value="$(sed -n "s/^[[:space:]]*${property_name}[[:space:]]*=[[:space:]]*//p" "$properties_file" | tail -n 1)"
  [[ -n "$property_value" ]] || return 0

  for resource in $property_value; do
    if [[ ! -f "$type_dir/resources/$resource_prefix$resource" ]]; then
      echo "Theme '$theme_name' type '$type_name' references missing resource: resources/$resource_prefix$resource" >&2
      exit 1
    fi
  done
}

validated_themes=0
validated_types=0

for theme_dir in "${THEME_DIRS[@]}"; do
  [[ -d "$theme_dir" ]] || continue

  theme_name="$(basename "$theme_dir")"
  theme_type_count=0

  for type_name in "${SUPPORTED_TYPES[@]}"; do
    type_dir="$theme_dir/$type_name"
    [[ -d "$type_dir" ]] || continue

    properties_file="$type_dir/theme.properties"
    if [[ ! -f "$properties_file" ]]; then
      echo "Theme '$theme_name' type '$type_name' is missing theme.properties" >&2
      exit 1
    fi

    validate_resource_list "$theme_name" "$type_name" "$type_dir" "$properties_file" styles ""
    validate_resource_list "$theme_name" "$type_name" "$type_dir" "$properties_file" scripts ""
    validate_resource_list "$theme_name" "$type_name" "$type_dir" "$properties_file" favicons ""

    # The modern Account and Admin console templates use these singular properties.
    for singular_property in logo favIcon; do
      singular_value="$(sed -n "s/^[[:space:]]*${singular_property}[[:space:]]*=[[:space:]]*//p" "$properties_file" | tail -n 1)"
      if [[ -n "$singular_value" && ! -f "$type_dir/resources/${singular_value#/}" ]]; then
        echo "Theme '$theme_name' type '$type_name' references missing $singular_property: resources/${singular_value#/}" >&2
        exit 1
      fi
    done

    echo "Validated Keycloak theme: $theme_name ($type_name)"
    theme_type_count=$((theme_type_count + 1))
    validated_types=$((validated_types + 1))
  done

  if [[ $theme_type_count -eq 0 ]]; then
    echo "Theme '$theme_name' contains none of the supported theme types: ${SUPPORTED_TYPES[*]}" >&2
    exit 1
  fi

  validated_themes=$((validated_themes + 1))
done

if [[ $validated_themes -eq 0 || $validated_types -eq 0 ]]; then
  echo "No Keycloak themes were validated below: $THEMES_ROOT" >&2
  exit 1
fi

echo "Validated $validated_types theme type(s) across $validated_themes theme(s)."

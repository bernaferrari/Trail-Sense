#!/usr/bin/env bash

# Generates multiplatform Material Symbols Rounded ImageVectors from Google's
# Kotlin vector endpoint. Keep this list intentionally small and product-facing.

set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly OUTPUT_DIR="$ROOT_DIR/shared/src/commonMain/kotlin/com/kylecorry/trail_sense/shared/icons"
readonly PACKAGE_NAME="com.kylecorry.trail_sense.shared.icons"
readonly BASE_URL="https://fonts.gstatic.com/render/v1/Material+Symbols+Rounded/24dp"
readonly ICONS=(
  explore map partly_cloudy_day stars_2 apps settings search arrow_back
  more_vert close straighten thermometer speed
    hiking menu_book privacy_tip gavel sensors_off location_on warning
)
readonly OUTLINED_ICONS=(explore map partly_cloudy_day stars_2 apps)

CHECK_ONLY=0
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=1
[[ $# -le 1 ]] || { printf 'Usage: %s [--check]\n' "$0" >&2; exit 2; }

to_pascal_case() {
  printf '%s' "$1" | perl -pe 's/(^|_)([a-z0-9])/$2 eq "" ? $1 : uc($2)/ge'
}

download_vector() {
  local icon="$1"
  local fill="$2"
  local output="$3"
  curl --compressed --fail --silent --show-error --location --retry 3 \
    --connect-timeout 15 --max-time 60 \
    "$BASE_URL/${icon}.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,${fill},0,50" \
    --output "$output"
}

normalize_vector() {
  local source="$1"
  local icon="$2"
  local prefix="$3"
  local pascal
  pascal="$(to_pascal_case "$icon")"
  perl -pi -e "s/^package .*/package ${PACKAGE_NAME}/" "$source"
  perl -pi -e "s/public val ${icon}\\b/internal val ${prefix}${pascal}/g" "$source"
  perl -pi -e "s/_${icon}\\b/_${prefix}${pascal}/g" "$source"
  perl -pi -e 's/^private var /internal var /' "$source"
}

vector_body() {
  sed -n '/^@Suppress("CheckReturnValue")/,$p' "$1"
}

generate_icon() {
  local icon="$1"
  local destination="$2"
  local pascal
  pascal="$(to_pascal_case "$icon")"
  local filled="$WORK_DIR/${icon}_filled.kt"
  local outlined="$WORK_DIR/${icon}_outlined.kt"
  download_vector "$icon" 1 "$filled"
  normalize_vector "$filled" "$icon" Filled

  {
    printf 'package %s\n\n' "$PACKAGE_NAME"
    printf '%s\n' '// Generated from Google Material Symbols Rounded. Do not edit by hand.'
    sed -n '/^import /p' "$filled"
    printf '\n'
    vector_body "$filled"
    if [[ " ${OUTLINED_ICONS[*]} " == *" $icon "* ]]; then
      download_vector "$icon" 0 "$outlined"
      normalize_vector "$outlined" "$icon" Outlined
      printf '\n'
      vector_body "$outlined"
    fi
  } > "$destination/${pascal}Icon.kt"
}

generate_facade() {
  local destination="$1/TrailSenseIcons.kt"
  {
    printf 'package %s\n\n' "$PACKAGE_NAME"
    printf 'import androidx.compose.ui.graphics.vector.ImageVector\n\n'
    printf 'object TrailSenseIcons {\n'
    printf '    object Filled {\n'
    for icon in "${ICONS[@]}"; do
      pascal="$(to_pascal_case "$icon")"
      printf '        val %s: ImageVector get() = Filled%s\n' "$pascal" "$pascal"
    done
    printf '    }\n\n    object Outlined {\n'
    for icon in "${OUTLINED_ICONS[@]}"; do
      pascal="$(to_pascal_case "$icon")"
      printf '        val %s: ImageVector get() = Outlined%s\n' "$pascal" "$pascal"
    done
    printf '    }\n}\n'
  } > "$destination"
}

readonly WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/trail-sense-icons.XXXXXX")"
readonly STAGING_DIR="$WORK_DIR/output"
trap 'rm -rf "$WORK_DIR"' EXIT
mkdir -p "$STAGING_DIR"

for icon in "${ICONS[@]}"; do
  generate_icon "$icon" "$STAGING_DIR"
done
generate_facade "$STAGING_DIR"

if [[ "$CHECK_ONLY" -eq 1 ]]; then
  for staged in "$STAGING_DIR"/*.kt; do
    destination="$OUTPUT_DIR/${staged##*/}"
    cmp -s "$staged" "$destination" || {
      printf 'Generated icon source is stale: %s\n' "$destination" >&2
      exit 1
    }
  done
  exit 0
fi

mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.kt
mv "$STAGING_DIR"/*.kt "$OUTPUT_DIR/"
printf 'Generated %d Material Symbols in %s\n' "${#ICONS[@]}" "$OUTPUT_DIR"

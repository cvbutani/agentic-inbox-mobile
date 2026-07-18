#!/usr/bin/env bash
# Bumps the app version across Android and iOS.
#
# Usage: scripts/bump_version.sh <major|minor|bugfix|build-number|none>
#
# build-number leaves the semver versionName untouched and only increments
# versionCode / CURRENT_PROJECT_VERSION (e.g. re-uploading the same version).
#
# - gradle.properties: versionName (semver) and versionCode (+1)
# - iosApp/iosApp.xcodeproj/project.pbxproj: MARKETING_VERSION and
#   CURRENT_PROJECT_VERSION (the project uses a generated Info.plist,
#   so these build settings are the source of truth for iOS versions)
#
# Writes versionName/versionCode to $GITHUB_OUTPUT when running in CI.
set -euo pipefail

BUMP="${1:?usage: bump_version.sh <major|minor|bugfix|build-number|none>}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/gradle.properties"
PBXPROJ="$ROOT/iosApp/iosApp.xcodeproj/project.pbxproj"

version_name="$(sed -n 's/^versionName=//p' "$PROPS")"
version_code="$(sed -n 's/^versionCode=//p' "$PROPS")"

if [[ ! "$version_name" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "versionName '$version_name' in gradle.properties is not semver (X.Y.Z)" >&2
  exit 1
fi

IFS=. read -r major minor patch <<<"$version_name"

case "$BUMP" in
  major)  major=$((major + 1)); minor=0; patch=0 ;;
  minor)  minor=$((minor + 1)); patch=0 ;;
  bugfix|patch) patch=$((patch + 1)) ;;
  build-number) ;; # versionName stays as-is; only the build number moves
  none) ;;
  *) echo "unknown bump type: $BUMP" >&2; exit 1 ;;
esac

if [[ "$BUMP" != "none" ]]; then
  version_name="$major.$minor.$patch"
  version_code=$((version_code + 1))

  # -i.bak keeps this portable between GNU sed (Linux) and BSD sed (macOS)
  sed -i.bak "s/^versionName=.*/versionName=$version_name/" "$PROPS"
  sed -i.bak "s/^versionCode=.*/versionCode=$version_code/" "$PROPS"
  sed -i.bak -E "s/MARKETING_VERSION = [^;]+;/MARKETING_VERSION = $version_name;/g" "$PBXPROJ"
  sed -i.bak -E "s/CURRENT_PROJECT_VERSION = [^;]+;/CURRENT_PROJECT_VERSION = $version_code;/g" "$PBXPROJ"
  rm -f "$PROPS.bak" "$PBXPROJ.bak"
fi

echo "versionName=$version_name"
echo "versionCode=$version_code"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "versionName=$version_name"
    echo "versionCode=$version_code"
  } >>"$GITHUB_OUTPUT"
fi

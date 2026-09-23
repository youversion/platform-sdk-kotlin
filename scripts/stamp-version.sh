#!/bin/bash
# Stamp a version string into the files that carry the SDK's published version.
#
# Targets:
#   gradle/libs.versions.toml  `youversionPlatform = "..."`  — the version the
#                              three modules publish under (see each module's
#                              `coordinates(...)` call).
#   README.md                  `youVersionPlatform = "..."`  — the install
#                              snippets consumers copy/paste. Both the version
#                              catalog snippet and the plain-Gradle snippet.
#
# Idempotent: works whether the files currently hold a prior release version or
# the version about to be released. Replaces only the literal between the
# double quotes, so re-running is a no-op rather than a corruption.
#
# Called by scripts/release.sh. Usage:
#   bash scripts/stamp-version.sh 2.2.0
set -e

cd "$(dirname "$0")/.."

VERSION=$1

if [ -z "$VERSION" ]; then
  echo "Error: Version parameter is required" >&2
  exit 1
fi

CATALOG="gradle/libs.versions.toml"
README="README.md"

for FILE in "$CATALOG" "$README"; do
  if [ ! -f "$FILE" ]; then
    echo "Error: $FILE not found" >&2
    exit 1
  fi
done

# macOS ships BSD sed, which requires an argument to -i; GNU sed rejects one.
# Wrap the difference here so the script runs identically on a dev machine and
# on the ubuntu-latest runner.
sed_inplace() {
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "$@"
  else
    sed -i "$@"
  fi
}

echo "Stamping version $VERSION..."

# `[^"]*` rather than `.*`: a greedy `.*` would swallow everything up to the
# LAST quote on the line, which eats a trailing comment such as `# .get()`.
sed_inplace "s/youversionPlatform = \"[^\"]*\"/youversionPlatform = \"$VERSION\"/" "$CATALOG"
sed_inplace "s/youVersionPlatform = \"[^\"]*\"/youVersionPlatform = \"$VERSION\"/g" "$README"

if ! grep -q "youversionPlatform = \"$VERSION\"" "$CATALOG"; then
  echo "Error: Failed to stamp version into $CATALOG" >&2
  exit 1
fi

# Every README occurrence must have moved; a leftover means the snippet drifted
# (renamed variable, different quoting) and consumers would copy a stale version.
STALE=$(grep -c "youVersionPlatform = \"" "$README" | tr -d ' ')
FRESH=$(grep -c "youVersionPlatform = \"$VERSION\"" "$README" | tr -d ' ')
if [ "$STALE" != "$FRESH" ]; then
  echo "Error: $README has $STALE 'youVersionPlatform = \"...\"' lines but only $FRESH read \"$VERSION\"" >&2
  grep -n "youVersionPlatform = \"" "$README" >&2
  exit 1
fi
if [ "$FRESH" = "0" ]; then
  echo "Error: no 'youVersionPlatform = \"...\"' line found in $README" >&2
  exit 1
fi

echo "  ✓ $CATALOG: $(grep 'youversionPlatform = ' "$CATALOG")"
echo "  ✓ $README: $FRESH occurrence(s) now read \"$VERSION\""
echo "Version stamp complete."

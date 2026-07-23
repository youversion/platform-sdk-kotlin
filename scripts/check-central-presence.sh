#!/usr/bin/env bash
#
# check-central-presence.sh — pre-publish idempotency probe for Maven Central.
#
# HEADs the .pom URL on repo1.maven.org for each module/version combination and
# reports which modules are missing. The release workflow uses this to skip
# publishing for coordinates that already exist (so a re-dispatch of a partially
# failed release republishes only the missing modules).
#
# Output contract:
#   - Writes a human-readable summary to stdout.
#   - When $GITHUB_OUTPUT is set, appends `missing_modules=<csv>` for downstream jobs.
#   - Exit 0 always (presence/absence is data, not failure). The caller decides.
#
# Usage:
#   scripts/check-central-presence.sh <version> <module> [<module>...]
#
# Example:
#   scripts/check-central-presence.sh 1.6.0 platform-core platform-ui platform-reader

set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "usage: $0 <version> <module> [<module>...]" >&2
    exit 2
fi

VERSION="$1"
shift

GROUP_PATH="com/youversion/platform"
BASE_URL="https://repo1.maven.org/maven2"
MAX_ATTEMPTS=3

# Treat 200 as present and 404 as missing. Anything else is a transient signal
# (rate-limit, edge node hiccup); retry a few times before declaring missing so
# we do not republish a version that is actually already on Central.
status_for() {
    local module="$1"
    local url="${BASE_URL}/${GROUP_PATH}/${module}/${VERSION}/${module}-${VERSION}.pom"
    local attempt=1
    local code
    while (( attempt <= MAX_ATTEMPTS )); do
        code=$(curl -sI -o /dev/null -w '%{http_code}' --max-time 15 "$url" || echo "000")
        case "$code" in
            200|404)
                echo "$code"
                return 0
                ;;
        esac
        sleep $(( attempt * 2 ))
        attempt=$(( attempt + 1 ))
    done
    echo "$code"
}

missing=()
present=()
inconclusive=()

for module in "$@"; do
    code=$(status_for "$module")
    case "$code" in
        200)
            present+=("$module")
            echo "  [present]      ${module}-${VERSION}.pom"
            ;;
        404)
            missing+=("$module")
            echo "  [missing]      ${module}-${VERSION}.pom"
            ;;
        *)
            # Inconclusive after retries; treat as missing so the workflow attempts
            # publish — Central will reject if it actually exists, surfacing the
            # error rather than silently skipping.
            missing+=("$module")
            inconclusive+=("$module(http=${code})")
            echo "  [inconclusive] ${module}-${VERSION}.pom (http=${code}, treating as missing)"
            ;;
    esac
done

echo
echo "version:           ${VERSION}"
echo "present_modules:   $(IFS=,; echo "${present[*]-}")"
echo "missing_modules:   $(IFS=,; echo "${missing[*]-}")"
if (( ${#inconclusive[@]} > 0 )); then
    echo "inconclusive:      $(IFS=,; echo "${inconclusive[*]}")"
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    {
        echo "missing_modules=$(IFS=,; echo "${missing[*]-}")"
        echo "present_modules=$(IFS=,; echo "${present[*]-}")"
        echo "all_present=$([[ ${#missing[@]} -eq 0 ]] && echo true || echo false)"
    } >> "$GITHUB_OUTPUT"
fi

#!/usr/bin/env bash
#
# verify-release.sh — smoke-test the SDK release pipeline without publishing.
#
# Tier 1: build platform-core's AAR with -PsdkVersion=<test version> and
#         confirm the value is baked into BuildConfig.SDK_VERSION.
# Tier 2: parse .releaserc.json and confirm the semantic-release publishCmd
#         passes -PsdkVersion=${nextRelease.version} to Gradle.
#
# Usage:
#   scripts/verify-release.sh [--version <version>] [--with-dry-run]
#
# --with-dry-run additionally runs `npx semantic-release --dry-run` against
# the current branch. This is best-effort and may skip if the branch has no
# release-triggering commits.

set -euo pipefail

TEST_VERSION="1.99.0-verify.local"
WITH_DRY_RUN=0
while (( "$#" )); do
    case "$1" in
        --version) TEST_VERSION="$2"; shift 2 ;;
        --with-dry-run) WITH_DRY_RUN=1; shift ;;
        -h|--help) sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
done

cd "$(git rev-parse --show-toplevel)"

readonly AAR_PATH="platform-core/build/outputs/aar/platform-core-release.aar"
readonly BUILD_CONFIG_CLASS="com/youversion/platform/core/BuildConfig.class"

failed=0
step()  { printf "\n==> %s\n" "$1"; }
pass()  { printf "    \033[32mPASS\033[0m  %s\n" "$1"; }
report_fail() { printf "    \033[31mFAIL\033[0m  %s\n" "$1"; failed=1; }
skip_step()   { printf "    \033[33mSKIP\033[0m  %s\n" "$1"; }

# --- Tier 1: AAR contents ---
step "Building platform-core AAR with -PsdkVersion=$TEST_VERSION"
./gradlew :platform-core:bundleReleaseAar -PsdkVersion="$TEST_VERSION" -q

step "Inspecting BuildConfig.SDK_VERSION inside the AAR"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
unzip -q -p "$AAR_PATH" classes.jar > "$tmp/classes.jar"
unzip -q -p "$tmp/classes.jar" "$BUILD_CONFIG_CLASS" > "$tmp/BuildConfig.class"
baked="$(javap -constants "$tmp/BuildConfig.class" \
    | sed -n 's/.*SDK_VERSION = "\(.*\)";/\1/p')"
if [[ "$baked" == "$TEST_VERSION" ]]; then
    pass "BuildConfig.SDK_VERSION = \"$baked\""
else
    report_fail "BuildConfig.SDK_VERSION = \"$baked\" (expected \"$TEST_VERSION\")"
fi

# --- Tier 2: .releaserc.json publishCmd template ---
step "Verifying .releaserc.json publishCmd template"
publish_cmd="$(node -e '
    const c = require("./.releaserc.json");
    const exec = c.plugins.find(p => Array.isArray(p) && p[0] === "@semantic-release/exec");
    process.stdout.write((exec && exec[1] && exec[1].publishCmd) || "");
')"
if [[ "$publish_cmd" == *'publishToMavenCentral'* && \
      "$publish_cmd" == *'-PsdkVersion=${nextRelease.version}'* ]]; then
    pass "publishCmd: $publish_cmd"
else
    report_fail "publishCmd missing expected pieces (got: '$publish_cmd')"
fi

# --- Optional: live semantic-release dry-run ---
if (( WITH_DRY_RUN )); then
    step "Running semantic-release --dry-run on current branch"
    [[ -d node_modules ]] || npm ci --silent
    branch="$(git rev-parse --abbrev-ref HEAD)"
    if out="$(npx --no-install semantic-release --dry-run --no-ci \
                  --branches="$branch" 2>&1)"; then
        planned="$(grep -oE 'publishToMavenCentral -PsdkVersion=\S+' <<< "$out" | head -1 || true)"
        if [[ -n "$planned" ]]; then
            pass "planned: $planned"
        else
            skip_step "no publish command surfaced (likely no release-triggering commits on '$branch')"
        fi
    else
        skip_step "semantic-release could not compute a release on '$branch'"
    fi
fi

echo
if (( failed )); then
    printf "\033[31m==> verify-release.sh: failed.\033[0m\n"
    exit 1
fi
printf "\033[32m==> verify-release.sh: all checks passed.\033[0m\n"

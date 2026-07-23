#!/usr/bin/env bash
#
# gradle-publish-wrapper.sh — runs `./gradlew publishToMavenCentral` for one or
# more modules and classifies failures so the workflow can fast-fail on
# unrecoverable conditions (expired GPG key, missing passphrase) instead of
# retrying transient-looking errors that will never succeed.
#
# Exit codes:
#   0   publish succeeded
#   42  GPG signing failure — KEY/PASSPHRASE/EXPIRED. Retry will not help; see runbook.
#   1   other failure (transient or unclassified; retry is reasonable)
#
# Usage:
#   scripts/gradle-publish-wrapper.sh <version> <module> [<module>...]
#
# Required env (set by release.yml):
#   ORG_GRADLE_PROJECT_mavenCentralUsername
#   ORG_GRADLE_PROJECT_mavenCentralPassword
#   ORG_GRADLE_PROJECT_signingInMemoryKey
#   ORG_GRADLE_PROJECT_signingInMemoryKeyPassword

set -uo pipefail

if [[ $# -lt 2 ]]; then
    echo "usage: $0 <version> <module> [<module>...]" >&2
    exit 2
fi

VERSION="$1"
shift

# Build a Gradle task list of the form ":platform-core:publishToMavenCentral …"
# so each module publishes independently and a single failure does not abort
# the others.
tasks=()
for module in "$@"; do
    tasks+=(":${module}:publishToMavenCentral")
done

log_file=$(mktemp -t gradle-publish.XXXXXX.log)
trap 'rm -f "$log_file"' EXIT

echo "==> ./gradlew ${tasks[*]} -PsdkVersion=${VERSION} --continue"
set +e
./gradlew "${tasks[@]}" -PsdkVersion="${VERSION}" --continue 2>&1 | tee "$log_file"
status=${PIPESTATUS[0]}
set -e

if (( status == 0 )); then
    echo "==> publish succeeded"
    exit 0
fi

# Classify the failure. Patterns are deliberately broad — we would rather
# fast-fail on a false-positive GPG match than retry an unrecoverable one.
if grep -E -i -q \
    -e 'signingInMemoryKey' \
    -e 'Could not read PGP secret key' \
    -e 'Bad passphrase' \
    -e 'Cannot perform signing task' \
    -e 'No suitable secret key was found' \
    -e 'gpg.*expired' \
    -e 'task .*sign[A-Za-z]*Publication.* FAILED' \
    "$log_file"; then
    echo "==> GPG signing failure detected — re-dispatch will not help."
    if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
        {
            echo "### ❌ GPG signing failure"
            echo
            echo "The publish task failed during artifact signing. This is unrecoverable until the signing secrets are fixed — re-dispatching this workflow will fail the same way."
            echo
            echo "See [docs/RELEASE-RUNBOOK.md → GPG signing failure](../blob/main/docs/RELEASE-RUNBOOK.md#gpg-signing-failure-expired-key--missing-passphrase) for the rotation steps."
        } >> "$GITHUB_STEP_SUMMARY"
    fi
    exit 42
fi

echo "==> publish failed (exit ${status}); retry may help"
exit 1

#!/usr/bin/env bash
#
# gradle-publish-wrapper.sh — runs `./gradlew publishToMavenCentral` for ONE
# module and classifies the outcome so the caller can fast-fail on
# unrecoverable conditions (expired GPG key, missing passphrase) instead of
# retrying errors that will never succeed.
#
# One module per invocation is deliberate. A single Gradle run publishing all
# three modules produces one interleaved log, so a real signing failure on one
# module and an already-published no-op on another are indistinguishable. The
# caller (scripts/release.sh) loops.
#
# Exit codes:
#   0   publish succeeded, or the coordinate is already on Maven Central
#   42  GPG signing failure — KEY/PASSPHRASE/EXPIRED. Retry will not help; see runbook.
#   1   other failure (transient or unclassified; retry is reasonable)
#
# Usage:
#   scripts/gradle-publish-wrapper.sh <version> <module>
#
# Required env (set by release.yml):
#   ORG_GRADLE_PROJECT_mavenCentralUsername
#   ORG_GRADLE_PROJECT_mavenCentralPassword
#   ORG_GRADLE_PROJECT_signingInMemoryKey
#   ORG_GRADLE_PROJECT_signingInMemoryKeyPassword

set -uo pipefail

if [[ $# -ne 2 ]]; then
    echo "usage: $0 <version> <module>" >&2
    exit 2
fi

VERSION="$1"
MODULE="$2"
task=":${MODULE}:publishToMavenCentral"

log_file=$(mktemp -t gradle-publish.XXXXXX.log)
trap 'rm -f "$log_file"' EXIT

echo "==> ./gradlew ${task} -PsdkVersion=${VERSION}"
set +e
./gradlew "${task}" -PsdkVersion="${VERSION}" 2>&1 | tee "$log_file"
status=${PIPESTATUS[0]}
set -e

if (( status == 0 )); then
    echo "==> publish succeeded"
    exit 0
fi

# Maven Central is immutable: a coordinate that already exists cannot be
# republished, and the rejection means the artifact this run wanted to ship is
# already there. That is the desired end state, so it is success — otherwise a
# re-dispatch after a partially-successful run could never get past the modules
# that did publish. Checked before the GPG patterns because an already-existing
# component is unambiguous.
if grep -E -i -q \
    -e 'already exists' \
    -e 'Component with package url .* already exists' \
    "$log_file"; then
    echo "==> ${MODULE} ${VERSION} is already on Maven Central — treating as success."
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

#!/usr/bin/env bash
#
# gradle-publish-wrapper.sh — publishes the SDK's modules to Maven Central and
# classifies the outcome so the caller can fast-fail on unrecoverable
# conditions (expired GPG key, missing passphrase) instead of retrying errors
# that will never succeed.
#
# Exit codes:
#   0   publish succeeded, or every coordinate is already on Maven Central
#   42  GPG signing failure — KEY/PASSPHRASE/EXPIRED. Retry will not help; see runbook.
#   1   other failure (transient or unclassified; retry is reasonable)
#
# Usage:
#   scripts/gradle-publish-wrapper.sh <version> <modules-csv> <group>
#
# Required env (set by release.yml):
#   ORG_GRADLE_PROJECT_mavenCentralUsername
#   ORG_GRADLE_PROJECT_mavenCentralPassword
#   ORG_GRADLE_PROJECT_signingInMemoryKey
#   ORG_GRADLE_PROJECT_signingInMemoryKeyPassword

set -uo pipefail

if [[ $# -ne 3 ]]; then
    echo "usage: $0 <version> <modules-csv> <group>" >&2
    exit 2
fi

VERSION="$1"
MODULES_CSV="$2"
GROUP="$3"
GROUP_PATH="${GROUP//.//}"

pom_url() {
    echo "https://repo1.maven.org/maven2/${GROUP_PATH}/$1/${VERSION}/$1-${VERSION}.pom"
}

is_on_central() {
    curl -fsS -I --max-time 30 "$(pom_url "$1")" >/dev/null 2>&1
}

IFS=',' read -ra ALL_MODULES <<< "$MODULES_CSV"

PRESENT=()
MISSING=()
for module in "${ALL_MODULES[@]}"; do
    module="${module// /}"
    [[ -z "$module" ]] && continue
    if is_on_central "$module"; then
        PRESENT+=("$module")
    else
        MISSING+=("$module")
    fi
done

if [[ ${#MISSING[@]} -eq 0 ]]; then
    echo "==> ${VERSION} is already on Maven Central for every module — nothing to publish."
    exit 0
fi

if [[ ${#PRESENT[@]} -gt 0 ]]; then
    echo "==> WARNING: ${VERSION} is already partially published." >&2
    echo "==>   on Central: ${PRESENT[*]}" >&2
    echo "==>   missing:    ${MISSING[*]}" >&2
    echo "==> Central coordinates are immutable, so this version can no longer ship as one" >&2
    echo "==> deployment. Publishing the missing modules to restore version parity." >&2
fi

tasks=()
for module in "${MISSING[@]}"; do
    tasks+=(":${module}:publishToMavenCentral")
done

log_file=$(mktemp -t gradle-publish.XXXXXX.log)
trap 'rm -f "$log_file"' EXIT

CLOSE_TIMEOUT="${CENTRAL_CLOSE_TIMEOUT_SECONDS:-2700}"

echo "==> ./gradlew ${tasks[*]} -PsdkVersion=${VERSION} -PSONATYPE_CLOSE_TIMEOUT_SECONDS=${CLOSE_TIMEOUT}"
set +e
./gradlew "${tasks[@]}" \
    -PsdkVersion="${VERSION}" \
    -PSONATYPE_CLOSE_TIMEOUT_SECONDS="${CLOSE_TIMEOUT}" \
    2>&1 | tee "$log_file"
status=${PIPESTATUS[0]}
set -e

if (( status == 0 )); then
    echo "==> publish succeeded"
    exit 0
fi

# Maven Central is immutable: a coordinate that already exists cannot be
# republished, and the rejection means the artifacts this run wanted to ship are
# already there. That is the desired end state, so it is success — otherwise a
# re-dispatch after a deployment that reached Central but timed out locally
# could never get past it. Checked before the GPG patterns because an
# already-existing component is unambiguous.
#
# The log phrase is only the trigger; repo1 is the proof. A bare "already
# exists" match is not enough, because Gradle emits that string for unrelated
# configuration errors ("Cannot add task 'x' as a task with that name already
# exists") and this grep runs over the whole log. Treating one of those as a
# publish no-op would exit 0 here, and release.sh would cut a GitHub release for
# modules that never reached Central.
#
# A 404 does not disprove the deployment — the Central Portal syncs to repo1 on
# a lag of minutes — so an unconfirmed match exits 1 (retryable) rather than
# claiming a publish it cannot see.
CENTRAL_ALREADY_EXISTS_RE='Component with package url.* already exists'

if grep -E -i -q -e "$CENTRAL_ALREADY_EXISTS_RE" "$log_file"; then
    unresolved=()
    for module in "${MISSING[@]}"; do
        is_on_central "$module" || unresolved+=("$module")
    done
    if [[ ${#unresolved[@]} -eq 0 ]]; then
        echo "==> ${VERSION} is already on Maven Central — treating as success."
        exit 0
    fi
    echo "==> Central reported the components exist, but these are not resolvable yet: ${unresolved[*]}" >&2
    echo "==> Not claiming success; retry once Central sync completes." >&2
    exit 1
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

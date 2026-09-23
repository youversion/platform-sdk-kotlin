#!/usr/bin/env bash
#
# poll-central-index.sh — post-publish indexing observer.
#
# After the Central Portal reports a deployment as released, the artifacts
# typically appear on repo1.maven.org (the resolvable dependency host) within
# a few minutes, but the propagation can take up to ~30 minutes. This script
# polls repo1 until every requested module's .pom is fetchable or the timeout
# elapses.
#
# repo1.maven.org is the source of truth for Gradle/Maven dependency
# resolution. search.maven.org (the discovery search index) is a separate,
# slower system and is intentionally not used here — Portal-published
# artifacts can take hours or never appear in that index even when they are
# fully resolvable.
#
# Output contract:
#   - Writes human-readable progress to stdout.
#   - When $GITHUB_STEP_SUMMARY is set, appends a markdown table of per-module
#     status plus the standard indexing-delay note.
#   - Exit 0 when every module resolves; exit 1 if any is still missing at the
#     deadline.
#
# Usage:
#   scripts/poll-central-index.sh <version> <module> [<module>...]
#
# Optional env:
#   POLL_INTERVAL_SECONDS (default 60)
#   POLL_MAX_SECONDS      (default 1800)

set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "usage: $0 <version> <module> [<module>...]" >&2
    exit 2
fi

VERSION="$1"
shift
MODULES=("$@")

INTERVAL="${POLL_INTERVAL_SECONDS:-60}"
DEADLINE_SECONDS="${POLL_MAX_SECONDS:-1800}"
GROUP_PATH="com/youversion/platform"
BASE_URL="https://repo1.maven.org/maven2"

is_resolvable() {
    local module="$1"
    local url="${BASE_URL}/${GROUP_PATH}/${module}/${VERSION}/${module}-${VERSION}.pom"
    local code
    code=$(curl -sI -o /dev/null -w '%{http_code}' --max-time 15 "$url" || echo "000")
    [[ "$code" == "200" ]]
}

declare -A status
for m in "${MODULES[@]}"; do status["$m"]=pending; done

start=$(date +%s)
while :; do
    all_done=true
    for module in "${MODULES[@]}"; do
        if [[ "${status[$module]}" == "resolvable" ]]; then
            continue
        fi
        if is_resolvable "$module"; then
            status["$module"]=resolvable
            echo "  [resolvable]   ${module} ${VERSION}"
        else
            all_done=false
        fi
    done

    if $all_done; then
        break
    fi

    elapsed=$(( $(date +%s) - start ))
    if (( elapsed >= DEADLINE_SECONDS )); then
        break
    fi

    echo "  ...waiting ${INTERVAL}s (elapsed ${elapsed}s of ${DEADLINE_SECONDS}s)"
    sleep "$INTERVAL"
done

resolvable_count=0
for module in "${MODULES[@]}"; do
    if [[ "${status[$module]}" == "resolvable" ]]; then
        resolvable_count=$(( resolvable_count + 1 ))
    fi
done

echo
echo "resolvable:        ${resolvable_count}/${#MODULES[@]}"
for module in "${MODULES[@]}"; do
    echo "  ${module}: ${status[$module]}"
done

if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    {
        echo "### Maven Central resolution status — \`${VERSION}\`"
        echo
        echo "| Module | repo1.maven.org |"
        echo "|---|---|"
        for module in "${MODULES[@]}"; do
            local_status="${status[$module]}"
            icon=$([[ "$local_status" == "resolvable" ]] && echo "✅" || echo "⏳")
            echo "| \`com.youversion.platform:${module}\` | ${icon} ${local_status} |"
        done
        echo
        if (( resolvable_count < ${#MODULES[@]} )); then
            echo "**\`${VERSION}\` did not become fully resolvable within $(( DEADLINE_SECONDS / 60 )) minutes.** The modules are released as a single deployment and pin each other at the same version, so a version that resolves for only some of them breaks consumers."
            echo
            echo "repo1.maven.org usually propagates within a few minutes but can take up to ~30. If Central Portal shows the deployment as RELEASED, this is mirror lag and the modules should appear shortly — see [docs/RELEASE-RUNBOOK.md](../blob/main/docs/RELEASE-RUNBOOK.md#stuck-released-not-indexed) for what to tell consumers in the meantime."
            echo
            echo "_Note: search.maven.org (the discovery index) is a separate, slower system and is not monitored here — consumers using Gradle/Maven dependency resolution only need repo1._"
        fi
    } >> "$GITHUB_STEP_SUMMARY"
fi

if (( resolvable_count < ${#MODULES[@]} )); then
    echo
    echo "❌ ${VERSION} is resolvable for only ${resolvable_count} of ${#MODULES[@]} modules after ${DEADLINE_SECONDS}s." >&2
    echo "   The modules pin each other at the same version, so a partially resolvable version breaks consumers." >&2
    echo "   See docs/RELEASE-RUNBOOK.md#stuck-released-not-indexed." >&2
    exit 1
fi

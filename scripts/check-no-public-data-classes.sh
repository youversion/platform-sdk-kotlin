#!/usr/bin/env bash
# Fails if a public data class appears in the given .api dumps (default: all of them).
#
# A data class's generated copy and componentN members change whenever a property is added, so adding a field to a
# public one breaks binary compatibility. The dumps have no "data" keyword, so these generated members are the signal.
# Run after apiCheck, which is what guarantees the dumps match the code.
set -euo pipefail

if [[ $# -eq 0 ]]; then
  set -- platform-*/api/*.api
fi

OFFENDERS="$(
  awk '
    /^public .*class / { for (i = 1; i <= NF; i++) if ($i == "class") current = $(i + 1) }
    /fun (component1(-[^ ]*)?|copy(-[^ ]*)?\$default) \(/ { print FILENAME ": " current }
  ' "$@" | sort -u
)"

if [[ -n "$OFFENDERS" ]]; then
  echo "Public data classes found in the API dumps:"
  echo "$OFFENDERS"
  echo "Use a plain class with hand-written equals, hashCode and toString, or make the type non-public."
  exit 1
fi

echo "No public data classes in the API dumps."

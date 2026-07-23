#!/usr/bin/env bash
set -euo pipefail

BASE_SHA="${1:-}"
HEAD_SHA="${2:-}"
PR_AUTHOR="${3:-}"
ALLOWLIST_FILE="${4:-config/i18n/localization-bot-allowlist.txt}"

if [[ -z "$BASE_SHA" || -z "$HEAD_SHA" ]]; then
  echo "Usage: $0 <base-sha> <head-sha> <pr-author-login> [allowlist-file]"
  exit 1
fi

if [[ -z "$PR_AUTHOR" ]]; then
  echo "PR author login is required."
  exit 1
fi

if [[ ! -f "$ALLOWLIST_FILE" ]]; then
  echo "Allowlist file not found: $ALLOWLIST_FILE"
  exit 1
fi

mapfile -t ALLOWED_AUTHORS < <(
  grep -v '^\s*#' "$ALLOWLIST_FILE" | grep -v '^\s*$' || true
)

for allowed in "${ALLOWED_AUTHORS[@]}"; do
  if [[ "$PR_AUTHOR" == "$allowed" ]]; then
    echo "PR author '$PR_AUTHOR' is allowed to modify synced localization files."
    exit 0
  fi
done

mapfile -t CHANGED_FILES < <(git diff --name-only "$BASE_SHA" "$HEAD_SHA")

PROTECTED_CHANGED=()
for file in "${CHANGED_FILES[@]}"; do
  if [[ "$file" == "platform-ui/src/main/res/values/strings_i18n.xml" ]] || \
     [[ "$file" == "platform-reader/src/main/res/values/strings_i18n.xml" ]] || \
     [[ "$file" =~ ^platform-(ui|reader)/src/main/res/values-[^/]+/strings\.xml$ ]]; then
    PROTECTED_CHANGED+=("$file")
  fi
done

if [[ ${#PROTECTED_CHANGED[@]} -eq 0 ]]; then
  echo "No protected localization files modified."
  exit 0
fi

echo "Protected localization files were modified by '$PR_AUTHOR' (not in allowlist):"
for file in "${PROTECTED_CHANGED[@]}"; do
  echo "  - $file"
done
echo
echo "Synced string catalogs must be updated via platform-localization and the sync workflow."
echo "Allowed authors are listed in $ALLOWLIST_FILE"
exit 1

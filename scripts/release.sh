#!/bin/bash
# Manual version-input release orchestrator.
#
# Called by .github/workflows/release.yml after `workflow_dispatch` with
# `inputs.version`. Does not run semantic-release. The commit analyzer and the
# release-notes generator are still used, but as *libraries* — via
# scripts/preview-release.mjs and scripts/generate-release-notes.mjs —
# so orchestration is local: no env-ci, no verifyAuth, no plugin lifecycle.
#
# Why this exists (the alternative considered and rejected was keeping
# semantic-release as the runner):
# - semantic-release has no hook to override the calculated version. Its
#   version is `semver.inc(lastRelease.version, type)` and nothing can
#   intercept it. The only way to ship a version that differs from what the
#   analyzer computes is to engineer the commit history, which is brittle and
#   invisible at the point of decision.
# - This script makes the chosen version an explicit input. The analyzer's
#   value is still computed and logged side-by-side for audit, but the
#   operator's input wins.
# - Everything semantic-release gave us that we actually want — conventional
#   -commit analysis, grouped release notes, the changelog entry, the tag, the
#   GitHub release — is retained. What is dropped is the orchestration we were
#   fighting.
#
# Vocabulary (see RELEASING.md § Vocabulary):
#   chosen version      what the operator typed; this is what ships
#   calculated version  what the analyzer computed; advisory only
#   fresh run           no remote tag, and main does not already carry this
#                       version's release commit
#   resume              remote tag already exists, or main already carries this
#                       version's release commit; auto-detected, not an input
#   rehearsal           DRY_RUN=1; stops after the local commit + tag
#
# Fresh-run pre-conditions (enforced below):
# - VERSION env var: the chosen target version.
# - HEAD = origin/main HEAD.
# - Working tree clean.
# - Git identity configured.
# - SSH remote configured for push (the workflow's deploy key).
# - GH_TOKEN (or GITHUB_TOKEN) exported for `gh release create`.
# - Maven Central + signing credentials exported for the publish step.
#
# Resume mode (auto-detected): if tag $VERSION already exists on origin with a
# tree whose gradle/libs.versions.toml is stamped to $VERSION, the script
# treats this as a re-dispatch after a partial run. It also resumes when main
# already carries this version's `chore(release)` commit but origin has no tag,
# adopting that commit rather than building a second one. It checks out the
# tag, regenerates the release notes, and proceeds to the idempotent post-tag
# steps (push, publish, GitHub release), each of which detects and skips
# already-completed work. See docs/RELEASE-RUNBOOK.md for failure-mode-by-
# failure-mode recovery details.
#
# Post-conditions on success:
# - Tag $VERSION points at the `chore(release)` commit that stamped the version
#   catalog + README and prepended the CHANGELOG entry.
# - main HEAD is that same commit.
# - All modules in $PUBLISHABLE_MODULES are on Maven Central at $VERSION.
# - GitHub release created from the generated notes.
#
# There is no "restore to Dev" commit. `gradle/libs.versions.toml` holds the
# last released version by design (it is what the README snippets and the
# three `coordinates(...)` calls resolve to), and `BuildConfig.SDK_VERSION`
# comes from `-PsdkVersion`, which is unset outside a release build and so
# falls back to "Dev" on its own.
#
# Local usage (validates and stops before push):
#   VERSION=2.2.0 DRY_RUN=1 bash scripts/release.sh
#
# CI usage:
#   VERSION=2.2.0 bash scripts/release.sh

set -euo pipefail

cd "$(dirname "$0")/.."

VERSION="${VERSION:-}"
DRY_RUN="${DRY_RUN:-0}"
PUBLISHABLE_MODULES="${PUBLISHABLE_MODULES:-platform-core,platform-ui,platform-reader}"

if [ -z "$VERSION" ]; then
  echo "❌ VERSION env var is required" >&2
  exit 1
fi

CATALOG="gradle/libs.versions.toml"

CURRENT_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "0.0.0")
echo "Current tag:    $CURRENT_TAG"
echo "Chosen version: $VERSION"

VALIDATION_CODE=0
node scripts/release-validate.mjs "$VERSION" "$CURRENT_TAG" || VALIDATION_CODE=$?

# In resume mode, $VERSION will equal $CURRENT_TAG (the tag already exists),
# which release-validate.mjs rejects with code 12 ("not strictly greater").
# Detect resume *before* failing on that, so re-dispatch with the same version
# recovers a partial run instead of exiting.
REMOTE_TAG_SHA=$(git ls-remote origin "refs/tags/$VERSION" 2>/dev/null | awk '{print $1}')
RESUME=0
if [ -n "$REMOTE_TAG_SHA" ]; then
  RESUME=1
elif [ "$(git log -1 --format=%s HEAD)" = "chore(release): $VERSION [skip ci]" ]; then
  # Split push: main landed, the tag did not. Subject match, not the version
  # stamp — the stamp persists into every later commit.
  echo "🔁 HEAD is already the $VERSION release commit with no remote tag — adopting it."
  git rev-parse "refs/tags/$VERSION" >/dev/null 2>&1 || git tag "$VERSION" HEAD
  RESUME=1
fi

if [ "$VALIDATION_CODE" -eq 11 ]; then
  echo "❌ '$VERSION' is not a bare release version (expected e.g. 2.2.0)." >&2
  echo "   No leading \"v\", prerelease, or build metadata — it becomes the tag and the Maven coordinate verbatim." >&2
  exit 1
elif [ "$VALIDATION_CODE" -eq 12 ] && [ "$RESUME" = "0" ]; then
  echo "❌ '$VERSION' is not strictly greater than current tag '$CURRENT_TAG'" >&2
  exit 1
elif [ "$VALIDATION_CODE" -ne 0 ] && [ "$VALIDATION_CODE" -ne 12 ]; then
  echo "❌ Version validation failed (exit $VALIDATION_CODE)" >&2
  exit 1
fi

PREVIEW_JSON=$(node scripts/preview-release.mjs --base "$CURRENT_TAG" --head HEAD 2>/dev/null || echo '{}')
CALCULATED=$(printf '%s' "$PREVIEW_JSON" | node scripts/read-preview-field.mjs --default unknown next current)
CALC_TYPE=$(printf '%s' "$PREVIEW_JSON" | node scripts/read-preview-field.mjs --default none release_type)
echo "Calculated:     $CALCULATED ($CALC_TYPE)"

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "## Release \`$VERSION\`"
    echo
    echo "| Source            | Version            |"
    echo "| ----------------- | ------------------ |"
    echo "| Current tag       | \`$CURRENT_TAG\`   |"
    echo "| Analyzer-computed | \`$CALCULATED\` ($CALC_TYPE) |"
    echo "| Chosen (input)    | **\`$VERSION\`**   |"
    if [ "$RESUME" = "1" ]; then
      echo
      if [ -n "$REMOTE_TAG_SHA" ]; then
        echo "> 🔁 **Resume** — tag \`$VERSION\` already exists on origin. Already-completed phases will be skipped."
      else
        echo "> 🔁 **Resume** — main already carries the \`$VERSION\` release commit but origin has no tag. Adopting that commit; already-completed phases will be skipped."
      fi
    fi
    if [ "$DRY_RUN" = "1" ]; then
      echo
      echo "> 🧪 **Rehearsal (dry-run)** — will stop before push, publish, and release."
    fi
  } >> "$GITHUB_STEP_SUMMARY"
fi

# Warn (don't block) if the chosen version is more than one major above the
# calculated one. Skip in resume mode — that decision was made on the first run.
if [ "$RESUME" = "0" ]; then
  node scripts/release-warn-version-jump.mjs "$VERSION" "$CALCULATED" >&2 || true
fi

# ---------------------------------------------------------------------------
# Resume: adopt the existing tag as the release commit.
#
# Why content-verify the tag: a rogue or mismatched tag (moved by hand, left
# over from a different process) must abort, not silently heal. Healing the
# wrong tag would push unrelated work to consumers.
#
# Why also overlay scripts/ from origin/main: the tag may predate this resume
# logic, so running the tagged tree's scripts would use stale post-tag steps.
# ---------------------------------------------------------------------------
if [ "$RESUME" = "1" ]; then
  echo
  if [ -n "$REMOTE_TAG_SHA" ]; then
    echo "🔁 Resume: tag $VERSION already exists on origin at $REMOTE_TAG_SHA."
  else
    echo "🔁 Resume: adopted the $VERSION release commit already on main."
  fi

  if ! git rev-parse "refs/tags/$VERSION^{}" >/dev/null 2>&1; then
    git fetch origin "refs/tags/$VERSION:refs/tags/$VERSION"
  fi

  TAG_CATALOG_LINE=$(git show "refs/tags/$VERSION:$CATALOG" 2>/dev/null \
    | grep 'youversionPlatform = ' | head -1 || echo "")
  if ! printf '%s' "$TAG_CATALOG_LINE" | grep -qF "youversionPlatform = \"$VERSION\""; then
    echo "❌ Tag $VERSION exists but its tree's $CATALOG does not read \"$VERSION\"." >&2
    echo "   Found: $TAG_CATALOG_LINE" >&2
    echo "   Refusing to resume against a tag whose content does not match. See docs/RELEASE-RUNBOOK.md." >&2
    exit 1
  fi
  echo "  ✓ Tag $VERSION content matches ($CATALOG stamped to $VERSION)."

  git checkout --detach "refs/tags/$VERSION"

  git fetch origin main 2>/dev/null || true
  if git rev-parse --verify origin/main >/dev/null 2>&1; then
    git checkout origin/main -- scripts/
    echo "  ✓ Overlaid scripts/ from origin/main so resume uses the latest post-tag logic."
  fi

  # Regenerate notes over the same commit range the original run used. Using
  # --head $VERSION (the tag) is intentional: commits that landed on main after
  # the release commit must not leak into this release's body.
  PREV_TAG=$(git describe --tags --abbrev=0 "refs/tags/$VERSION^" 2>/dev/null || echo "")
  if [ -z "$PREV_TAG" ]; then
    echo "❌ Could not derive previous tag (parent of $VERSION)." >&2
    exit 1
  fi
  echo "  Regenerating release notes for $VERSION (from $PREV_TAG)..."
  node scripts/generate-release-notes.mjs \
    --base "$PREV_TAG" \
    --head "$VERSION" \
    --version "$VERSION" \
    > notes.md
  echo "  Notes: $(wc -l < notes.md | tr -d ' ') lines."
fi

# ---------------------------------------------------------------------------
# Fresh run: pre-flight, then build the release commit and tag.
# ---------------------------------------------------------------------------
if [ "$RESUME" = "0" ]; then
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "❌ Working tree is dirty — aborting" >&2
    git status --short >&2
    exit 1
  fi

  # Guard against a non-main dispatch landing feature-branch commits on main.
  # `gh workflow run release.yml --ref feature-branch` would pass every other
  # pre-flight, and `git push origin HEAD:main` would then bypass branch
  # protection via the deploy key. Rehearsals are explicitly supported on any
  # branch (no push fires), so the guard is skipped for them.
  if [ "$DRY_RUN" != "1" ]; then
    HEAD_SHA_CHECK=$(git rev-parse HEAD)
    MAIN_SHA_CHECK=$(git rev-parse origin/main 2>/dev/null || echo "")
    if [ -z "$MAIN_SHA_CHECK" ]; then
      echo "❌ origin/main ref not found locally — workflow checkout must use fetch-depth: 0" >&2
      exit 1
    fi
    if [ "$HEAD_SHA_CHECK" != "$MAIN_SHA_CHECK" ]; then
      echo "❌ HEAD ($HEAD_SHA_CHECK) is not at origin/main ($MAIN_SHA_CHECK)" >&2
      echo "   Live releases must be dispatched on the main branch. Re-run with --ref main." >&2
      exit 1
    fi
  fi

  # Tag-already-exists is only a fresh-run failure. Resume mode requires it.
  if git rev-parse "refs/tags/$VERSION" >/dev/null 2>&1; then
    echo "❌ Tag $VERSION already exists locally but not on origin — refusing to fresh-run." >&2
    echo "   If you intended to resume, push the tag first or delete it locally." >&2
    exit 1
  fi

  echo
  echo "Generating release notes..."
  node scripts/generate-release-notes.mjs \
    --base "$CURRENT_TAG" \
    --head HEAD \
    --version "$VERSION" \
    > notes.md
  echo "Notes: $(wc -l < notes.md | tr -d ' ') lines."

  if [ -f CHANGELOG.md ]; then
    # Preserve title + intro; prepend the new entry above the first existing
    # version heading. If there is no existing version heading, append.
    node scripts/prepend-changelog.mjs notes.md CHANGELOG.md
  else
    printf '# Changelog\n\nAll notable changes to this project will be documented in this file.\n\n' > CHANGELOG.md
    cat notes.md >> CHANGELOG.md
  fi
  echo "CHANGELOG.md updated."

  bash scripts/stamp-version.sh "$VERSION"

  git add CHANGELOG.md "$CATALOG" README.md

  # Subject + blank + notes body. [skip ci] prevents push-triggered workflows
  # from re-running on the release commit.
  {
    printf 'chore(release): %s [skip ci]\n\n' "$VERSION"
    cat notes.md
  } > .git/COMMIT_EDITMSG
  git commit -F .git/COMMIT_EDITMSG

  RELEASE_SHA=$(git rev-parse HEAD)
  echo "Release commit created at $RELEASE_SHA."

  git tag "$VERSION"
  echo "Tag $VERSION created at $RELEASE_SHA."
else
  RELEASE_SHA=$(git rev-parse HEAD)
  echo "  ✓ Using existing tag $VERSION at $RELEASE_SHA as the release commit."
fi

if [ "$DRY_RUN" = "1" ]; then
  echo
  echo "DRY_RUN=1 (rehearsal) — stopping before push, publish, GitHub release."
  echo "Inspect locally:"
  echo "  cat notes.md"
  echo "  git log -1 $VERSION"
  echo "  git show $VERSION:$CATALOG | head -5"
  exit 0
fi

# ---------------------------------------------------------------------------
# Push. The remote tag is the sole resume key, so on a fresh run main must
# never land without it. Resume pushes whichever ref is still missing.
# ---------------------------------------------------------------------------
echo
echo "Pushing main and tag $VERSION..."

if [ "$RESUME" = "0" ]; then
  git push --atomic origin "HEAD:refs/heads/main" "refs/tags/$VERSION"
  echo "  ✓ Pushed main and tag $VERSION together."
else
  git fetch origin main 2>/dev/null || true
  ORIGIN_MAIN_SHA=$(git rev-parse origin/main 2>/dev/null || echo "")

  if [ "$ORIGIN_MAIN_SHA" = "$RELEASE_SHA" ]; then
    echo "  ✓ origin/main already at $RELEASE_SHA — skipping main push."
  elif [ -n "$ORIGIN_MAIN_SHA" ] && git merge-base --is-ancestor "$RELEASE_SHA" "$ORIGIN_MAIN_SHA" 2>/dev/null; then
    echo "  ✓ origin/main has advanced past the release commit — skipping main push."
  else
    echo "❌ Resume: tag $VERSION at $RELEASE_SHA is not reachable from origin/main ($ORIGIN_MAIN_SHA)." >&2
    echo "   Refusing to push from a detached state that doesn't descend from main. See docs/RELEASE-RUNBOOK.md." >&2
    exit 1
  fi

  REMOTE_TAG_NOW=$(git ls-remote origin "refs/tags/$VERSION" 2>/dev/null | awk '{print $1}')
  if [ -z "$REMOTE_TAG_NOW" ]; then
    git push origin "refs/tags/$VERSION"
  elif [ "$REMOTE_TAG_NOW" = "$RELEASE_SHA" ]; then
    echo "  ✓ Tag $VERSION already at $RELEASE_SHA on origin — skipping tag push."
  else
    echo "❌ Tag $VERSION on origin points at $REMOTE_TAG_NOW, but the local release commit is $RELEASE_SHA." >&2
    echo "   Refusing to force-update tags. See docs/RELEASE-RUNBOOK.md." >&2
    exit 1
  fi
fi

# ---------------------------------------------------------------------------
# Publish to Maven Central, one module per wrapper invocation so each module's
# outcome is classified independently. The wrapper treats an already-published
# coordinate as success, which is what makes a resume survive this step.
# ---------------------------------------------------------------------------
echo
echo "Publishing modules: $PUBLISHABLE_MODULES"
IFS=',' read -ra MODULES <<< "$PUBLISHABLE_MODULES"
for module in "${MODULES[@]}"; do
  module="${module// /}"
  [ -z "$module" ] && continue
  echo
  echo "--- $module ---"
  bash scripts/gradle-publish-wrapper.sh "$VERSION" "$module"
done
echo
echo "  ✓ All modules published at $VERSION."

# ---------------------------------------------------------------------------
# GitHub release (idempotent).
# ---------------------------------------------------------------------------
echo
if gh release view "$VERSION" >/dev/null 2>&1; then
  echo "GitHub release $VERSION already exists — leaving as-is."
else
  echo "Creating GitHub release..."
  gh release create "$VERSION" --notes-file notes.md --title "$VERSION"
fi

rm -f notes.md

echo
echo "✅ Release $VERSION complete."
echo "   Tag $VERSION -> $RELEASE_SHA"

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo
    echo "### ✅ Released"
    echo
    echo "- Tag \`$VERSION\` → \`$RELEASE_SHA\`"
    echo "- Modules published: \`$PUBLISHABLE_MODULES\`"
    echo "- GitHub release: [\`$VERSION\`](https://github.com/${GITHUB_REPOSITORY:-youversion/platform-sdk-kotlin}/releases/tag/$VERSION)"
  } >> "$GITHUB_STEP_SUMMARY"
fi

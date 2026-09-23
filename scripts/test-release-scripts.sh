#!/bin/bash
# Unit tests for the release helper scripts.
#
# These run in CI on every pull request (.github/workflows/commitlint.yml).
# The release pipeline itself only runs on a manual dispatch, so without these
# a broken helper would not be discovered until someone tried to ship.
#
# Three classes of test here:
#
#   1. Pure argument/exit-code behaviour of the small .mjs helpers
#      (release-validate, release-warn-version-jump, read-preview-field,
#      prepend-changelog). Cheap, and they are the pieces that decide whether
#      a release is allowed to proceed.
#   2. The commit-analyzer bump table (scripts/assert-bump-table.mjs). This is
#      the YPE-5781 regression guard: the analyzer degrades to the angular
#      preset *silently* if either `.releaserc.json`'s preset key or the
#      installed conventional-changelog-conventionalcommits major is wrong, so
#      only an end-to-end assertion catches it.
#   3. scripts/preview-release.mjs against disposable git histories. The ranges
#      it walks only misbehave in states this repo is rarely in — unreleased
#      commits sitting on main, a branch that forked before the last release —
#      so the test builds those states rather than waiting for them.
#
# Usage:
#   bash scripts/test-release-scripts.sh

set -uo pipefail

cd "$(dirname "$0")/.."

PASS=0
FAIL=0

assert_exit() {
  local expected=$1 label=$2
  shift 2
  local actual=0
  "$@" >/dev/null 2>&1 || actual=$?
  if [ "$actual" = "$expected" ]; then
    echo "  ✓ $label"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $label (expected exit $expected, got $actual)"
    FAIL=$((FAIL + 1))
  fi
}

assert_stderr_contains() {
  local needle=$1 label=$2
  shift 2
  local err
  err=$("$@" 2>&1 >/dev/null)
  if printf '%s' "$err" | grep -qF "$needle"; then
    echo "  ✓ $label"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $label (stderr did not contain '$needle')"
    echo "      stderr: $err"
    FAIL=$((FAIL + 1))
  fi
}

assert_stderr_empty() {
  local label=$1
  shift
  local err
  err=$("$@" 2>&1 >/dev/null)
  if [ -z "$err" ]; then
    echo "  ✓ $label"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $label (expected empty stderr)"
    echo "      stderr: $err"
    FAIL=$((FAIL + 1))
  fi
}

assert_stdout_equals() {
  local expected=$1 label=$2
  shift 2
  local actual
  actual=$("$@" 2>/dev/null)
  if [ "$actual" = "$expected" ]; then
    echo "  ✓ $label"
    PASS=$((PASS + 1))
  else
    echo "  ✗ $label (expected '$expected', got '$actual')"
    FAIL=$((FAIL + 1))
  fi
}

echo "commit-analyzer bump table (YPE-5781 regression guard):"
if node scripts/assert-bump-table.mjs 2>/dev/null; then
  PASS=$((PASS + 1))
else
  echo "  ✗ bump table did not match — see above"
  FAIL=$((FAIL + 1))
fi

echo
echo "release-validate.mjs:"
assert_exit  0  "2.2.0 > 2.1.2 → accept"                node scripts/release-validate.mjs 2.2.0 2.1.2
assert_exit  0  "2.1.3 > 2.1.2 → accept"                node scripts/release-validate.mjs 2.1.3 2.1.2
assert_exit  0  "3.0.0 > 2.1.2 → accept"                node scripts/release-validate.mjs 3.0.0 2.1.2
assert_exit  0  "2.1.3 > 0.0.0 → accept (fresh repo)"   node scripts/release-validate.mjs 2.1.3 0.0.0
assert_exit 11  "'garbage' is not semver"               node scripts/release-validate.mjs garbage 2.1.2
assert_exit 11  "'2.1' is not semver"                   node scripts/release-validate.mjs 2.1 2.1.2
assert_exit 11  "empty version is not semver"           node scripts/release-validate.mjs '' 2.1.2
assert_exit 11  "'v2.2.0' is not bare (leading v)"      node scripts/release-validate.mjs v2.2.0 2.1.2
assert_exit 11  "'3.0.0-beta.1' prerelease unsupported" node scripts/release-validate.mjs 3.0.0-beta.1 2.1.2
assert_exit 11  "'2.2.0+build.5' carries metadata"      node scripts/release-validate.mjs 2.2.0+build.5 2.1.2
assert_exit 12  "2.1.2 is not greater than 2.1.2"       node scripts/release-validate.mjs 2.1.2 2.1.2
assert_exit 12  "2.1.1 is not greater than 2.1.2"       node scripts/release-validate.mjs 2.1.1 2.1.2
assert_exit 12  "1.9.9 is not greater than 2.1.2"       node scripts/release-validate.mjs 1.9.9 2.1.2
assert_exit  1  "missing args → usage error"            node scripts/release-validate.mjs 2.2.0
assert_stderr_contains "not_semver"  "rejects with not_semver token"  node scripts/release-validate.mjs garbage 2.1.2
assert_stderr_contains "not_greater" "rejects with not_greater token" node scripts/release-validate.mjs 2.1.1 2.1.2
assert_stderr_contains "not_bare"    "rejects v-prefix with not_bare token"        node scripts/release-validate.mjs v2.2.0 2.1.2
assert_stderr_contains "prerelease_unsupported" "rejects prerelease with its token" node scripts/release-validate.mjs 3.0.0-beta.1 2.1.2

echo
echo "release-warn-version-jump.mjs:"
assert_exit  0  "no jump (2.1.3 vs 2.1.2) → exit 0"        node scripts/release-warn-version-jump.mjs 2.1.3 2.1.2
assert_exit  0  "one-major jump (3.0.0 vs 2.1.2) → exit 0" node scripts/release-warn-version-jump.mjs 3.0.0 2.1.2
assert_exit  0  "two-major jump (4.0.0 vs 2.1.2) → exit 0" node scripts/release-warn-version-jump.mjs 4.0.0 2.1.2
assert_exit  0  "invalid calc → silent exit 0"             node scripts/release-warn-version-jump.mjs 2.2.0 unknown
assert_stderr_empty    "no jump prints no warning"        node scripts/release-warn-version-jump.mjs 2.1.3 2.1.2
assert_stderr_empty    "one-major jump prints no warning" node scripts/release-warn-version-jump.mjs 3.0.0 2.1.2
assert_stderr_contains "more than one major above" "two-major jump warns"   node scripts/release-warn-version-jump.mjs 4.0.0 2.1.2
assert_stderr_contains "more than one major above" "four-major jump warns"  node scripts/release-warn-version-jump.mjs 6.0.0 2.1.2
assert_stderr_empty    "invalid calc is silent"           node scripts/release-warn-version-jump.mjs 2.2.0 unknown

echo
echo "read-preview-field.mjs:"
run_read_field() { printf '%s' "$1" | node scripts/read-preview-field.mjs "${@:2}"; }
assert_stdout_equals "2.2.0"   "picks the first present field"  run_read_field '{"next":"2.2.0","current":"2.1.2"}' --default unknown next current
assert_stdout_equals "2.1.2"   "falls through a null field"     run_read_field '{"next":null,"current":"2.1.2"}'  --default unknown next current
assert_stdout_equals "unknown" "falls back on all-null"         run_read_field '{"next":null,"current":null}'     --default unknown next current
assert_stdout_equals "none"    "falls back on empty input"      run_read_field '' --default none release_type
assert_stdout_equals "none"    "falls back on malformed JSON"   run_read_field 'not json' --default none release_type
assert_exit 0 "never fails on malformed JSON"                   run_read_field 'not json' --default none release_type

echo
echo "prepend-changelog.mjs:"
CL_TMP=$(mktemp -d)
trap 'rm -rf "$CL_TMP"' EXIT
printf '## [3.0.0](x) (2026-09-21)\n\n### Features\n\n* new thing\n' > "$CL_TMP/notes.md"

# This repo's changelog has no title: line 1 is the newest entry, and entries
# alternate between `# [x.y.0]` (minor/major) and `## [x.y.z]` (patch).
printf '## [2.1.2](x) (2026-09-18)\n\n* patch\n\n# [2.1.0](x) (2026-09-16)\n\n* minor\n' > "$CL_TMP/no-title.md"
node scripts/prepend-changelog.mjs "$CL_TMP/notes.md" "$CL_TMP/no-title.md" 2>/dev/null
assert_stdout_equals "## [3.0.0](x) (2026-09-21)" "new entry lands on line 1 when there is no title" head -1 "$CL_TMP/no-title.md"
assert_exit 0 "old entries survive" grep -qF "## [2.1.2](x)" "$CL_TMP/no-title.md"

# A single-`#` heading must still be stepped above, not below.
printf '# [2.1.0](x) (2026-09-16)\n\n* minor\n' > "$CL_TMP/hash1.md"
node scripts/prepend-changelog.mjs "$CL_TMP/notes.md" "$CL_TMP/hash1.md" 2>/dev/null
assert_stdout_equals "## [3.0.0](x) (2026-09-21)" "inserts above a single-# version heading" head -1 "$CL_TMP/hash1.md"

# A title and intro must be preserved above the new entry.
printf '# Changelog\n\nIntro.\n\n# [2.1.0](x)\n\n* minor\n' > "$CL_TMP/titled.md"
node scripts/prepend-changelog.mjs "$CL_TMP/notes.md" "$CL_TMP/titled.md" 2>/dev/null
assert_stdout_equals "# Changelog" "preserves an existing title" head -1 "$CL_TMP/titled.md"
assert_exit 0 "intro survives" grep -qF "Intro." "$CL_TMP/titled.md"

# No version headings at all → append rather than lose the entry.
printf '# Changelog\n\nNothing released yet.\n' > "$CL_TMP/empty.md"
node scripts/prepend-changelog.mjs "$CL_TMP/notes.md" "$CL_TMP/empty.md" 2>/dev/null
assert_exit 0 "appends when there are no existing entries" grep -qF "## [3.0.0](x)" "$CL_TMP/empty.md"

assert_exit 1 "missing args → usage error" node scripts/prepend-changelog.mjs "$CL_TMP/notes.md"

echo
echo "preview-release.mjs:"
assert_exit 2 "no args → usage exit 2"                  node scripts/preview-release.mjs
assert_exit 2 "--head alone → usage exit 2"             node scripts/preview-release.mjs --head main
assert_exit 2 "--base and --main together → usage exit 2" node scripts/preview-release.mjs --base a --main b --head c

# Disposable histories. The two range bugs these cover are invisible to a
# grep-level assertion and to any test run against this repo's own history:
# both need a main that carries unreleased commits, and a branch that forked
# before the last release.
PV_TMP=$(mktemp -d)
trap 'rm -rf "$CL_TMP" "$PV_TMP"' EXIT

pv_init() {
  git init -q -b main "$1"
  git -C "$1" config user.email release-test@example.com
  git -C "$1" config user.name "Release Test"
  git -C "$1" config commit.gpgsign false
}
pv_commit() { git -C "$1" commit -q --allow-empty --no-verify -m "$2"; }
pv_field() {
  node scripts/preview-release.mjs --repo "$1" --main "$2" --head "$3" 2>/dev/null \
    | node scripts/read-preview-field.mjs --default none "$4"
}

# A fix PR sitting on top of a feat that is merged but unreleased. The release
# dispatch analyzes 1.0.0 → main and computes a minor; a preview scoped to the
# PR alone computes a patch and prints a version nobody will ever ship.
UNREL="$PV_TMP/unreleased-on-main"
pv_init "$UNREL"
pv_commit "$UNREL" "chore(release): 1.0.0 [skip ci]"
git -C "$UNREL" tag 1.0.0
pv_commit "$UNREL" "feat: merged to main but not yet released"
git -C "$UNREL" checkout -q -b pr-fix
pv_commit "$UNREL" "fix: the only commit on this PR"

assert_stdout_equals "1.1.0" "unreleased feat on main → PR preview is the release's minor" \
  pv_field "$UNREL" main pr-fix next
assert_stdout_equals "minor" "…and reports it as a minor" \
  pv_field "$UNREL" main pr-fix release_type
assert_stdout_equals "patch" "…while still reporting the PR's own patch separately" \
  pv_field "$UNREL" main pr-fix pr_release_type
assert_stdout_equals "1" "…and counting only this PR's commits as the PR's own" \
  pv_field "$UNREL" main pr-fix pr_commit_count

# A `feat!:` on the PR must read as major from both angles, so the signoff
# check (which reads pr_release_type) and the comment agree.
git -C "$UNREL" checkout -q -b pr-break main
pv_commit "$UNREL" "feat!: remove the old entry point

BREAKING CHANGE: the old entry point is gone."
assert_stdout_equals "major" "a breaking change on the PR reads as major" \
  pv_field "$UNREL" main pr-break pr_release_type
assert_stdout_equals "2.0.0" "…and previews the major version" \
  pv_field "$UNREL" main pr-break next

# A branch that forked before the last release still reaches the older tag.
# Describing the checked-out branch would bump from 1.0.0 and print 1.0.1 —
# a version that shipped two releases ago.
STALE="$PV_TMP/stale-branch"
pv_init "$STALE"
pv_commit "$STALE" "chore(release): 1.0.0 [skip ci]"
git -C "$STALE" tag 1.0.0
git -C "$STALE" checkout -q -b pr-stale
pv_commit "$STALE" "fix: authored before 2.0.0 shipped"
git -C "$STALE" checkout -q main
pv_commit "$STALE" "feat: shipped in 2.0.0"
pv_commit "$STALE" "chore(release): 2.0.0 [skip ci]"
git -C "$STALE" tag 2.0.0
# Leave the stale branch checked out, as the PR job does.
git -C "$STALE" checkout -q pr-stale

assert_stdout_equals "1.0.0" "the stale branch really does reach only the older tag" \
  git -C "$STALE" describe --tags --abbrev=0
assert_stdout_equals "2.0.0" "current is taken from main, not the stale branch" \
  pv_field "$STALE" main pr-stale current
assert_stdout_equals "2.0.1" "…so the preview bumps from the current release" \
  pv_field "$STALE" main pr-stale next

echo
echo "release.sh wiring:"
# The validation logic used to live as inline `node -e` inside release.sh,
# where it could not be tested and a quoting mistake was invisible. Keep it in
# .mjs files so the assertions above actually cover what ships.
if grep -nE "^[[:space:]]*node[[:space:]]+-e" scripts/release.sh >/dev/null; then
  echo "  ✗ scripts/release.sh contains inline 'node -e' — extract it to a .mjs script"
  grep -nE "^[[:space:]]*node[[:space:]]+-e" scripts/release.sh
  FAIL=$((FAIL + 1))
else
  echo "  ✓ scripts/release.sh has no inline 'node -e'"
  PASS=$((PASS + 1))
fi

# The remote tag is the sole resume key: main landing without it makes the next
# dispatch build a duplicate release commit. Grep, since the push needs a remote.
if grep -qE '^[[:space:]]*git push --atomic origin "HEAD:refs/heads/main" "refs/tags/\$VERSION"$' scripts/release.sh; then
  echo "  ✓ scripts/release.sh pushes main + tag atomically on a fresh run"
  PASS=$((PASS + 1))
else
  echo "  ✗ scripts/release.sh fresh-run push is not a single atomic push of main + tag"
  FAIL=$((FAIL + 1))
fi

if grep -nE '^[[:space:]]*git push origin HEAD:(refs/heads/)?main[[:space:]]*$' scripts/release.sh >/dev/null; then
  echo "  ✗ scripts/release.sh pushes main on its own — the tag must go with it"
  grep -nE '^[[:space:]]*git push origin HEAD:(refs/heads/)?main[[:space:]]*$' scripts/release.sh
  FAIL=$((FAIL + 1))
else
  echo "  ✓ scripts/release.sh never pushes main as a standalone operation"
  PASS=$((PASS + 1))
fi

# Adoption keys off the subject release.sh itself writes; if one moves without
# the other it stops firing and duplicate release commits come back.
if grep -qF "printf 'chore(release): %s [skip ci]" scripts/release.sh \
  && grep -qF 'chore(release): $VERSION [skip ci]" ]' scripts/release.sh; then
  echo "  ✓ scripts/release.sh adopts an untagged release commit by its own subject"
  PASS=$((PASS + 1))
else
  echo "  ✗ scripts/release.sh adoption branch and commit subject have drifted apart"
  FAIL=$((FAIL + 1))
fi

for script in release.sh stamp-version.sh gradle-publish-wrapper.sh; do
  if bash -n "scripts/$script" 2>/dev/null; then
    echo "  ✓ scripts/$script parses"
    PASS=$((PASS + 1))
  else
    echo "  ✗ scripts/$script has a syntax error"
    bash -n "scripts/$script"
    FAIL=$((FAIL + 1))
  fi
done

# release.sh must refuse to run without a chosen version rather than defaulting
# to something. A release that picks its own version is the failure mode this
# whole ticket exists to remove.
assert_exit 1 "release.sh without VERSION → exit 1" env -u VERSION bash scripts/release.sh
assert_stderr_contains "VERSION env var is required" "release.sh says why" env -u VERSION bash scripts/release.sh

# stamp-version.sh must refuse an empty version rather than writing `""` into
# the version catalog.
assert_exit 1 "stamp-version.sh without a version → exit 1" bash scripts/stamp-version.sh
assert_exit 2 "gradle-publish-wrapper.sh with no args → usage exit 2" bash scripts/gradle-publish-wrapper.sh
assert_exit 2 "gradle-publish-wrapper.sh with one arg → usage exit 2" bash scripts/gradle-publish-wrapper.sh 2.2.0
assert_exit 2 "gradle-publish-wrapper.sh without a group → usage exit 2" bash scripts/gradle-publish-wrapper.sh 2.2.0 platform-core

central_re=$(sed -n "s/^CENTRAL_ALREADY_EXISTS_RE='\(.*\)'$/\1/p" scripts/gradle-publish-wrapper.sh)
if [ -z "$central_re" ]; then
  echo "  ✗ CENTRAL_ALREADY_EXISTS_RE not found in scripts/gradle-publish-wrapper.sh"
  FAIL=$((FAIL + 1))
else
  central_real="  * Component with package url: 'pkg:maven/com.youversion.platform/platform-core@2.3.0?type=aar' already exists"
  if printf '%s\n' "$central_real" | grep -E -i -q -e "$central_re"; then
    echo "  ✓ already-exists pattern matches Central's real rejection message"
    PASS=$((PASS + 1))
  else
    echo "  ✗ already-exists pattern does not match Central's real rejection message"
    echo "      pattern: $central_re"
    echo "      message: $central_real"
    FAIL=$((FAIL + 1))
  fi

  central_decoy="> Cannot add task 'javaDocReleaseJar' as a task with that name already exists"
  if printf '%s\n' "$central_decoy" | grep -E -i -q -e "$central_re"; then
    echo "  ✗ already-exists pattern matches an unrelated Gradle 'already exists' error"
    FAIL=$((FAIL + 1))
  else
    echo "  ✓ already-exists pattern ignores unrelated Gradle 'already exists' errors"
    PASS=$((PASS + 1))
  fi
fi

if grep -qF 'FAILED_MODULES="${FAILED_MODULES:+$FAILED_MODULES }$module"' scripts/release.sh \
  && grep -qF 'exit 42' scripts/release.sh; then
  echo "  ✓ scripts/release.sh collects per-module failures and fast-fails on signing"
  PASS=$((PASS + 1))
else
  echo "  ✗ scripts/release.sh publish loop no longer collects per-module failures"
  FAIL=$((FAIL + 1))
fi

echo
if [ "$FAIL" -gt 0 ]; then
  echo "❌ $FAIL test(s) failed, $PASS passed"
  exit 1
fi
echo "✅ All $PASS tests passed"

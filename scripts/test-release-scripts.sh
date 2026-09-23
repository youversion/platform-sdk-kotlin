#!/bin/bash
# Unit tests for the release helper scripts.
#
# These run in CI on every pull request (.github/workflows/commitlint.yml).
# The release pipeline itself only runs on a manual dispatch, so without these
# a broken helper would not be discovered until someone tried to ship.
#
# Seven classes of test here, in the order they run:
#
#   1. The commit-analyzer bump table (scripts/assert-bump-table.mjs). This is
#      the YPE-5781 regression guard: the analyzer degrades to the angular
#      preset *silently* if either `.releaserc.json`'s preset key or the
#      installed conventional-changelog-conventionalcommits major is wrong, so
#      only an end-to-end assertion catches it.
#   2. Pure argument/exit-code behaviour of the small .mjs helpers
#      (release-validate, release-warn-version-jump, read-preview-field,
#      prepend-changelog). Cheap, and they are the pieces that decide whether
#      a release is allowed to proceed.
#   3. scripts/preview-release.mjs against disposable git histories. The ranges
#      it walks only misbehave in states this repo is rarely in — unreleased
#      commits sitting on main, a branch that forked before the last release —
#      so the test builds those states rather than waiting for them.
#   4. scripts/release.sh's wiring, read as source text. These guard shape
#      rather than behaviour — validation stays in testable .mjs files, and
#      main is only ever pushed together with its tag — and the push itself
#      needs a remote, so grep is the honest seam here.
#   5. scripts/stamp-version.sh against disposable copies of the two files it
#      rewrites. It is the only release script that edits committed content,
#      and what it writes is what consumers resolve and copy/paste, so the test
#      runs it rather than reading its sed expressions.
#   6. scripts/gradle-publish-wrapper.sh against a stubbed ./gradlew and curl.
#      Its 0/42/1 exit codes are what release.sh branches on, and every branch
#      fires only on a Gradle log this repo never produces deliberately, so the
#      test replays those logs instead.
#   7. scripts/release.sh's post-tag steps against a disposable origin, with
#      the publish wrapper and gh stubbed. Covers the one outcome here that
#      consumers see and that cannot be walked back: a GitHub release cut for a
#      version that never reached Central.
#
# Classes 6 and 7 stub rather than grep on purpose. A source-text assertion
# cannot tell whether the failure paths still wire together, and the release
# pipeline is only ever exercised for real by shipping.
#
# Usage:
#   bash scripts/test-release-scripts.sh

set -uo pipefail

cd "$(dirname "$0")/.."
REPO_ROOT=$PWD

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

assert_stderr_lacks() {
  local needle=$1 label=$2
  shift 2
  local err
  err=$("$@" 2>&1 >/dev/null)
  if printf '%s' "$err" | grep -qF "$needle"; then
    echo "  ✗ $label (stderr contained '$needle')"
    echo "      stderr: $err"
    FAIL=$((FAIL + 1))
  else
    echo "  ✓ $label"
    PASS=$((PASS + 1))
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

wrapper_calls=$(grep -cF 'bash scripts/gradle-publish-wrapper.sh' scripts/release.sh)
if [ "$wrapper_calls" = "1" ] \
  && grep -qF 'bash scripts/gradle-publish-wrapper.sh "$VERSION" "$PUBLISHABLE_MODULES" "$MAVEN_GROUP"' scripts/release.sh; then
  echo "  ✓ scripts/release.sh publishes every module in one wrapper invocation"
  PASS=$((PASS + 1))
else
  echo "  ✗ scripts/release.sh no longer publishes all modules in a single invocation ($wrapper_calls call(s))"
  FAIL=$((FAIL + 1))
fi

gradlew_calls=$(grep -cE '^\./gradlew ' scripts/gradle-publish-wrapper.sh)
if [ "$gradlew_calls" = "1" ] && grep -qF '"${tasks[@]}"' scripts/gradle-publish-wrapper.sh; then
  echo "  ✓ gradle-publish-wrapper.sh publishes all modules in one Gradle invocation"
  PASS=$((PASS + 1))
else
  echo "  ✗ gradle-publish-wrapper.sh no longer uses a single Gradle invocation ($gradlew_calls found)"
  FAIL=$((FAIL + 1))
fi

if grep -qF 'exit 1' scripts/poll-central-index.sh \
  && grep -qF 'resolvable for only ${resolvable_count}' scripts/poll-central-index.sh; then
  echo "  ✓ poll-central-index.sh fails when a module is still missing at the deadline"
  PASS=$((PASS + 1))
else
  echo "  ✗ poll-central-index.sh no longer fails on a partially resolvable version"
  FAIL=$((FAIL + 1))
fi

echo
echo "stamp-version.sh:"
# Runs the real script against disposable copies of the two files it rewrites.
# This is the only release script that mutates committed content, and a bad
# stamp is what consumers resolve: the catalog line is the coordinate the three
# modules publish under, and the README lines are the snippets people paste.
SV_TMP=$(mktemp -d)
trap 'rm -rf "$CL_TMP" "$PV_TMP" "$SV_TMP"' EXIT
SV_N=0

# The catalog line carries a trailing `# .get()` comment and the README holds
# the version twice, in two different snippet forms — both are properties of
# the real files that the sed expressions have to survive.
sv_new() {
  SV_N=$((SV_N + 1))
  SV_DIR="$SV_TMP/case$SV_N"
  mkdir -p "$SV_DIR/gradle" "$SV_DIR/scripts"
  cp scripts/stamp-version.sh "$SV_DIR/scripts/"
  printf '[versions]\nyouversionPlatform = "2.0.0" # .get()\nagp = "8.5.0"\n' \
    > "$SV_DIR/gradle/libs.versions.toml"
  printf 'youVersionPlatform = "2.0.0"\n\nval youVersionPlatform = "2.0.0"\n' \
    > "$SV_DIR/README.md"
}
sv_stamp() { ( cd "$SV_DIR" && bash scripts/stamp-version.sh "$@" ); }
sv_catalog_line() { grep 'youversionPlatform = ' "$SV_DIR/gradle/libs.versions.toml"; }
sv_readme_count() { grep -c "youVersionPlatform = \"$1\"" "$SV_DIR/README.md" | tr -d ' '; }

sv_new
assert_exit 0 "stamps a fresh version" sv_stamp 3.1.0
# A greedy `.*` instead of `[^"]*` would swallow the trailing comment here.
assert_stdout_equals 'youversionPlatform = "3.1.0" # .get()' \
  "…rewrites the catalog without eating the trailing comment" sv_catalog_line
assert_stdout_equals "2" "…rewrites every README occurrence" sv_readme_count 3.1.0
assert_exit 1 "…and leaves no occurrence on the old version" \
  grep -qF 'youVersionPlatform = "2.0.0"' "$SV_DIR/README.md"
assert_exit 0 "…leaving unrelated catalog versions alone" \
  grep -qF 'agp = "8.5.0"' "$SV_DIR/gradle/libs.versions.toml"

# release.sh re-runs the stamp on a resumed release, so a second run against an
# already-stamped tree must be a no-op rather than a corruption.
SV_BEFORE=$(cat "$SV_DIR/gradle/libs.versions.toml" "$SV_DIR/README.md")
assert_exit 0 "re-stamping the same version succeeds" sv_stamp 3.1.0
assert_stdout_equals "$SV_BEFORE" "…and changes nothing" \
  cat "$SV_DIR/gradle/libs.versions.toml" "$SV_DIR/README.md"
assert_exit 0 "stamping a newer version over a stamped tree succeeds" sv_stamp 3.2.0
assert_stdout_equals 'youversionPlatform = "3.2.0" # .get()' \
  "…and moves the catalog on" sv_catalog_line

# Both sed expressions are written defensively for lines the current files do
# not have: `[^"]*` rather than `.*` matters only when a later quote sits on the
# line, and `/g` only when one line carries two occurrences. Neither choice can
# regress noticeably until such a line is added, which is what this case is.
sv_new
printf '[versions]\nyouversionPlatform = "2.0.0" # pinned; see "runbook"\n' \
  > "$SV_DIR/gradle/libs.versions.toml"
printf 'youVersionPlatform = "2.0.0" and youVersionPlatform = "2.0.0"\n' \
  > "$SV_DIR/README.md"
assert_exit 0 "stamps lines carrying extra quotes and repeats" sv_stamp 3.1.0
assert_stdout_equals 'youversionPlatform = "3.1.0" # pinned; see "runbook"' \
  "…without swallowing the rest of the catalog line" sv_catalog_line
assert_stdout_equals 'youVersionPlatform = "3.1.0" and youVersionPlatform = "3.1.0"' \
  "…rewriting both occurrences on a single README line" cat "$SV_DIR/README.md"

# The verification branches below exist so a drifted snippet fails the release
# instead of shipping a stale version to consumers. Each needs a tree the
# sed expressions cannot fully rewrite.
sv_new
printf '[versions]\nagp = "8.5.0"\n' > "$SV_DIR/gradle/libs.versions.toml"
assert_exit 1 "catalog without the version key → exit 1" sv_stamp 3.1.0
assert_stderr_contains "Failed to stamp version into" "…and says the catalog is the problem" sv_stamp 3.1.0

sv_new
printf 'youVersionPlatform = "2.0.0"\n\nyouVersionPlatform = "\n' > "$SV_DIR/README.md"
assert_exit 1 "a README occurrence the stamp could not rewrite → exit 1" sv_stamp 3.1.0
assert_stderr_contains "lines but only" "…and reports the mismatch" sv_stamp 3.1.0

sv_new
printf 'No install snippet here.\n' > "$SV_DIR/README.md"
assert_exit 1 "README with no version snippet → exit 1" sv_stamp 3.1.0
assert_stderr_contains "no 'youVersionPlatform" "…and says the snippet is missing" sv_stamp 3.1.0

sv_new
rm "$SV_DIR/README.md"
assert_exit 1 "missing README → exit 1" sv_stamp 3.1.0
assert_stderr_contains "README.md not found" "…and names the missing file" sv_stamp 3.1.0

sv_new
rm "$SV_DIR/gradle/libs.versions.toml"
assert_exit 1 "missing catalog → exit 1" sv_stamp 3.1.0
assert_stderr_contains "libs.versions.toml not found" "…and names the missing file" sv_stamp 3.1.0

echo
echo "gradle-publish-wrapper.sh outcome classification:"
WR_TMP=$(mktemp -d)
trap 'rm -rf "$CL_TMP" "$PV_TMP" "$SV_TMP" "$WR_TMP"' EXIT
mkdir -p "$WR_TMP/bin"

# Answers the wrapper's HEAD on a repo1 .pom URL with a status code, because
# the wrapper reads %{http_code} rather than curl's exit. A module is 200 iff
# it is listed in $CENTRAL_PRESENT or the deployment landed mid-run (below),
# 000 iff listed in $CENTRAL_UNREACHABLE, and 404 otherwise.
cat > "$WR_TMP/bin/curl" <<'EOF'
#!/bin/bash
url=${!#}
module=$(printf '%s' "$url" | awk -F/ '{print $(NF-2)}')
case ",${CENTRAL_PRESENT:-}," in
  *",$module,"*) echo 200; exit 0 ;;
esac
if [[ -n "${CENTRAL_LANDED_FILE:-}" && -f "$CENTRAL_LANDED_FILE" ]]; then
  echo 200; exit 0
fi
case ",${CENTRAL_UNREACHABLE:-}," in
  *",$module,"*) exit 6 ;;
esac
echo 404
EOF
chmod +x "$WR_TMP/bin/curl"

# Replays a canned log and exit status, and records the task list it was handed
# so the partial-publish path can be asserted on. Under $CENTRAL_LANDS it also
# drops the marker the curl stub reads, modelling a deployment that Central
# accepted while the local build was failing.
cat > "$WR_TMP/gradlew" <<'EOF'
#!/bin/bash
printf '%s\n' "$*" > "$GRADLE_ARGS_FILE"
[[ -n "${CENTRAL_LANDS:-}" ]] && : > "$CENTRAL_LANDED_FILE"
printf '%s\n' "${GRADLE_LOG:-}"
exit "${GRADLE_EXIT:-0}"
EOF
chmod +x "$WR_TMP/gradlew"

WR_ARGS="$WR_TMP/gradle.args"
WR_LANDED="$WR_TMP/central.landed"
wr_run() {
  # wr_run <present-csv> <gradle-exit> <gradle-log> [modules-csv]
  # $CENTRAL_LANDS and $CENTRAL_UNREACHABLE are set by the caller's environment.
  : > "$WR_ARGS"
  rm -f "$WR_LANDED"
  ( cd "$WR_TMP" \
    && PATH="$WR_TMP/bin:$PATH" \
       CENTRAL_PRESENT="$1" GRADLE_EXIT="$2" GRADLE_LOG="$3" GRADLE_ARGS_FILE="$WR_ARGS" \
       CENTRAL_LANDED_FILE="$WR_LANDED" \
       bash "$REPO_ROOT/scripts/gradle-publish-wrapper.sh" \
         2.3.0 "${4:-platform-core,platform-ui,platform-reader}" com.youversion.platform )
}

# The deployment reaches Central during the Gradle run — the pre-flight 404s
# and the post-failure re-check 200s.
wr_run_landing() { ( export CENTRAL_LANDS=1; wr_run "$@" ); }
# repo1 does not answer for <unreachable-csv>; those modules are neither 200
# nor 404.
wr_run_unreachable() { ( export CENTRAL_UNREACHABLE="$1"; wr_run "${@:2}" ); }

GPG_LOG='> Task :platform-core:signReleasePublication FAILED
Could not read PGP secret key'
EXISTS_LOG="  * Component with package url: 'pkg:maven/com.youversion.platform/platform-core@2.3.0?type=aar' already exists"
DECOY_LOG="> Cannot add task 'javaDocReleaseJar' as a task with that name already exists"

assert_exit 0 "every module already on Central → exit 0 without publishing" \
  wr_run "platform-core,platform-ui,platform-reader" 99 "should not run"
assert_exit 0 "publish succeeds → exit 0" \
  wr_run "" 0 "BUILD SUCCESSFUL"
assert_exit 42 "signing failure → exit 42 (fast-fail, no retry)" \
  wr_run "" 1 "$GPG_LOG"
assert_exit 1 "unclassified failure → exit 1 (retryable)" \
  wr_run "" 1 "Connection reset by peer"
assert_exit 1 "already-exists that repo1 cannot confirm → exit 1, not a claimed success" \
  wr_run "" 1 "$EXISTS_LOG"
# The branch a re-dispatch depends on: Central took the deployment, the local
# build failed anyway, and the re-check now resolves what the pre-flight could
# not see. Nothing else reaches the exit-0 arm — with every module already on
# Central the pre-flight exits first and Gradle never runs.
assert_exit 0 "already-exists confirmed by repo1 on the re-check → exit 0" \
  wr_run_landing "" 1 "$EXISTS_LOG"
# Gradle emits "already exists" for configuration errors too, and the wrapper
# greps the whole log. Reading one as a Central no-op would exit 0 and let
# release.sh cut a release for modules that never shipped.
assert_exit 1 "unrelated Gradle 'already exists' error is not mistaken for a no-op" \
  wr_run "" 1 "$DECOY_LOG"

# An unreadable repo1 is not a 404. Reading it as one makes a published module
# look missing and announces a partial release over a run that may be clean.
assert_stderr_contains "repo1 did not answer for: platform-ui" \
  "repo1 not answering is reported as unknown, not as missing" \
  wr_run_unreachable "platform-ui" "platform-core" 0 "BUILD SUCCESSFUL"
assert_stderr_lacks "already partially published" \
  "…and does not claim a partial release it cannot see" \
  wr_run_unreachable "platform-ui" "platform-core" 0 "BUILD SUCCESSFUL"

# Central coordinates are immutable, so a half-published version can only be
# completed, never re-deployed: the Gradle invocation must cover the missing
# modules and nothing else.
wr_run "platform-core" 0 "BUILD SUCCESSFUL" >/dev/null 2>&1
assert_stdout_equals \
  ":platform-ui:publishToMavenCentral :platform-reader:publishToMavenCentral" \
  "partial publish targets only the modules missing from Central" \
  awk '{print $1, $2}' "$WR_ARGS"

echo
echo "release.sh publish-outcome handling:"
# Resume mode, because it is the path a re-dispatch actually takes and it
# reaches the publish step without a buildable tree. $VERSION deliberately
# equals the existing tag — that is what release.sh keys resume detection off.
RS_TMP=$(mktemp -d)
trap 'rm -rf "$CL_TMP" "$PV_TMP" "$SV_TMP" "$WR_TMP" "$RS_TMP"' EXIT
RS_ORIGIN="$RS_TMP/origin.git"
git init -q --bare -b main "$RS_ORIGIN"

rs_seed="$RS_TMP/seed"
git init -q -b main "$rs_seed"
git -C "$rs_seed" config user.email release-test@example.com
git -C "$rs_seed" config user.name "Release Test"
git -C "$rs_seed" config commit.gpgsign false
mkdir -p "$rs_seed/gradle" "$rs_seed/scripts"
# The real orchestrator, plus the two helpers whose exit codes it branches on.
cp scripts/release.sh scripts/release-validate.mjs scripts/read-preview-field.mjs \
  "$rs_seed/scripts/"
# Notes generation is stubbed: it needs the changelog toolchain and is covered
# by the prepend-changelog tests above. preview-release.mjs and
# release-warn-version-jump.mjs are left absent on purpose — release.sh must
# survive their failure, and their absence is what proves it does.
printf 'console.log("## 2.0.0\\n\\n* stub note");\n' > "$rs_seed/scripts/generate-release-notes.mjs"
cat > "$rs_seed/scripts/gradle-publish-wrapper.sh" <<'EOF'
#!/usr/bin/env bash
printf '%s\n' "$*" > "$PUBLISH_ARGS_FILE"
exit "${STUB_PUBLISH_EXIT:-0}"
EOF
printf 'youversionPlatform = "1.0.0"\n' > "$rs_seed/gradle/libs.versions.toml"
git -C "$rs_seed" add -A
git -C "$rs_seed" commit -q --no-verify -m "feat: initial"
git -C "$rs_seed" tag 1.0.0
printf 'youversionPlatform = "2.0.0"\n' > "$rs_seed/gradle/libs.versions.toml"
git -C "$rs_seed" commit -q --no-verify -am "chore(release): 2.0.0 [skip ci]"
git -C "$rs_seed" tag 2.0.0
git -C "$rs_seed" push -q "$RS_ORIGIN" main --tags

mkdir -p "$RS_TMP/bin"
cat > "$RS_TMP/bin/gh" <<'EOF'
#!/bin/bash
printf '%s\n' "$*" >> "$GH_LOG"
if [ "${1:-} ${2:-}" = "release view" ]; then exit "${GH_VIEW_EXIT:-1}"; fi
exit 0
EOF
chmod +x "$RS_TMP/bin/gh"

RS_RUN=0
rs_run() {
  # rs_run <wrapper-exit> [gh-release-view-exit]; sets $RS_GH_LOG and $RS_PUB_ARGS
  RS_RUN=$((RS_RUN + 1))
  local dir="$RS_TMP/run$RS_RUN"
  git clone -q "$RS_ORIGIN" "$dir"
  git -C "$dir" config user.email release-test@example.com
  git -C "$dir" config user.name "Release Test"
  git -C "$dir" config commit.gpgsign false
  # release-validate.mjs imports semver; the clone resolves it through here.
  ln -s "$REPO_ROOT/node_modules" "$dir/node_modules"
  RS_GH_LOG="$RS_TMP/gh$RS_RUN.log"
  RS_PUB_ARGS="$RS_TMP/publish$RS_RUN.args"
  : > "$RS_GH_LOG"
  : > "$RS_PUB_ARGS"
  ( cd "$dir" \
    && PATH="$RS_TMP/bin:$PATH" \
       VERSION=2.0.0 \
       STUB_PUBLISH_EXIT="$1" GH_VIEW_EXIT="${2:-1}" \
       GH_LOG="$RS_GH_LOG" PUBLISH_ARGS_FILE="$RS_PUB_ARGS" \
       bash scripts/release.sh )
}

assert_exit 0 "publish succeeds → release.sh exits 0" rs_run 0
assert_exit 0 "…and creates the GitHub release" grep -qF "release create 2.0.0" "$RS_GH_LOG"
assert_stdout_equals "2.0.0 platform-core,platform-ui,platform-reader com.youversion.platform" \
  "…having published every module in one wrapper call" cat "$RS_PUB_ARGS"

# The pair below is the point of this whole section: a GitHub release for a
# version that is not on Central is the one outcome here consumers see and that
# cannot be walked back.
assert_exit 42 "signing failure (42) propagates out of release.sh" rs_run 42
assert_exit 1 "…and cuts no GitHub release" grep -qF "release create" "$RS_GH_LOG"

assert_exit 1 "retryable publish failure (1) → release.sh exits 1" rs_run 1
assert_exit 1 "…and cuts no GitHub release" grep -qF "release create" "$RS_GH_LOG"

assert_exit 0 "an existing GitHub release is left alone" rs_run 0 0
assert_exit 1 "…so release create is never called twice" grep -qF "release create" "$RS_GH_LOG"

echo
if [ "$FAIL" -gt 0 ]; then
  echo "❌ $FAIL test(s) failed, $PASS passed"
  exit 1
fi
echo "✅ All $PASS tests passed"

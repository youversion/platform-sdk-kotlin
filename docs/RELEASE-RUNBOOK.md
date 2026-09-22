# Release Runbook

Operational guide for recovering from failures in the Kotlin SDK release
pipeline. The day-to-day "how releases work" doc is
[`RELEASING.md`](../RELEASING.md); this file covers the failure modes.

## The shape of a release run

A release is a **manual dispatch**: **Actions → Release → Run workflow**, on
`main`, with the version typed into the `version` input. There is no push
trigger. [`.github/workflows/release.yml`](../.github/workflows/release.yml)
runs three sequential jobs:

1. **test** — `./gradlew test`. A release reaches consumers without a pull
   request in front of it, so the suite runs again here.
2. **release** — runs [`scripts/release.sh`](../scripts/release.sh) in the
   `production` environment, which is where the Maven Central and GPG
   credentials live. One script does everything: validate the version, generate
   notes, prepend the changelog, stamp the version, commit, tag, push, publish
   each module, create the GitHub Release.
3. **post-publish-verify** — polls `repo1.maven.org` and writes an indexing
   table to the run summary. Never fails the workflow.

Two properties matter for almost every recovery below.

**The chosen version always wins.** It is an operator input, not something
inferred from the commits. The analyzer's calculated version is logged beside it
for audit and nothing more. So "the pipeline picked the wrong version" is never
the diagnosis — someone typed it.

**Re-dispatching the same version is the primary recovery.**
`scripts/release.sh` auto-detects resume from the presence of the remote tag,
and every step after that point is idempotent: pushes that already landed are
skipped, a module Central already has counts as success, an existing GitHub
Release is left alone. There is no separate preflight job and no `modules`
input — the script re-derives what is left to do from the world's actual state
each time it runs.

## Release commit on main with no tag

**Symptom:** the `release` job failed at or after the push step, and
`git ls-remote origin refs/tags/<version>` is empty while `main` already
carries a `chore(release): <version>` commit.

**Fix:** re-dispatch with the same `version` input. `release.sh` detects that
`main` already carries this version's release commit, adopts it, and pushes the
missing tag — no second commit, no duplicate changelog entry.

The fresh-run push is atomic (`git push --atomic`, main + tag in one
transaction), so the pipeline cannot produce this state any more. It survives
for remotes split by an older run or a hand push. Adoption matches on the
commit subject, so it does not fire if commits have landed on `main` on top of
the release commit; in that case tag it by hand at the release commit
(`git tag <version> <sha> && git push origin <version>`) and re-dispatch, which
then resumes normally.

## Upload transient failure

**Symptom:** the `release` job fails with a network error, a 5xx from
`central.sonatype.com`, or the Gradle daemon dying mid-upload.
[`scripts/gradle-publish-wrapper.sh`](../scripts/gradle-publish-wrapper.sh)
exits `1` (not `42`), and its message says a retry may help.

**Fix:** re-dispatch with the same `version` input. The tag already exists, so
the run resumes: it skips straight past the commit, tag and push, and publishes
only what Central does not already have. No manual cleanup.

## Stuck released-not-indexed

**Symptom:** the `release` job reports success, but `post-publish-verify` shows
⏳ for one or more modules in the run summary, and `repo1.maven.org` returns 404
for those `.pom` URLs.

**Fix:** wait. Central Portal usually propagates to `repo1.maven.org` within
minutes but can take up to ~30 minutes. The verify job polls for 30 minutes and
never fails the workflow — it reports the current state and stops. If the wait
exceeds 30 minutes:

1. Check
   [Central Portal → Publishing Deployments](https://central.sonatype.com/publishing/deployments).
   "RELEASED" means Portal is done and the lag is purely the repo1 mirror.
2. Open a Sonatype support issue if `repo1.maven.org` still 404s more than two
   hours after a "RELEASED" deployment.
3. **What to tell consumers in the gap:** "Version X is released to Maven
   Central and will become resolvable within ~30 minutes (occasionally longer).
   If your build cannot find it yet, force a refresh with
   `./gradlew --refresh-dependencies` once propagation completes."

Note: `search.maven.org` (the discovery search index) is a separate, slower
system. Portal-published artifacts often take hours, or never appear there, even
when fully resolvable. The pipeline does not monitor it.

## GPG signing failure (expired key / missing passphrase)

**Symptom:** the `release` job exits with code **42** and the run summary
contains "GPG signing failure". This is unrecoverable until the signing secrets
are fixed — re-dispatching will fail the same way.

**Fix:**

1. **Diagnose** from the failed step's Gradle output. Common patterns:
   - `Could not read PGP secret key` → `SIGNINGKEY` is missing, empty, or malformed.
   - `Bad passphrase` → `SIGNINGKEY_PASSWORD` does not match the key in `SIGNINGKEY`.
   - `No suitable secret key was found` → the key has no signing-capable subkey, or the wrong key was exported.
   - `gpg: ... expired` → the key (or its signing subkey) has passed its expiration date.
2. **Rotate or extend the key.** A manual procedure touching GitHub secrets and
   Maven Central; do not attempt it from inside the release workflow.
   - Generate a new GPG key, or extend the existing one: `gpg --edit-key <id>`, `expire`, `save`.
   - Publish the public key to a keyserver Maven Central trusts (e.g. `keys.openpgp.org`).
   - Re-export with `gpg --armor --export-secret-keys <id> | base64` and update the
     **`SIGNINGKEY` environment secret on `production`** (Settings → Environments →
     production). Update `SIGNINGKEY_PASSWORD` too if the passphrase changed.
     These are environment secrets, not repository secrets — updating a
     repository secret of the same name will have no effect.
3. **Re-dispatch** with the same `version`. The run resumes and publishes only
   the modules Central is still missing.

## Partial publish across the three coordinates

**Symptom:** only some of `platform-core`, `platform-ui`, `platform-reader`
reached Central. Usually one module's signing or upload errored and the run
stopped before the rest.

**Fix:** re-dispatch with the same `version`. The wrapper runs one Gradle
invocation per module and classifies each independently, and Central's
"component already exists" rejection is treated as **success**, not as an error
to retry past — the coordinate being immutable is precisely what makes it done.
Safe to re-run repeatedly.

## Sonatype-vs-Central drift

**Symptom:**
[Central Portal → Publishing Deployments](https://central.sonatype.com/publishing/deployments)
and `repo1.maven.org` disagree about whether a version exists.

**Fix:**

- **Portal RELEASED, repo1 404:** propagation lag — see
  [Stuck released-not-indexed](#stuck-released-not-indexed).
- **repo1 has it, Portal shows PENDING:** the Portal UI is stale; refresh. If it
  is still pending after an hour, contact Sonatype.
- **Portal shows FAILED but some coordinates are on repo1:** treat as a partial
  publish (above) and re-dispatch.

`repo1.maven.org` is the pipeline's source of truth, because that is the host
Gradle and Maven actually resolve against.

## Rogue tag

**Symptom:** a tag exists but the artifacts never reached Central, or the tag
points at the wrong commit.

This is only safe **before** the version has been published. Once Central has
accepted a coordinate the version number is spent forever; bump to the next one
instead.

Check first — the answer decides everything:

```bash
curl -sI "https://repo1.maven.org/maven2/com/youversion/platform/platform-core/<version>/platform-core-<version>.pom" | head -1
```

If it is a 404 on all three modules:

```bash
git tag -d <version>
git push --delete origin <version>
gh release delete <version> --yes
```

The release *commit* on `main` is a separate matter. If it was pushed, leave it:
it is a normal commit that stamped the catalog and prepended the changelog, and
rewriting `main` to remove it costs more than it is worth. Dispatch the next
release with a corrected version; the stamping is idempotent and the changelog
entry for the abandoned version can be edited out in a normal PR.

Do **not** delete a tag to "retry" a version that did publish. `release.sh`
verifies on resume that the tag's tree actually reads that version in
`gradle/libs.versions.toml` and aborts if it does not, specifically so a
hand-moved tag cannot quietly produce a release whose contents do not match its
number.

## Wrong version typed into the dispatch

**Symptom:** the dispatch ran with an incorrect `version` input.

**Fix:**

- **Caught before the `release` job starts:** cancel the run from the Actions
  UI. The `test` job is read-only and safe to abandon.
- **Caught after the tag was pushed but before publish:** see
  [Rogue tag](#rogue-tag).
- **Caught after publishing:** the version is permanent on Central. Treat it as
  a published release — correct `CHANGELOG.md` in a normal PR if the notes are
  wrong, tell consumers, and move on. A published Maven Central coordinate
  cannot be deleted, by design.

The guardrails that catch most typos before they cost anything:
`scripts/release.sh` refuses a version that is not semver or not strictly
greater than the current tag, and warns loudly when the chosen version is more
than one major above the calculated one. Neither is a substitute for reading
the run summary's chosen-vs-calculated table, which is printed before anything
is pushed.

## `release.sh` refused to start

It enforces its pre-conditions before doing any work, and the message names the
one that failed:

| Message | Cause | Fix |
|---|---|---|
| *not a bare release version* | Typo in the input, a `v` prefix, a `-beta.1` prerelease, or `+build` metadata. | Enter a bare version, e.g. `2.2.0`. The string is used verbatim as the git tag and the Maven Central coordinate, so it is refused rather than normalised — a silently corrected `v2.2.0` would publish an immutable, permanently wrong coordinate. |
| *not strictly greater than current tag* | The version was already released, or is a downgrade. | Pick the next version. If you meant to resume, the remote tag must exist — check `git ls-remote origin refs/tags/<version>`. |
| *Working tree is dirty* | Something modified tracked files before the script ran. | Investigate; do not release from a dirty tree. |
| *HEAD is not at origin/main* | Dispatched from a branch. | Re-dispatch on `main`. Rehearsals (`dry-run`) may run anywhere. |
| *Tag exists locally but not on origin* | Leftover local tag from an interrupted local rehearsal. | `git tag -d <version>`. |
| *tag's catalog does not read that version* | The tag was moved by hand. | See [Rogue tag](#rogue-tag). Do not resume. |

## Rehearsing

Before the first release after any change to this pipeline, dispatch once with
**`dry-run` ticked**. It runs end-to-end through notes generation, the changelog
prepend, version stamping, the release commit and the tag, then stops — nothing
pushed, nothing published.

Locally, on any branch:

```bash
npm ci
VERSION=2.2.0 DRY_RUN=1 bash scripts/release.sh
```

Then inspect `git show HEAD`, `git show HEAD --stat`, and `CHANGELOG.md`. Undo
with `git tag -d 2.2.0 && git reset --hard HEAD~1`.

The helper scripts have their own suite, which runs on every pull request:

```bash
bash scripts/test-release-scripts.sh
```

## Required GitHub Environments

| Environment | Purpose |
|---|---|
| `production` | Holds the four Maven Central and GPG secrets, and a deployment-branch policy limiting deployments to `main`. The `release` job declares `environment: production`; nothing else can read those secrets. |

There is no second environment. Breaking releases run through the same path as
any other: the version is typed by an operator either way, so an environment
gate adds no decision that has not already been made. The breaking-change
acknowledgment happens earlier, on the pull request — see
[RELEASING.md § Major version signoff](../RELEASING.md#major-version-signoff).

`DEPLOY_KEY` is a **repository** secret, not an environment one. It is the SSH
key the release job pushes the release commit and tag with, which is how it gets
past the branch-protection ruleset on `main`.

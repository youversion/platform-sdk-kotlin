# Releasing

A release is a **manual dispatch with an explicit version input**. Nothing ships
because a pull request merged. Someone opens **Actions → Release → Run
workflow**, types the version, and runs it.

Commit messages still do real work — they decide what the *recommended* version
is, they group the release notes, and they drive the breaking-change signoff
check on pull requests. What they no longer do is decide, on their own, that a
release happens or what number it carries.

For recovery when a release fails partway, see
[docs/RELEASE-RUNBOOK.md](docs/RELEASE-RUNBOOK.md).

## Vocabulary

These five terms are used consistently in this file, in
[`scripts/release.sh`](scripts/release.sh), and in the runbook. They are worth
learning because several of them look interchangeable and are not.

| Term | Meaning |
|---|---|
| **Chosen version** | What the operator types into the `version` input. **This is what ships.** |
| **Calculated version** | What the commit analyzer computed from the commits since the last tag. Advisory; logged beside the chosen version for audit. |
| **Fresh run** | No remote tag for that version, and main does not already carry its release commit. Commits, tags, pushes, publishes, releases. |
| **Resume** | A remote tag for that version already exists, or main already carries its `chore(release)` commit without a tag. Auto-detected, not an input; picks up where the last run stopped. |
| **Rehearsal** | The `dry-run` input. Runs end-to-end up to the local release commit and tag, then stops. Never pushes, never publishes. |

## How It Works

1. **Actions → Release → Run workflow**, on `main`. Enter the `version`
   (bare semver, e.g. `2.2.0`). Optionally tick `dry-run` for a rehearsal.
2. The **test** job runs `./gradlew test`. A release is the one path that reaches
   consumers without a pull request in front of it, so the suite runs again here
   even though it already ran on the PR.
3. The **release** job runs [`scripts/release.sh`](scripts/release.sh) inside the
   `production` environment, which is where the Maven Central and signing
   credentials live. In order, the script:
   - validates that the chosen version is semver and strictly greater than the
     current tag;
   - computes the **calculated version** and writes both numbers side-by-side
     into the run summary, so an override is on the record;
   - warns (does not block) if the chosen version is more than one major above
     the calculated one — the usual sign of a typo;
   - generates the release notes from the commits since the last tag;
   - prepends them to `CHANGELOG.md`;
   - stamps the version into `gradle/libs.versions.toml` and the `README.md`
     install snippets ([`scripts/stamp-version.sh`](scripts/stamp-version.sh));
   - commits `chore(release): <version> [skip ci]` and tags it;
   - pushes the commit and tag to `main` over the deploy key;
   - publishes each module in `PUBLISHABLE_MODULES` to Maven Central, one Gradle
     invocation per module
     ([`scripts/gradle-publish-wrapper.sh`](scripts/gradle-publish-wrapper.sh));
   - creates the GitHub Release from the generated notes.
4. The **post-publish-verify** job polls `repo1.maven.org` for up to 30 minutes
   and writes a table to the run summary showing when each coordinate became
   resolvable. It never fails the workflow — mirror lag is expected, and by that
   point the artifacts are already immutable on Central.

All three modules always ship on the same version. `PUBLISHABLE_MODULES` in
[`.github/workflows/release.yml`](.github/workflows/release.yml) is the single
place that list is declared. A consumer mixing `2.1.2` `platform-core` with
`2.2.0` `platform-ui` is not a configuration we support.

### How the version reaches the artifacts

The workflow passes the chosen version to Gradle as `-PsdkVersion`. That single
property does two things:

- It is the **published coordinate** — each module's `coordinates(...)` call
  reads it, falling back to `libs.versions.youversionPlatform` for local builds.
- It is baked into `platform-core`'s `BuildConfig.SDK_VERSION`, which every SDK
  request sends as an `x-yvp-sdk: KotlinSDK={version}` header so the data team
  can attribute traffic. Outside a release build the property is unset and the
  value falls back to `Dev`.

There is no "restore to Dev" commit after a release.
`gradle/libs.versions.toml` holds the last released version by design — it is
what the README snippets and the three `coordinates(...)` calls resolve to.

## Choosing the version

The calculated version is computed for you in three places. Use whichever is at
hand:

- **On the pull request.** Every PR gets a Commit Lint comment showing the
  version that would ship if it merged and nothing else landed first.
- **Locally.**
  ```bash
  npm ci
  node scripts/preview-release.mjs --base "$(git describe --tags --abbrev=0)" --head main
  ```
- **In the release run summary**, beside the chosen version — though by then
  you have already typed it.

You are free to enter something different. The chosen version always wins; the
workflow just makes the divergence visible. Reasons you might: shipping a `2.0.0`
that the commits describe as a minor because the API break is in behaviour
rather than signature, or skipping a number that was burned by a failed publish
(Maven Central coordinates are immutable — a version that partially uploaded can
never be reused).

## Conventional Commits

All commits must follow the
[Conventional Commits](https://www.conventionalcommits.org/) format. This is
enforced on pull requests by the
[Commit Lint workflow](.github/workflows/commitlint.yml).

### Format

```
<type>[optional scope][!]: <description>

[optional body]

[optional footer(s)]
```

### Common Types

| Type       | Description                          | Version Bump |
|------------|--------------------------------------|--------------|
| `feat`     | A new feature                        | Minor        |
| `fix`      | A bug fix                            | Patch        |
| `perf`     | Performance improvements             | Patch        |
| `docs`     | Documentation changes                | None         |
| `style`    | Code style changes (formatting, etc) | None         |
| `refactor` | Code refactoring                     | None         |
| `test`     | Adding or updating tests             | None         |
| `build`    | Build system or dependency changes   | None         |
| `ci`       | CI/CD configuration changes          | None         |
| `chore`    | Other changes                        | None         |

The bump column is asserted in CI by
[`scripts/assert-bump-table.mjs`](scripts/assert-bump-table.mjs), which runs the
real analyzer against the real config on every pull request. If this table and
the analyzer ever disagree, the build goes red.

### Breaking Changes

**Both forms work, and both bump the major.** Use either:

```
feat!: remove deprecated BibleText API
```

```
feat: remove deprecated BibleText API

BREAKING CHANGE: The `BibleText(passage: String)` overload has been removed. Use `BibleText(reference: BibleReference)` instead.
```

Prefer the footer when there is anything to say, and say it in full: the footer
lands verbatim in both `CHANGELOG.md` and the GitHub Release, and it is the
entire migration guide consumers get. The `!` marker alone gets them a subject
line.

> **This changed in YPE-5781.** The repository previously pinned
> `conventional-changelog-angular`, whose header pattern does not recognise `!`.
> A `feat!:` commit parsed with `type`, `scope`, and `subject` all `null`: it
> silently produced no release at all, or a release where the commit appeared
> with no type and an empty subject. Two changes were needed to fix it, and
> either one alone is a silent no-op that looks like it worked — the
> `conventionalcommits` preset key in `.releaserc.json`, **and** a direct
> `conventional-changelog-conventionalcommits@^8` dependency, because the v7
> preset exports a shape the current analyzer does not understand and falls back
> to angular without complaining. `assert-bump-table.mjs` exists to keep that
> from regressing unnoticed.

Note for squash merges: the merge commit body is the PR description, and footers
run to the end of the message. Put `BREAKING CHANGE:` on its own line at the
bottom. Do not put `!` in a PR *title* unless you mean it — the title becomes the
squashed subject.

## Major version signoff

When a pull request's commits would produce a **major** bump, the
[Major Release Signoff workflow](.github/workflows/major-release-signoff.yml)
asks for a written acknowledgment on the PR and posts a
`major-release-signoff` commit status.

**This check is advisory. It does not block merging.** The "Stable Main" ruleset
declares no required status checks, so a red signoff can be merged straight past.
It is there so that a deliberate break is attributable, not so that it is
impossible. The release itself is a manual dispatch with an operator-typed
version, so a major cannot ship by accident even if the check is ignored.

To sign off, a repository collaborator with **write access who is not the PR
author** comments with all three of:

1. the verbatim acknowledgment phrase,
2. the precise next version (`3.0.0` or `v3.0.0`), and
3. a 🚀.

The bot posts a copy-paste-ready reply on the PR. The check re-runs
automatically when a qualifying comment is posted, edited, or deleted.

To make the check actually gate, add `major-release-signoff` to
`required_status_checks` on the ruleset (**Settings → Rules → Stable Main**) and
update this section.

## Rehearsing a release

Before the first release after a change to this pipeline, dispatch once with
**`dry-run` ticked**. The workflow runs end-to-end through notes generation,
the changelog prepend, version stamping, the release commit, and the tag — then
stops. Nothing is pushed, nothing is published, no GitHub Release is created.

The run summary shows the chosen and calculated versions and flags itself as a
rehearsal. Read the log to confirm the notes look right and the stamped files
changed where you expect, then dispatch again without `dry-run`.

You can rehearse locally too, on any branch:

```bash
VERSION=2.2.0 DRY_RUN=1 bash scripts/release.sh
```

## Resuming a partial release

If a release fails after the tag was pushed — the network dropped mid-upload,
one module's signing errored, the job timed out — **re-dispatch with the same
version**. Resume is auto-detected, not an input: `release.sh` sees the existing
remote tag and picks up from there.

It first verifies that the tag's tree is actually stamped to that version, and
aborts if not, rather than healing a tag someone moved by hand. Then every
remaining step is idempotent: pushes that already landed are skipped, a module
Maven Central already has counts as success, and an existing GitHub Release is
left alone.

Central's immutability means a coordinate that fully published can never be
re-uploaded, which is why "already exists" is treated as success rather than as
an error to retry past. The wrapper classifies the failures it cannot recover
from: **exit 42 = GPG signing failure** (re-dispatching will not help; fix the
secret first), exit 1 = transient.

[docs/RELEASE-RUNBOOK.md](docs/RELEASE-RUNBOOK.md) covers each failure mode in
detail.

## Why not semantic-release?

The pipeline still uses semantic-release's
[commit-analyzer](https://github.com/semantic-release/commit-analyzer) and
[release-notes-generator](https://github.com/semantic-release/release-notes-generator)
— but as **libraries**, called directly from
[`scripts/preview-release.mjs`](scripts/preview-release.mjs) and
[`scripts/generate-release-notes.mjs`](scripts/generate-release-notes.mjs).
semantic-release itself is no longer the runner. Two reasons:

**It cannot be told what version to ship.** semantic-release computes
`semver.inc(lastRelease.version, type)` and exposes no hook to override it. The
only way to ship a different number is to engineer the commit history — brittle,
and invisible at the point where the decision is being made.

**Its lifecycle fights the preview.** On a `pull_request` event the
`GITHUB_TOKEN` has `contents: read`, so semantic-release's `verifyAuth()` step
(a `git push --dry-run`) aborts *before* `analyzeCommits` ever runs. The preview
fails open as "no bump" — the most dangerous possible wrong answer. `--no-ci`
does not help; it skips the CI environment check, not auth verification. Calling
the analyzer directly skips the whole lifecycle and just answers the question.

Everything that was worth keeping is kept: conventional-commit analysis, grouped
release notes, the changelog entry, the tag, the GitHub Release. What was dropped
is the orchestration we were fighting. This mirrors how the Swift SDK's release
pipeline works.

## Troubleshooting

### The PR comment says "no release"

At least one commit must use a release-triggering type (`feat:`, `fix:`, or
`perf:`). `docs:`, `chore:`, `ci:`, `style:`, `test:` and `refactor:` do not
bump on their own. You can still dispatch a release by hand with any version you
like — the analyzer is advisory.

### `release.sh` refuses to run

It enforces its pre-conditions loudly and each message says which one failed:

- *not valid semver* / *not strictly greater than current tag* — check the input.
- *Working tree is dirty* — something modified tracked files before the script ran.
- *HEAD is not at origin/main* — the workflow was dispatched from a branch. Live
  releases must run on `main`; rehearsals may run anywhere.
- *Tag exists locally but not on origin* — a leftover local tag; push or delete it.
- *Tag exists but its tree's catalog does not read that version* — the tag was
  moved by hand. Do not resume; see the runbook.

### Release workflow failed

- [docs/RELEASE-RUNBOOK.md](docs/RELEASE-RUNBOOK.md) has the per-failure-mode
  procedure.
- Check the
  [Actions tab](https://github.com/youversion/platform-sdk-kotlin/actions/workflows/release.yml).
- Confirm the credentials below are present **on the `production` environment**
  (not as repository secrets) — see the next section.

### Testing the helper scripts

```bash
bash scripts/test-release-scripts.sh
```

Runs on every pull request. Covers the argument and exit-code behaviour of the
small `.mjs` helpers, and asserts the analyzer's bump table end-to-end. Since the
release pipeline itself only runs on a manual dispatch, without these a broken
helper would not be discovered until someone tried to ship.

## Credentials

### Required secrets

These are **environment secrets on `production`**, not repository secrets
(**Settings → Environments → production → Environment secrets**). Only a job
that declares `environment: production` can read them.

| Secret | Description |
|--------|-------------|
| `ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME` | Maven Central (Sonatype) username |
| `ORG_GRADLE_PROJECT_MAVENCENTRALPASSWORD` | Maven Central (Sonatype) password |
| `SIGNINGKEY` | GPG signing key (armored, base64-encoded) |
| `SIGNINGKEY_PASSWORD` | GPG signing key passphrase |

`DEPLOY_KEY` is a **repository** secret. It is the SSH key the release job uses
to push the release commit and tag to `main` past the branch-protection ruleset.

### Required environments

| Environment | Purpose |
|---|---|
| `production` | Holds the Maven Central and signing credentials. Its deployment-branch policy (`main`) is what keeps a dispatch from a feature branch from reaching them. `scripts/release.sh` independently refuses to push when HEAD is not `origin/main`. |

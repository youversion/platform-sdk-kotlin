# Release Hardening — Engineering Plan (YPE-2791)

This is the implementation plan committed in response to [YPE-2791 — Harden and document manual release procedure (Kotlin SDK)](https://youversion.atlassian.net/browse/YPE-2791). It documents the design choices the release workflow now reflects and the rationale behind each, so future contributors can extend the pipeline without re-deriving the constraints.

## Context

The Kotlin SDK releases via `semantic-release` on push to `main`. The flow worked end-to-end but had no safety net: no pre-publish check that the version already existed on Central, no recovery path when a publish partially failed across `platform-core` / `platform-ui` / `platform-reader`, no documented break-glass procedure when CI was unavailable, no awareness of Central's resolution lag after a successful publish, and no special approval for breaking changes.

Two ticket-text assumptions did not match the repo and shaped the plan:

- **Not KMP.** All three modules are single-target Android libraries. The ticket's "per-target loop" guidance applies as a **per-coordinate loop** across the three modules instead.
- **Already on Central Portal** via `com.vanniktech.maven.publish` with `automaticRelease = true` ([`build.gradle.kts:60-63`](../build.gradle.kts)). The legacy OSSRH `open → closed → released → dropped` staging-repo state machine is abstracted away by the plugin. Observable states are: **submitted → validating → released → resolvable on repo1.maven.org**.

## What changed

### 1. `.github/workflows/release.yml` — four sequential jobs

- **compute-version** — derives the next version (`semantic-release --dry-run` on push, dispatch input on workflow_dispatch) and detects whether the commit window contains a BREAKING CHANGE. Emits `version`, `modules`, `breaking` as job outputs.
- **preflight** — runs [`scripts/check-central-presence.sh`](../scripts/check-central-presence.sh) to probe `repo1.maven.org` for each module/version. Outputs `missing_modules` (CSV). When everything is already present, downstream jobs short-circuit to "no-op success" — the AC#2/AC#3 idempotency guarantee.
- **publish** — environment selected at runtime: `production-breaking` when `breaking == true`, `production` otherwise. On push: runs `npx semantic-release` as before. On workflow_dispatch: runs [`scripts/gradle-publish-wrapper.sh`](../scripts/gradle-publish-wrapper.sh) against only the missing modules.
- **post-publish-verify** — runs [`scripts/poll-central-index.sh`](../scripts/poll-central-index.sh) to wait up to 30 minutes for `repo1.maven.org` propagation, then re-runs the presence script. Always writes a `$GITHUB_STEP_SUMMARY` table; never fails the workflow on propagation lag.

A composite action at [`.github/actions/setup-release/action.yml`](../.github/actions/setup-release/action.yml) deduplicates the JDK 17 / Gradle / Node setup steps across the three jobs that need them.

### 2. Idempotency probes — `repo1.maven.org`, not `search.maven.org`

`scripts/check-central-presence.sh` and `scripts/poll-central-index.sh` both probe `https://repo1.maven.org/maven2/com/youversion/platform/<module>/<version>/<module>-<version>.pom`. `repo1.maven.org` is the host Gradle/Maven actually resolve dependencies against — it is the source of truth for "is this release usable?"

`search.maven.org` (the discovery search index) is a separate, slower system; Portal-published artifacts can take hours or never appear in that index even when they are fully resolvable. Probing it would generate false negatives.

### 3. GPG signing fast-fail classification

`scripts/gradle-publish-wrapper.sh` tees Gradle output and classifies failures:

- **Exit 42** — signing failed (expired key, missing passphrase, missing signing-capable subkey). Re-dispatch will not help. The wrapper writes a clear `$GITHUB_STEP_SUMMARY` block pointing at the runbook's GPG section.
- **Exit 1** — other failure (network, transient Central error). Re-dispatch is reasonable.
- **Exit 0** — success.

Pattern list (from the wrapper, mid-2026): `signingInMemoryKey`, `Could not read PGP secret key`, `Bad passphrase`, `Cannot perform signing task`, `No suitable secret key was found`, `gpg.*expired`, `task .*sign[A-Za-z]*Publication.* FAILED`. Patterns are deliberately broad — false-positive fast-fails are recoverable; false-negative retries are not.

### 4. BREAKING CHANGE gate

`compute-version` inspects the commit window between the last tag and `HEAD` for `^(feat|fix)(\([^)]*\))?!:` or `BREAKING CHANGE:`. When found, the `publish` job uses a dedicated `production-breaking` GitHub Environment whose required reviewers gate the run.

The non-breaking path continues to use the existing `production` environment. Rationale: gating every release behind a reviewer creates friction on routine patch/minor work; gating only breaking releases pushes the human cost to where it matters.

`production-breaking` is a one-time repo-settings configuration documented in [RELEASE-RUNBOOK.md](RELEASE-RUNBOOK.md#required-github-environments).

### 5. Commit-lint hardening

[`.github/workflows/commitlint.yml`](../.github/workflows/commitlint.yml) now resolves `--from` to the tip of `origin/main` at the time of each run, rather than the PR's recorded base SHA. The recorded base goes stale as soon as `main` moves forward, which previously caused already-merged commits to re-lint when a branch rebased.

[`commitlint.config.js`](../commitlint.config.js) gained an `ignores` callback for `chore(release): ...`. These are bot-authored after every gate has passed; linting them serves no purpose and would block re-running an old commit window through the lint job.

## Files at a glance

| Path | Change | Purpose |
|---|---|---|
| [`.github/workflows/release.yml`](../.github/workflows/release.yml) | rewritten | 4-job pipeline with idempotency + breaking-change gate |
| [`.github/workflows/commitlint.yml`](../.github/workflows/commitlint.yml) | edited | `--from` = current `origin/main` |
| [`.github/actions/setup-release/action.yml`](../.github/actions/setup-release/action.yml) | new | shared JDK/Gradle/Node setup |
| [`commitlint.config.js`](../commitlint.config.js) | edited | ignore `chore(release):` messages |
| [`scripts/check-central-presence.sh`](../scripts/check-central-presence.sh) | new | HEAD per-module .pom on `repo1.maven.org` |
| [`scripts/poll-central-index.sh`](../scripts/poll-central-index.sh) | new | post-publish poll of `repo1.maven.org` |
| [`scripts/gradle-publish-wrapper.sh`](../scripts/gradle-publish-wrapper.sh) | new | publish + GPG fast-fail classifier |
| [`docs/RELEASE-RUNBOOK.md`](RELEASE-RUNBOOK.md) | new | operational guide for release failures |
| [`RELEASING.md`](../RELEASING.md) | edited | document dispatch flow, breaking gate, runbook link |
| [`README.md`](../README.md) | edited | link the runbook |

## What is intentionally not addressed

- Migrating away from Maven Central — out of scope per the ticket.
- Rotating the GPG key — the runbook documents the procedure but rotation itself is a manual operational task, not a code change.
- KMP per-target publishing — this repo is not KMP. The per-coordinate loop is across the three module artifacts instead.
- A separate workflow for break-glass — the same `release.yml` handles both push and `workflow_dispatch` to avoid drift between automated and manual code paths.

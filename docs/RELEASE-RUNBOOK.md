# Release Runbook

Operational guide for recovering from failures in the Kotlin SDK release pipeline. The day-to-day "how releases work" doc is [`RELEASING.md`](../RELEASING.md); this file covers the failure modes.

The release pipeline (see [`.github/workflows/release.yml`](../.github/workflows/release.yml)) runs as four sequential jobs:

1. **compute-version** — derives the next version (from `semantic-release --dry-run` on push, or the dispatch input) and detects whether the commit window contains a `BREAKING CHANGE`.
2. **preflight** — probes `repo1.maven.org` for each module/version and reports which (if any) are already on Central. Used to skip publishing for coordinates that already exist.
3. **publish** — runs through the `production` GitHub Environment (or `production-breaking` for breaking releases) and invokes either `npx semantic-release` (push trigger) or `scripts/gradle-publish-wrapper.sh` for the missing modules (workflow_dispatch).
4. **post-publish-verify** — polls `repo1.maven.org` until every module is resolvable, then writes the indexing-status table to the workflow summary.

For a hands-on dispatch: **Actions → Release → Run workflow**, enter the target version and optionally a comma-separated module list.

## Upload transient failure

**Symptom:** `publish` job fails with a network error, 5xx from `central.sonatype.com`, or the Gradle daemon dying mid-upload. `scripts/gradle-publish-wrapper.sh` exits `1` (not `42`).

**Fix:** Re-dispatch with the same `version` input. The `preflight` job will detect any modules that did make it onto Central and only republish the ones still missing. No manual cleanup is needed.

## Stuck closing / stuck released-not-indexed

**Symptom:** `publish` reports success, but `post-publish-verify` shows ⏳ for one or more modules in the workflow summary. `repo1.maven.org` returns 404 for those `.pom` URLs.

**Fix:** Wait. Central Portal usually propagates to `repo1.maven.org` within minutes but can take up to ~30 minutes. The verify step polls for 30 minutes and never fails the workflow — it just reports the current state. If the wait exceeds 30 minutes:

1. Check [Central Portal → Publishing Deployments](https://central.sonatype.com/publishing/deployments) for the deployment status. "RELEASED" means Portal is done and the lag is purely on the repo1 mirror side.
2. Open a Sonatype support issue if `repo1.maven.org` still returns 404 more than 2 hours after a "RELEASED" deployment.
3. **What to tell consumers in the gap:** "Version X is released to Maven Central and will become resolvable within ~30 minutes (occasionally longer). If your build cannot find it yet, force a refresh with `./gradlew --refresh-dependencies` after the propagation completes."

Note: `search.maven.org` (the discovery search index) is a separate, slower system. Portal-published artifacts often take hours or never appear in that index even when they are fully resolvable. The pipeline does not monitor it.

## GPG signing failure (expired key / missing passphrase)

**Symptom:** `publish` exits with code **42** and the workflow summary contains "GPG signing failure". This is unrecoverable until the signing secrets are fixed — re-dispatching will fail the same way.

**Fix:**

1. **Diagnose** by inspecting the failed step's Gradle output. Common patterns:
   - `Could not read PGP secret key` → `SIGNINGKEY` secret is missing, empty, or malformed.
   - `Bad passphrase` → `SIGNINGKEY_PASSWORD` does not match the key in `SIGNINGKEY`.
   - `No suitable secret key was found` → the key was generated without a signing-capable subkey, or the wrong key was exported.
   - `gpg: ... expired` → the key (or subkey used for signing) has reached its expiration date.
2. **Rotate or extend the key.** This is a manual procedure that touches GitHub secrets and Maven Central; do not attempt to rotate from within the release workflow. The high-level steps:
   - Generate a new GPG key (or extend the existing one's expiration via `gpg --edit-key <id>`, `expire`, `save`).
   - Publish the public key to a keyserver Maven Central trusts (e.g. `keys.openpgp.org`).
   - Re-export the secret key with `gpg --armor --export-secret-keys <id> | base64` and update the `SIGNINGKEY` repo secret. Update `SIGNINGKEY_PASSWORD` if the passphrase changed.
3. **Re-dispatch** the release workflow with the same `version` input. The preflight step will gate the republish to only the modules still missing.

## Partial-publish across the three coordinates

**Symptom:** `publish` succeeded but only some of `platform-core`, `platform-ui`, `platform-reader` appear on Central; the other(s) are missing. Most commonly happens when a single module's signing or upload errored and the wrapper aborted before the remaining modules ran.

**Fix:** Re-dispatch with the same `version` input. `scripts/check-central-presence.sh` will list only the still-missing modules in `missing_modules`, and `scripts/gradle-publish-wrapper.sh` will receive that narrowed list and run only `:<missing>:publishToMavenCentral` for each. The plugin does not attempt to re-upload coordinates that Central already has, so this is safe to re-run repeatedly.

## Sonatype-vs-Central drift

**Symptom:** [Central Portal → Publishing Deployments](https://central.sonatype.com/publishing/deployments) reports "RELEASED" but `repo1.maven.org` still 404s for the `.pom` URL, or vice versa.

**Fix:**

- **Portal RELEASED, repo1 404:** propagation lag — see the "stuck released-not-indexed" section above.
- **repo1 has the artifact, Portal shows "PENDING":** Portal UI is stale; force a refresh. If still pending after an hour, contact Sonatype.
- **Portal shows "FAILED" but partial coordinates on repo1:** treat as partial-publish (above) and re-dispatch; the missing-modules logic will republish only what is missing.

The pipeline's source of truth is `repo1.maven.org` because that is the host Gradle/Maven actually resolve against.

## Rogue tag

**Symptom:** A git tag (e.g. `1.7.0`) was created but the corresponding artifacts never made it to Maven Central, or the tag points at the wrong commit.

**Fix:** Only safe before the version has been published to Central. Once Central has accepted a coordinate it is permanent.

```bash
# Delete the local + remote tag.
git tag -d <version>
git push --delete origin <version>

# Delete the GitHub Release if one was created (uses gh CLI).
gh release delete <version> --yes
```

Then either re-dispatch the release workflow with the corrected version input, or merge a new commit to `main` and let `semantic-release` regenerate the version. Confirm with `scripts/check-central-presence.sh` that the rogue version never reached Central; if it did, you cannot recover the version number — bump to the next one.

## Wrong VERSION input on workflow_dispatch

**Symptom:** A workflow_dispatch was triggered with an incorrect `version` input.

**Fix:**

- **Caught before the `publish` job starts:** cancel the workflow run from the Actions UI. The `compute-version` and `preflight` jobs are read-only and safe to abandon.
- **Caught after `publish` succeeds:** the version is now permanent on Central. Treat the mistake as a published release: update `RELEASING.md`/CHANGELOG if needed, communicate to consumers, and move on. Do not attempt to "delete" a published Maven Central coordinate — it is impossible by design.

## Required GitHub Environments

The workflow depends on two GitHub Environments:

| Environment | Purpose | Reviewers |
|---|---|---|
| `production` | Gates every non-breaking release. | Existing release approvers (already configured). |
| `production-breaking` | Gates releases whose commit window contains `feat!:`, `fix!:`, or `BREAKING CHANGE:`. | Required reviewer(s) selected for breaking-change approval. |

The `production-breaking` environment is a manual repo-settings configuration. To set it up: **Settings → Environments → New environment → `production-breaking` → Required reviewers**. No secrets need to be configured on the breaking environment — it inherits from the repo-level secrets used by the publish job.

## Pre-release validation

Before merging the PR that will trigger a release, run [`scripts/verify-release.sh`](../scripts/verify-release.sh) locally to confirm the version-stamping pipeline is healthy. It does not publish anything. The release runbook above covers the post-merge recovery surface; the verify script catches misconfiguration earlier.

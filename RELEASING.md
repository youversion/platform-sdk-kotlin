# Releasing

Releases are fully automated via [semantic-release](https://github.com/semantic-release/semantic-release). When a pull request is merged to `main`, the release workflow analyzes commits, determines the next version, publishes to Maven Central, and creates a GitHub Release.

## How It Works

1. **Merge to `main`** triggers the [Release workflow](.github/workflows/release.yml).
2. **semantic-release** analyzes commit messages since the last tag using [Conventional Commits](https://www.conventionalcommits.org/).
3. The next version is determined automatically:
   - `fix:` commits bump the **patch** version (e.g., `0.5.0` -> `0.5.1`)
   - `feat:` commits bump the **minor** version (e.g., `0.5.0` -> `0.6.0`)
   - `BREAKING CHANGE:` or `feat!:` / `fix!:` bumps the **major** version (e.g., `0.5.0` -> `1.0.0`)
4. The version in `gradle/libs.versions.toml` is updated.
5. `CHANGELOG.md` is generated/updated.
6. All three modules (`platform-core`, `platform-ui`, `platform-reader`) are published to Maven Central. The workflow passes the resolved version to Gradle via `-PsdkVersion=${nextRelease.version}`, which bakes it into `platform-core`'s `BuildConfig.SDK_VERSION`. At runtime, every SDK request sends an `x-yvp-sdk: KotlinSDK={version}` header so the data team can attribute traffic accurately. Non-release builds use the default value `Dev`.
7. A git tag and GitHub Release are created.
8. The version bump and changelog are committed back to the branch.

## Conventional Commits

All commits must follow the [Conventional Commits](https://www.conventionalcommits.org/) format. This is enforced on pull requests by the [Commitlint workflow](.github/workflows/commitlint.yml).

### Format

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Common Types

| Type       | Description                          | Version Bump |
|------------|--------------------------------------|--------------|
| `feat`     | A new feature                        | Minor        |
| `fix`      | A bug fix                            | Patch        |
| `docs`     | Documentation changes                | None         |
| `style`    | Code style changes (formatting, etc) | None         |
| `refactor` | Code refactoring                     | None         |
| `perf`     | Performance improvements             | Patch        |
| `test`     | Adding or updating tests             | None         |
| `build`    | Build system or dependency changes   | None         |
| `ci`       | CI/CD configuration changes          | None         |
| `chore`    | Other changes                        | None         |

### Breaking Changes

Append `!` after the type or include a `BREAKING CHANGE:` footer:

```
feat!: remove deprecated BibleText API

BREAKING CHANGE: The `BibleText(passage: String)` overload has been removed. Use `BibleText(reference: BibleReference)` instead.
```

## Pre-release Branches

Push to `beta` or `alpha` branches to publish pre-release versions:

- `beta` branch: publishes versions like `1.0.0-beta.1`
- `alpha` branch: publishes versions like `1.0.0-alpha.1`

## Maintenance Releases

For patching older major versions, create a branch named `N.x` (e.g., `1.x`). Commits merged to that branch will produce patch releases for that major version line.

## Verifying a Release Locally

`scripts/verify-release.sh` smoke-tests the release pipeline without publishing. Run it before merging to confirm that the version-stamping pieces still work end-to-end.

### What it checks

1. **AAR contents** — builds `platform-core`'s release AAR with a fake `-PsdkVersion` and confirms the value is present in `BuildConfig.SDK_VERSION` inside the published bytes.
2. **`.releaserc.json` template** — confirms semantic-release's `publishCmd` still passes `-PsdkVersion=${nextRelease.version}` to `publishToMavenCentral`.
3. *(optional, with `--with-dry-run`)* **semantic-release dry-run** — runs `npx semantic-release --dry-run` against your current branch, reports the version that would be released, and reconstructs the planned gradle command.

### Usage

```bash
# Tiers 1 + 2 (Gradle build only, ~15s)
scripts/verify-release.sh

# Override the test version used for AAR inspection
scripts/verify-release.sh --version 2.0.0-beta.3

# Add Tier 3 — npx semantic-release --dry-run
scripts/verify-release.sh --with-dry-run
```

Sample successful output:

```
==> Building platform-core AAR with -PsdkVersion=1.99.0-verify.local
==> Inspecting BuildConfig.SDK_VERSION inside the AAR
    PASS  BuildConfig.SDK_VERSION = "1.99.0-verify.local"

==> Verifying .releaserc.json publishCmd template
    PASS  publishCmd: ./gradlew publishToMavenCentral -PsdkVersion=${nextRelease.version}

==> Running semantic-release --dry-run (treating current branch as a release branch)
    PASS  computed next version: 1.3.0
    PASS  would invoke: ./gradlew publishToMavenCentral -PsdkVersion=1.3.0
```

### Notes

- The script never publishes anything — Tier 1 builds locally, Tier 3 uses dry-run mode.
- `--with-dry-run` requires `node_modules` (the script runs `npm ci` if missing). It does **not** require `GITHUB_TOKEN`: the script temporarily mutates `.releaserc.json` to strip the GitHub plugin and pin `branches` to your current branch, which must already be pushed to the remote.
- `.releaserc.json` is restored unconditionally by an `EXIT` trap, even on `Ctrl-C` or unexpected failure. A leftover `.releaserc.json.verify-release.bak` would indicate the trap didn't fire — the file is gitignored so it won't accidentally be committed.

## Troubleshooting

### No release created after merge

- Ensure at least one commit uses a release-triggering type (`feat:` or `fix:`).
- Commits with types like `docs:`, `chore:`, `ci:`, `style:`, `test:`, or `refactor:` do not trigger a release on their own.

### Release workflow failed

- Check the [Actions tab](https://github.com/youversion/platform-sdk-kotlin/actions/workflows/release.yml) for logs.
- Verify that Maven Central and signing secrets are configured as repo-level secrets.

### Required Secrets

| Secret | Description |
|--------|-------------|
| `ORG_GRADLE_PROJECT_MAVENCENTRALUSERNAME` | Maven Central (Sonatype) username |
| `ORG_GRADLE_PROJECT_MAVENCENTRALPASSWORD` | Maven Central (Sonatype) password |
| `SIGNINGKEY` | GPG signing key (armored, base64-encoded) |
| `SIGNINGKEY_PASSWORD` | GPG signing key passphrase |

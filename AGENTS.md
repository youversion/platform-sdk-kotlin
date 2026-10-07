# Agent instructions for `platform-sdk-kotlin`

Public Android library at https://github.com/youversion/platform-sdk-kotlin. Third-party apps integrate YouVersion Bible text through API helpers and Jetpack Compose, up to a drop-in `BibleReader`. Multiplatform is not available.
Consumer docs: README.md and https://developers.youversion.com/sdks/kotlin.
`CONTEXT.md` is the glossary. Use each defined term's canonical name, not a wording listed under `_Avoid_`.

## Read these, do not copy them

- Setup, build, tests, sample app, formatting, and public API dumps: CONTRIBUTING.md
- Versions and publishing: RELEASING.md
- Failed release recovery: docs/RELEASE-RUNBOOK.md
- CI: the `.github/workflows` directory
- User-facing strings: docs/localization-guardrails.md
- Reader classpath split: platform-reader/build.gradle.kts

## Modules

- `platform-core`: Bible, VOTD, Highlights, Languages, Users, Data Exchange, and Organizations clients; `YouVersionPlatformConfiguration`; highlights domain; Koin; models. No UI.
- `platform-ui`: Compose components plus `rememberSignIn`, `rememberDataExchange`, and `SignInWithYouVersionActivity`. Depends on `platform-core`.
- `platform-reader`: main entry for apps. `api()` on `platform-core` because `BibleReader` takes a `BibleReference`. `implementation` on `platform-ui` because `platform-ui` never appears in public signatures.
- `examples/sample-android` demonstrates the SDK.
- Flow: `platform-core` ← `platform-ui` ← `platform-reader` ← `sample-android`.

## Initialization

- Apps call `YouVersionPlatformConfiguration.configure()` with an `appKey` from `Application.onCreate()`, which starts Koin (`startYouVersionPlatform()`).
- Koin provides `HttpClient` (Ktor), `Store` (SharedPreferences), and `Logger`.
- Call sites use `YouVersionApi` (`bible`, `dataExchange`, `highlights`, `languages`, `organizations`, `users`, `votd`). Every API method is a suspend function.
- Configuration: `platform-core/src/main/java/com/youversion/platform/core/YouVersionPlatformConfiguration.kt`
- DI: `platform-core/src/main/java/com/youversion/platform/core/utilities/koin/`
- API entry: `platform-core/src/main/java/com/youversion/platform/core/api/YouVersionApi.kt`
- Highlights domain: `platform-core/src/main/java/com/youversion/platform/core/highlights/domain/`

## Permissions

- Values are `SignInWithYouVersionPermission`: `OPENID`, `PROFILE`, `EMAIL`, `HIGHLIGHTS`.
- A grant cannot be revoked from the app. Do not design a flow that loses a permission.
- A signed-out user grants inside `rememberSignIn`. A signed-in user grants through `rememberDataExchange`, which needs an access token or `dataExchangeToken()` throws `MISSING_AUTHENTICATION`. Both return on `youversionauth://callback`, handled by `SignInWithYouVersionActivity`.
- Grants follow the session. `configure()` and `saveAuthData()` drop stored grants when the tokens they are given name a different session, and `configure()` does not fill omitted tokens from storage in that case. Compare each given token with its own stored counterpart.
- A data-exchange grant belongs to the session that requested it. `DataExchangeHandler` records that session before opening the browser, and `persistGrantedPermissions` drops the grant if another user is signed in when it returns. A grant that returns after process death has no recorded session and is kept.
- Highlights load only for a signed-in user who has `HIGHLIGHTS`. `BibleText` skips the fetch when either is missing.

## Branches

- Every change, including docs and tooling, is a `<JIRA-TICKET>-<kebab-description>` branch. Merge to `main` only through a pull request. No initials prefix and no `feature/` prefix.
- The pull request title matches the first line of the commit message.
- A multi-ticket feature uses an epic branch from `main`, task branches from that epic branch, task pull requests into the epic branch, then one pull request from the epic branch to `main`.
- Bring `main` in by merging it into the epic branch first, then merge that epic branch into the task branch.

## Kotlin

- Follow https://developer.android.com/kotlin/style-guide. Do not make whitespace-only changes.
- Prefer suspend functions. An asynchronous function that returns a value is a noun phrase and does not start with "get", "load", or "request".
- Document new non-private functions. Do not add inline comments inside functions, and do not delete existing ones.
- Use the strictest access. Prefer `val`. Properties come before functions. Do not leave unused code or commented-out code.
- A Boolean starts with "is", "has", "should", "shows", or "showing". A non-Boolean name ends with its type, as in `shadowColor`.
- Do not write an unnecessary `this.`. A class is `open` only when it is subclassed. Prefer a data class or a value class.
- Types are PascalCase. Properties and functions are camelCase. Avoid abbreviations.
- Internal catalog: `gradle/libs.versions.toml`. Local SDK path: `local.properties` `sdk.dir`, or `ANDROID_HOME`.

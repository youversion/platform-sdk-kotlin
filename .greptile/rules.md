# Localization Guardrails

These rules are **advisory** in Greptile PR review. The merge gate is the Gradle task `verifyNoHardcodedUiStrings`.

## Hardcoded UI strings (platform-ui, platform-reader)

- Flag string literals used for user-visible UI: `Text(...)`, `BasicText(...)`, `title = "..."`, `contentDescription = "..."`, `Toast.makeText(...)`, and enum labels rendered as tabs or section headers.
- Prefer `stringResource(R.string.*)` or `pluralStringResource()` backed by synced `yv_*` keys from platform-localization.
- Do not suggest suppressing violations — wire existing keys or add keys in platform-localization and sync.

## Protected localization files

Do not edit synced catalogs in feature PRs:

- `**/strings_i18n.xml`
- `**/values-*/strings.xml`

Add or change keys in **platform-localization**, then sync into this repo.

## Exclusions (do not flag)

| Category | Examples / paths |
|----------|------------------|
| Sample app | `examples/sample-android/**` (entire tree) |
| Font display names | `ReaderFontSettings.kt` — Untitled Serif, Serif, System Default, etc. |
| Brand / proper names | YouVersion, YouVersion Logo, Bible Logo (`BibleCard.kt`, `VerseOfTheDay.kt`, `SignInWithYouVersionButton.kt`) |
| Formal name fallbacks | `"English"` default for `activeLanguageName` in `BibleVersionsViewModel.kt` |
| `@Preview` / `@CombinedPreview` | Compose preview scaffolding only |
| `testTag` strings | `Modifier.testTag("...")` — UI test selectors |
| Log messages | `Log.*`, `logger.*` |
| USFM / CSS constants | Markup/format tokens in reader rendering |
| Storage keys | SharedPreferences / DataStore key strings |
| Navigation routes | Compose Navigation route constants (e.g. `BibleReaderDestination`) |

## References

- Full policy: `docs/localization-guardrails.md`
- Local check: `./gradlew verifyNoHardcodedUiStrings`

## agents-md-index-contract

Advisory. This comment does not block merge.

`AGENTS.md` is an index of 100 lines or fewer. Flag a change that names a workflow file (the substring `.yml`), mentions `CLAUDE.md`, pastes localization policy instead of the single pointer to `docs/localization-guardrails.md`, or says `platform-reader` depends on both `platform-core` and `platform-ui` with `api()`. The split stays `api()` for `platform-core` and `implementation` for `platform-ui`.

## reader-core-api-ui-implementation

Advisory. This comment does not block merge.

`platform-reader` declares `api(projects.platformCore)` because `BibleReader` takes a `BibleReference`, and `implementation(projects.platformUi)` because `platform-ui` does not appear in public signatures. Flag a change that uses `implementation` for `platform-core` or `api` for `platform-ui`.

## permission-grant-not-revoked

Advisory. This comment does not block merge.

A grant cannot be revoked while the same user stays signed in. Flag a new `grantedPermissionValues = emptySet()` (or a helper that does that) outside the three existing clears: `configure` when `!keepsStoredSession` (line 211), `saveAuthData` when the tokens name a different session (line 312), and `clearAuthData` on sign-out (line 374). Do not flag `completePermissionGrant` when it returns false because the recorded session no longer matches.

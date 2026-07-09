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
| Brand / proper names | YouVersion, YouVersion Logo, Bible Logo (`BibleAppLogo.kt`, `SignInWithYouVersionButton.kt`) |
| Formal name fallbacks | `"English"` default for `activeLanguageName` in `BibleVersionsViewModel.kt` |
| `@Preview` / `@CombinedPreview` | Compose preview scaffolding only |
| `testTag` strings | `Modifier.testTag("...")` — UI test selectors |
| Log messages | `Log.*`, `logger.*` |
| USFM / CSS constants | Markup/format tokens in reader rendering |
| Storage keys | SharedPreferences / DataStore key strings |
| Navigation routes | Compose Navigation route constants (e.g. `BibleReaderDestination`) |

## References

- Plan: `.omc/plans/YPE-3549-localization-guardrails.md`
- Local check: `./gradlew verifyNoHardcodedUiStrings`

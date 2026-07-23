# Localization Guardrails

User-facing copy in the YouVersion Platform SDK (Kotlin) is owned by [platform-localization](https://github.com/youversion/platform-localization) and synced into this repository. SDK modules must not ship new hardcoded UI strings, and synced string catalogs must not be edited directly in feature PRs.

## String architecture

| Layer | File | Who edits |
|-------|------|-----------|
| Canonical (synced) | `**/values/strings_i18n.xml` | platform-localization sync workflow only |
| Locale overlays | `**/values-*/strings.xml` | platform-localization sync workflow only |
| SDK aliases | `**/values/strings_aliases.xml` | SDK developers (maps `R.string.*` → `@string/yv_*`) |

**Modules with UI strings:** `platform-ui`, `platform-reader`

**Out of scope:** `examples/sample-android` (demo app)

## How to add or change user-facing copy

1. Add or update keys in **platform-localization**.
2. Run the localization sync workflow to update `strings_i18n.xml` and locale files in this repo.
3. Add an alias in `strings_aliases.xml` if the SDK needs an unprefixed `R.string.*` name.
4. Reference the string in Kotlin with `stringResource(R.string.*)` or `pluralStringResource()`.

Do **not** hand-edit `strings_i18n.xml` or locale `strings.xml` files in feature PRs.

## Enforcement

| Layer | Mechanism | Blocks merge? |
|-------|-----------|---------------|
| **CI** | `./gradlew verifyNoHardcodedUiStrings` | Yes |
| **CI** | Protected localization file guard | Yes |
| **Greptile** | `.greptile/` rules | No (advisory PR comments) |

### Local checks

```bash
# Fail on hardcoded UI strings in platform-ui and platform-reader
./gradlew verifyNoHardcodedUiStrings

# Root check also runs the guardrail task
./gradlew check
```

## Hardcoded string policy

The Gradle task scans `platform-ui/src/main` and `platform-reader/src/main` for user-facing string literals in:

- `Text(...)`, `BasicText(...)`, `text = "..."`
- `title = "..."`, `contentDescription = "..."`
- `Toast.makeText(...)`
- Enum labels used as UI tab/section headers

**Fix violations** by wiring existing `R.string.*` keys or adding keys in platform-localization and syncing.

### Exclusions (not flagged)

| Category | Examples |
|----------|----------|
| Sample app | `examples/sample-android/**` |
| Font display names | `ReaderFontSettings.kt` enum labels (Untitled Serif, Serif, etc.) |
| Brand / proper names | YouVersion, YouVersion Logo, Bible Logo |
| Formal name fallbacks | `"English"` default in `BibleVersionsViewModel.kt` |
| `@Preview` blocks | Compose preview scaffolding |
| `testTag` strings | UI test selectors |
| Log messages | `Log.*`, `logger.*` |
| USFM / CSS constants | Markup tokens in reader rendering |
| Storage keys | SharedPreferences / DataStore keys |
| Navigation routes | Internal route constants |

## Protected localization files

These paths may only be modified by the localization sync bot (see `config/i18n/localization-bot-allowlist.txt`):

- `platform-ui/src/main/res/values/strings_i18n.xml`
- `platform-ui/src/main/res/values-*/strings.xml`
- `platform-reader/src/main/res/values/strings_i18n.xml`
- `platform-reader/src/main/res/values-*/strings.xml`

`strings_aliases.xml` is **not** protected — SDK developers maintain aliases locally.

## Known migration work

Seventeen hardcoded strings in `platform-ui` and `platform-reader` still need to be wired to existing `stringResource()` keys. Keys already exist in `strings_i18n.xml` / `strings_aliases.xml` — migration is wire-up only, not new key creation.

Until migrated, `./gradlew verifyNoHardcodedUiStrings` and CI will fail. Fix by replacing literals with `stringResource()` in a follow-up PR.

## Greptile

PR review rules live in `.greptile/`. Greptile is advisory and reviews PR diffs — it supplements but does not replace the Gradle CI gate.

Re-trigger review after config changes with `@greptileai review`.

## References

- Greptile rules: `.greptile/rules.md`
- Bot allowlist: `config/i18n/localization-bot-allowlist.txt`
- CI workflow: `.github/workflows/localization.yml`

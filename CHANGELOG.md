# [2.0.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.10.1...2.0.0) (2026-09-10)


* Release/2.0.0 ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))


### BREAKING CHANGES

* BibleReference can no longer be built with only one of
verseStart and verseEnd, and the field-by-field constructor is no longer
public. Use BibleReference(versionId, bookUSFM, chapter) for a whole chapter,
the single-verse constructor for one verse, or pass both verses for a range.

The single-verse constructor's verse argument is also no longer nullable.
Callers passing an Int? to mean "this verse, or the whole chapter if I have
none" must now branch on the null themselves and pick the matching
constructor.

This breaks binary compatibility as well as source compatibility. Every
constructor descriptor a 1.x caller linked against is gone or private: the
field-by-field constructor took (int, String, int, Integer, Integer), and the
old single-verse constructor took (int, String, int, Integer), reaching the
whole-chapter case through its synthetic default bridge. Calls that still
compile unchanged now bind to new (int, String, int), (int, String, int, int)
and (int, String, int, int, int) descriptors, so anything compiled against
1.x fails with NoSuchMethodError against this version and must be recompiled.

copy() keeps its signature and stays binary compatible, but now throws
IllegalArgumentException for a copy that would produce a half-specified
reference, where it previously succeeded.

* refactor(core): remove the unreachable half-reference handling

Ticket 05 made a reference with only one of its two verses impossible to
build, so the defensive fallbacks for that shape are now dead code.

- Replace the duplicated verse-span unpacking in compare, overlaps, contains,
  isAdjacentOrOverlapping and referenceByMerging with one private verseRange,
  which maps a whole chapter to every verse in it
- Drop isRange's redundant null check
- No behavior change for any legal shape; no test assertion changed

* fix(core): parse chapter-only passage strings as whole chapters

- GEN.1 now resolves to the whole chapter instead of verse 1
- GEN.1-2 resolves to the whole first chapter; a reference cannot span
  chapters, so the second is dropped either way
- Verse and verse-range forms are unchanged
- Unparseable input still returns null rather than throwing

Deliberate divergence from the Swift SDK, which still returns verse 1
for both forms.

* style(core): drop inline comments added inside functions

AGENTS.md prohibits inline comments inside function bodies. Removes the
six added across this branch, in the adjacency check, the serializer's
decode branch and the share-url selector.

No behavior change; full suite green.

* test(core): pin whole-chapter adjacency against verse overflow

A whole chapter's open-ended last verse is Int.MAX_VALUE, so computing
adjacency by adding to it wraps negative and makes a chapter report
itself non-adjacent to everything.

- Names the trap and probes its boundary with a highest-verse reference
- Verified by mutation: reverting to the additive form fails this test
  and the two existing null-verse cases

* fix(core): refuse passage strings whose verse range crosses chapters

- A cross-chapter range kept the first chapter but took its end verse from
  the second, returning a different passage rather than a truncated one.
- Both the abbreviated and spelled-out forms now return null when the two
  chapters differ, matching what the parser already does for two books.
- Single verses, same-chapter ranges, chapter-only and chapter-range strings
  are unchanged.

* refactor(core, highlights): drop the last half-reference branches

- Ask isRange in passage strings and USFM instead of re-deriving it
- Share whole-chapter references as chapter links via one null check
- Drop the unreachable "start verse with no end" title arm
- Expand highlight ranges over the reference's own verse range

* style(core): address the standards review on the reference changes

- Rename the range constructor's widening helper to optionalVerse
- Spell out the passage parser's locals; drop bText/c2/v2 abbreviations
- Restore the whole-chapter overlap and containment rules as KDoc
- Ask the reference whether it is a range instead of re-deriving it

* refactor(core): collapse the duplicate whole-chapter branch

- `titleChunks` split "whole chapter" across two `when` branches returning
  an identical list, while `shareUrl` in the same file already combines the
  two conditions into one arm. Both now read the same way.
- Drop `test shareUrl verse start nil but verse end not nil`. The shape its
  name describes became unconstructible, and its body had already degraded
  into a copy of `test shareUrl chapter only`.

* test(highlights): pin chapter-key normalization across both whole-chapter shapes

- Assert null/null and 1/999 chapter references hit the same loading entry,
  dedupe to a single load, share the throttle entry, and unmark together
- Guards the normalizeToChapter call sites, which are the only thing keeping
  the two deliberately-unequal shapes from splitting the raw-keyed maps

* docs(core): warn that a whole chapter's verseRange is unbounded

- Whole-chapter references have no verse bounds, so verseRange widens to
  1..Int.MAX_VALUE; say so on the declaration
- Callers should compare or intersect endpoints, never iterate. Today they
  all do: the merge/overlap helpers read only first and last, and the one
  map in BibleHighlightsRepository sits behind an isRange gate

* docs(core)!: record the remaining breaking changes for the release notes

This branch squash-merges into a single commit, so semantic-release renders
one "Bug Fixes" bullet from the pull request title and takes everything else
from BREAKING CHANGE footers. Three public behavior changes had no footer and
would have shipped in the major release unannounced.
* BibleReference.unvalidatedReference now returns a whole
chapter for a chapter-only passage string. "GEN.1" and "GEN.1-2" previously
returned verse 1 of that chapter, with verseStart and verseEnd both 1; they
now return a reference whose verses are both null. Callers that read
verseStart or verseEnd off a parsed chapter-only string will see null where
they saw 1, and equality against a hand-built verse-1 reference no longer
holds.

This is a deliberate divergence from the Swift SDK, which still returns verse
1 for both forms. Code that relies on the two SDKs parsing a chapter-only
string identically must special-case the Kotlin result until Swift follows.
* BibleReference.unvalidatedReference now returns null for a
passage string whose verse range crosses a chapter boundary. "GEN.1.3-2.5"
and "GEN.1.3-GEN.2.5" previously returned a reference in the first chapter
that took its end verse from the second, which is a different passage rather
than a truncated one. Callers that passed such strings must handle null, or
split the range into one reference per chapter.
* BibleVersion.shareUrl now returns a chapter link for a
whole-chapter reference, where it previously returned a verse-range link
covering verses 1 through 999. The signature is unchanged; only the returned
URL differs.

* feat(ui): typography alignment with iOS, phase 1

* fix(core): ignore unsupported HTML nodes in BibleTextNode parser

- self-close all 14 HTML void elements via regex, replacing the <br>-only hack
- lowercase element names so uppercase tags like <BR> parse
- drop unsupported elements from the tree, hoisting their children into the parent
- collapse the space seam between coalesced text segments
- port Swift's three parser-hardening tests

* feat(ui): typography revamp

- Add em-based font option set (BibleTextFontOption) and style resolution
  on BibleTextFonts, including small-caps merging and verse-number
  baseline shift/opacity
- Rework block styling: indents become plain multipliers
  (firstLineHeadIndent/headIndent) rendered as real Compose text indents;
  margins move to per-block marginTop/marginBottom with CSS-style
  margin collapsing between adjacent blocks
- Align the USFM class table with the Swift SDK (poetry, lists,
  headers, titles, intro classes), including the poetry-indent fix
- Change reader line-spacing steps from [1.2, 1.5, 1.8] to
  [0.3, 0.4, 0.6] with default 0.4; persisted values snap to the
  nearest step
- Update tests across platform-ui and platform-reader for the new
  block model, spacing defaults, and line-spacing steps

* fix(reader): render footnote sheet content at a fixed size

- stop forcing footnote children into the footnote font; they now style
  from their own USFM classes
- footnote sheet re-renders footnotes at Untitled Serif 16sp instead of
  the user's reader font settings, filtered to the tapped reference,
  falling back to the passed-in list

* fix(ui): scale the footnote icon with the font size

- derive the inline placeholder and icon sizes from the configured font
  size instead of hardcoding them
- constrain the placeholder to the line height and center the icon on it
  so it no longer rides above the text

* fix(ui): render ord spans as passed instead of small caps

- remove small-caps font features from the verse number font; verse
  numbers are digits, so the features only affected lettered spans
- ord suffixes (7th, 1st) now render lowercase, matching iOS, where
  Helvetica Neue never applied the requested small-caps feature

* fix(reader): keep fallback footnotes when re-render filter matches none

- Only replace the passed-in footnotes when the re-rendered filter produces results, so the sheet is never blanked

* test(reader): cover footnote re-render and fallback paths

- Assert re-rendered footnotes replace the passed-in list when a
  BibleReferenceAttribute annotation matches the sheet's reference
- Assert the passed-in footnotes survive when no annotation matches

* docs(reader): document the fixed-size footnotes sheet

- Add KDoc noting the sheet renders at a fixed size and ignores the
  BibleTextOptions it is passed

* feat(ui): align Bible typography with the Swift SDK

- Replace BibleTextOptions.lineSpacing with lineSpacingFraction, and
  resolve line height through resolvedLineHeight/extraLeading instead of
  repeating the 1.2/0.4 constants at each call site
- Rename the reader's line-spacing API to match, and move persisted
  values to a new storage key so 1.x multipliers are not reread as
  fractions
- Make the rendering state classes and the font-option enum internal
* Bible typography now mirrors the Swift SDK. Public API
changed across platform-ui, platform-reader, and platform-core.

platform-ui

* `BibleTextOptions.lineSpacing: TextUnit?` is replaced by
  `lineSpacingFraction: Float?`. The old value was an absolute line
  height; the new one is extra leading expressed as a fraction of
  `fontSize`. Line height is now `fontSize * (1.2 + fraction)`, where
  1.2 is the height a font already carries. Convert an explicit value
  with `fraction = oldLineHeight / fontSize - 1.2`; if you were passing
  `fontSize * multiplier`, pass `multiplier - 1.2f`. The default when
  unset moves from `fontSize * 1.5` to `fontSize * 1.6`.
  `BibleTextOptions.DEFAULT_LINE_SPACING_FRACTION` (0.4f) names the
  default.
* `BibleTextFontOption` is now `internal`, and its cases were replaced
  wholesale. It described semantic roles (TEXT, HEADER, VERSE_NUM); it
  now describes concrete type ramps (FONT_100EM, FONT_117EM_500). The
  two sets do not correspond one-to-one, so there is no mapping table.
* `BibleTextFonts.styleFor()`, `.verseNumBaselineShift`, and
  `.verseNumOpacity` are now `internal`. `BibleTextFonts` itself, along
  with `fontFamily` and `baseSize`, stays public; build `SpanStyle`s
  directly from those if you were using the removed members.
* `BibleTextBlock.headIndent` changed from `TextUnit` to `Int` and is
  now an indent *level*, multiplied by 8.dp at render time. Two required
  properties were added: `firstLineHeadIndent: Int`, which indents only
  the block's first line, and `marginBottom: Dp`.
* `BibleVersionRendering.StateIn`, `StateDown`, and `StateUp` are now
  `internal`. `BibleVersionRendering`'s own entry points are unchanged,
  as are the `BibleText` and `BibleIntroText` signatures.

platform-reader

* `BibleReaderFootnotesSheet` no longer takes `textOptions`. The sheet
  renders at a fixed size and ignores the reader's font settings, so the
  parameter had nothing left to influence. Drop the argument.
* `ReaderFontSettings.availableLineSpacings` becomes
  `availableLineSpacingFractions`, and its values change from
  `[1.2f, 1.5f, 1.8f]` to `[0.3f, 0.4f, 0.6f]`.
  `DEFAULT_LINE_SPACING` (1.5f) becomes `DEFAULT_LINE_SPACING_FRACTION`
  (0.4f). In effective line height the three options move from
  1.2x/1.5x/1.8x to 1.5x/1.6x/1.8x, so the tightest option is less
  tight than it was.
* `ReaderFontSettings.nextLineSpacing` becomes
  `nextLineSpacingFraction`. The signature is unchanged; it was renamed
  precisely because `Float -> Float` would otherwise compile against the
  new value set while meaning something different.
* `BibleReaderViewModel.State.lineSpacing` becomes
  `lineSpacingFraction`, and `BibleReaderFontSettingsSheet`'s
  `lineSpacing` parameter becomes `lineSpacingFraction`. Both stay
  `Float`.
* `UserSettingsRepository.readerLineSpacing` becomes
  `readerLineSpacingFraction` and reads/writes a new storage key,
  `bible-reader-view--line-spacing-fraction` instead of
  `bible-reader-view--line-spacing`. A value written by 1.x is a
  multiplier; read back as a fraction, 1.5 would mean 1.5x the font size
  of extra leading, a 2.7x line height. Rather than convert on every
  read forever, 2.0 starts fresh under a new key. Consequence: on first
  launch after upgrading, a user who had customized line spacing falls
  back to the default; their next adjustment persists normally. The old
  key is left in place, so hosts that prefer to migrate can seed the new
  key once at startup with `oldMultiplier - 1.2f`.

platform-core

* `BibleTextNode.parse` keeps its signature but is stricter about HTML.
  Element names are lowercased, so case-sensitive matches on `node.name`
  now see lowercase. Only `block`, `div`, `root`, `span`, `table`, `td`,
  `text`, and `tr` survive as nodes; any other element is dropped and
  its children hoisted into the parent, so text still renders but the
  wrapper is gone. All HTML void elements are self-closed rather than
  just `<br>`, so an unclosed `<img>` no longer fails the XML parse. Runs
  of spaces are collapsed when adjacent text nodes are joined.

Behavior changes with no compile error

* The default line height moves from `fontSize * 1.5` to
  `fontSize * 1.6`.
* Table cells now use the resolved line height. In 1.x they fell back to
  `TextUnit.Unspecified` when `lineSpacing` was null.
* Verse numbers render in a sans-serif face at 0.65x rather than the
  body family at 0.7x, no longer use small caps, and shift by a
  Swift-matched 0.2 x baseSize instead of `BaselineShift.Superscript`.
* Footnote text uses the configured font family rather than always
  sans-serif, and the inline footnote marker is sized from the current
  font size instead of a fixed 24x32sp box, so it scales with reader
  text.
* Blocks carry an explicit `marginBottom` that includes the extra
  leading, changing vertical rhythm between paragraphs and headings.

* docs(releasing): correct the breaking-change trigger

- Document the BREAKING CHANGE: footer as the only major-bump trigger;
  the pinned angular preset 7.0.0 has no breakingHeaderPattern, so a `!`
  subject parses as type/scope/subject null
- Spell out both silent failure modes: with a footer the commit vanishes
  from the changelog, without one it produces no release at all while
  still tripping the production-breaking gate
- Drop `!` from the example commit

* perf(reader): render the footnotes sheet chapter once

- Add an optional onBlocksChange callback to BibleText so a caller can
  reuse the blocks it already rendered
- Drop the footnotes sheet's duplicate textBlocks call and take the
  footnotes from its own BibleText render instead
- Words of Christ inside footnotes now take the sheet's woc color,
  matching the verse text above them
- Guard the single render with a coVerify in the sheet tests

* refactor(sdk): tighten the public API surface

- Mark internal every declaration that is not part of the documented
  consumer contract, across platform-core, platform-ui and platform-reader
- Tighten test-fixture properties to private where nothing else reads them
- Delete the unused AbbreviationSplit helper and its tests
- Scope the @PlatformInternalApi opt-in to the three SDK modules, so the
  sample app compiles against the same surface a consumer sees

* build: split api() from implementation() by public signature

- Promote to api() only the dependencies whose types appear in a module's
  own public signatures; everything else drops to implementation
- Add the missing coroutines dependency to platform-ui, which already
  exposed StateFlow from its public view models
- Keep platform-ui off the consumer compile classpath: it backs the
  reader's internals but never appears in platform-reader's public API
- Correct the AGENTS.md claim that platform-reader re-exposes both
  modules via api()

* refactor(sdk): hide four leaked public members

Found by sweeping members of public types, not just top-level declarations.

- PlatformKoinGraph.getContext: DI plumbing, called only from
  PlatformCoreKoinComponent and a test helper
- LanguageRepository.localeCountryCode/localeLanguageCode: read only by
  the repository itself, so private rather than internal
- BibleTextOptions.inlineContentMap: derived footnote-rendering detail
  consumed only by BibleText and BibleIntroText

* refactor(ui): gate platform-ui internals behind @PlatformInternalApi

- Annotate 31 declarations that are public only so platform-reader can
  reach them across the module boundary, not for consumers.
- Covers reader theming and its presets, BibleReaderTheme/ColorScheme/
  Typography, the version and language pickers, rendering internals,
  the top app bar and search bar, sign-in dialogs, the Koin module,
  and the Int.convertToEnumeration footnote helper.
- Leaves BibleTextOptions, BibleTextFonts, BibleTextFootnoteMode,
  BibleTextLoadingPhase, BibleTextBlock and BibleTextCategory open,
  since BibleText's public signature exposes them.
- internal is unavailable here: Kotlin has no friend-module visibility
  and these cross the platform-ui to platform-reader boundary.
* The SDK's public API is now only its documented consumer
surface. Declarations that were public only by accident are now either
`internal`, or still public but annotated `@PlatformInternalApi`, whose
opt-in level is ERROR — code that touches one fails to compile until it
opts in. `BibleText`, `BibleCard`, `BibleReader` and the sign-in surface
are unchanged.

Now internal, with no consumer access at all

* platform-core: `ApiResponse`; the `BiblesEndpoints`,
  `HighlightsEndpoints`, `LanguagesEndpoints`, `OrganizationsEndpoints`,
  `UsersEndpoints` and `VotdEndpoints` API objects; `SessionRepository`,
  `TokenResponse`, `RefreshTokenResponse`, `SharedPreferencesStorage`,
  `BibleVersionFileCache` with its temporary and persistent caches,
  `BibleVersionDownloadStatus`, and `PlatformKoinGraph.getContext`.
  `LanguageRepository.localeLanguageCode` and `.localeCountryCode` are
  private. The unused `AbbreviationSplit` helper and its tests are
  deleted.
* platform-reader: `BibleReaderViewModel`, `BibleReaderRepository`,
  `UserSettingsRepository`, `ReaderFontSettings`, `CopyManager`,
  `ShareManager`, `ReferencesViewModel`, `ReferenceRow`, and the reader's
  own chrome — `BibleReaderFontSettingsSheet`,
  `BibleReaderFootnotesSheet`, `BibleReaderBanner`, `BibleReaderHeader`,
  `BibleReaderHeaderDropdownMenu`, `BibleReaderHalfPillPicker`,
  `BibleReaderPassageSelection` and its state types. Reach these through
  `BibleReader` instead; it is unchanged.
* platform-ui: `StandardPlaceholder`, `YouVersionAuthentication`,
  `BibleVersionRow`, `BibleLanguageRow`, `BibleVersionPickingButton`,
  `BibleVersionsStack`, `LanguageSelector`, `VersionInfoBottomSheet`, the
  reader color and palette token objects with their individual `Color`
  values, the `AktivGrotesk` family,
  `BibleReference.Companion.fromAnnotation`,
  `AnnotatedString.Builder.addTextCategoryAnnotation`, and
  `BibleTextOptions.inlineContentMap`.

Now @PlatformInternalApi: public, but opt-in

These stay public only because platform-reader has to reach them across a
module boundary and Kotlin has no friend-module visibility. They are not
a consumer contract and may change or disappear in any release. Using one
means `@OptIn(PlatformInternalApi::class)`, from
`com.youversion.platform.core.di`, plus the opt-in compiler argument in
your own build; the annotation exists to make that a deliberate choice,
not to promise support.

`BibleIntroText`, `BibleVersionRendering`, `BibleReferenceAttribute`,
`BibleTextCategoryAttribute`, `PlatformKoinGraph`,
`PlatformUIKoinModule`, `Int.convertToEnumeration`, the reader theming
surface (`BibleReaderTheme`, `BibleReaderColorScheme`,
`BibleReaderTypography`, `BibleReaderMaterialTheme`, `ReaderTheme`,
`ReaderColorScheme`, `MaterialTheme.readerColorScheme`, the named presets
and `UntitledSerif`), `BibleReaderTopAppBar`, `SearchBar`,
`VersionsScreen`, `LanguagesScreen`, `LanguageRowItem`,
`BibleVersionsViewModel`, `SignInWithYouVersionPromptSheet`,
`SignInErrorAlert`, `SignOutConfirmationAlert`, and the PKCE request
models.

* fix(reader): pin intro footnote typography to the sheet size

- Render intro footnotes at Untitled Serif 16sp so the sheet matches the
  chapter footnotes sheet and ignores the reader's font settings
- Overlay the style rather than re-rendering the passage; span styles
  merge per attribute, so emphasis and color survive

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01W3fEVyJXQXfLjx4oHUnXXd

* docs(ui): document the block indent unit convention

- Explain on BibleTextBlock why firstLineHeadIndent scales with the font
  size while headIndent stays a fixed 8dp per unit, matching Swift
- Cover the asymmetry with a compose test that doubles the font size

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01W3fEVyJXQXfLjx4oHUnXXd

* fix(reader): reset the footnote fallback when the reference changes

- Key the displayFootnotes remember on reference and footnotes so a
  reopened sheet never shows the previous verse's notes while its own
  render is still in flight

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01W3fEVyJXQXfLjx4oHUnXXd

## [1.10.1](https://github.com/youversion/platform-sdk-kotlin/compare/1.10.0...1.10.1) (2026-09-02)


### Bug Fixes

* **i18n:** sync kotlin localization from platform-localization ([#183](https://github.com/youversion/platform-sdk-kotlin/issues/183)) ([74f117c](https://github.com/youversion/platform-sdk-kotlin/commit/74f117c6e1181eed24a68801eac02c2272ee4df0))

# [1.10.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.9.0...1.10.0) (2026-08-26)


### Features

* **core:** obey cache-control headers for Bible content ([d482877](https://github.com/youversion/platform-sdk-kotlin/commit/d482877eaa238ccce10d3466c635dadd991ad9c3))

# [1.9.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.8.4...1.9.0) (2026-08-24)


### Features

* **core:** add excluded version ids configuration option ([4010532](https://github.com/youversion/platform-sdk-kotlin/commit/4010532ca4c522ad758beaf3f2743123b4e0b658))

## [1.8.4](https://github.com/youversion/platform-sdk-kotlin/compare/1.8.3...1.8.4) (2026-08-20)


### Bug Fixes

* **reader:** remove duplicate intro chapter headers (YPE-1654) ([6bd95e4](https://github.com/youversion/platform-sdk-kotlin/commit/6bd95e4fb2f999844cc657eb881499e835389d4d))

## [1.8.3](https://github.com/youversion/platform-sdk-kotlin/compare/1.8.2...1.8.3) (2026-08-19)


### Bug Fixes

* **ui:** update the bible app logo ([83af387](https://github.com/youversion/platform-sdk-kotlin/commit/83af387954bac9b8fb1f09014c30b18d16931ee4))

## [1.8.2](https://github.com/youversion/platform-sdk-kotlin/compare/1.8.1...1.8.2) (2026-08-19)


### Bug Fixes

* **i18n:** sync kotlin localization from platform-localization ([fc939e3](https://github.com/youversion/platform-sdk-kotlin/commit/fc939e3a281053c1e8dd67f05a7291d85995e0ae))

## [1.8.1](https://github.com/youversion/platform-sdk-kotlin/compare/1.8.0...1.8.1) (2026-08-05)


### Bug Fixes

* **i18n:** sync kotlin localization from platform-localization ([637ff57](https://github.com/youversion/platform-sdk-kotlin/commit/637ff574c2903c11d9f2339ddc4ff10633bebbfb))

# [1.8.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.7.1...1.8.0) (2026-08-05)


### Features

* add highlights ([6053b71](https://github.com/youversion/platform-sdk-kotlin/commit/6053b717390d42b07828c1eab122f7c82edc137b))

## [1.7.1](https://github.com/youversion/platform-sdk-kotlin/compare/1.7.0...1.7.1) (2026-07-29)


### Bug Fixes

* **i18n:** sync kotlin localization from platform-localization ([5339613](https://github.com/youversion/platform-sdk-kotlin/commit/5339613bc08a69a0e6be43cb80dd1167f6a79bb4))

# [1.7.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.6.1...1.7.0) (2026-07-16)


### Features

* **reader:** add line-spacing control to font settings sheet ([cd45d3e](https://github.com/youversion/platform-sdk-kotlin/commit/cd45d3ef4b52c8fd1fb6bdfeaf07fcadcad6f54f))

## [1.6.1](https://github.com/youversion/platform-sdk-kotlin/compare/1.6.0...1.6.1) (2026-07-15)


### Bug Fixes

* **ci:** allow bracketed [bot] form of platform-localization-pr-bot in guardrail ([#160](https://github.com/youversion/platform-sdk-kotlin/issues/160)) ([43b04de](https://github.com/youversion/platform-sdk-kotlin/commit/43b04de0dcffb19c65eb55f53e95f4ab5605cb30))

# [1.6.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.5.0...1.6.0) (2026-05-12)


### Features

* **core, ui:** filter versions by permitted languages and ids YPE-2359 ([#140](https://github.com/youversion/platform-sdk-kotlin/issues/140)) ([1ca3e10](https://github.com/youversion/platform-sdk-kotlin/commit/1ca3e10db7b1e17167152f7b62651869efadabe0))

# [1.5.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.4.1...1.5.0) (2026-05-12)


### Features

* **ui:** add search to language picker ([6e7580c](https://github.com/youversion/platform-sdk-kotlin/commit/6e7580c1f26ed5e42c5acfe31b8994064f56969f))

## [1.4.1](https://github.com/youversion/platform-sdk-kotlin/compare/1.4.0...1.4.1) (2026-05-11)


### Bug Fixes

* **ui:** respect BibleTextOptions.textColor in BibleText and BibleIntroText YPE-2346 ([#138](https://github.com/youversion/platform-sdk-kotlin/issues/138)) ([5d5a01c](https://github.com/youversion/platform-sdk-kotlin/commit/5d5a01c0be5f9d7f43a70ecb841c91e464630a9e))

# [1.4.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.3.0...1.4.0) (2026-05-11)


### Features

* **ui, reader:** add search to version picker ([8315746](https://github.com/youversion/platform-sdk-kotlin/commit/83157463d7b5fcf2951228dd59822e1c3abec5df))

# [1.3.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.2.0...1.3.0) (2026-05-06)


### Features

* **core:** send x-yvp-sdk header identifying SDK version YPE-2294 ([#137](https://github.com/youversion/platform-sdk-kotlin/issues/137)) ([ea67e4a](https://github.com/youversion/platform-sdk-kotlin/commit/ea67e4a3ea68c8eedfe04bdda492faf1b3619263))

# [1.2.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.1.2...1.2.0) (2026-05-04)


### Features

* **ui:** add version picker to BibleCard ([3ab6c92](https://github.com/youversion/platform-sdk-kotlin/commit/3ab6c92974bde83dad342f4a274779e2b50d11d7))

## [1.1.2](https://github.com/youversion/platform-sdk-kotlin/compare/1.1.1...1.1.2) (2026-04-28)


### Bug Fixes

* **reader:** prevent navigation from intro of first book YPE-1889 ([#135](https://github.com/youversion/platform-sdk-kotlin/issues/135)) ([06be46d](https://github.com/youversion/platform-sdk-kotlin/commit/06be46dfd696bf5d70fff880c5ea390fe14eac9f))

## [1.1.1](https://github.com/youversion/platform-sdk-kotlin/compare/1.1.0...1.1.1) (2026-04-12)


### Bug Fixes

* downgrade to kotlin version 2.2.21 ([24b1d2f](https://github.com/youversion/platform-sdk-kotlin/commit/24b1d2fc86f25936b5086387819fc797b938e10f))

# [1.1.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.0.3...1.1.0) (2026-04-06)


### Features

* **tests:** test coverage for ReferencesScreen YPE-1677 ([#126](https://github.com/youversion/platform-sdk-kotlin/issues/126)) ([b0a4a85](https://github.com/youversion/platform-sdk-kotlin/commit/b0a4a85f8b8f4c9defd85dab8c9e828fc3477e84))

## [1.0.3](https://github.com/youversion/platform-sdk-kotlin/compare/1.0.2...1.0.3) (2026-03-25)


### Bug Fixes

* **test:** use UnconfinedTestDispatcher and remove resetMain to fix flaky test ([#118](https://github.com/youversion/platform-sdk-kotlin/issues/118)) ([d5b2564](https://github.com/youversion/platform-sdk-kotlin/commit/d5b256441b8c8a5d4e183a6cdacd83a0ac852c25))

## [1.0.2](https://github.com/youversion/platform-sdk-kotlin/compare/1.0.1...1.0.2) (2026-03-25)


### Bug Fixes

* **ci:** add production environment to release workflow ([#116](https://github.com/youversion/platform-sdk-kotlin/issues/116)) ([bdb97e9](https://github.com/youversion/platform-sdk-kotlin/commit/bdb97e9c81929d16b2816a4c72147c0d053220bc))
* commitlint config for broken release action ([#110](https://github.com/youversion/platform-sdk-kotlin/issues/110)) ([6aae41c](https://github.com/youversion/platform-sdk-kotlin/commit/6aae41c034d8bba2929bb9c848f5f86ba8a6bb07))
* **release:** sync SDK version with Maven Central ([#117](https://github.com/youversion/platform-sdk-kotlin/issues/117)) ([524e721](https://github.com/youversion/platform-sdk-kotlin/commit/524e7216a2788f2c6fd3f02f12fc4ed88be1ac20))

## [0.8.1](https://github.com/youversion/platform-sdk-kotlin/compare/0.8.0...0.8.1) (2026-03-25)


### Bug Fixes

* **ci:** add production environment to release workflow ([#116](https://github.com/youversion/platform-sdk-kotlin/issues/116)) ([bdb97e9](https://github.com/youversion/platform-sdk-kotlin/commit/bdb97e9c81929d16b2816a4c72147c0d053220bc))

# [0.8.0](https://github.com/youversion/platform-sdk-kotlin/compare/0.7.0...0.8.0) (2026-03-20)


### Bug Fixes

* **bible-screen:** update attribution text color styling using BibleReaderTheme ([f534c90](https://github.com/youversion/platform-sdk-kotlin/commit/f534c9056800df48ea41517611fdc986308221f2))
* commitlint config for broken release action ([#110](https://github.com/youversion/platform-sdk-kotlin/issues/110)) ([6aae41c](https://github.com/youversion/platform-sdk-kotlin/commit/6aae41c034d8bba2929bb9c848f5f86ba8a6bb07))
* **reader:** remove the spacing above the reference text in footnotes… ([0d48347](https://github.com/youversion/platform-sdk-kotlin/commit/0d48347fdeae3852c3132add4ab5a1ae1f1e29d1))
* Remove platform-foundation module in favor of InternalApi annotation ([#64](https://github.com/youversion/platform-sdk-kotlin/issues/64)) ([83cb040](https://github.com/youversion/platform-sdk-kotlin/commit/83cb0407e332834ed3ef054cea3f54d3c08076e2))
* **versions-screen:** adjust padding for BibleVersionRow component ([#62](https://github.com/youversion/platform-sdk-kotlin/issues/62)) ([fd3bcf1](https://github.com/youversion/platform-sdk-kotlin/commit/fd3bcf11550b449150ed3da0e673f4f4f4cd3453))


### Features

* **bible-version:** use default version for references ([#52](https://github.com/youversion/platform-sdk-kotlin/issues/52)) ([ac7e459](https://github.com/youversion/platform-sdk-kotlin/commit/ac7e459c2670e818a102bb7f658b09d530050e5e))
* **version-info:** Agreement UI and action buttons to match Figma ([8531551](https://github.com/youversion/platform-sdk-kotlin/commit/85315513e653fc8ad6c7e4d3820831c7804904e5))

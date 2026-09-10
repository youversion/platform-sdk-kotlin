# [2.0.0](https://github.com/youversion/platform-sdk-kotlin/compare/1.10.1...2.0.0) (2026-09-10)

2.0.0 breaks compatibility in all three modules. Changes are grouped by the
module they land in; the API-surface reduction described first applies across
all of them.


### Public API surface

The SDK's public API is now only its documented consumer surface. Declarations
that were public only by accident are now either `internal`, or still public but
annotated `@PlatformInternalApi`, whose opt-in level is `ERROR` — code that
touches one fails to compile until it opts in. `BibleText`, `BibleCard`,
`BibleReader` and the sign-in surface are unchanged.

`@PlatformInternalApi` declarations stay public only because platform-reader has
to reach them across a module boundary and Kotlin has no friend-module
visibility. They are not a consumer contract and may change or disappear in any
release. Using one means `@OptIn(PlatformInternalApi::class)`, from
`com.youversion.platform.core.di`, plus the opt-in compiler argument in your own
build. Each module's breaking-change list below names what it moved.


### platform-core

#### Features

* parse chapter-only passage strings as whole chapters ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))

#### Bug Fixes

* make half-specified Bible references unconstructible ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* repair half-specified references when decoding stored data ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* refuse passage strings whose verse range crosses chapters ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* share whole-chapter references as chapter links ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* ignore unsupported HTML nodes in the BibleTextNode parser ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))

`BibleVersion.shareUrl` keeps its signature, but a reference covering a whole
chapter — either shape, `verseEnd` 999 or no verses at all — now returns the
chapter URL (`.../GEN.3.NIV`) where 1.x returned a verse range
(`.../GEN.3.1-999.NIV`). Both forms resolve to the same passage, so existing
links keep working; assertions on the exact URL string may need updating.

#### BREAKING CHANGES

* **`BibleReference` construction.** `BibleReference` can no longer be built with
only one of `verseStart` and `verseEnd`, and the field-by-field constructor is no
longer public. Use `BibleReference(versionId, bookUSFM, chapter)` for a whole
chapter, the single-verse constructor for one verse, or pass both verses for a
range. The single-verse constructor's verse argument is also no longer nullable;
callers passing an `Int?` to mean "this verse, or the whole chapter if I have
none" must branch on the null themselves and pick the matching constructor.

  This breaks binary compatibility as well as source compatibility. Every
constructor descriptor a 1.x caller linked against is gone or private, so
anything compiled against 1.x fails with `NoSuchMethodError` and must be
recompiled. `copy()` keeps its signature and stays binary compatible, but now
throws `IllegalArgumentException` for a copy that would produce a
half-specified reference.

* **Chapter-only passage strings.** `BibleReference.unvalidatedReference` now
returns a whole chapter for a chapter-only passage string. `"GEN.1"` and
`"GEN.1-2"` previously returned verse 1 of that chapter, with `verseStart` and
`verseEnd` both 1; they now return a reference whose verses are both null.
Callers that read `verseStart` or `verseEnd` off a parsed chapter-only string
will see null where they saw 1, and equality against a hand-built verse-1
reference no longer holds.

* **Cross-chapter verse ranges.** `BibleReference.unvalidatedReference` now
returns null for a passage string whose verse range crosses a chapter boundary.
`"GEN.1.3-2.5"` and `"GEN.1.3-GEN.2.5"` previously returned a reference in the
first chapter that took its end verse from the second, which is a different
passage rather than a truncated one. Callers that passed such strings must
handle null, or split the range into one reference per chapter.

* **`BibleTextNode.parse` is stricter about HTML.** It keeps its signature.
Element names are lowercased, so case-sensitive matches on `node.name` now see
lowercase. Only `block`, `div`, `root`, `span`, `table`, `td`, `text` and `tr`
survive as nodes; any other element is dropped and its children hoisted into the
parent. All HTML void elements are self-closed rather than just `<br>`, so an
unclosed `<img>` no longer fails the XML parse, and runs of spaces are collapsed
when adjacent text nodes are joined.

* **Now `internal`.** `ApiResponse`, the `BiblesEndpoints`,
`HighlightsEndpoints`, `LanguagesEndpoints`, `OrganizationsEndpoints`,
`UsersEndpoints` and `VotdEndpoints` API objects, `SessionRepository`,
`TokenResponse`, `RefreshTokenResponse`, `SharedPreferencesStorage`,
`BibleVersionFileCache` with its temporary and persistent caches, and
`BibleVersionDownloadStatus`.

* **Now `@PlatformInternalApi`.** `PlatformKoinGraph` and the PKCE request
models. `PlatformKoinGraph.getContext()` did not come with it — it is now
`internal`, so it is unreachable even with the opt-in.

* **Now `private`.** `LanguageRepository.localeLanguageCode` and
`LanguageRepository.localeCountryCode`. Both read from `Locale.getDefault()`;
callers that used them can read the default locale directly.


### platform-ui

#### Features

* revamp Bible typography and line spacing ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))

#### Bug Fixes

* scale the footnote icon with the font size ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* render ord spans as passed instead of small caps ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))

#### BREAKING CHANGES

* **Line spacing is now a fraction.** `BibleTextOptions.lineSpacing: TextUnit?`
is replaced by `lineSpacingFraction: Float?`. The old value was an absolute line
height; the new one is extra leading expressed as a fraction of `fontSize`. Line
height is now `fontSize * (1.2 + fraction)`. Convert an explicit value with
`fraction = oldLineHeight / fontSize - 1.2`; if you passed `fontSize *
multiplier`, pass `multiplier - 1.2f`. The default when unset moves from
`fontSize * 1.5` to `fontSize * 1.6`, named by
`BibleTextOptions.DEFAULT_LINE_SPACING_FRACTION`.

* **`BibleTextBlock` indents and margins.** `headIndent` changed from `TextUnit`
to `Int` and is now an indent *level*, multiplied by 8.dp at render time. Two
required properties were added: `firstLineHeadIndent: Int` and
`marginBottom: Dp`.

* **Font plumbing is no longer public.** `BibleTextFontOption` is now `internal`
and its cases were replaced wholesale, with no one-to-one mapping.
`BibleTextFonts.styleFor()`, `.verseNumBaselineShift` and `.verseNumOpacity` are
now `internal`; `BibleTextFonts`, `fontFamily` and `baseSize` stay public.
`BibleVersionRendering.StateIn`, `StateDown` and `StateUp` are now `internal`.

* **Rendering changes with no compile error.** The default line height moves from
`fontSize * 1.5` to `fontSize * 1.6`; table cells now use the resolved line
height instead of falling back to `TextUnit.Unspecified`; verse numbers render in
a sans-serif face at 0.65x rather than the body family at 0.7x, no longer use
small caps, and shift by 0.2 x `baseSize`; footnote text uses the configured font
family rather than always sans-serif; and blocks carry an explicit `marginBottom`
that includes the extra leading, changing vertical rhythm between paragraphs and
headings.

* **Now `internal`.** `StandardPlaceholder`, `YouVersionAuthentication`,
`BibleVersionRow`, `BibleLanguageRow`, `BibleVersionPickingButton`,
`BibleVersionsStack`, `LanguageSelector`, `VersionInfoBottomSheet`, the reader
color and palette token objects, the `AktivGrotesk` family,
`BibleReference.Companion.fromAnnotation`,
`AnnotatedString.Builder.addTextCategoryAnnotation` and
`BibleTextOptions.inlineContentMap`.

* **Now `@PlatformInternalApi`.** `BibleIntroText`, `BibleVersionRendering`,
`BibleReferenceAttribute`, `BibleTextCategoryAttribute`, `PlatformUIKoinModule`,
`Int.convertToEnumeration`, the reader theming surface, `BibleReaderTopAppBar`,
`SearchBar`, `VersionsScreen`, `LanguagesScreen`, `LanguageRowItem`,
`BibleVersionsViewModel`, `SignInWithYouVersionPromptSheet`, `SignInErrorAlert`
and `SignOutConfirmationAlert`.


### platform-reader

#### Bug Fixes

* render footnote sheet content at a fixed size ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* pin intro footnote typography to the sheet size ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* keep fallback footnotes when the re-render filter matches none ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))
* reset the footnote fallback when the reference changes ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))

#### Performance Improvements

* render the footnotes sheet chapter once ([05f4484](https://github.com/youversion/platform-sdk-kotlin/commit/05f4484f8038b9d013dbc922942a19b45da20701))

#### BREAKING CHANGES

* **Now `internal`, with no consumer access.** `BibleReaderViewModel`,
`BibleReaderRepository`, `UserSettingsRepository`, `ReaderFontSettings`,
`CopyManager`, `ShareManager`, `ReferencesViewModel`, `ReferenceRow`, and the
reader's own chrome. Reach the reader through `BibleReader` instead, which is
unchanged.

* **Line-spacing settings renamed behind that boundary.** For 1.x callers who
reached these while they were still public:
`ReaderFontSettings.availableLineSpacings` becomes
`availableLineSpacingFractions`, with values moving from `[1.2f, 1.5f, 1.8f]` to
`[0.3f, 0.4f, 0.6f]`, and `DEFAULT_LINE_SPACING` (1.5f) becomes
`DEFAULT_LINE_SPACING_FRACTION` (0.4f). `nextLineSpacing` becomes
`nextLineSpacingFraction`, renamed precisely because `Float -> Float` would
otherwise compile against the new value set while meaning something different.
`BibleReaderViewModel.State.lineSpacing` and `BibleReaderFontSettingsSheet`'s
`lineSpacing` parameter both become `lineSpacingFraction`, and
`BibleReaderFootnotesSheet` no longer takes `textOptions`.

* **A saved line-spacing preference resets once.**
`UserSettingsRepository.readerLineSpacing` becomes `readerLineSpacingFraction`
and reads and writes a new storage key. A value written by 1.x is a multiplier,
so 2.0 starts fresh under the new key rather than converting on every read: on
first launch after upgrading, a user who had customized line spacing falls back
to the default, and their next adjustment persists normally. The old key is left
in place, so the 1.x value is still available to a host that wants to carry it
forward as `oldMultiplier - 1.2f`.

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

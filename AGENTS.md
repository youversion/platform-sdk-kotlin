# AGENTS.md

## Cursor Cloud specific instructions

This repo is the **YouVersion Platform SDK for Kotlin** — an Android library monorepo (Gradle) plus a
sample app. It builds AAR libraries (`platform-core`, `platform-ui`, `platform-reader`) and one
runnable demo app (`examples:sample-android`). All standard build/test/lint commands are documented in
`CLAUDE.md` and `README.md`; prefer those. Notes below are non-obvious caveats for this environment.

### Toolchain (already installed in the VM snapshot)
- **JDK 17** (Temurin) at `/opt/java/jdk17`, exported as `JAVA_HOME` in `~/.bashrc`. CI uses JDK 17 (zulu);
  do not rely on the system JDK 21 for Gradle — run Gradle from a login shell so `JAVA_HOME` is set.
- **Android SDK** at `$HOME/Android/sdk` (`ANDROID_HOME`/`ANDROID_SDK_ROOT` in `~/.bashrc`), with
  `platforms;android-36`, `build-tools;36.0.0`, `platform-tools`. `local.properties` (gitignored) points
  Gradle at it via `sdk.dir`.
- Because these env vars live in `~/.bashrc`, invoke Gradle through a login shell, e.g.
  `bash -lc './gradlew test'`, otherwise `JAVA_HOME`/`ANDROID_HOME` may be unset.

### Running / testing
- No Android emulator is available (the VM has no `/dev/kvm`), so the sample app cannot be launched on a
  device here. Validate changes with the JVM unit tests (Robolectric/MockK/Ktor mock) and by building the
  APK with `./gradlew :examples:sample-android:assembleDebug`.
- The sample app needs a real YouVersion Platform app key at runtime (`MainApplication.kt` uses `TODO()`),
  so it will crash on launch without one — this only matters for on-device runs, not for building or tests.
- Full run `./gradlew test` can exhibit a **flaky** UI test
  (`VerseOfTheDayTests > ... onFullChapterClick ...`) due to test ordering/timing; it passes in isolation
  and on rerun. Re-run before assuming a real failure.
- First Gradle invocation downloads dependencies and is slow (a few minutes); later runs are cached.

### Git hooks
- `npm ci` installs husky hooks (`core.hooksPath=.husky/_`); the `commit-msg` hook runs commitlint, so
  commit messages **must** follow Conventional Commits or the commit is rejected locally.

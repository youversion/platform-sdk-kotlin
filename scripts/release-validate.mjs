#!/usr/bin/env node
// Validate a release version against the current tag.
//
// Called by scripts/release.sh.
//
// Usage:
//   node scripts/release-validate.mjs <chosen-version> <current-tag>
//
// Exit codes:
//    0  valid: chosen is bare release semver AND strictly greater than current
//   11  chosen is not a bare release semver
//   12  chosen is not strictly greater than current
//    1  usage error (missing args)
//
// Stderr is one of: "not_semver", "not_bare", "prerelease_unsupported",
// "not_greater", or a usage message. Exit codes are the contract that
// release.sh switches on; the tokens exist for human-readable logs.
//
// Bare is enforced, not normalised: the version becomes the git tag and the
// Maven Central coordinate verbatim, so a silently corrected "v2.2.0" would
// burn an immutable, wrong coordinate. Prereleases are unsupported here.

import semver from "semver";

const args = process.argv.slice(2);

if (args.length < 2) {
  console.error("Usage: release-validate.mjs <chosen-version> <current-tag>");
  process.exit(1);
}

const [chosen, current] = args;

if (!semver.valid(chosen)) {
  console.error("not_semver");
  process.exit(11);
}

// valid() returns the parsed version, so any differing spelling is
// non-canonical: "v2.2.0" -> "2.2.0", "2.2.3+build" -> "2.2.3".
if (semver.valid(chosen) !== chosen) {
  console.error("not_bare");
  process.exit(11);
}

if (semver.prerelease(chosen) !== null) {
  console.error("prerelease_unsupported");
  process.exit(11);
}

if (!semver.gt(chosen, current)) {
  console.error("not_greater");
  process.exit(12);
}

process.exit(0);

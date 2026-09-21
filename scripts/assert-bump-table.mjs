#!/usr/bin/env node
// Assert that the configured commit analyzer maps conventional-commit headers
// to the bumps this project expects.
//
// This is the regression test for YPE-5781. Two independent things have to be
// right for `feat!:` to bump a major, and getting either one wrong degrades
// *silently* back to the angular preset's behaviour:
//
//   1. `.releaserc.json` must set `"preset": "conventionalcommits"` on
//      @semantic-release/commit-analyzer.
//   2. `conventional-changelog-conventionalcommits` must be installed at a
//      version whose export shape matches what the analyzer expects.
//      v7-era presets export {gitRawCommitsOpts, parserOpts, writerOpts,
//      recommendedBumpOpts, conventionalChangelog}; v8+ export
//      {commits, parser, writer, whatBump}. @semantic-release/commit-analyzer
//      v13 wants the v8 shape, and a v7 preset load falls back to angular
//      without erroring.
//
// So a unit test against the config file alone would pass while the pipeline
// is broken. This drives real messages through the real `analyzeCommits` with
// the real `.releaserc.json` and the real installed dependency tree.
//
// Exit 0 if every case matches, 1 otherwise. Run by
// scripts/test-release-scripts.sh.

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { format } from "node:util";
import { analyzeCommits } from "@semantic-release/commit-analyzer";

const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");

const releaserc = JSON.parse(
  readFileSync(resolve(REPO_ROOT, ".releaserc.json"), "utf8")
);
const entry = releaserc.plugins.find(
  (p) => (Array.isArray(p) ? p[0] : p) === "@semantic-release/commit-analyzer"
);
if (!entry) {
  console.error("  ✗ @semantic-release/commit-analyzer not configured in .releaserc.json");
  process.exit(1);
}
const pluginConfig = Array.isArray(entry) ? entry[1] : {};

// `analyzeCommits` expects a signale-shaped logger. Route it to stderr so the
// pass/fail table on stdout stays clean.
const noop = () => {};
const logger = {
  log: (...a) => process.stderr.write(format(...a) + "\n"),
  error: (...a) => process.stderr.write(format(...a) + "\n"),
  success: noop,
  warn: noop,
};

const CASES = [
  // [commit message, expected release type]
  ["feat!: drop the old API", "major"],
  ["fix(core)!: tighten the token refresh window", "major"],
  ["feat: add a highlight colour picker", "minor"],
  ["fix: stop dropping the sdk header", "patch"],
  ["perf: cache the parsed reference", "patch"],
  ["fix: repair a thing\n\nBREAKING CHANGE: the old overload is gone.", "major"],
  ["docs: clarify the install snippet", null],
  ["chore: bump a dev dependency", null],
];

let failures = 0;

for (const [message, expected] of CASES) {
  const commits = [{ hash: "0".repeat(40), message, subject: message.split("\n")[0] }];
  let actual;
  try {
    actual = await analyzeCommits(pluginConfig, { commits, logger, cwd: REPO_ROOT });
  } catch (error) {
    console.log(`  ✗ ${JSON.stringify(message)} threw: ${error.message}`);
    failures += 1;
    continue;
  }
  const label = JSON.stringify(message);
  if (actual === expected) {
    console.log(`  ✓ ${label} → ${actual}`);
  } else {
    console.log(`  ✗ ${label} → ${actual} (expected ${expected})`);
    failures += 1;
  }
}

if (failures > 0) {
  console.log(
    `  ${failures} bump-table case(s) failed. Check "preset": "conventionalcommits" in ` +
      `.releaserc.json AND that conventional-changelog-conventionalcommits is on ^8.`
  );
  process.exit(1);
}

process.exit(0);

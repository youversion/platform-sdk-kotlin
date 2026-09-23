#!/usr/bin/env node
// Compute the release type and next version by calling
// `@semantic-release/commit-analyzer` directly. Avoids the full
// `semantic-release --dry-run` lifecycle, which on a `pull_request` event
// fails at the core `verifyAuth()` step (`git push --dry-run`) because the
// PR-event `GITHUB_TOKEN` has `contents: read` only. That failure aborts
// before the analyzer runs and produces a silent false-negative "no bump"
// preview.
//
// Two modes, because the two callers ask different questions:
//
//   --base <ref> --head <ref>   Release window. Analyzes `base..head`.
//                               What scripts/release.sh ships.
//
//   --main <ref> --head <ref>   PR preview. Analyzes the *whole* window the
//                               next dispatch would cover: the latest tag
//                               reachable from `main` through `main` itself,
//                               plus the commits this PR adds. A PR-only
//                               range would under-report whenever main
//                               already carries unreleased commits — a fix
//                               PR on top of an unreleased feat previews as
//                               a patch while the release computes a minor.
//                               Also reports the PR's own contribution
//                               separately, under the `pr_*` keys, for
//                               callers that must judge the branch rather
//                               than the release (the major signoff check).
//
// Output (stdout): one line of JSON:
//   { current, next, release_type, is_major, commit_count }
// plus, in `--main` mode:
//   { pr_release_type, pr_is_major, pr_commit_count }
// `release_type` is one of `"major" | "minor" | "patch" | null`.

import { readFileSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { format } from "node:util";
import semver from "semver";
import { analyzeCommits } from "@semantic-release/commit-analyzer";

const REPO_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i]?.replace(/^--/, "");
    if (!key) continue;
    out[key] = argv[i + 1];
  }
  return out;
}

// Read the same plugin config the release uses, so the preview applies
// identical semver-inference rules (preset, releaseRules, parserOpts, etc.).
// If the config shape ever changes, the preview tracks it automatically.
function readCommitAnalyzerConfig() {
  const releasercPath = resolve(REPO_ROOT, ".releaserc.json");
  const releaserc = JSON.parse(readFileSync(releasercPath, "utf8"));
  const entry = releaserc.plugins.find(
    (p) => (Array.isArray(p) ? p[0] : p) === "@semantic-release/commit-analyzer"
  );
  if (!entry) {
    throw new Error(
      `@semantic-release/commit-analyzer not configured in ${releasercPath}`
    );
  }
  return Array.isArray(entry) ? entry[1] : {};
}

function git(repo, args) {
  return execFileSync("git", args, {
    cwd: repo,
    encoding: "utf8",
    maxBuffer: 64 * 1024 * 1024,
  });
}

// Collect commits reachable from any of `include` but not from `exclude`, as
// `{hash, message}` records. Use ASCII US (0x1f) between fields and RS (0x1e)
// between records so commit bodies with arbitrary whitespace, quotes, and
// Unicode can't break parsing.
function getCommits(repo, { exclude, include }) {
  const args = ["log", "--reverse", "--format=%H%x1f%B%x1e", ...include];
  if (exclude) args.push(`^${exclude}`);
  return git(repo, args)
    .split("\x1e")
    .map((rec) => rec.replace(/^\n+/, ""))
    .filter(Boolean)
    .map((rec) => {
      const [hash, message] = rec.split("\x1f");
      return { hash: (hash ?? "").trim(), message: (message ?? "").trim() };
    });
}

// The latest tag reachable from `ref`. Always describe an explicit ref: a PR
// job has the branch checked out, and a branch that forked before the last
// release reaches only an older tag, which would preview a bump off a stale
// base.
function latestTag(repo, ref) {
  try {
    return git(repo, ["describe", "--tags", "--abbrev=0", ref]).trim() || null;
  } catch {
    return null;
  }
}

// Strip a leading `v` so the caller always gets a bare semver string
// (e.g. "1.2.3"). This repo's tags are already bare, but semver.inc() also
// returns without a prefix — keeping both in sync avoids a double-prefix in
// the PR comment's display strings.
function tagToVersion(tag) {
  return tag ? tag.replace(/^v/, "") || "0.0.0" : "0.0.0";
}

// commit-analyzer expects a signale-shaped logger and calls methods with
// printf-style format strings (e.g. `log("Analyzing commit: %s", msg)`).
// Route everything through util.format so the substitutions actually
// happen, and to stderr so stdout stays clean for the JSON payload.
const logger = {
  log: (...a) => console.error("[analyzer]", format(...a)),
  info: (...a) => console.error("[analyzer]", format(...a)),
  warn: (...a) => console.error("[analyzer]", format(...a)),
  error: (...a) => console.error("[analyzer]", format(...a)),
  success: (...a) => console.error("[analyzer]", format(...a)),
};

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (!args.head || (!args.base && !args.main) || (args.base && args.main)) {
    console.error(
      "Usage: preview-release.mjs --base <ref> --head <ref>\n" +
        "       preview-release.mjs --main <ref> --head <ref>"
    );
    process.exit(2);
  }

  // `--repo` exists so the tests can point the analyzer at a disposable
  // history. The plugin config still comes from this repository, so the test
  // exercises the rules production uses.
  const repo = args.repo ? resolve(args.repo) : REPO_ROOT;
  const pluginConfig = readCommitAnalyzerConfig();
  const analyze = (commits) =>
    analyzeCommits(pluginConfig, { commits, logger, cwd: REPO_ROOT });

  const prMode = Boolean(args.main);
  // In release-window mode the caller names the boundary and the version it is
  // releasing from; in PR mode both come from main, never from the branch.
  const versionRef = prMode ? args.main : args.head;
  const exclude = prMode ? latestTag(repo, args.main) : args.base;
  const include = prMode ? [args.main, args.head] : [args.head];

  const current = prMode
    ? tagToVersion(exclude)
    : tagToVersion(latestTag(repo, versionRef));

  const commits = getCommits(repo, { exclude, include });
  const releaseType = await analyze(commits);
  const next = releaseType ? semver.inc(current, releaseType) : current;

  const result = {
    current,
    next,
    release_type: releaseType,
    is_major: releaseType === "major",
    commit_count: commits.length,
  };

  if (prMode) {
    const prCommits = getCommits(repo, {
      exclude: args.main,
      include: [args.head],
    });
    const prReleaseType = await analyze(prCommits);
    result.pr_release_type = prReleaseType;
    result.pr_is_major = prReleaseType === "major";
    result.pr_commit_count = prCommits.length;
  }

  process.stdout.write(JSON.stringify(result) + "\n");
}

main().catch((err) => {
  console.error(err?.stack || String(err));
  process.exit(1);
});

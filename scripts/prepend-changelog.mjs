#!/usr/bin/env node
// Insert a generated release-notes block into CHANGELOG.md above the newest
// existing version entry, so the file stays newest-first without disturbing
// its title or intro paragraph.
//
// Exists as a file rather than an inline heredoc in release.sh for the same
// reason as read-preview-field.mjs: the logic is fiddly enough to deserve
// being readable, and the "no inline node -e" check in
// scripts/test-release-scripts.sh keeps it that way.
//
// Usage:
//   node scripts/prepend-changelog.mjs <notes-file> <changelog-file>
//
// Placement rules, in order:
//   1. If the changelog has an existing version heading, insert immediately
//      above the first one. That preserves a `# Changelog` title and any intro
//      text when they exist — this repo's changelog has neither, so the new
//      entry lands at the very top.
//   2. Otherwise append to the end — the file exists but has no entries yet.

import fs from 'node:fs';

const [notesPath, changelogPath] = process.argv.slice(2);

if (!notesPath || !changelogPath) {
  console.error('usage: prepend-changelog.mjs <notes-file> <changelog-file>');
  process.exit(1);
}

const notes = fs.readFileSync(notesPath, 'utf8').trimEnd();
const existing = fs.readFileSync(changelogPath, 'utf8');

const lines = existing.split('\n');
// `#{1,2}` because semantic-release emits `# [2.1.0]` for minor/major entries
// and `## [2.1.2]` for patches — matching only `##` would step over every
// major and insert below it. Matches the linked form (`## [2.1.2](...)`) and a
// plain `## 2.1.2` alike, with or without a trailing date.
const headingIndex = lines.findIndex((line) => /^#{1,2}\s+\[?\d+\.\d+\.\d+/.test(line));

let output;
if (headingIndex === -1) {
  output = `${existing.trimEnd()}\n\n${notes}\n`;
} else {
  const before = lines.slice(0, headingIndex).join('\n').trimEnd();
  const after = lines.slice(headingIndex).join('\n').trimStart();
  // No preamble (this repo's changelog starts straight in on the newest entry)
  // must not yield a file that opens with two blank lines.
  const preamble = before ? `${before}\n\n` : '';
  output = `${preamble}${notes}\n\n${after}`;
  if (!output.endsWith('\n')) output += '\n';
}

fs.writeFileSync(changelogPath, output);

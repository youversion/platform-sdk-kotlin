#!/usr/bin/env node
// Read one field out of the JSON that scripts/preview-release.mjs writes to
// stdout, with an ordered list of fallback fields and a literal default.
//
// Exists as a file rather than an inline `node -e` in release.sh so that
// scripts/test-release-scripts.sh's "no inline node -e" check stays meaningful
// and so the JSON handling can be read (and fixed) in one place.
//
// Usage (JSON on stdin):
//   printf '%s' "$json" | node scripts/read-preview-field.mjs --default unknown next current
//     -> prints j.next, else j.current, else "unknown"
//
// Always exits 0 and always prints exactly one line: a malformed or empty
// payload yields the default. The caller is a release script whose job is to
// log an advisory value — it must not die because a preview failed.

const argv = process.argv.slice(2);

let fallback = '';
const fields = [];
for (let i = 0; i < argv.length; i++) {
  if (argv[i] === '--default') {
    fallback = argv[++i] ?? '';
  } else {
    fields.push(argv[i]);
  }
}

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (chunk) => {
  raw += chunk;
});
process.stdin.on('end', () => {
  let parsed = {};
  try {
    parsed = JSON.parse(raw || '{}');
  } catch {
    parsed = {};
  }
  for (const field of fields) {
    const value = parsed[field];
    if (value !== undefined && value !== null && value !== '') {
      console.log(String(value));
      return;
    }
  }
  console.log(fallback);
});

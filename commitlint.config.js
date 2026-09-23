module.exports = {
  // @commitlint/config-conventional already sets
  // `parserPreset: "conventional-changelog-conventionalcommits"`, so `feat!:`
  // and `fix(scope)!:` parse as breaking here. That is the same preset the
  // release analyzer uses (see .releaserc.json), which keeps what commitlint
  // accepts and what the analyzer bumps on in agreement.
  extends: ['@commitlint/config-conventional'],
  // Skip the release commits this repo's own release script emits. Anchored to
  // `chore(release): <semver>` so a hand-written `chore(release): cleanup` is
  // still linted.
  ignores: [(message) => /^chore\(release\): \d+\.\d+\.\d+/.test(message)],
  rules: {
    // Release notes render the body verbatim, so a long migration note is
    // legitimate — but an unbounded line is almost always a paste accident.
    // 300 is generous enough for a URL or a one-line code sample.
    'body-max-line-length': [2, 'always', 300],
    'footer-max-line-length': [2, 'always', 300],
  },
};

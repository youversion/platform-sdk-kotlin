module.exports = {
  extends: ['@commitlint/config-conventional'],
  // Skip release commits emitted by semantic-release. These are authored by
  // the bot after the workflow has already passed every gate, so linting
  // them serves no purpose and would block re-running an old commit window
  // through the lint job if a release commit ever slipped into the range.
  ignores: [(message) => /^chore\(release\):/i.test(message)],
  rules: {
    'body-max-line-length': [0, 'always'],
    'footer-max-line-length': [0, 'always'],
  },
};

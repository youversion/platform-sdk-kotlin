---
status: accepted
---

# Stored highlights are bound to the account ID, and unsynced changes outlive sign-out

Once highlights persist across launches and sign-outs, every stored row needs an owner that is stable for the same person and different for anyone else. We key rows by the ID token's `sub` claim. On sign-out or account switch, that account's synced rows are deleted and its unsynced changes are kept on the device. They sync the next time the same account signs in here, and they are never sent under a different account.

When there is no account ID (an integrator supplied tokens without an ID token), highlights are off: the flow is empty, writes are refused, and nothing is stored. The SDK's own sign-in always requests `openid`, so this only affects integrators who bring their own tokens. It is documented on `configure` and `saveAuthData` and in the release notes.

## Considered Options

- **Key rows by a hash of the refresh or access token**, as the in-memory session check does. Rejected because tokens rotate, so the same person would lose their rows after a refresh. A hash also can't tell "same person, new token" from "someone else".
- **Discard unsynced changes on sign-out**, which was the previous behavior. Rejected because sign-out shouldn't destroy a reader's work once the device can keep it safely, scoped to its owner.
- **Keep highlights working without an account ID** by falling back to a device-wide owner. Rejected because rows could then pass from one person to the next on a shared device.

## Consequences

- Unsynced changes for an account that never signs in on this device again stay in the database indefinitely. This is accepted for now and marked as a shortcut in code.
- The sign-out confirmation shown when there are unsynced changes no longer warns about losing them. It says they will sync the next time the reader signs in on this device.
- `reset()` and `clearCachedHighlights()` delete only the current account's synced rows and never its unsynced changes.

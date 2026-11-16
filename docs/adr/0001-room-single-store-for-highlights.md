---
status: accepted
---

# Room is the single store for highlights, with sync derived from row state

Highlights used to live in an in-memory cache, with a separate in-memory queue of pending operations, so unsynced changes were lost when the process died. We moved them into an SDK-internal Room database that is both the source of truth and the only read path. The public `highlights` StateFlow is the DAO `Flow` passed through `stateIn`, and no second in-memory copy exists.

Each row is one verse: account, version, passage, color (null means removed), and whether it is server-backed and dirty. Sync reads the dirty rows and works out create, update, delete, or a local drop with no call from each row's final state. It does not replay a log of operations.

## Considered Options

- **Persist the existing operation queue alongside the cache.** Rejected because it keeps two stores that can disagree. It also keeps the coalescing problems the queue already has: add → remove → recolor sequences have to be replayed or merged in the right order.
- **Room as a write-through backup behind the in-memory list.** Rejected because the list and the table drift apart, and every bug fix has to land in both.
- **One row per highlight as the server stores it, including ranges.** Rejected because readers clear single verses out of ranges, which would mean splitting rows on every edit. Server ranges are expanded per verse on the way in.

## Consequences

- Several edits to one verse before it syncs collapse into its final state for free. Add then remove before any sync sends nothing.
- A chapter refresh must never touch dirty rows. Clean rows mirror the server, and a synced delete stays as a clean tombstone until a load that started after the delete confirms it. The load sequence counter and the per-chapter throttle stay in memory, because they describe this process, not the data.
- Sends are per verse. Batching ranges into one call is a later optimization, and it doesn't change the schema.
- Every schema change ships with a migration and a migration test. The database is only wiped on downgrade.
- Sync runs only while the app process is alive (no WorkManager). It resumes on the next launch because the dirty rows are still there.

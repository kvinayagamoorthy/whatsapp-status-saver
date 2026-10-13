# Decision Log

Append only. To change a decision, add a new entry that supersedes the old one and
mark the old one `Superseded by D-xxx`.

Format: ID · date · status · decision · why · alternatives considered.

---

## D-001 · 2026-10-13 · Accepted
**Decision:** The app is a WhatsApp status saver: browse cached status images/videos
and save chosen ones to the phone gallery. Not a general gallery viewer.
**Why:** Matches the repository's purpose; user confirmed.
**Alternatives:** Generic gallery viewer; status saver plus gallery tab (rejected to
keep the app simple).

## D-002 · 2026-10-13 · Accepted
**Decision:** Kotlin + Jetpack Compose + Material 3, single module, minSdk 26.
**Why:** Modern Android default; Material 3 gives dynamic color and system
light/dark nearly for free. API 26 covers almost all active devices and supports
`EXTRA_INITIAL_URI` for the folder picker.
**Alternatives:** Views/XML (more boilerplate); minSdk 29 (smaller reach) or 24
(more legacy branches).

## D-003 · 2026-10-13 · Accepted
**Decision:** Read statuses only via Storage Access Framework tree URIs with
persisted permission, on all API levels.
**Why:** One code path for API 26+; avoids `MANAGE_EXTERNAL_STORAGE` (Play policy
risk) and broad read permissions; `Android/media` is reachable through SAF on
Android 11+.
**Alternatives:** `MANAGE_EXTERNAL_STORAGE` (policy risk, scary prompt);
`READ_EXTERNAL_STORAGE` + `File` API (breaks on Android 11+).

## D-004 · 2026-10-13 · Accepted (folder names superseded by D-009)
**Decision:** Save via MediaStore into `Pictures/StatusSaver` and `Movies/StatusSaver`
on API 29+; public directory + media scan with `WRITE_EXTERNAL_STORAGE`
(`maxSdkVersion=28`) on API 26–28. Original file name is the identity for
"already saved".
**Why:** Files appear in every gallery app; no extra permission on modern Android;
WhatsApp file names are unique per status.
**Alternatives:** App-private storage (invisible to gallery apps); content hashing
for duplicates (slower, unnecessary given unique names).

## D-005 · 2026-10-13 · Accepted
**Decision:** No DI framework; repositories created once in `Application`.
**Why:** Handful of classes in one module; a framework would be speculative
complexity.
**Alternatives:** Hilt, Koin.

## D-006 · 2026-10-13 · Accepted
**Decision:** No `INTERNET` permission, no analytics, ads, or crash-reporting SDKs.
**Why:** "Simple, hassle free" and privacy; nothing in the spec needs the network.

## D-007 · 2026-10-13 · Accepted
**Decision:** Package / application ID is `com.moorthy.statussaver`.
**Why:** User's choice. Fixed before scaffolding because the application ID cannot
change after release without breaking upgrades.
**Alternatives:** `com.moothy.statussaver` (initial draft, rejected).

## D-008 · 2026-10-13 · Accepted
**Decision:** Distribute as a sideloaded, signed release APK for now. Play Store may
come later if needed.
**Why:** Simplest path for personal use; no store review or listing work.
**Consequences:** Release must be an APK (not only an AAB). Every update must be
signed with the same keystore, so it must be backed up outside the repo. Store
listing and privacy policy (T6.4) are deferred. Keep the no-network and no-"WhatsApp"-
branding rules anyway so a later Play Store move stays easy.
**Alternatives:** Play Store now (listing, policy review overhead); F-Droid
(reproducible build requirements).

## D-009 · 2026-10-13 · Accepted
**Decision:** App display name is **Keepsake**. Launcher icon is a bookmark ribbon
inside a circle (adaptive icon with a monochrome layer). Saved media goes to
`Pictures/Keepsake` and `Movies/Keepsake` (replaces the `StatusSaver` folder names
in D-004) so the gallery album matches the app name.
**Why:** User's choice. Distinctive, no trademark risk, and the shape stays legible
as a monochrome themed icon. Package ID stays `com.moorthy.statussaver` (D-007).
**Alternatives:** Status Saver, Glimpse, Stashy, Kept; icons "ring + down arrow"
and "photo/play stack with check".

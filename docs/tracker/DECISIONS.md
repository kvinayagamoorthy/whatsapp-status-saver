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

## D-010 · 2026-10-13 · Accepted
**Decision:** Toolchain: Gradle 9.8.1 (wrapper with pinned distribution SHA-256),
AGP 9.4.1 using its built-in Kotlin support, Kotlin 2.4.21, compileSdk and targetSdk
37, Java 17 bytecode compiled with JDK 21. `android:allowBackup="false"`.
**Why:** Latest stable versions at scaffold time (checked against the Gradle, Google
Maven, and Maven Central metadata). AGP 9 compiles Kotlin itself, so the separate
Kotlin Android plugin is not applied. Backup is off because a restored DataStore
would hold SAF folder URIs whose permissions do not survive a restore, so restored
state would only send the user through onboarding anyway.
**Alternatives:** The older versions cached on this machine (Gradle 8.9, AGP 8.5.2,
Kotlin 2.0.21); compileSdk 35 (the only platform installed before scaffolding).

## D-011 · 2026-10-13 · Accepted
**Decision:** Remove `ACCESS_NETWORK_STATE` and `WAKE_LOCK`, which Media3 ExoPlayer
adds to the merged manifest, using `tools:node="remove"`.
**Why:** The app plays only local files and never uses the network (D-006), so the
fewest permissions possible. Checked in the Media3 1.11.1 bytecode: the network-type
lookup catches `SecurityException` and falls back to "unknown", and the wake lock is
only used when `ExoPlayer.setWakeMode()` is called, which this app must not do.
**Consequences:** Never call `setWakeMode`; keep the screen on while a video plays
instead. Re-check this if Media3 is upgraded across a major version. Playback is
confirmed on device in T3.3.
**Alternatives:** Keep both permissions (harmless, but against the minimal-permission
goal in spec G6 / NFR4).

## D-012 · 2026-10-13 · Accepted
**Decision:** Add `data_extraction_rules.xml` that excludes everything from cloud
backup and device transfer, alongside `allowBackup="false"`. Suppress lint's
`DataExtractionRules` check on `<application>`.
**Why:** Extends D-010. On Android 12+ `allowBackup="false"` no longer stops
device-to-device transfer, so the rules file is needed to keep stale folder URIs off a
new phone. Lint then asks for `fullBackupContent` for Android 8-11, but that file is
never read when `allowBackup` is false, so the warning is a false positive here.
**Alternatives:** Rely on allowBackup only (transfer would still copy DataStore on
Android 12+); add an unused `fullBackupContent` file just to satisfy lint.

## D-013 · 2026-10-13 · Accepted
**Decision:** `StatusItem` carries the provider's MIME type (`mimeType`) next to
`MediaType`. design.md §1 concept table updated to list it.
**Why:** Saving needs the exact MIME type for the MediaStore row (§4) and sharing sets
the intent type from the item (§5). The children query already returns it, so keeping
it costs nothing. `MediaType` stays as the coarse image/video split used for tabs and
gallery folders.
**Alternatives:** Derive the MIME type from the file extension when saving or sharing
(a second, weaker source for a fact the provider already gives); send `image/*` or
`video/*` (loses the exact type MediaStore and receiving apps use).

## D-014 · 2026-10-13 · Accepted
**Decision:** JVM unit tests use Robolectric 4.17 (test-only dependency) wherever code
touches Android types. `testOptions` enables `isIncludeAndroidResources` so Robolectric
runs on the target SDK (37), and passes
`--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` to the test JVM, which
Robolectric's Android 16+ runtimes need on JDK 21.
**Why:** `StatusSourceStore` works with `Uri` and `ContentResolver` persisted
permissions, and `StatusItem` holds a `Uri` (T2.1/T2.2 tests need it too). Robolectric's
`ContentResolver` keeps a real persisted-permission list, so the store takes a plain
`ContentResolver` and no test-only interface exists in production code.
**Consequences:** First test run downloads Robolectric's Android runtime (about
100 MB) into `~/.m2`. Robolectric does not throw when releasing a grant the app no
longer holds, while a device does, so that guard in `StatusSourceStore.grant()` is
checked on a device (T1.5).
**Alternatives:** A small permissions interface with a hand-written fake plus String
URIs in the store (keeps tests plain JVM, but adds an abstraction with one production
caller and a second encoding of the same URI); mocking libraries.

## D-015 · 2026-10-13 · Accepted
**Decision:** A picked tree is a statuses folder when the last folder of its document
id is exactly `.Statuses` and its path contains a known app folder name, compared as a
whole name: `com.whatsapp.w4b` or `WhatsApp Business` means Business, otherwise
`com.whatsapp` or `WhatsApp` means WhatsApp. Any storage volume is accepted. The
names live in `data/StatusFolders.kt`, which T1.4's initial-URI paths will share.
**Why:** Matching folder names instead of the two full documented paths keeps the
R1 mitigation (the user can pick a `.Statuses` folder in another location) while still
telling the two apps apart. Whole-name comparison keeps "WhatsApp" from matching
"WhatsApp Business".
**Alternatives:** Accept only the four documented full paths (breaks the R1 fallback);
accept any `.Statuses` folder and ask the user which app it belongs to (an extra step
for a case not seen in practice).

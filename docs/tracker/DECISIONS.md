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
instead. Re-check this if Media3 is upgraded across a major version. Playback was
confirmed on the emulator in T3.3 (merged manifest still without both permissions).
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

## D-016 · 2026-10-13 · Accepted
**Decision:** On onboarding, the folder the user picks decides which app it is granted
for (`StatusFolders.classify`), not the card whose button opened the picker. A folder
that classifies as neither app is reported as a wrong folder and nothing is stored or
persisted.
**Why:** The folder is the fact; the button is only where the user started. Picking
the Business folder from the WhatsApp card is still a valid Business grant, and the
screen needs no memory of which button was pressed.
**Alternatives:** Reject a folder that does not match the button's app (an extra error
for a correct folder); remember the requested kind and store under it (could store a
Business folder as WhatsApp).

## D-017 · 2026-10-13 · Accepted
**Decision:** Onboarding shows a "Continue" button once at least one app is granted,
instead of moving on automatically after the first grant. Continuing removes
onboarding from the back stack. The start route is decided once per app start in
`StartViewModel`.
**Why:** FR1 lets the user grant both WhatsApp and WhatsApp Business; jumping away
after the first grant would hide the second card until settings exist (T5.2). Popping
onboarding makes Back from the list leave the app, as users expect from a home screen.
**Alternatives:** Navigate right after the first grant (simpler, but blocks granting
the second app); keep onboarding on the back stack (Back would return to setup).

## D-018 · 2026-10-13 · Accepted
**Decision:** `StatusRepository.load()` lists each valid source with one children query
and keeps only rows whose MIME type starts with `image/` or `video/` (this also drops
`.nomedia`, sub-folders and other files). Items are sorted by last-modified time,
newest first, and items with the same time by file name. A source whose folder no
longer exists gives no items instead of an error: on a device the documents provider
logs `FileNotFoundException` and returns a null cursor. Missing time or size columns
read as zero.
**Why:** FR2 asks for newest first; the name tie-break keeps the grid from reshuffling
between refreshes. A deleted folder with a kept grant is a real case (WhatsApp
uninstalled; checked on the emulator in T2.1) and should look like an empty folder, not
a crash. `Cursor.getLong` on a null value is implementation-defined, so the zero is
explicit.
**Alternatives:** Treat a missing folder as a lost grant and send the user to onboarding
(re-granting cannot help until WhatsApp recreates the folder); sort by name only (not
newest first).

## D-019 · 2026-10-13 · Accepted
**Decision:** `StatusRepository.load()` returns null when no granted folder is readable
and an empty list when readable folders hold no statuses. `StatusesViewModel` maps this
to `NeedsAccess`, `Empty` or `Content` (plus `Loading` before the answer) and takes the
load as a `suspend () -> List<StatusItem>?` function, so tests pass a fake answer
without an interface. `kotlinx-coroutines-test` 1.11.0 is a test-only dependency for
`Dispatchers.setMain`.
**Why:** "Grant again" and "nothing viewed yet" need different screens (FR2 empty
state, FR8 lost permission), and only the repository's single `validSources()` read
can tell them apart without a race. A function parameter is the smallest seam for one
caller. The test library is one minor version ahead of the core the app resolves to
(1.10.2): pinning it to 1.10.2 left a lint warning that the version catalog cannot
suppress, and core 1.11 is backward compatible.
**Alternatives:** A sealed result type from the repository (one more type for the same
two cases); the ViewModel calling `validSources()` itself (two reads, can disagree); a
`StatusRepository` interface with a fake class (abstraction for one caller).

## D-020 · 2026-10-13 · Accepted
**Decision:** The status grid's tabs are keyed by `MediaType` (Images = `IMAGE`, Videos
= `VIDEO`) and the selected tab is saved across rotation. Grid items are keyed by
document URI, not by file name. Coil's shared `ImageLoader` is built in
`StatusSaverApp` (`SingletonImageLoader.Factory`) with `VideoFrameDecoder` and
crossfade. The video badge is a local vector drawable. Video duration is not shown.
**Why:** `MediaType` already names the two kinds, so no tab type is needed and the
viewer route can reuse it. Names are the identity for saved copies (I3), but two
granted folders could hold the same name, and a duplicate key crashes a lazy grid.
Video frames need the decoder registered once for every `AsyncImage`. A vector avoids
the Material icons dependency for one icon. Duration needs a `MediaMetadataRetriever`
call per video, which is not "cheaply available" (FR2).
**Alternatives:** Per-tab ViewModel lists (more state for a one-line filter); name as
grid key (crash on a cross-app duplicate); `material-icons-core` for the play icon;
reading duration lazily per tile (more work per scroll for a small detail).

## D-021 · 2026-10-13 · Accepted
**Decision:** The status list loads only through `StatusesViewModel.reload()`, which the
screen calls on every `ON_RESUME` (the first appearance included); there is no load in
`init`. Pull-to-refresh calls `refresh()`, which shows the indicator and then reloads.
Only one load runs at a time: a request made while one is running is dropped, and a
pull during a quiet reload keeps the indicator until that load ends. Quiet reloads keep
the current list on screen without a spinner.
**Why:** Each time the user returns from WhatsApp there may be new files, and the first
appearance is just another return, so one trigger covers both. Without `init` loading
there is no double load on start. A load takes a few hundred milliseconds, so a second
one started meanwhile would read the same files; dropping it avoids racing results
and needs no cancellation.
**Alternatives:** Load in `init` and on resume (two loads at start); cancel and restart
on each request (the cancelled load's indicator state is easy to get wrong); watching
the folder for changes (a SAF tree cannot be observed with `FileObserver`, and resume
covers the real flow).

## D-022 · 2026-10-13 · Accepted
**Decision:** When the status list finds no readable folder (`NeedsAccess`), it
navigates to onboarding and removes itself from the back stack; onboarding's Continue
returns to the list as before (D-017). A readable but empty folder shows one message:
statuses appear after viewing them in WhatsApp, and they show up on return without any
action (T2.4). There is no "Open WhatsApp" button. If only one of two granted folders
is lost, the list keeps showing the other.
**Why:** FR8 asks to return to the FR1 flow, and onboarding already shows which app
needs granting again. Popping the list means Back from onboarding leaves the app
instead of returning to a list that cannot load. A launch button would need a
`<queries>` manifest entry and the spec does not ask for it. A partly lost source is
shown in settings (T5.2) rather than interrupting a working list. Note that a granted
folder deleted by uninstalling WhatsApp keeps its grant and shows as empty, not as lost
access (seen in T2.1).
**Alternatives:** A message with a "Grant again" button on the list (one more screen
for the same action as onboarding); keeping the list under onboarding on the back stack.

## D-023 · 2026-10-13 · Accepted
**Decision:** The viewer (`viewer/{tab}/{index}`) reads its items from the status
list's own `StatusesViewModel`, obtained with the `statuses` back stack entry as owner,
and pages through `itemsOf(tab)` starting at `index`. It has no ViewModel or load of its
own, except that it starts the list's load when that list was never loaded (the app was
killed while the viewer was open). If the tab has nothing to show, or there is no
readable folder, the viewer closes and the list shows why.
**Why:** The index must mean the item the user tapped. The list only reloads when its
own screen resumes (D-021), which cannot happen while the viewer is on top, so the
shared list stays fixed for as long as the viewer is open and no index can drift. A
separate load in the viewer could see new statuses at the front and open the wrong
item. Returning from the viewer resumes the list, which then reloads quietly as usual.
**Alternatives:** A viewer ViewModel loading on its own (index can drift); passing the
item's URI in the route (longer route, and the pager still needs the list); type-safe
serializable routes (needs the serialization plugin for one route).
**Known gap:** After the app is killed in the viewer, the reloaded list can differ from
the one tapped, so the restored page can be a neighbouring item. Vanished files are
T3.4.

## D-024 · 2026-10-13 · Accepted
**Decision:** Image zoom in the viewer is a small hand-written modifier
(`ui/viewer/Zoomable.kt`, `ZoomState` plus `Modifier.zoomable`), not a library. Pinch
zooms around the fingers between 1x (fitted) and 4x; double-tap zooms to 2.5x at the tap
or back to fitted. Panning stops where the image's edges meet the screen's, using the
image's real aspect ratio, so letterbox bands never grow. While fitted, one-finger drags
are left to the pager; while zoomed they pan the image and never change page, so the
user zooms out to swipe on. A gesture that ends below 1.05x snaps back to fitted. The
viewer decodes images at their original size. Zoom is not saved: it resets when the
page leaves the pager or the screen rotates.
**Why:** The spec asks only for pinch and double-tap, which take about 150 lines; a
zoom library would add a dependency and APK size for features not in the spec. The
snap exists because a pinch back in multiplies per-event factors to values such as
1.000000119 (seen on the emulator), which counted as zoomed and blocked paging; snapping
inside each step instead would stall a slow pinch. Original-size decoding lets zoom show
real detail; status images are at most a few megapixels. Handing a drag from a zoomed
image at its edge over to the pager mid-gesture is more code for a small gain.
**Alternatives:** Telephoto or another zoom library; `detectTransformGestures` (it
consumes one-finger drags too, which would stop paging); pan hand-off to the pager at
the edges; saving zoom across rotation.

## D-025 · 2026-10-13 · Accepted
**Decision:** Videos in the viewer play with one ExoPlayer owned by
`VideoPlayerViewModel`, scoped to the viewer's back stack entry, and are drawn with
Media3's Compose modules instead of the View-based `PlayerView`: `ContentFrame` (from
`media3-ui-compose`) for the picture and `PlayPauseButton`, `PositionText`,
`ProgressSlider`, `DurationText` and `MuteButton` (from `media3-ui-compose-material3`)
in an always-visible bottom bar. `media3-ui` is removed. The video on the settled page
starts playing from the start; swiping to another video replaces it. Going to the
background pauses it (a rotation does not), returning leaves it paused, and leaving the
viewer releases the player. The picture uses a TextureView. The player takes audio
focus and pauses when headphones are unplugged. The screen is kept on only while a
video plays (no wake lock, D-011).
**Why:** `PlayerView` has no mute button (checked in the 1.11.1 AAR), and adding one
means a custom controller layout; the Compose controls have all five the spec asks for
(FR3), follow the app's Material 3 theme and dynamic colour, and carry their own content
descriptions. The full `Player` composable is `@ExperimentalApi`, so the stable-ish
(`@UnstableApi`) building blocks are used instead. Holding the player in a ViewModel
keeps playback going through a rotation (NFR7) without saving and restoring a position,
and gives one clear release point. A TextureView moves with the page while swiping; a
SurfaceView does not follow view movement before Android 10, and minSdk is 26. One player
also means one hardware decoder in use. Autoplay matches how statuses are watched.
**Alternatives:** `PlayerView` in `AndroidView` with a separate mute button; the
experimental all-in-one `Player` composable; a player per page (two decoders while
swiping); a player remembered in the composable with its position saved across rotation;
controls that hide after a few seconds.
**Consequences:** Media3 Compose APIs need `@OptIn(UnstableApi::class)`; re-check them
when Media3 is upgraded. Debug APK went from 16.5 to 16.8 MB; the release size is checked
in T6.2.

## D-026 · 2026-10-13 · Accepted
**Decision:** When the page the user is looking at fails to load, the viewer asks
`StatusRepository.stillExists(item)` (one SAF query for the document) through
`StatusesViewModel.confirmVanished`. If the file is gone, the viewer closes and the list
shows "This status is no longer available..." in a snackbar, once; the list's reload on
resume drops the item (D-021). If the file is still there, the page says "This status
can't be shown." and the viewer stays, so the user can swipe on. Pages next to the
current one never close the viewer. A lost folder grant also counts as gone; the list's
reload then leads to onboarding (D-022).
**Why:** The plan asks for a message and a return to a refreshed list when WhatsApp has
expired a file, but a corrupt or unsupported file is not "no longer available", and
sending the user back for it would hide the rest of the list. Checking the folder is the
only reliable way to tell the two apart: Coil and ExoPlayer report both as load errors.
On the device, querying a deleted document through its tree URI throws
`IllegalArgumentException` (wrapping `FileNotFoundException`) rather than returning
null; a deleted folder gives a null cursor (T2.1). The notice lives in the shared list
ViewModel because the list is where it is shown, after the viewer is gone.
**Alternatives:** Treat every load error as expired (wrong message for broken files);
stay in the viewer with a message and no automatic return (the plan asks for the
return); check existence before opening each page (a query per swipe for a rare case).
**Known gap:** A rotation while the snackbar is showing dismisses it early; it is not
shown again.

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

## D-027 · 2026-10-14 · Accepted
**Decision:** `SavedMediaRepository.save(item)` (API 29+, `@RequiresApi(Q)` until T4.2
adds the API 26-28 path) inserts a MediaStore row first (`DISPLAY_NAME` = status name,
`MIME_TYPE`, `RELATIVE_PATH` = `Pictures/Keepsake/` or `Movies/Keepsake/`,
`IS_PENDING` = 1), then opens the status through its SAF URI and copies the bytes, then
clears `IS_PENDING`. Any `IOException`, `SecurityException` or `IllegalArgumentException`
deletes the row again and returns false; success returns true. It takes a plain
`ContentResolver`, like `StatusRepository`, and is not wired into `StatusSaverApp` until
a caller exists (T4.3/T4.4).
Instrumented tests use AndroidX Test runner 1.7.0 and ext-junit 1.3.0
(`androidTestImplementation`, `AndroidJUnitRunner`). They read status files from the
app's cache through `file://` URIs and query with `QUERY_ARG_MATCH_PENDING`, so the test
class needs API 30.
**Why:** Inserting before opening the source means a status deleted by WhatsApp between
listing and saving goes through the same cleanup as a failed copy, so the one cleanup
path is exercised by a test. A Boolean is all the M4 callers need now; T4.3 turns it into
a result that can also say "already saved". SAF grants cannot be created from a test,
and the copy only needs a readable URI; the real SAF source was checked with a probe on
the emulator (byte-identical copies, source folder unchanged). Without including pending
rows a copy left pending would be invisible to the test and pass.
**Run instrumented tests with `install` + `am instrument`** (CLAUDE.md), not
`connectedDebugAndroidTest`: the Gradle task uninstalls the app afterwards, which drops
the emulator's folder grant, and it runs on every attached device, including a phone.
**Alternatives:** Open the source before inserting (no row for a vanished file, but the
cleanup path then needs a stream that fails mid-copy to be tested); a sealed result type
now (no caller needs more than success or failure yet); Robolectric with a fake MediaStore
provider (would test the fake, not where Android puts the file).

## D-028 · 2026-10-14 · Accepted
**Decision:** On Android 8-9, `SavedMediaRepository.save(item)` copies the status into
`<public Pictures or Movies>/Keepsake/` as a hidden `.<name>.part` file, renames it to
the status's name once complete, then calls `MediaScannerConnection.scanFile` and waits
for its callback before returning true. A failed copy deletes the `.part` file and
returns false; a missing storage permission shows up as a failed copy (`mkdirs` or the
file write fails). The repository now takes a `Context` (for the scanner) instead of a
`ContentResolver`. Asking the user for `WRITE_EXTERNAL_STORAGE` belongs to the first
save action in the UI (T4.4), not to the repository.
**Why:** The `.part` file plays the role `IS_PENDING` plays on Android 10+ (D-027): the
scanner never sees a hidden file, and a broken copy never sits under the status's name,
where T4.3 would count it as already saved. Waiting for the scan means a saved file is
in the gallery (and in MediaStore queries) when `save` returns, so the check mark and the
"Saved" message are true. A permission prompt needs an activity, which only the UI has.
**Alternatives:** MediaStore insert with `DATA` on Android 8-9 (works, but the design
chose the documented file path, and it needs the same permission anyway); writing the
target file directly (a failed copy could leave a truncated file under the status's
name); fire-and-forget scan (the file could be missing from the gallery right after
"Saved").
**Checked on the API 28 emulator:** Android adds `READ_EXTERNAL_STORAGE` to any app that
declares `WRITE_EXTERNAL_STORAGE`; `pm grant` of WRITE alone left the process with the
default storage view and `mkdirs` failed, while the permission dialog grants both. A grant
given to an already running instrumentation process did not reach it; whether a grant
from the real dialog reaches the running app must be checked in T4.4. The Android 9 media
scanner only lists files it can parse, so tests use a real one-second MP4 asset.

## D-029 · 2026-10-14 · Accepted
**Decision:** `SavedMediaRepository.save(item)` returns `SaveResult` (`SAVED`,
`ALREADY_SAVED`, `FAILED`, in `data/model/`). Before copying it checks the storage for a
finished copy with the item's name (API 29+: one MediaStore query on `RELATIVE_PATH` and
`DISPLAY_NAME`; API 26-28: `File.isFile`), and a `Mutex` runs saves one at a time.
`savedNames: StateFlow<Set<String>>` holds the names of saved statuses: empty until
`refreshSavedNames()` reads both Keepsake folders, then updated by every `SAVED` or
`ALREADY_SAVED` result. The repository is still not wired into `StatusSaverApp`; T4.4
does that, and calls the refresh when the list loads and on resume.
**Why:** Asking the storage per save keeps saving idempotent even before the names were
read and after the user deleted a copy in a gallery app (that status can then be saved
again). Without the lock, two saves started together both found no copy: on Android 10+
MediaStore then made `name (1).jpg`, and on Android 8-9 both wrote the same `.part`
file; a test with a 16 MB file catches both. One flow of names serves the grid and the
viewer (FR5) with no per-tile queries. Pending rows and `.part` files are copies still
being written, so they do not count.
**Alternatives:** Decide from the cached names alone (wrong right after a restart, and
stale after a deletion in a gallery app); a cold `Flow` that queries on every collection
(a query per collector and no update after a save without another query); keep the
Boolean and let callers check names (spreads I4 over every caller).

## D-030 · 2026-10-14 · Accepted
**Decision:** The viewer's Save action is one round button at the top end, on a dark
scrim, over every page; it acts on the pager's current page. It turns into a check mark
when the status's name is in `savedNames`, and shows a spinner while that name is being
copied (taps are then ignored). Grid tiles show a small check badge at the top end.
`StatusesViewModel` (shared by list and viewer, D-023) gets `savedNames`, `savingNames`,
`save(item)` and a one-shot `saveNotice` (`SaveResult?`), and re-reads saved names in
every `reload`. The notice is shown as a snackbar by whichever screen is in front
(`SaveNoticeEffect`); a newer notice replaces the one on screen. On Android 8-9
`rememberSaveWithPermission` (in `ui/`) asks for `WRITE_EXTERNAL_STORAGE` before the
first save; if refused, a snackbar points to the app's settings.
**Why:** The bottom of a video page holds the player's control bar (D-025), and status
media can be any colour, so a scrimmed button at the top stays visible and out of the
way. One button for both the action and the state needs no extra space in the viewer,
and tapping it again gives the "Already saved" message the spec asks for. The notice
lives in the ViewModel because the user can leave the viewer while a large video is
still being copied. The permission prompt needs an activity, so it is a UI helper; the
data layer only reports failure (D-028). Checked on the API 28 emulator: a grant from the
real dialog reaches the running app, and the save right after it succeeds.
**Alternatives:** A top app bar in the viewer (takes space from the media and needs a
back button design); a bottom action row (collides with the video controls); a
floating action button (covers media and the video slider); asking for the permission
at onboarding (asks before the user wants to save anything).

## D-031 · 2026-10-14 · Accepted
**Decision:** Multi-select lives in `StatusesViewModel`: `selectedUris` (document URIs,
the grid's keys), `toggleSelection`, `clearSelection` and `saveSelected`. Selection mode
is "the set is not empty"; a long press toggles a tile, and in the mode a tap toggles
instead of opening the viewer. The selection spans both tabs. A reload keeps only URIs
still listed. `saveSelected` takes the selected items in list order, clears the
selection at once, and saves them one after another through the same path as the
viewer's single save; each item's spinner state ends with its own copy, and one
`SaveSummary` (saved / already saved / failed counts) is the notice for the whole
action. A single save is a summary of one and keeps its own messages. The storage
permission helper became `rememberStoragePermissionGate`, which wraps any save action.
**Why:** The ViewModel already owns the list and the save path, survives rotation, and
can be tested without a device; a `rememberSaveable` set would need its own saver and
would not see reloads. URIs, not names, because both apps' folders can hold the same
name. Leaving the mode right away lets the user carry on while a large selection copies.
One message per action keeps several results from queueing as separate snackbars.
**Alternatives:** Selection per tab (the counts then hide items of the other tab, but
switching tabs would silently drop a selection); a progress dialog during multi-save
(blocks the app for what is usually a few seconds); a notice per item.

## D-032 · 2026-10-14 · Accepted
**Decision:** Share (FR6) is a Share button in the viewer, left of Save, on the same
dark scrim. It opens the system chooser for an `ACTION_SEND` of the status's own SAF
document URI with its MIME type, `FLAG_GRANT_READ_URI_PERMISSION`, and the URI as clip
data (`ui/viewer/ShareIntent.kt`). Tapping it pauses a playing video. Only the viewer
shares, one status at a time.
**Why:** design.md §5: the folder grant lets Keepsake hand a read grant for a document
inside it to another app, so no copy or `FileProvider` is needed; checked on API 35
(Messages attached the image, the chooser showed its preview) and API 28 (Messaging
attached the image). The clip data is what carries the grant through the chooser. On
API 35 the chooser is translucent, so the viewer is only paused, not stopped, and the
video went on playing with sound behind the sheet until the end; pausing on tap keeps
the user's attention on picking an app, and the user can resume with the play button.
The spec asks for sharing "an item", so the list's selection mode has no Share action.
**Alternatives:** Copying to the app's cache and sharing through a `FileProvider`
(extra copy, extra manifest entry, and cleanup); sharing the selected items with
`ACTION_SEND_MULTIPLE` (no current need in the spec); pausing in an `ON_PAUSE` handler
(would also pause for unrelated dialogs and on multi-window focus changes).

## D-033 · 2026-10-14 · Accepted
**Decision:** The theme mode is a `ThemeMode` enum (`SYSTEM`, `LIGHT`, `DARK`) stored by
name in its own DataStore file `settings` by `SettingsStore`; an unknown stored name
reads as `SYSTEM`. `MainActivity` collects the store, draws nothing until the first
value arrives, and passes `isDarkTheme(mode, isSystemInDarkTheme())` to `KeepsakeTheme`
and to the system bar styles. The settings screen (gear in the list's top bar, route
`settings`) only writes the store. On Android 12+ the activity also calls
`UiModeManager.setApplicationNightMode` (`SYSTEM` → `MODE_NIGHT_AUTO`, `LIGHT` → `NO`,
`DARK` → `YES`).
**Why:** One source of truth: the screen writes, the activity follows, so the app
changes at once and after a restart in the same way. Waiting for the first value
costs a few milliseconds and avoids drawing the wrong theme first (as the start route
already does, D-017). Without telling the system, the API 35 emulator showed the light
launch screen for about 2 s on a cold start before a dark app; with
`setApplicationNightMode` it is dark from the first frame, and with `SYSTEM` the app
still followed `cmd uimode night yes/no` both ways. The system keeps that value per
app, and the app's configuration then reports the chosen mode, which `isDarkTheme`
reads unchanged. Status bar icons now follow the app's theme, not only the phone's
(closes that pending check). A separate file keeps settings apart from the folder
grants.
**Limits:** Below Android 12 there is no such call, and the system draws the launch
window from the manifest theme before any app code runs; on the API 28 emulator a
cold start with Dark chosen on a light phone showed the light window for about 1.8 s
(less on a real phone, which starts faster). Accepted: it happens only when the choice
differs from the phone's setting.
**Alternatives:** AppCompat's `setDefaultNightMode` (adds AppCompat for one call);
reading the choice synchronously from `SharedPreferences` in `onCreate` to pick a dark
or light window theme (a second store, and the pre-process launch window would still
flash); `windowDisablePreview` (no flash, but the launcher stays on screen until the
first frame, which feels like a slow start); `distinctUntilChanged` on the flow
(Compose state already ignores equal values).

## D-034 · 2026-10-14 · Accepted
**Decision:** Access management (FR8) lives in settings as a "Statuses folders" section
that reuses onboarding's per-app cards and ViewModel. `OnboardingViewModel` became
`FolderAccessViewModel` (same package, one instance per screen) and gained `revoke`;
the card moved to `ui/onboarding/SourceCard.kt` and shows Remove access only when
given an `onRevoke` (settings). `StatusSourceStore.revoke(kind)` removes the stored URI
and releases the persisted read permission if the system still holds it. Remove access
asks first; the dialog survives rotation. Removing the last folder needs no special
case: the list reloads on resume, finds no source and goes to onboarding (D-022).
**Why:** Granting, re-picking and the per-app state are the same concept on both
screens; one ViewModel and one card keep them from drifting apart. The confirmation
says that saved copies stay in the gallery, because "remove" could be read as deleting
them, and losing the last folder means going through onboarding again. The release is
guarded as in `grant`, since a device throws when releasing a grant it no longer holds
(T1.5); Robolectric does not throw there, so the mutant that drops the guard survives
the unit tests. Checked on API 35 and API 28: after Remove the persisted grant is gone
(`persisted=0x0`) and the stored URI is deleted. On API 28 the running activity still
held the picker's temporary, non-persisted grant until it finished; the app does not
use it, because sources count only with a persisted grant.
**Alternatives:** A separate settings ViewModel duplicating the pick-and-classify
logic; navigating from settings straight to onboarding after the last revoke (a second
path for what the list already does); revoking without asking.

## D-035 · 2026-10-14 · Accepted
**Decision:** TalkBack describes a status by its kind and age: "Image status, 3 hours
ago" / "Video status, Yesterday" (`statusDescription`, `DateUtils.getRelativeTimeSpanString`
against the file's last-modified time, minute resolution). The same words label the
grid tile (as its content description; the play badge is no longer read separately)
and the viewer's picture (the image itself; for videos an empty layer with merged
semantics on top of the player's view, since semantics on `ContentFrame` did not reach
the interop view).
**Why:** Before, image tiles had no label and every video tile said "Video", so a
TalkBack user could not tell statuses apart, and touching the viewer's picture said
nothing. File names (`IMG-…`) mean nothing when spoken; the age is what differs between
statuses, which are all under a day old. Checked with TalkBack 15 on API 35: tiles read
"Image status, 22 hours ago. In list", a saved one adds "Saved", selection mode prefixes
"Selected" / "Not selected", the viewer reads "Image status, 22 hours ago. Image. In
horizontal pager" and "Video status, 16 hours ago", buttons and radio buttons read their
labels and states. All actionable nodes are at least 48 dp (API 35 and 28). At font
scale 2.0 the list, selection bar, settings, viewer controls and snackbar still fit.
**Known gap:** Media3's `ProgressSlider` reads its position as a fraction ("0.18.
Slider"). It is Media3's control (D-025); the position and duration texts beside it are
visible but not read with it. Left for a Media3 update or a later polish task.
**Alternatives:** Reading the file name; numbering tiles ("Image 3 of 150", which
changes whenever statuses arrive); a label on a wrapper around each viewer page (not
focusable, so TalkBack skipped it).

## D-036 · 2026-10-14 · Accepted
**Decision:** The launcher icon is a single adaptive icon in `mipmap-anydpi`
(`ic_launcher`, also the round icon): brand terracotta `#8C4A2F` background, and a
vector foreground with a light ring (radius 26, inside the 66-unit safe circle) around
a white bookmark ribbon. The same foreground drawable is the monochrome layer.
**Why:** minSdk 26 supports adaptive icons everywhere, so no bitmap fallbacks are
needed. Themed icons use only the monochrome layer's shape, and ring plus ribbon is
already a single-colour shape, so a second drawable would only repeat it. Checked on
API 35 (circle mask in the app drawer; with themed icons on, the home-screen icon is
tinted like Gmail and Photos) and API 28 (Launcher3's rounded-square mask). Lint now
reports no issues.
**Alternatives:** A separate monochrome drawable (same shape twice); PNG fallbacks per
density (only needed below API 26).

## D-037 · 2026-10-14 · Accepted
**Decision:** The status list calls `ReportDrawnWhen { uiState != Loading }`, so Android
logs "Fully drawn" (and counts the start as finished) once the grid shows statuses or
the reason it has none. Performance (NFR2, NFR3) is measured on a release build: the
start time with `am start -W` (first frame) and the "Fully drawn" logcat line, the
scrolling with `dumpsys gfxinfo` over 24 fast swipes, each on `keepsake_api35` with 202
statuses and `-gpu host`, and again after `cmd package compile -m speed-profile`, the
state a phone reaches once its background compile job has run.
**Why:** The first frame alone is not "visible grid": the folders are read after it.
Results (release build, temporarily signed with the debug key, not minified): grid
drawn in 417-573 ms just after install (median about 470 ms) and 341-659 ms after the
profile compile (median about 360 ms), first frame about 200 ms; the debug build takes
about 1.28 s, which is why debug numbers are not used. Scrolling: 2.8-3.0% janky frames
(p50 17 ms, p90 25 ms, p99 34-38 ms) against 0.6% for the system Settings app under the
same swipes; T2.3 measured 2.1% with 146 statuses. NFR2 is met on the emulator with
room to spare; a mid-range phone is slower, so the real-phone check stays pending
(T6.3).
**Notes:** Running two `-gpu host` emulators at once made the API 35 emulator crash
three times ("context was preempted by others", EGL fence errors); with one emulator
it ran stable. A crash can also lose the last install and the compile state.
**Alternatives:** Macrobenchmark with a baseline profile module (a second Gradle module
and a device-side test harness for one measurement; worth it only if a real phone
misses NFR2); timing with log lines in the app (removed again, and less exact than the
system's own report).

## D-038 · 2026-10-14 · Accepted
**Decision:** Release builds use R8 (`isMinifyEnabled`, `isShrinkResources`, the default
`proguard-android-optimize.txt`, no app rules) and are signed with a release key: a
PKCS12 keystore at `~/keystores/keepsake-release.jks` (RSA 4096, alias `keepsake`,
`CN=Keepsake`, valid until 2059, certificate SHA-256
`a58d635f66f4e8104e3d5722846b8130ced71c2c427e8d6e009a350f5c12fd8c`), created by Claude
with a random password at the user's request. Gradle reads its path and passwords from
`keystore.properties` in the repo root (git-ignored, mode 600); without that file the
release APK is built unsigned.
**Why:** User's choice (keystore made for them; secrets in the project's own git-ignored
file, not in the user-wide Gradle properties). Every sideloaded update must carry the
same signature (D-008), so the keystore and `keystore.properties` must be backed up off
this machine. R8 brought the APK from 12.4 MB to 2.16 MB (NFR5 asks for under 10 MB).
The minified build was checked on `keepsake_api35`: list, video frames, video playback
with sound, save with snackbar, share sheet preview, dark theme, folder cards and About
all work and logcat shows no crash; a fresh install of the signed APK went through
onboarding and listed the statuses. Libraries ship their own keep rules, so no app
rules were needed.
**Alternatives:** Keystore made by the user (more steps for the same result); passwords
in `~/.gradle/gradle.properties` (visible to every build on the machine); a debug-signed
release (cannot be updated by a properly signed build later).

## D-039 · 2026-10-14 · Accepted
**Decision:** The onboarding hint (`onboarding_picker_hint`) has one wording per folder
picker: `values` (Android 8-9) "tap “Select”"; `values-v29` (Android 10) "tap “Allow
access to ".Statuses"” at the bottom, then “Allow”"; `values-v30` (Android 11+) "tap “Use
this folder”, then “Allow”".
**Why:** The hint named Android 11's buttons on every version, but Android 9's picker
has a single "SELECT" button and Android 10's a single "ALLOW ACCESS TO ".STATUSES"" button
followed by an "ALLOW" dialog (seen on the API 28 and 29 emulators; API 35 and the user's
Android 15 phone show "Use this folder" and "Allow"). Version-qualified resources are
Android's own way to pick text by version and need no code.
**Alternatives:** A neutral hint without button names (less help exactly where users get
stuck); choosing the text in code by `Build.VERSION.SDK_INT` (code for what resources
already do).

## D-040 · 2026-10-14 · Accepted
**Decision:** The video seek bar is Media3's `ProgressSlider`, wrapped in `SeekBar`
(`ui/viewer/VideoPage.kt`), which reads the position once a second
(`rememberProgressStateWithTickInterval`) and sets the slider's `stateDescription` to
"00:07 of 00:30" (`videoPositionDescription`, `DateUtils.formatElapsedTime`, the same
minutes:seconds form as the position and duration texts); only the position while the
length is unknown.
**Why:** TalkBack read the slider as a bare fraction ("0.18. Slider", D-035). Media3's
`ProgressSlider` passes its modifier to the Material slider, so a `stateDescription` there
replaces the fraction without forking the control. Checked with TalkBack 15 on API 35:
"00:08 of 00:30. Slider". A one-second tick is the finest step the description names.
**Alternatives:** Writing an own slider (more code to keep in step with Media3); merging
the position and duration texts into the slider node (they would no longer be visible
to TalkBack as separate items, and the slider would still say the fraction).

## D-041 · 2026-10-14 · Accepted
**Decision:** The version stays 0.1.0 (versionCode 1) for now.
**Why:** User's choice after the phone update. The build on the user's phone already
carries all work up to T5.8, and no APK is being handed to other phones yet.
**Consequences:** About shows 0.1.0 for every build until the version is raised.
`adb install -r` and the Files app's "Update" both accept a build with the same
versionCode; raise versionCode before handing out an APK that should replace one already
on other phones, so the builds can be told apart.

# Design — Keepsake (Android status saver)

Status: Draft v1 (2026-10-13). Changes to this file need a `tracker/DECISIONS.md` entry.

## 1. Program theory

This section is the short version of "why the program is shaped this way". Read it
before any non-trivial change (see the `programming-as-theory-building` skill).

**World mapping.** WhatsApp writes each status a user views into a hidden folder as
an ordinary image or video file and deletes it after roughly 24 hours. The user wants
to keep some of them. The app is a window onto that folder plus a one-way copy into
the shared gallery. Nothing more.

**Core concepts** (use these names everywhere; do not invent synonyms):

| Concept | Meaning | Code home |
|---------|---------|-----------|
| `StatusSource` | One granted statuses folder (WhatsApp or WhatsApp Business), identified by its SAF tree URI | `data/StatusSourceStore` |
| `StatusItem` | One media file currently in a source folder: uri, file name, type (image/video), MIME type, modified time, size | `data/StatusRepository` |
| Saved copy | A copy of a `StatusItem` in the shared gallery, matched by file name | `data/SavedMediaRepository` |
| Theme mode | System / Light / Dark | `data/SettingsStore`, `ui/theme/` |

**Invariants.**

- I1. The app only reads from WhatsApp folders, never writes or deletes there.
- I2. Reading goes only through SAF tree URIs the user granted (persisted permission).
- I3. A status file's name is its identity. WhatsApp names are unique per status, so
  "already saved" means "a file with the same name exists in our gallery folder".
- I4. Saving is idempotent: saving an already-saved item does not create a new file.
- I5. The app has no network access (no `INTERNET` permission in the merged manifest).
- I6. UI state is derived from repositories; screens never touch `ContentResolver`
  directly.

**Boundary.** Not a gallery, not a file manager, not a WhatsApp client. Requests that
need network, WhatsApp interaction, or editing are out of scope (see `spec.md` §4).

## 2. Architecture

Single Gradle module `:app`. Layers by package:

```
com.moorthy.statussaver
├── StatusSaverApp.kt          Application; builds repositories (manual DI)
├── MainActivity.kt            Single activity, hosts Compose NavHost
├── data/
│   ├── StatusSourceStore.kt   Persisted tree URIs (DataStore) + permission checks
│   ├── StatusFolders.kt       Known statuses folders: classify a picked tree, picker start URI
│   ├── StatusRepository.kt    Lists StatusItems from all granted sources
│   ├── SavedMediaRepository.kt Save to MediaStore, query saved names
│   ├── SettingsStore.kt       Theme mode (DataStore)
│   └── model/                 StatusItem, MediaType, StatusSourceKind
└── ui/
    ├── KeepsakeNavHost.kt     Routes and NavHost
    ├── StartViewModel.kt      First route: statuses if a source is readable, else onboarding
    ├── theme/                 Material 3 theme, dynamic color, ThemeMode handling
    ├── onboarding/            FR1 access explanation + picker launch
    ├── statuses/              FR2 grid with Images/Videos tabs, multi-select
    ├── viewer/                FR3 pager, image zoom, video player
    └── settings/              FR7 theme, FR8 access management, about
```

Flow: Compose screen → `ViewModel` (exposes `StateFlow<UiState>`) → repository →
Android APIs. Repositories are plain classes created once in `StatusSaverApp`.
No Hilt/Koin: one module and a handful of classes do not need it.

Navigation: Navigation Compose with routes `onboarding`, `statuses`,
`viewer/{tab}/{index}`, `settings`. Start destination chosen from whether any
`StatusSource` has a valid persisted permission. Onboarding offers "Continue" once one
app is granted and leaves the back stack when the user continues (D-017). The list
replaces itself with onboarding when no granted folder is readable any more (D-022).
The viewer route's `{tab}` is a `MediaType` name and `{index}` a position in that tab's
list; the viewer shares the list's `StatusesViewModel` (scoped to the `statuses` back
stack entry), so the index names the item the user tapped (D-023).

## 3. Reading statuses (SAF)

Known folder locations (relative to primary external storage):

| App | Android 11+ path | Android 10 and below |
|-----|------------------|----------------------|
| WhatsApp | `Android/media/com.whatsapp/WhatsApp/Media/.Statuses` | `WhatsApp/Media/.Statuses` |
| WhatsApp Business | `Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses` | `WhatsApp Business/Media/.Statuses` |

Approach (one code path for all API levels 26+):

1. Launch `ACTION_OPEN_DOCUMENT_TREE` via `ActivityResultContracts.OpenDocumentTree`,
   with `EXTRA_INITIAL_URI` built by
   `DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:<path>")`
   for the most likely path on the running API level.
2. Validate the returned tree (`StatusFolders.classify`): the last folder of its
   document id must be `.Statuses`, and the path must contain a WhatsApp Business
   folder name (`com.whatsapp.w4b`, `WhatsApp Business`) or a WhatsApp one
   (`com.whatsapp`, `WhatsApp`), compared as whole folder names. Any storage volume is
   accepted. Otherwise show "wrong folder" and retry (D-015).
3. `takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)`; store the URI
   in DataStore keyed by source kind.
4. List with `DocumentsContract.buildChildDocumentsUriUsingTree` and a single
   `ContentResolver.query` projecting id, display name, mime type, last modified,
   size. Avoid `DocumentFile.listFiles()` + per-file calls (slow on large folders).
5. Keep rows whose MIME type starts with `image/` or `video/`; this drops `.nomedia`
   and sub-folders. Merge all sources, newest first, same time ordered by name. A
   granted folder that no longer exists lists as empty (D-018).
6. On each load, check `contentResolver.persistedUriPermissions`; a missing grant
   marks the source invalid. When no source is valid, `load()` returns null (not an
   empty list), the list shows `NeedsAccess` and routes to onboarding (D-019).

Why SAF everywhere: avoids `MANAGE_EXTERNAL_STORAGE` (Play policy risk) and broad
read permissions, and `Android/media` stays reachable through SAF on Android 11+.
See DECISIONS D-003.

## 4. Saving to gallery (MediaStore)

- **API 29+:** insert into `MediaStore.Images.Media` / `MediaStore.Video.Media`
  (`VOLUME_EXTERNAL_PRIMARY`) with `DISPLAY_NAME` = original file name,
  `RELATIVE_PATH` = `Pictures/Keepsake/` or `Movies/Keepsake/`, `IS_PENDING=1`;
  stream-copy from the SAF URI; then set `IS_PENDING=0`. Delete the row on failure.
  The row is inserted before the source is opened, so a status that vanished before
  saving takes the same cleanup path as a failed copy (D-027).
- **API 26–28:** requires `WRITE_EXTERNAL_STORAGE` (manifest `maxSdkVersion="28"`,
  requested at first save, T4.4). Copy into
  `Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES|MOVIES)/Keepsake/`
  as a hidden `.<name>.part` file, rename it once complete, then
  `MediaScannerConnection.scanFile` and wait for the scan, so the copy is in the gallery
  when the save reports success (D-028). Android adds `READ_EXTERNAL_STORAGE` to the
  request; without both granted the shared folders are not writable.
- **Already-saved check (I3, I4):** API 29+ query MediaStore for our `RELATIVE_PATH`
  and collect `DISPLAY_NAME`s (own files are visible without read permission; pending
  rows are left out); API 26–28 list the public directory, skipping hidden `.part`
  files. Result is `SavedMediaRepository.savedNames`, a `StateFlow<Set<String>>` filled
  by `refreshSavedNames()` and updated by each save. `save()` itself asks the storage for
  the one name before copying (not the flow, which may not have been read yet), and
  saves run one at a time, so a double tap cannot make a second copy. It returns
  `SaveResult.SAVED`, `ALREADY_SAVED` or `FAILED` (D-029).
- Copy runs on `Dispatchers.IO`; multi-save processes items sequentially and reports
  counts (saved / already saved / failed).

Known limitation: after app reinstall on API 29+, previously saved files are no
longer "owned" and may not appear in the query, so the indicator can miss them and
MediaStore may create `name (1).jpg`. Accepted for v1 (simplicity over a read
permission). Tracked as risk R2 in `plan.md`.

## 5. Sharing

Share the SAF content URI directly with `Intent.ACTION_SEND`, MIME type from the item,
and `FLAG_GRANT_READ_URI_PERMISSION`. No `FileProvider` and no temp copies needed.
The URI is also set as the intent's clip data, which carries the grant through the
system chooser (`Intent.createChooser`) to the app the user picks. Keepsake may pass the
grant on because the document lies inside its persisted folder grant (checked on API 35
and API 28). The intent is built by `ui/viewer/ShareIntent.kt`; the viewer's Share button
sits beside Save and acts on the current page (FR6). Sharing is single-item only. The
share sheet covers only part of the viewer, which keeps running behind it, so the Share
button pauses a playing video first (D-032).

## 6. UI and theming

- Material 3, `dynamicColorScheme` on API 31+, static brand scheme below.
- `ThemeMode` (System/Light/Dark) from `SettingsStore`; `System` uses
  `isSystemInDarkTheme()`. Applied in `MainActivity` before first frame; edge-to-edge.
- Grid: `LazyVerticalGrid` adaptive columns (min ~110 dp), square tiles, Coil
  `AsyncImage`; videos via Coil `VideoFrameDecoder` registered in `StatusSaverApp`.
  Tabs are keyed by `MediaType`; grid items by document URI (D-020).
- Viewer: `HorizontalPager` over the tapped tab's items, keyed by URI, starting at the
  tapped index; the saved pager state keeps the page across rotation (D-023). Images
  are decoded at full size with a small hand-written zoomable modifier
  (`ui/viewer/Zoomable.kt`): pinch zooms around the fingers up to 4x, double-tap zooms
  to 2.5x at the tap or back out, panning stops at the image's edges, and one-finger
  drags pan only while zoomed, otherwise they change page (D-024). Videos: one Media3
  `ExoPlayer` per viewer, held by `VideoPlayerViewModel` (kept across rotation, released
  when the viewer leaves the back stack); the settled page plays from the start, other
  video pages show their first frame. The page is drawn with Media3's Compose
  `ContentFrame` (TextureView) under a Material 3 control bar from
  `media3-ui-compose-material3`: play/pause, position, seek slider, duration, mute.
  Leaving the app pauses (not a rotation); the screen stays on only while playing
  (D-025).
- Saving (FR4, FR5): a round Save button on a dark scrim at the viewer's top end acts on
  the current page; it shows a spinner while copying and a check mark once the status
  is in the gallery, and tapping the check says "Already saved". Grid tiles of saved
  statuses carry a small check badge. Results appear as a snackbar on whichever screen
  is in front (the viewer's sits above the video controls). Saved names come from
  `SavedMediaRepository.savedNames`, re-read with every list reload. On Android 8-9
  the first save asks for the storage permission (D-030).
- Multi-select: long-press enters selection mode; top app bar switches to
  "N selected" with Save action. Taps then select or unselect (a ring or a filled check
  at the tile's top start, plus a border); Back or the cross leaves the mode. The
  selection is held by URI in `StatusesViewModel` (survives rotation, drops statuses
  that vanished on reload). "Save selected" leaves the mode at once, saves in list order
  and reports one `SaveSummary` snackbar, e.g. "3 statuses saved to your gallery. 1 was
  already saved." (D-031).
- Refresh: pull-to-refresh plus reload on `ON_RESUME` (also the first load); one load
  at a time, quiet reloads keep the current list on screen (D-021).

## 7. Error handling

Only where the world can actually fail:

- Lost permission → onboarding.
- Source file deleted between listing and open/save (WhatsApp expired it) →
  "This status is no longer available", refresh list. In the viewer, a page that fails
  to load (Coil error for images, player error for videos) asks
  `StatusRepository.stillExists`; if the file is gone the viewer closes and the list shows
  the message in a snackbar and drops the item on its resume reload. A file that is still
  there but cannot be decoded shows "This status can't be shown." on its page, and the
  viewer stays (D-026).
- Storage full / IO failure on save → snackbar with failure, MediaStore row cleaned up.

## 8. Testing strategy

- JVM unit tests (Robolectric where Android types are involved, D-014): folder
  classification from tree document id, MIME filtering,
  sorting, save-result aggregation, ViewModel state transitions with fake repositories.
- Instrumented tests (emulator): MediaStore save + already-saved detection on API 29+
  and API 28; manifest has no `INTERNET` permission.
- Manual checklist per release: real device with WhatsApp, light/dark, TalkBack,
  rotation, large fonts.

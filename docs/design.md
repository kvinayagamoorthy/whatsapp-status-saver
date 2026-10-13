# Design — Status Saver (Android)

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
| `StatusItem` | One media file currently in a source folder: uri, file name, type (image/video), modified time, size | `data/StatusRepository` |
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
com.moothy.statussaver
├── StatusSaverApp.kt          Application; builds repositories (manual DI)
├── MainActivity.kt            Single activity, hosts Compose NavHost
├── data/
│   ├── StatusSourceStore.kt   Persisted tree URIs (DataStore) + permission checks
│   ├── StatusRepository.kt    Lists StatusItems from all granted sources
│   ├── SavedMediaRepository.kt Save to MediaStore, query saved names
│   ├── SettingsStore.kt       Theme mode (DataStore)
│   └── model/                 StatusItem, MediaType, StatusSourceKind
└── ui/
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
`StatusSource` has a valid persisted permission.

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
2. Validate the returned tree: its document id must end with `.Statuses`. Classify as
   WhatsApp or Business from the path. Otherwise show "wrong folder" and retry.
3. `takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)`; store the URI
   in DataStore keyed by source kind.
4. List with `DocumentsContract.buildChildDocumentsUriUsingTree` and a single
   `ContentResolver.query` projecting id, display name, mime type, last modified,
   size. Avoid `DocumentFile.listFiles()` + per-file calls (slow on large folders).
5. Keep rows whose MIME type starts with `image/` or `video/`; drop `.nomedia`.
6. On each load, check `contentResolver.persistedUriPermissions`; a missing grant
   marks the source invalid and routes to onboarding.

Why SAF everywhere: avoids `MANAGE_EXTERNAL_STORAGE` (Play policy risk) and broad
read permissions, and `Android/media` stays reachable through SAF on Android 11+.
See DECISIONS D-003.

## 4. Saving to gallery (MediaStore)

- **API 29+:** insert into `MediaStore.Images.Media` / `MediaStore.Video.Media`
  (`VOLUME_EXTERNAL_PRIMARY`) with `DISPLAY_NAME` = original file name,
  `RELATIVE_PATH` = `Pictures/StatusSaver/` or `Movies/StatusSaver/`, `IS_PENDING=1`;
  stream-copy from the SAF URI; then set `IS_PENDING=0`. Delete the row on failure.
- **API 26–28:** requires `WRITE_EXTERNAL_STORAGE` (manifest `maxSdkVersion="28"`,
  requested at first save). Copy into
  `Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES|MOVIES)/StatusSaver/`,
  then `MediaScannerConnection.scanFile`.
- **Already-saved check (I3, I4):** API 29+ query MediaStore for our `RELATIVE_PATH`
  and collect `DISPLAY_NAME`s (own files are visible without read permission);
  API 26–28 list the public directory. Result is a `Set<String>` exposed as a `Flow`
  and refreshed after each save.
- Copy runs on `Dispatchers.IO`; multi-save processes items sequentially and reports
  counts (saved / already saved / failed).

Known limitation: after app reinstall on API 29+, previously saved files are no
longer "owned" and may not appear in the query, so the indicator can miss them and
MediaStore may create `name (1).jpg`. Accepted for v1 (simplicity over a read
permission). Tracked as risk R2 in `plan.md`.

## 5. Sharing

Share the SAF content URI directly with `Intent.ACTION_SEND`, MIME type from the item,
and `FLAG_GRANT_READ_URI_PERMISSION`. No `FileProvider` and no temp copies needed.

## 6. UI and theming

- Material 3, `dynamicColorScheme` on API 31+, static brand scheme below.
- `ThemeMode` (System/Light/Dark) from `SettingsStore`; `System` uses
  `isSystemInDarkTheme()`. Applied in `MainActivity` before first frame; edge-to-edge.
- Grid: `LazyVerticalGrid` adaptive columns (min ~110 dp), square tiles, Coil
  `AsyncImage`; videos via Coil `VideoFrameDecoder`.
- Viewer: `HorizontalPager`; images with a small zoomable modifier (pinch,
  double-tap); videos with Media3 `ExoPlayer` + `PlayerView` in `AndroidView`,
  one player instance released on dispose.
- Multi-select: long-press enters selection mode; top app bar switches to
  "N selected" with Save action.
- Refresh: pull-to-refresh plus reload on `ON_RESUME`.

## 7. Error handling

Only where the world can actually fail:

- Lost permission → onboarding.
- Source file deleted between listing and open/save (WhatsApp expired it) →
  "This status is no longer available", refresh list.
- Storage full / IO failure on save → snackbar with failure, MediaStore row cleaned up.

## 8. Testing strategy

- JVM unit tests: folder classification from tree document id, MIME filtering,
  sorting, save-result aggregation, ViewModel state transitions with fake repositories.
- Instrumented tests (emulator): MediaStore save + already-saved detection on API 29+
  and API 28; manifest has no `INTERNET` permission.
- Manual checklist per release: real device with WhatsApp, light/dark, TalkBack,
  rotation, large fonts.

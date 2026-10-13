# Session Log

Newest entry at the top. One entry per working session or before a compaction.
Keep entries short: what was done, what was verified, what is next.

Template:

```
## YYYY-MM-DD — <short title>
- Done: <task IDs and one-line outcome>
- Verified: <commands run / device checks>
- Decisions: <D-xxx added, if any>
- Next: <task ID>
- Notes: <anything the next session must know>
```

---

## 2026-10-13 — T3.3 committed, T3.4 vanished files; M3 complete
- Done: Committed T3.3 as `7ad1850`. T3.4: `StatusRepository.stillExists` (document
  query; IllegalArgumentException, null cursor or SecurityException mean gone),
  `StatusesViewModel.confirmVanished` + `showVanishedNotice`, viewer pages remember a
  load failure (Coil `onError`, player `onPlayerError`) and the settled page checks it;
  gone → close and snackbar on the list, present → "This status can't be shown.". Not
  yet committed.
- Verified: 4 new tests (63 in total), 6 of 6 mutations caught. Throwaway probe in
  `MainActivity` (reverted, `cmp` checked): querying or opening a missing document
  throws IllegalArgumentException wrapping FileNotFoundException. Emulator API 35:
  deleted image and video → back to the list with the message, item gone; broken .jpg and
  .mp4 → message on the page, viewer stays, swipe on works. Lint: only
  MissingApplicationIcon.
- Bug found on the device: the snackbar never showed, because clearing the notice
  ended the effect that showed it; now shown in the screen's coroutine scope.
- Decisions: D-026.
- Next: T4.1 — save to MediaStore on API 29+ (M4).
- Notes: emulator folder restored (NEW-2.jpg and AUD-1.mp4 pushed back with their
  times; BAD files removed). Backups are only in the session scratchpad.

## 2026-10-13 — T3.2 committed, T3.3 video player
- Done: Committed T3.2 as `3486a37`. T3.3: `VideoPlayerViewModel` (one ExoPlayer for the
  viewer: `show(uri)` plays from the start unless already shown, `pause()`, released in
  `onCleared`; audio focus and becoming-noisy handled) and `VideoPage` (`ContentFrame`
  with TextureView and the first frame as shutter, bottom bar with play/pause, position,
  seek slider, duration, mute). The pager plays the settled page; other video pages show
  first frame + badge. ON_STOP pauses unless the activity is rotating. Swapped
  `media3-ui` (PlayerView, no mute button) for `media3-ui-compose-material3`. Not yet
  committed.
- Verified: 5 new Robolectric tests with a real ExoPlayer (58 in total), 5 of 5
  mutations caught. Emulator API 35 with 30 s H.264 + AAC test videos (AUD-1 test
  pattern 440 Hz, AUD-2 orange 880 Hz), checked through `dumpsys audio` player states and
  screenshots: plays on open, mute/unmute (`mutedState:client`), tap and drag seek
  without paging, rotation keeps playing and position, Home pauses and it stays paused
  on return, swipe plays the next video from the start, Back leaves no audio player,
  keep-screen-on only while playing. Merged manifest still has no network or wake-lock
  permission (closes that pending check). Lint: only MissingApplicationIcon.
- Decisions: D-025.
- Next: T3.4 — vanished file handling.
- Notes: the emulator's `.Statuses` now also holds AUD-1.mp4 and AUD-2.mp4 (newest
  videos). An empty file pushed by mistake was overwritten. Debug APK 16.8 MB.

## 2026-10-13 — T3.1 committed, T3.2 zoomable image
- Done: Committed T3.1 as `2904702`. T3.2: `ui/viewer/Zoomable.kt` with `ZoomState`
  (scale 1 to 4, focal-point pinch, double-tap 2.5x toggle, pan clamped to the fitted
  image's edges, snap to fitted below 1.05x at gesture end) and `Modifier.zoomable`
  (one-finger drags only consumed while zoomed, so the pager still swipes). Image pages
  decode at original size. Not yet committed.
- Verified: 10 new JVM tests (53 in total), 8 of 8 mutations caught (focal point,
  clamp, image shape, double-tap out, min scale, late size re-clamp, no snap, snap too
  wide). Emulator API 35: `adb input` cannot pinch or double-tap, so touches were
  injected as raw multi-touch events (see CLAUDE.md). Double-tap in and out, zoomed drag
  pans without paging, pinch out and in, swipe pages afterwards, rotation resets zoom.
  Lint: only MissingApplicationIcon.
- Bug found on the device: after pinching back in, swipes stopped paging. A throwaway
  log showed the scale at 1.000000119; fixed by the snap at gesture end and covered by
  a test. Probe reverted and checked with `cmp`.
- Decisions: D-024.
- Next: T3.3 — video player page.
- Notes: the emulator's adbd was put back to non-root (`adb unroot`) after the checks.

## 2026-10-13 — T3.1 pager viewer
- Done: `ui/viewer/ViewerScreen.kt` (`HorizontalPager` over `itemsOf(tab)`, keyed by
  URI, fitted `AsyncImage`, video first frame with play badge until T3.3); route
  `viewer/{tab}/{index}` in `KeepsakeNavHost` with the list's ViewModel shared through
  the `statuses` back stack entry; tiles clickable with an "Open" label. Not yet
  committed.
- Verified: build, lint (only MissingApplicationIcon), 43 unit tests unchanged.
  Emulator API 35: tap yellow tile → yellow; swipe → green; back twice → cyan; rotation
  keeps green both ways; Back → list on the same tab; Videos tab opens a video frame
  with badge and swipes; `am kill` with the viewer open then relaunch → same item, Back
  → list. Device mutation (viewer never starts the load) → endless spinner after the
  kill, so that branch is needed; file restored and checked with `cmp`.
- Decisions: D-023.
- Next: T3.2 — zoomable image.
- Notes: grid does not scroll to the last viewed item on return (not asked for). The
  viewer uses the theme background, not black, so status-bar icons stay readable until
  T5.1. Image content descriptions are left to T5.4.

## 2026-10-13 — Handoff before compaction (end of M2)
- Done: M2 complete and committed: `a6e2a28` T2.1, `ccbec21` T2.2, `000d410` T2.3,
  `c572878` T2.4, `aa73d2c` T2.5 (T1.6 was `77332dc`). Decisions D-018 to D-022.
  CLAUDE.md gained the mutation-run hygiene, the throwaway-probe practice, ffmpeg
  recipes, grant/reinstall facts, a `tapText` helper and the lint staleness note.
- Verified: after T2.5, pull-to-refresh on a centered message (the "No video statuses
  right now" tab) reloaded the list on the emulator, closing the open T2.4 risk. The
  `tapText` helper in CLAUDE.md was run as written.
- Next: T3.1 — pager viewer route (`viewer/{tab}/{index}`, design.md §2 and §6).
- Notes for T3.1: tabs are `MediaType` (D-020), so the route's `{tab}` can be the enum
  name; grid tiles have no click or content description yet (T3.1 and T5.4). The list
  reloads on every resume (D-021), so returning from the viewer reloads it quietly; the
  index the viewer opens at must come from the list the user tapped, and the viewer
  should tolerate the list changing underneath (T3.4 handles vanished files).
- Workflow that worked: the user says "commit and start T<n>"; each task follows five
  questions → code → tests → mutations (clean results each run) → emulator check with
  screenshots (combined with `magick ... +append`) → design/DECISIONS/trackers → report.
- Environment: emulator `keepsake_api35` runs headless with `-gpu host` (Intel Iris Xe
  via Mesa); it must be restarted with that flag after a reboot. Its WhatsApp
  `.Statuses` holds 148 entries (w1/w2 PNG, w3.mp4, IMG-001..120 JPEG, VID-01..20
  H.264, NEW-1/NEW-2 JPEG, .nomedia, notes.txt, sub/); the Business folder holds b1.png
  and is not granted; one WhatsApp grant is held; night mode off, auto-rotate off. The
  physical phone `z9lzvojfkf7pknpv` is also attached: never use it without asking.
  Generated media live only in the session scratchpad; recreate with the CLAUDE.md
  ffmpeg recipes if needed.

## 2026-10-13 — T2.4 committed, T2.5 empty and lost access; M2 complete
- Done: Committed T2.4 as `c572878`. T2.5: Empty shows "No statuses yet..." (centered,
  scrollable for pull); NeedsAccess navigates to onboarding and pops the list (D-022).
  Placeholder string removed. Not yet committed.
- Verified: 43 unit tests (VM states already covered; the navigation glue is checked
  on the device). Emulator API 35 with a throwaway probe that released every grant in
  `MainActivity.onStop` (reverted): list → Home → return lands on onboarding with no
  Continue; Back goes to the launcher; no crashes. Clean build: re-grant → Continue →
  list. Empty `.Statuses` → message; pull on it works; image added while away shows on
  return. Lint: only MissingApplicationIcon. Closed the "NeedsAccess seen on a device"
  pending check.
- Decisions: D-022.
- Next: T3.1 — pager viewer route (M3).
- Notes: There is no shell command to revoke a persisted SAF grant; a probe in the app
  is the way. Emulator: WhatsApp grant re-made through onboarding; folder restored with
  all test files.

## 2026-10-13 — T2.3 committed, T2.4 refresh
- Done: Committed T2.3 as `000d410`. T2.4: `StatusesViewModel.reload()` (quiet, on every
  `ON_RESUME` via `LifecycleEventEffect`, replaces the `init` load) and `refresh()`
  (pull-to-refresh indicator); one load at a time. `PullToRefreshBox` wraps the tab
  content; centered messages scroll so pulling works on them too. Not yet committed.
- Verified: 11 ViewModel tests (43 unit tests in total), 4 of 4 mutations caught (no
  in-flight guard, indicator never cleared, quiet reload shows indicator, load back in
  `init`). Emulator API 35 `-gpu host`: new image pushed while the app was in the
  background showed first on return; image pushed while open did not show until a pull,
  then did; indicator cleared; no crashes. Lint: only MissingApplicationIcon.
- Decisions: D-021.
- Next: T2.5 — empty and lost-permission states.
- Notes: Emulator WhatsApp folder now also has NEW-1.jpg (yellow) and NEW-2.jpg (cyan).
  Pull on the centered message (empty tab) was not exercised on the device.

## 2026-10-13 — T2.2 committed, T2.3 status grid
- Done: Committed T2.2 as `ccbec21`. T2.3 `StatusesScreen`: top bar, Images/Videos
  tabs (`MediaType`, saved across rotation), adaptive `LazyVerticalGrid` of square Coil
  thumbnails keyed by URI, play badge on videos, spinner while loading, per-tab "none
  right now" text. `StatusSaverApp` provides Coil's loader with `VideoFrameDecoder`.
  `StatusesUiState.Content.itemsOf(type)` splits tabs. Not yet committed.
- Verified: 37 unit tests (new tab-split test; its mutation caught). Lint: only
  MissingApplicationIcon. Emulator API 35 with 140 extra statuses (120 JPEG 720x1280,
  20 H.264 720x1280 5 s, made with ffmpeg): images and video frames render newest
  first, badges shown, light and dark OK, Videos tab kept in landscape (8 columns), no
  crashes. Scroll jank via `dumpsys gfxinfo` over 24 swipes, `-gpu host`: release 2.1%
  janky (p50 17 ms, 1 slow UI frame), debug 1.9%; Settings baseline 0.2%.
- Decisions: D-020.
- Next: T2.4 — pull-to-refresh and reload on resume.
- Notes: First measurement on the default headless emulator (SwiftShader) gave ~50%
  janky even for a grid without images, Settings 3%: the CPU renderer, not the app.
  Restarted with `-gpu host` (CLAUDE.md updated). A temporarily debug-signed release
  build was used for one measurement and reverted. Reinstalling via uninstall drops the
  SAF grant; `install -r` keeps it. ffmpeg here has no libx264: use `-c:v libopenh264`.
  Emulator WhatsApp folder now holds 146 files (w1, w2, w3, IMG-001..120, VID-01..20,
  .nomedia, notes.txt, sub/); the WhatsApp grant was re-made through onboarding.

## 2026-10-13 — T2.1 committed, T2.2 StatusesViewModel
- Done: Committed T2.1 as `a6e2a28`. T2.2 `ui/statuses/StatusesViewModel.kt` with
  `StatusesUiState` (Loading, Content, Empty, NeedsAccess); `StatusRepository.load()`
  now returns null when no folder is readable (D-019). `kotlinx-coroutines-test` added
  as a test dependency. Screen not wired yet (T2.3). Not yet committed.
- Verified: 4 ViewModel tests with a fake load function plus 3 new repository tests
  (36 unit tests in total); 4 of 4 mutations caught (null shown as empty, empty shown as
  content, no loading state, repository answering empty instead of null). Emulator API
  35 with a throwaway screen probe (reverted): Loading → Content [w3.mp4, w2.png,
  w1.png]; empty `.Statuses` → Empty; rotation keeps Content without reloading. Lint:
  only MissingApplicationIcon.
- Decisions: D-019.
- Next: T2.3 — grid UI with tabs and thumbnails.
- Notes: NeedsAccess not seen on the device (start routing sends a fresh install to
  onboarding first); it is reached in T2.5. Lint ignores `#noinspection` in
  `libs.versions.toml`, and `lintDebug --rerun` does not rewrite the report: use
  `--rerun-tasks` after catalog changes. The emulator's fake statuses are back in place
  and auto-rotate is off (user_rotation 0).

## 2026-10-13 — T1.6 committed, T2.1 StatusRepository
- Done: Committed T1.6 as `77332dc`. T2.1 `data/StatusRepository.kt` `load()`: one
  children query per valid source, image/video MIME filter, merge, newest first with
  name tie-break; wired into `StatusSaverApp`. Not yet committed.
- Verified: 8 Robolectric tests against a fake documents provider (30 unit tests in
  total); 7 of 8 mutations caught (video dropped, any MIME accepted, wrong document id,
  oldest first, no tie-break, valid-source filter ignored, null cursor throws).
  Emulator API 35 with a throwaway probe (reverted): 3 items newest first with correct
  MIME/size/time, `.nomedia`, `notes.txt` and a sub-folder dropped; every item URI
  opens and reads its full size; deleting the granted folder gives 0 items, no crash
  (provider logs FileNotFoundException, null cursor). First load ~330 ms cold.
- Decisions: D-018.
- Next: T2.2 — `StatusesViewModel` + `UiState`.
- Notes: Surviving mutation: dropping the null guard on last-modified, because
  MatrixCursor and SQLite both return 0 for null; kept since the Cursor contract calls
  it implementation-defined. Emulator now holds fake statuses: WhatsApp folder has
  w1.png, w2.png, w3.mp4, .nomedia, notes.txt, sub/; Business folder has b1.png (not
  granted). `adb push` works under Android/media (CLAUDE.md tips updated). Mutation
  runs: delete `app/build/test-results` first and check the Gradle exit code, or a
  mutant that fails to compile leaves old results that look like a catch.

## 2026-10-13 — Handoff before compaction (end of M1)
- Done: M1 complete. Commits on `main` this block: `857cb99` T1.1, `30b0d14` T1.2,
  `5defa0e` T1.3, `b9c966c` T1.4, `39aace7` T1.5. T1.6 is done and verified but not
  committed (user had not said "commit" yet).
- Verified: working tree holds only the T1.6 changes plus these tracker updates.
- Decisions: D-013 to D-017 added in this block.
- Next: commit T1.6 when asked, then T2.1 — `StatusRepository.load()`.
- Notes:
  - Workflow the user confirmed by repetition: one task per turn; "commit and start
    T<next>" commits the finished task and starts the next. Report as Theory /
    What the skill changed / Verified / Risk / Files, no diffs.
  - Theory-skill habits used every task and worth keeping: five questions before
    coding; mutation checks after tests (now written into CLAUDE.md); throwaway
    device probes for platform facts (e.g. the SecurityException check in T1.5),
    always reverted before commit.
  - Emulator tips moved into CLAUDE.md Commands (fake folders, `pm clear`, `run-as`
    DataStore read, persisted-grant dump, uiautomator tapping). The tap helper
    script lived in the session scratchpad and is gone after this session; rebuild
    it from those commands if needed.
  - Robolectric left an unused API 23 runtime (79 MB) in `~/.m2` from its first run;
    the user has not asked to delete it.
  - Emulator state: fake `.Statuses` folders exist for both apps (Android/media) and
    the legacy `/sdcard/WhatsApp/Media/.Statuses`; all empty. The app on it has one
    WhatsApp grant from the T1.6 run.

## 2026-10-13 — T1.6: start destination; M1 complete
- Done: Committed T1.5 (`39aace7`). T1.6: `ui/KeepsakeNavHost.kt` (string routes,
  NavHost), `ui/StartViewModel.kt` (start route decided once from `validSources()`),
  placeholder `ui/statuses/StatusesScreen.kt`, "Continue" on onboarding once an app
  is granted. design.md §2 tree and navigation note updated. M1 complete.
- Verified on emulator: see the T1.6 row in STATUS.md. Unit tests and lint green.
- Decisions: D-017 (Continue button; onboarding popped from the back stack).
- Next: T2.1 — `StatusRepository.load()`. Reuse `StatusSourceStore.validSources()`;
  remember `StatusItem.mimeType` (D-013); Robolectric is available for `Uri` (D-014).
  The emulator's `.Statuses` folders are empty, and `touch` under `Android/media` from
  `adb shell` fails with "Operation not permitted": find another way to put sample
  files there (e.g. `adb push`, or the legacy `/sdcard/WhatsApp/Media/.Statuses`).
- Notes: Nothing is drawn while the start route loads (a few ms). Not yet committed
  (see the handoff entry above).

## 2026-10-13 — T1.5: onboarding screen
- Done: Committed T1.4 (`b9c966c`). T1.5: `StatusSaverApp` (Application; DataStore
  `status_sources` + `StatusSourceStore`), `ui/onboarding/OnboardingViewModel` and
  `OnboardingScreen` (one card per app with path, Grant access / Access granted +
  Choose again, wrong-folder error, read-only note). `MainActivity` shows onboarding;
  placeholder screen and string removed. `StatusFolders.expectedPath()` added.
- Verified on emulator `keepsake_api35` (folders made with `adb shell mkdir`): see the
  T1.5 row in STATUS.md. Both T1.5 Pending checks closed. 22 unit tests and lint green.
- Decisions: D-016 (picked folder decides the app, not the button). plan.md R3
  updated: the picker hides `.Statuses` unless "Show hidden files" is on.
- Next: T1.6 — start destination (Navigation Compose; onboarding vs statuses).
- Notes: Driving the picker: `uiautomator dump` + tap by text works (DocumentsUI
  button text is upper case "USE THIS FOLDER", dialog "ALLOW"); scroll the breadcrumb
  bar to reach the storage root. Cold emulator boot can show a "System UI isn't
  responding" dialog: tap Wait. Card shows the expected path even if the user picked
  another `.Statuses` folder (fine for now; T5.2 shows the granted folder).

## 2026-10-13 — T1.4: initial-URI builder
- Done: Committed T1.3 (`5defa0e`). T1.4: `StatusFolders.initialUri(kind)` (uses
  `Build.VERSION.SDK_INT`) over the testable `initialDocumentId(kind, sdkInt)`; folder
  names became shared constants used by `classify` too.
- Verified: 5 new JVM tests (22 total) green, including a round trip that every
  start folder classifies as its own app on API 26/29/30/37; `>=` → `>` mutation on
  the API 30 boundary failed the two Android 11 tests. `lintDebug`: only
  MissingApplicationIcon.
- Decisions: none new (design.md §3 step 1 already specified this).
- Next: T1.5 — onboarding screen. Needs `StatusSaverApp` (Application) to create
  the DataStore and `StatusSourceStore` (design.md §2), and emulator checks: picker
  opens at `.Statuses`, grant survives restart, plus the two Pending checks.
- Notes: The emulator has no WhatsApp; T1.5 will need to create the folder
  (`adb shell mkdir -p /sdcard/Android/media/com.whatsapp/WhatsApp/Media/.Statuses`)
  to test the picker.

## 2026-10-13 — T1.3: tree validation and classification
- Done: Committed T1.2 (`30b0d14`). T1.3: `data/StatusFolders.kt` with
  `classify(treeDocumentId): StatusSourceKind?` (null = wrong folder). design.md §2
  and §3 step 2 updated.
- Verified: 10 new JVM tests (17 total) green; mutations (substring instead of last
  folder; substring "WhatsApp" match before Business) failed the expected tests.
  `lintDebug`: only MissingApplicationIcon.
- Decisions: D-015 (classification rule).
- Next: T1.4 — initial-URI builder. Put its paths in `StatusFolders` too (one place,
  R1); `classify` takes a document id string so its tests need no Robolectric.
- Notes: Picked-tree document id format to be confirmed on the emulator in T1.5
  (Pending checks).

## 2026-10-13 — T1.2: StatusSourceStore
- Done: Committed T1.1 (`857cb99`). T1.2: `data/StatusSourceStore.kt` with
  `StatusSource(kind, treeUri)`, `validSources()` (stored and still granted) and
  `grant(kind, treeUri)` (takes the persisted read grant, stores it, releases a
  replaced folder's grant if the system still holds it).
- Verified: 7 unit tests green on Robolectric SDK 37; mutation checks (removed the
  permission filter, broke the release condition) made tests fail as expected.
  `assembleDebug` and `lintDebug` green, only MissingApplicationIcon.
- Decisions: D-014 (Robolectric, test JVM flags). design.md §8 notes Robolectric.
- Next: T1.3 — tree validation + classification.
- Notes: Robolectric first ran on API 23 because it could not see the manifest;
  `isIncludeAndroidResources` fixed that, then SDK 37 needed the `--add-exports` flag.
  An unused Robolectric API 23 runtime (79 MB) stays in `~/.m2` from that first run.
  `Application` wiring of the store waits for its first caller (T1.5/T1.6). No
  revoke yet (T5.2).

## 2026-10-13 — T1.1: models
- Done: `data/model/` with `StatusSourceKind` (WHATSAPP, BUSINESS), `MediaType`
  (IMAGE, VIDEO), and `StatusItem` (uri, name, type, mimeType, lastModifiedMillis,
  sizeBytes). design.md §1 table now lists the MIME type.
- Verified: `assembleDebug` and `lintDebug` green; only MissingApplicationIcon remains.
- Decisions: D-013 (`StatusItem.mimeType`).
- Next: T1.2 — `StatusSourceStore`.
- Notes: Folder paths deliberately not on `StatusSourceKind` yet; T1.3/T1.4 decide
  where they live (one place, risk R1). `StatusItem` holds `android.net.Uri`: JVM
  tests that build items (T2.1/T2.2) need a plan for the stubbed `Uri` (pure mapping
  functions over plain values, or `isReturnDefaultValues`). Not yet committed.

## 2026-10-13 — Handoff before compaction (end of M0)
- Done: Committed T0.6 (`9e11289`). M0 complete. Commits so far on `main`:
  `d238047` and `833927c` docs (made on branch `docs/project-setup`, fast-forward
  merged); `03b3c8a` T0.1+T0.2; `9317d20` T0.3; `a19a3cb` T0.4; `35ca13e` T0.5;
  `9e11289` T0.6.
- Verified: working tree clean after commit.
- Decisions: D-001 to D-012 cover everything decided so far.
- Next: T1.1 — models (`StatusSourceKind`, `MediaType`, `StatusItem`). M1 is the
  first milestone where design.md §1 concepts become code: use the
  `programming-as-theory-building` skill and its five questions before T1.1.
- Notes:
  - The user is trying out the programming-as-theory-building skill on this
    project. Show it in each report (Theory / Verified / Risk), carry the theory
    line into STATUS notes, and say plainly when the skill changed a choice (e.g.
    no placeholder test committed, `documentfile` left out, scope asked first).
  - Environment: JDK 21 at `/usr/lib64/jvm/java-21-openjdk-21`; Android SDK at
    `~/Android/Sdk` with platforms 35 and 37.0, build-tools 34/35; no system Gradle
    (use `./gradlew`). First build after a clean cache takes about 5-7 minutes.
  - Tool quirks: `sdkmanager` is deprecated in favour of the `android` CLI in
    `cmdline-tools/latest/bin`; `avdmanager create avd` silently does nothing, so
    AVD `keepsake_api35` was written by hand under `~/.android/avd/`. Emulator
    boots in about 15 s.
  - A physical phone is attached over adb. Target the emulator with
    `-s emulator-5554`; ask before installing on or changing the phone.
  - See "Pending checks" in STATUS.md for partly verified items.

## 2026-10-13 — T0.6: test and lint wiring; M0 complete
- Done: Committed T0.5 (`35ca13e`). T0.6: JUnit 4.13.2 added; backup/transfer
  exclusion rules for Android 12+ (D-012). M0 is complete.
- Verified: a temporary probe test ran green through `testDebugUnitTest`, then was
  deleted (no placeholder tests in the repo; real tests start in T1.2/T1.3).
  `lintDebug` with `--rerun-tasks`: only MissingApplicationIcon (T5.5).
  `aapt2` shows allowBackup=false and dataExtractionRules in the APK manifest.
- Decisions: D-012.
- Next: T1.1 — models.
- Notes: One lint run returned stale results after a manifest change; `--rerun-tasks`
  gave the correct report. Not yet committed.

## 2026-10-13 — T0.5: manifest permissions
- Done: Committed T0.4 (`a19a3cb`). T0.5: declared WRITE_EXTERNAL_STORAGE with
  maxSdkVersion 28; removed Media3's ACCESS_NETWORK_STATE and WAKE_LOCK.
- Verified: merged manifest and aapt2 badging; Media3 bytecode (SecurityException
  catch in NetworkTypeObserver, wake lock only via setWakeMode); emulator API 35
  launch with no crash and no android.permission.* requested.
- Decisions: D-011.
- Next: T0.6 — unit test + lint wiring.
- Notes: Not yet committed. Playback without the removed permissions is confirmed in
  T3.3.

## 2026-10-13 — T0.4: core dependencies
- Done: Committed T0.3 (`9317d20`). T0.4 added Navigation Compose, Lifecycle
  ViewModel/runtime Compose, DataStore Preferences, Coil 3 (compose + video), and
  Media3 (exoplayer + ui) at the latest stable versions to the catalog and `:app`.
- Verified: `assembleDebug` green; merged manifest checked: no INTERNET.
- Decisions: none new (the libraries were chosen in design.md; versions are in the
  catalog). `documentfile` was not added because design.md §3 avoids
  `DocumentFile.listFiles()`.
- Next: T0.5 — manifest permissions. Media3 brings ACCESS_NETWORK_STATE and WAKE_LOCK.
- Notes: Not yet committed.

## 2026-10-13 — T0.3: Compose theme and MainActivity
- Done: T0.3 Compose + Material 3 (`KeepsakeTheme`: system dark, dynamic color on
  API 31+, terracotta brand palette below), edge-to-edge `MainActivity` with a
  placeholder screen, light/dark window themes to avoid a launch flash.
- Verified: `assembleDebug` green; Kotlin compiled with 2.4.21. On emulator
  `keepsake_api35`: installed, launched, screenshots in light and dark after
  `cmd uimode night no/yes`; no crashes in the crash log.
- Decisions: none new.
- Next: T0.4 — remaining core dependencies.
- Notes: `avdmanager create avd` silently does nothing with cmdline-tools 23 (the
  new `android` CLI replaces it), so the AVD config was written by hand under
  `~/.android/avd/`. Not yet committed.

## 2026-10-13 — T0.1 and T0.2: Gradle scaffold
- Done: Merged `docs/project-setup` into `main` (fast-forward). T0.1 Gradle project
  (`settings.gradle.kts`, root and `:app` build files, version catalog, manifest,
  `app_name` = Keepsake). T0.2 `.gitignore` and Gradle wrapper, done together with
  T0.1 because the wrapper is needed to verify it.
- Verified: `./gradlew assembleDebug` succeeded (first run 6m 48s, downloads platform
  37); `aapt2 dump badging` shows package, minSdk 26, targetSdk 37, label, no
  permissions.
- Decisions: D-010 (toolchain versions, allowBackup off).
- Next: T0.3 — Compose + Material 3 theme and `MainActivity`.
- Notes: `local.properties` (sdk.dir) is created locally and git-ignored. Not yet
  committed.

## 2026-10-13 — Package rename, distribution, name/icon candidates
- Done: Package changed to `com.moorthy.statussaver` (D-007). T6.1 resolved:
  sideload APK for now (D-008); T6.4 deferred, T6.5 (sideload install notes) added.
- Verified: n/a (documentation only).
- Decisions: D-007, D-008, D-009.
- Next: T0.1 — scaffold the Gradle project.
- Notes: Name candidates offered: Status Saver, Keepsake, Glimpse, Stashy, Kept.
  Icon concepts: ring + down arrow, bookmark in ring, play/photo stack with check.
  User chose Keepsake + bookmark in ring (D-009); gallery folders renamed to
  `Pictures/Keepsake` and `Movies/Keepsake` to match.

## 2026-10-13 — Project docs and trackers
- Done: Created `CLAUDE.md`, `docs/spec.md`, `docs/design.md`, `docs/plan.md`, and
  trackers (`STATUS.md`, `DECISIONS.md`, `SESSION_LOG.md`).
- Verified: n/a (documentation only).
- Decisions: D-001 to D-006.
- Next: T0.1 — scaffold the Gradle project.
- Notes: Scope confirmed with user (status saver, Compose, minSdk 26). Code work
  should use the `programming-as-theory-building` skill; program theory lives in
  `docs/design.md` §1.

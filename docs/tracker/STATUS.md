# Status Board

Last updated: 2026-10-14 (all planned tasks done; T6.4 deferred; handoff before compaction)

## Resume here

- **Current milestone:** none open. M0-M6 done; T6.4 (store listing, privacy policy) is
  deferred until a Play Store move (D-008).
- **In progress:** none
- **Next task:** none planned. New ideas from the user start as new tasks or a new
  milestone in `docs/plan.md` (settle scope with the user first; keep spec.md's "no
  feature without a current need").
- **Blockers:** none
- **Last task commit:** `048e7ee` (T5.8), then docs `fa5f7f7` and this handoff. Earlier: `c32f0bc` (T5.7), `c80fcbe` (T6.5), `2d1c34b` (T6.3), `8354222` (T6.2), `88f94c1` (T5.6), `a725ae3` (T5.5), `3077f9d` (T5.4), `13c726b` (T5.3), `5a28682` (T5.2), `aeca0c6` (T5.1), `571647c` (T4.6), `d41eb39` (T4.5), `5a832c0` (T4.4), `201daee` (T4.3), `04909aa` (T4.2), `39c520a` (T4.1), `c64f0c2` (tools/emulator_touch.py), `c6c87be` (docs handoff), `6979ffc` (T3.4), `7ad1850` (T3.3), `3486a37` (T3.2), `2904702` (T3.1), `aa73d2c` (T2.5), `c572878` (T2.4), `000d410` (T2.3), `ccbec21` (T2.2), `a6e2a28` (T2.1), `77332dc` (T1.6), `39aace7` (T1.5), `b9c966c` (T1.4), `5defa0e` (T1.3), `30b0d14` (T1.2), `857cb99` (T1.1), `9e11289` (T0.6), docs-only handoff `4cdea90`.
- **Notes for next session:**
  - Release state: version 0.1.0 / versionCode 1, kept there by the user's choice (D-041).
    `./gradlew assembleRelease` gives a signed 2.2 MB APK when `keystore.properties`
    exists (D-038); the user backed up it and `~/keystores/keepsake-release.jks`.
  - The user's phone (`z9lzvojfkf7pknpv`, Xiaomi, Android 15, HyperOS 3) runs the signed
    release build of `048e7ee` with its WhatsApp folder granted. Use it only when the user
    says so (CLAUDE.md has the HyperOS switches and the privacy rules).
  - Emulators: `keepsake_api35` (`emulator-5554`) and `keepsake_api28` (`emulator-5556`)
    were left running with the debug build and their folder grants; `keepsake_api29` is
    shut down (has the debug build and a grant; it takes the next free serial). Run only
    one `-gpu host` emulator for long checks (crashes, D-037).
  - Tests: 89 JVM unit tests (Robolectric where Android types are involved, D-014);
    instrumented tests in `app/src/androidTest` run with `install` + `am instrument`, not
    `connectedDebugAndroidTest` (CLAUDE.md, D-027). Lint: no issues.
  - Code map: `data/` (`model/`, `StatusSourceStore`, `StatusFolders`, `StatusRepository`,
    `SavedMediaRepository`, `SettingsStore`), `ui/` (`KeepsakeNavHost`, `StartViewModel`,
    `StoragePermission`), `ui/onboarding/` (`OnboardingScreen`, `FolderAccessViewModel`,
    `SourceCard`), `ui/statuses/` (`StatusesScreen`, `StatusesViewModel`, `SaveSummary`,
    `StatusDescription`), `ui/viewer/` (`ViewerScreen`, `Zoomable`, `VideoPage`,
    `VideoPlayerViewModel`, `ShareIntent`), `ui/settings/`, `ui/theme/`. Decisions D-001 to
    D-041. Tools: `tools/emulator_touch.py`.

## Pending checks

Things verified only partly so far. Close them in the task named.

| Check | Why it is open | Close in |
|-------|----------------|----------|

## Legend

`TODO` not started · `WIP` in progress · `DONE` finished and verified ·
`BLOCKED` waiting on something (say what) · `DROPPED` removed from scope (link decision)

## Milestones

| Milestone | Status | Tasks done |
|-----------|--------|------------|
| M0 Project scaffold | DONE | 6 / 6 |
| M1 Folder access | DONE | 6 / 6 |
| M2 Status list | DONE | 5 / 5 |
| M3 Viewer | DONE | 4 / 4 |
| M4 Save, indicator, share | DONE | 6 / 6 |
| M5 Settings and polish | DONE | 8 / 8 |
| M6 Release | DONE | 4 / 5 (T6.4 deferred) |

## Tasks

Notes column: for DONE tasks, record how it was verified and the one-line theory
summary (world rule → code location). Keep each note to one or two lines.

### M0 — Project scaffold

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T0.1 | Gradle project, version catalog, `:app`, minSdk 26 | DONE | `assembleDebug` green; aapt2 badging: package com.moorthy.statussaver, min 26, target 37, label Keepsake, no permissions |
| T0.2 | `.gitignore` + Gradle wrapper | DONE | Done with T0.1 (needed to verify it). Wrapper 9.8.1 with pinned SHA-256; build outputs and local.properties ignored. One Gradle deprecation (`Configuration.setVisible`) comes from AGP, not our scripts |
| T0.3 | Compose M3 theme, dynamic color, system dark, edge-to-edge | DONE | Emulator API 35: placeholder renders edge-to-edge; `cmd uimode night yes/no` flips theme live, dynamic color applied. Brand palette (API < 31) not yet seen on a device. Theory: system dark setting → `ui/theme/Theme.kt` `KeepsakeTheme` |
| T0.4 | Core dependencies | DONE | Navigation 2.10.2, Lifecycle 2.11.0, DataStore 1.2.1, Coil 3.6.3 (+video, no network module), Media3 1.11.1. `assembleDebug` green at minSdk 26. Debug APK 16.5 MB unminified (release target < 10 MB is checked in T6.2) |
| T0.5 | Manifest permissions and label | DONE | Merged manifest: WRITE_EXTERNAL_STORAGE (max 28) + AndroidX internal signature permission only; Media3 ACCESS_NETWORK_STATE and WAKE_LOCK removed (D-011, bytecode-checked). Emulator API 35: launches, no crash, no android.permission.* requested |
| T0.6 | Unit test + lint wiring | DONE | JUnit 4.13.2 as testImplementation; a temporary probe test ran green (1 test) and was removed so no placeholder test is committed. `lintDebug`: only MissingApplicationIcon left (fixed by T5.5). Added data_extraction_rules.xml (D-012). If lint or test output looks stale, rerun with `--rerun-tasks` |

### M1 — Folder access

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T1.1 | Models | DONE | `data/model/`: `StatusSourceKind`, `MediaType`, `StatusItem` (adds `mimeType`, D-013). `assembleDebug` + `lintDebug` green, no new lint issues. No test: pure data, behavior arrives in T1.3/T2.1. Theory: status file → `StatusItem`, name is identity (I3) |
| T1.2 | `StatusSourceStore` | DONE | `validSources()` + `grant()`; 7 Robolectric tests on SDK 37 (D-014); mutation checks showed the permission filter and old-grant release are covered. Lint unchanged. Theory: granted folder → `StatusSource` that counts only while the system still holds its read grant |
| T1.3 | Tree validation + classification | DONE | `data/StatusFolders.kt` `classify(treeDocumentId)`; 10 plain JVM tests (both apps, both path styles, SD card volume, five wrong-folder cases); two mutation checks caught. Lint unchanged (D-015). Theory: picked tree → which app's `.Statuses` folder, or wrong folder |
| T1.4 | Initial-URI builder | DONE | `StatusFolders.initialUri(kind)` / `initialDocumentId(kind, sdkInt)`, Android/media path from API 30; paths built from the same name constants as `classify`. 5 JVM tests incl. round trip classify(initialDocumentId) = kind; API-boundary mutation caught. Picker actually opening there is checked in T1.5. Theory: app + Android version → where its statuses folder most likely is |
| T1.5 | Onboarding screen | DONE | Emulator API 35: picker opens at `.Statuses`; WhatsApp grant survives force-stop; wrong folder (`Media`) shows error, nothing stored or persisted; Business granted by hand via "Show hidden files"; re-pick to legacy path released the old grant (2 persisted grants left); light/dark OK, no crashes. Closed Pending checks: real document id is `primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses`; device throws SecurityException releasing a missing grant (throwaway probe, reverted). No ViewModel unit test: logic is classify + grant, both tested. Theory: user picks folder → classify decides app → store keeps grant (D-016) |
| T1.6 | Start destination logic | DONE | `ui/KeepsakeNavHost.kt` (routes `onboarding`, `statuses`) + `ui/StartViewModel.kt`; placeholder `StatusesScreen` until T2.3. Emulator API 35: fresh install (`pm clear`) → onboarding without Continue; grant → Continue → statuses; Back exits to launcher; relaunch and cold start → statuses; landscape keeps statuses; no crashes (D-017). Theory: readable source exists → list first, else onboarding |

### M2 — Status list

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T2.1 | `StatusRepository.load()` | DONE | 8 Robolectric tests on a fake documents provider, 7 of 8 mutations caught (null-time guard survives by design, D-018). Emulator API 35 (probe reverted): lists 3 fake statuses newest first, drops `.nomedia`/txt/sub-folder, item URIs open; deleted folder → 0 items, no crash. Theory: granted folders' image/video files → merged `StatusItem`s, newest first |
| T2.2 | `StatusesViewModel` + `UiState` | DONE | `ui/statuses/StatusesViewModel.kt`; `load()` returns null when no folder is readable (D-019). 4 VM tests with fake load + 3 repository tests, 4/4 mutations caught. Emulator (probe reverted): Loading → Content newest first; empty folder → Empty; rotation keeps state. Theory: folder readability + contents → Loading / Content / Empty / NeedsAccess |
| T2.3 | Grid UI with tabs and thumbnails | DONE | Emulator API 35 `-gpu host`, 146 statuses (120 phone-size JPEGs, 21 videos): thumbnails and video frames newest first, badges, light/dark, tab kept in landscape. 24-swipe `gfxinfo`: release 2.1% janky (p50 17 ms), debug 1.9%, Settings 0.2%. Tab-split test + mutation caught (D-020). Theory: newest-first statuses → one grid per media type |
| T2.4 | Pull-to-refresh + reload on resume | DONE | Emulator API 35: image added while app in background shows first on return; image added while open appears after pull, indicator clears. 11 VM tests, 4/4 mutations caught (D-021). Theory: user returns from WhatsApp or pulls → folders read again, one load at a time |
| T2.5 | Empty and lost-permission states | DONE | Emulator API 35: grants released by a throwaway probe → return lands on onboarding, Back exits; re-grant → list; empty folder → message, status added while away shows on return (D-022). Closed the NeedsAccess pending check. Theory: no readable folder → FR1 again; readable but empty → explain where statuses come from |

### M3 — Viewer

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T3.1 | Pager viewer route | DONE | `ui/viewer/ViewerScreen.kt` + route `viewer/{tab}/{index}`; tiles clickable ("Open"). Emulator API 35: tapped tile opens that item, swipes go forward and back in order, rotation keeps the page, Back returns with tab kept, videos show first frame + badge, app killed in viewer restores the same item. Device mutation (no load in viewer) → endless spinner, caught. No unit test: navigation glue (as T1.6). Theory: tap → same list, same index (D-023) |
| T3.2 | Zoomable image | DONE | `ui/viewer/Zoomable.kt` (`ZoomState` + `Modifier.zoomable`), images decoded at full size. 10 JVM tests on the zoom arithmetic; 8 of 8 mutations caught. Emulator API 35 with raw touch injection: double-tap zooms at the tap and back; zoomed drag pans to the image edge without paging; pinch out and back in; then a swipe pages again (bug found and fixed: scale ended at 1.000000119, snap added); rotation resets zoom, keeps the page. Theory: fingers on an image → scale and offset clamped to its edges (D-024) |
| T3.3 | Video player page | DONE | `VideoPlayerViewModel` (one ExoPlayer per viewer) + `VideoPage` (Compose `ContentFrame` + Material 3 controls, D-025); `media3-ui` swapped for `media3-ui-compose-material3`. 5 Robolectric tests with a real ExoPlayer; 5 of 5 mutations caught. Emulator API 35 with 30 s videos with sound, `dumpsys audio` states: plays on open (keep-screen-on set), mute/unmute, seek by tap and by drag (page stays), rotation keeps playing and position, Home → paused and stays paused on return, swipe → next video from the start, Back → no audio player left, keep-screen-on cleared. Merged manifest: no network or wake-lock permission. Theory: the settled page's video → one player owned by the viewer, released with it |
| T3.4 | Vanished file handling | DONE | `StatusRepository.stillExists`, `StatusesViewModel.confirmVanished` + notice, viewer page failure check, list snackbar (D-026). 4 new tests (63 in total), 6 of 6 mutations caught. Emulator API 35: image deleted while the list was open → tap → back on the list with the message, item gone; same for a video deleted while the viewer was open (on swiping to it); a text file named .jpg and random bytes named .mp4 → "can't be shown", viewer stays, swiping on works. Bug found and fixed on the device: the snackbar was cancelled by its own effect. Probe: deleted document query throws IllegalArgumentException. Theory: load error → ask the folder → expired means back to a refreshed list |

### M4 — Save, indicator, share

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T4.1 | MediaStore save (API 29+) | DONE | `data/SavedMediaRepository.kt` `save(item)`: pending row in `Pictures/Keepsake/` or `Movies/Keepsake/`, copy from SAF, clear pending; row deleted and false on failure (D-027). AndroidX Test runner 1.7.0 + ext-junit 1.3.0 added. 4 instrumented tests on emulator API 35 (image and video land in the right folder, complete and not pending; source unchanged; vanished source leaves no row); 7 of 7 mutations caught. Probe with real SAF statuses: w1.png and AUD-1.mp4 copied byte-identical (md5), missing document returned false with no row, `.Statuses` still 150 entries; probe and its rows removed. Theory: keeping a status → a complete, never half-visible copy in the gallery under its own name |
| T4.2 | Legacy save (API 26–28) | DONE | `save()` now covers Android 8-9: hidden `.part` copy in public `Pictures`/`Movies` `/Keepsake`, rename, awaited media scan; failure deletes the `.part` and returns false (D-028). Repository takes a `Context`. 4 instrumented tests on new emulator `keepsake_api28` (image and video complete in the right folder and in MediaStore; source unchanged; vanished source leaves nothing); 8 of 8 mutations caught after fixing a test that took the expected folder from the code under test. API 35 suite still green. Closed the API 29 pending check: new emulator `keepsake_api29` runs `SavedMediaRepositoryTest` green, and the two pending-row mutations (pending never cleared, no row cleanup) are caught there too. Probe with real SAF statuses on API 28 (`/sdcard/WhatsApp/Media/.Statuses`): without storage permission both saves false and nothing written; with READ+WRITE granted both true, md5-identical, rows in images/video MediaStore (video with duration). Permission request UI is T4.4. Theory: on Android 8-9 the gallery is files in the public folders plus the media scanner |
| T4.3 | Saved-names flow, idempotent save | DONE | `save()` returns `SaveResult` (SAVED / ALREADY_SAVED / FAILED); checks the storage for the name before copying, one save at a time (Mutex); `savedNames` StateFlow + `refreshSavedNames()` (D-029). Instrumented: 12 tests on API 35 and API 29, 9 on API 28, three runs each stable. Saving twice gives one copy and ALREADY_SAVED; two saves at once give one copy; a fresh repository lists earlier copies after refresh; `.part` files and failed saves are not listed; a copy deleted in a gallery app can be saved again. 10 of 10 mutation kinds caught (12 runs) after adding two tests for survivors (no lock on Android 9 with a small file; name filter dropped). Theory: identity = file name in our Keepsake folders; the storage, not a cache, decides |
| T4.4 | Save action + check marks | DONE | Viewer: round Save button top end (scrim), spinner while copying, check mark when saved; tiles get a check badge; snackbar "Saved to your gallery" / "Already saved" / failure, shown by the screen in front, above the video controls (D-030). `SavedMediaRepository` wired into `StatusSaverApp`; saved names re-read on every list reload. Android 8-9: permission asked at first save (`ui/StoragePermission.kt`). 4 new ViewModel unit tests (67 total), 5 of 5 mutations caught. Emulator API 35: image save → check + snackbar, second tap "Already saved" with one MediaStore row; video save while playing, snackbar above controls; list badge in light and dark; copy deleted outside the app loses its badge on resume. Emulator API 28: dialog → ALLOW → saved at once without restart (closes the T4.2 risk); DENY → settings message, nothing written. Theory: keep = one tap on what you are looking at; saved = name in the gallery folder |
| T4.5 | Multi-select + Save selected | DONE | Long-press selects, taps toggle, top bar "N selected" with cross and Save, Back leaves the mode; selection by URI in `StatusesViewModel`, kept across rotation, pruned on reload; saves in list order, one summary snackbar with counts (`SaveSummary`, D-031). Permission helper now `rememberStoragePermissionGate`. 73 unit tests (6 new), 9 of 9 mutations caught after rewriting one test that blocked on the wrong item. Emulator API 35: 3 selected → "3 statuses saved to your gallery." (3 MediaStore rows); 1 saved + 1 new → "1 status saved to your gallery. 1 was already saved." (one new row); selection kept through landscape and back; Back clears it; viewer save still works. API 28: Save selected → permission dialog → ALLOW → saved. Theory: pick many, one save action, one honest count |
| T4.6 | Share | DONE | Share button left of Save in the viewer; `ACTION_SEND` of the status's SAF URI with its MIME type, read grant and clip data, through the chooser (`ui/viewer/ShareIntent.kt`, D-032); pauses a playing video first. 76 unit tests (3 new), 5 of 5 mutations caught (no grant flag, no clip data, `*/*` type, no stream, no chooser). Emulator API 35: image → chooser shows preview → Messages attaches it; video → chooser lists AUD-1.mp4; without the pause the video kept playing with sound behind the sheet, with it the player is `paused`. API 28: image → Messaging attaches it. Nothing sent. Theory: share by the grant Keepsake already holds, no copy |

### M5 — Settings and polish

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T5.1 | Theme mode setting | DONE | `SettingsStore` (DataStore `settings`, `ThemeMode` by name, unknown → System); settings screen from the list's gear, radio group System default / Light / Dark; `MainActivity` waits for the stored choice, themes the app and the system bar icons, and on Android 12+ calls `setApplicationNightMode` so the launch screen matches (D-033). 82 unit tests (6 new), 6 of 6 mutations caught (default, write, read, and three `isDarkTheme` branches). Emulator API 35: Dark on a light phone applies at once with white status icons; Back/forward crossfade stays dark; cold start keeps Dark and the launch screen is dark (was white for ~2 s before `setApplicationNightMode`); System follows `cmd uimode night yes/no` both ways; Light on a dark phone is light with dark icons; choice kept through landscape. API 28: Dark applies, brand palette in dark mode looks right (closes that pending check); cold start still shows the light launch window ~1.8 s (accepted, D-033). Theory: one stored choice, the activity follows it |
| T5.2 | Access management | DONE | Settings "Statuses folders": onboarding's cards (`SourceCard.kt`) and ViewModel (renamed `FolderAccessViewModel`) with Grant access / Choose again / Remove access; Remove asks first (dialog kept through rotation) and calls `StatusSourceStore.revoke`, which forgets the URI and releases the persisted grant (D-034). 86 unit tests (4 new), 4 of 5 mutations caught; the dropped release guard survives because Robolectric does not throw there (device does, T1.5). Emulator API 35: Remove → 0 persisted grants, DataStore empty, Back → onboarding; re-grant through onboarding; Choose again keeps one grant and the list returns. API 28: Remove → `persisted=0x0` (the picker's temporary grant stays with the running activity, unused); re-grant from settings → list. Theory: one access concept, two screens |
| T5.3 | About section | DONE | Last settings section: app name, "Version <versionName>" read from the package, non-affiliation note (WhatsApp/Meta), responsibility note (spec §7). No unit test: static text plus the package's own version name (a test would only restate it). Seen on API 35 and API 28: "Version 0.1.0", matching `build.gradle.kts`. Theory: say who made it and whose media it is |
| T5.4 | Accessibility pass | DONE | Checklist (API 35 unless noted): (1) content descriptions — every actionable node labelled (audit of uiautomator dumps on list, Videos tab, selection mode, viewer image/video, settings, also API 28); fixed: image tiles had none and video tiles all said "Video", now "Image/Video status, <age>" (`StatusDescription.kt`, D-035); viewer picture now described (image; video via labelled layer). (2) touch targets — all actionable nodes ≥ 48 dp (buttons 48×48, tiles ~131 dp, radio rows 56 dp, tabs 48 dp high). (3) TalkBack 15 — tiles "Image status, 22 hours ago. In list", saved adds "Saved", selection mode "Selected/Not selected …", tabs "Videos. Tab. 2 of 2", gear "Settings. Button", viewer "Image status, 22 hours ago. Image. In horizontal pager" / "Video status, 16 hours ago", Share/Save/Pause/Mute buttons, radios "Selected. System default. Radio button. 1 of 3"; known gap: video slider reads "0.18. Slider" (Media3). (4) font scale 2.0 — list, selection bar, settings ("Remove access" wraps to two lines), viewer controls, snackbar all fit. 87 unit tests (1 new), 3 of 3 mutations caught. Theory: a status is spoken as what it is and how old |
| T5.5 | Launcher icon (bookmark in ring) | DONE | `mipmap-anydpi/ic_launcher.xml` (icon and round icon): terracotta background, `drawable/ic_launcher_foreground.xml` ring + bookmark ribbon inside the safe circle, same drawable as monochrome layer (D-036). Lint: no issues at all now. Seen on API 35 drawer (circle mask), API 35 home screen with themed icons on (tinted like Gmail/Photos), API 28 drawer (rounded-square mask). Theory: the bookmark says keep, the ring makes it legible in any mask |
| T5.6 | Performance check | DONE | `ReportDrawnWhen` on the list (D-037). Emulator API 35 `-gpu host`, 202 statuses, release build (temporarily debug-signed, not minified): NFR2 grid drawn median ~470 ms just after install (417-573 ms, 10 runs), ~360 ms after `compile -m speed-profile` (341-659 ms), first frame ~200 ms; debug build ~1.28 s. NFR3 24 fast swipes: 2.8-3.0% janky, p50 17 ms, p90 25 ms, p99 34-38 ms; Settings app 0.6% under the same swipes. Real phone still pending (needs the user's OK). Theory: measure to the visible grid, not the first frame |
| T5.7 | Onboarding picker hint per Android version | DONE | `onboarding_picker_hint` in `values` (8-9: “Select”), `values-v29` (10: “Allow access to ".Statuses"”, then “Allow”), `values-v30` (11+: “Use this folder”, then “Allow”) (D-039). `aapt2 dump resources` shows the three configs. Seen on screen: API 28 “Select”, API 29 the Android 10 text (quotes kept after escaping); following each hint re-granted the folder on API 28 and 29. Lint no issues. Theory: the hint names the buttons this phone's picker shows |
| T5.8 | Spoken position for the video seek bar | DONE | `SeekBar` wraps Media3's `ProgressSlider` and sets its `stateDescription` to "<position> of <length>" from a one-second progress state (D-040). TalkBack 15 on API 35: "00:08 of 00:30. Slider" (was "0.18. Slider"). 2 new unit tests (89 in all), 3 of 3 mutations caught (arguments swapped, no unknown-length guard, milliseconds as seconds). Lint no issues. Theory: say where the video is, the way the screen shows it |

### M6 — Release

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T6.1 | Distribution decision | DONE | Sideloaded APK for now; Play Store maybe later (D-008) |
| T6.2 | Release build + signing | DONE | R8 + resource shrinking, no app keep rules; signing from git-ignored `keystore.properties` → `~/keystores/keepsake-release.jks` (made by Claude at the user's request; unsigned build without the file) (D-038). `assembleRelease`: signed APK 2.16 MB (debug 16.8, unminified release 12.4), `apksigner` v2, `CN=Keepsake`. Minified build smoke-tested on API 35 (list, video frames, playback with sound, save, share preview, dark theme, folder cards, About, no crash in logcat); fresh install of the signed APK through onboarding works. Theory: one key for every update, kept outside the repo |
| T6.3 | Release manual test | DONE | Signed release APK on the user's phone: Xiaomi 2312DRAABI (Redmi Note 13 5G), Android 15, HyperOS 3, 120 Hz, 37 real statuses (25 JPEG, 12 MP4). Checklist: install (needs Developer options "Install via USB" + "USB debugging (Security settings)"); onboarding grants the real `.Statuses` folder (persisted); grid of real statuses in system dark mode with wallpaper colours; NFR2 grid drawn median ~930 ms right after install (730-1612 ms), ~680 ms after a profile compile (540-1359 ms), with 37 statuses (no test files may be added to WhatsApp's folder, so ~200 was not tried on the phone); NFR3 24 swipes 0.65% janky, p50 5 ms, p99 19 ms; viewer image and video (sound playing); save image and video → rows in `Pictures/Keepsake`/`Movies/Keepsake`, sizes equal to the statuses, tap again → "Already saved"; share sheet opens; multi-select 1 saved + 1 new → one new row, 2 already saved → "2 were already saved." (wording kept by the user's choice); in-app Light over the dark phone; landscape; font scale 2.0; installed app requests no INTERNET (only androidx's internal receiver permission); test copies deleted by `_id`, phone settings restored (auto-rotate on, font 1.0, theme System). Fixed: at font 2.0 "Remove access" broke mid-word beside "Choose again" → the card's buttons now wrap (`FlowRow`). TalkBack not run on the phone (checked on the emulator, T5.4). Theory: the release build, on a real phone, with real statuses |
| T6.4 | Store listing + privacy policy | TODO | Deferred until a Play Store move (D-008) |
| T6.5 | Sideload install notes | DONE | New `README.md`: what Keepsake is, privacy, disclaimer, requirements, install, first start, update, uninstall, build from source. Walked through on `keepsake_api35` with the signed APK in Downloads via the Files app: "isn't allowed to install unknown apps from this source" → Settings → Allow from this source → "Do you want to install this app?" → Install → Open → onboarding; reopening the APK → "Do you want to update this app?" → Update keeps the folder grant; a differently signed APK → "App not installed as package conflicts with an existing package." README quotes these. Real-device install was done over adb in T6.3 (the user keeps that build, so it was not reinstalled through Files). Emulator restored afterwards. Theory: write down exactly what the phone says |

## Open questions

| ID | Question | Status |
|----|----------|--------|
| OQ1 | Distribution channel (Play / F-Droid / sideload) | Resolved — sideload for now (D-008) |
| OQ2 | App display name and icon | Resolved — Keepsake, bookmark-in-ring icon (D-009) |

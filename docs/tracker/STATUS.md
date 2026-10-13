# Status Board

Last updated: 2026-10-13 (T2.3 done, uncommitted)

## Resume here

- **Current milestone:** M2 — Status list (M1 complete)
- **In progress:** none (T2.3 done, waiting for the user to say commit)
- **Next task:** T2.4 — pull-to-refresh and reload on resume
- **Blockers:** none
- **Last task commit:** `ccbec21` (T2.2). Earlier: `a6e2a28` (T2.1), `77332dc` (T1.6), `39aace7` (T1.5), `b9c966c` (T1.4), `5defa0e` (T1.3), `30b0d14` (T1.2), `857cb99` (T1.1), `9e11289` (T0.6), docs-only handoff `4cdea90`.
- **Notes for next session:** T2.3 is finished and verified but NOT committed (subject
  `feat: show statuses in a thumbnail grid with image and video tabs (T2.3)`); commit
  it first when the user says so. Start the emulator with `-gpu host` (CLAUDE.md).
  Code so far: `data/model/` (T1.1), `data/StatusSourceStore.kt` (T1.2),
  `data/StatusFolders.kt` (T1.3, T1.4), `StatusSaverApp.kt` + `ui/onboarding/`
  (T1.5), `ui/KeepsakeNavHost.kt` + `ui/StartViewModel.kt` + placeholder
  `ui/statuses/StatusesScreen.kt` (T1.6), `data/StatusRepository.kt` (T2.1),
  `ui/statuses/StatusesViewModel.kt` (T2.2), grid in `ui/statuses/StatusesScreen.kt`
  (T2.3). 37 unit tests (Robolectric where Android types are involved, D-014). Toolchain and settled
  scope: D-001 to D-020; see
  SESSION_LOG "Handoff after M1" for environment and emulator tips. Only add
  dependencies a task actually uses.

## Pending checks

Things verified only partly so far. Close them in the task named.

| Check | Why it is open | Close in |
|-------|----------------|----------|
| Brand (terracotta) palette on Android 8-11 | Only an API 35 system image is installed; dynamic color hides the palette there | T5.4 or earlier on an API 26-30 emulator image |
| Video playback with Media3 network/wake-lock permissions removed | No player yet (D-011) | T3.3 |
| Status-bar icon colors follow the in-app theme override | `enableEdgeToEdge()` follows the system theme only | T5.1 |
| Release APK under 10 MB | Debug APK is 16.5 MB unminified | T6.2 |
| Grid smoothness on a real phone with real WhatsApp media | Measured only on the emulator with generated files | T5.6 |
| `NeedsAccess` seen on a device | A fresh install starts on onboarding, so the state was only unit tested in T2.2 | T2.5 |

## Legend

`TODO` not started · `WIP` in progress · `DONE` finished and verified ·
`BLOCKED` waiting on something (say what) · `DROPPED` removed from scope (link decision)

## Milestones

| Milestone | Status | Tasks done |
|-----------|--------|------------|
| M0 Project scaffold | DONE | 6 / 6 |
| M1 Folder access | DONE | 6 / 6 |
| M2 Status list | WIP | 3 / 5 |
| M3 Viewer | TODO | 0 / 4 |
| M4 Save, indicator, share | TODO | 0 / 6 |
| M5 Settings and polish | TODO | 0 / 6 |
| M6 Release | TODO | 1 / 5 |

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
| T2.4 | Pull-to-refresh + reload on resume | TODO | |
| T2.5 | Empty and lost-permission states | TODO | |

### M3 — Viewer

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T3.1 | Pager viewer route | TODO | |
| T3.2 | Zoomable image | TODO | |
| T3.3 | Video player page | TODO | Confirm playback works with Media3's network/wake-lock permissions removed (D-011). Never call `setWakeMode`; use keep-screen-on while playing |
| T3.4 | Vanished file handling | TODO | |

### M4 — Save, indicator, share

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T4.1 | MediaStore save (API 29+) | TODO | |
| T4.2 | Legacy save (API 26–28) | TODO | |
| T4.3 | Saved-names flow, idempotent save | TODO | |
| T4.4 | Save action + check marks | TODO | |
| T4.5 | Multi-select save | TODO | |
| T4.6 | Share | TODO | |

### M5 — Settings and polish

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T5.1 | Theme mode setting | TODO | `enableEdgeToEdge()` picks bar icon colors from the system theme; pass explicit `SystemBarStyle` once the in-app override exists |
| T5.2 | Access management | TODO | |
| T5.3 | About section | TODO | |
| T5.4 | Accessibility pass | TODO | |
| T5.5 | Launcher icon (bookmark in ring) | TODO | Name already decided: Keepsake (D-009). Setting `android:icon` clears the last lint warning (MissingApplicationIcon) |
| T5.6 | Performance check | TODO | |

### M6 — Release

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T6.1 | Distribution decision | DONE | Sideloaded APK for now; Play Store maybe later (D-008) |
| T6.2 | Release build + signing | TODO | |
| T6.3 | Release manual test | TODO | |
| T6.4 | Store listing + privacy policy | TODO | Deferred until a Play Store move (D-008) |
| T6.5 | Sideload install notes | TODO | |

## Open questions

| ID | Question | Status |
|----|----------|--------|
| OQ1 | Distribution channel (Play / F-Droid / sideload) | Resolved — sideload for now (D-008) |
| OQ2 | App display name and icon | Resolved — Keepsake, bookmark-in-ring icon (D-009) |

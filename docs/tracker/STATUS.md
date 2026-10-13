# Status Board

Last updated: 2026-10-13 (T0.4 done)

## Resume here

- **Current milestone:** M0 — Project scaffold
- **In progress:** none
- **Next task:** T0.5 — manifest permissions (see `docs/plan.md` M0 and the T0.5 note below)
- **Blockers:** none
- **Notes for next session:** Docs and trackers created. Scope confirmed: WhatsApp
  status saver, Kotlin + Compose, minSdk 26, package `com.moorthy.statussaver`,
  sideload distribution, app name "Keepsake" with bookmark-in-ring icon (D-009).
  No open questions. Toolchain: Gradle 9.8.1 wrapper, AGP 9.4.1 (built-in Kotlin, so
  no `org.jetbrains.kotlin.android` plugin), compileSdk/targetSdk 37, JDK 21, SDK at
  `~/Android/Sdk` (D-010). Compose plugin pins Kotlin to 2.4.21 (AGP's bundled 2.2.10
  is upgraded). Emulator AVD `keepsake_api35` exists for device checks (see
  CLAUDE.md Commands). Only add dependencies a task actually uses.

## Legend

`TODO` not started · `WIP` in progress · `DONE` finished and verified ·
`BLOCKED` waiting on something (say what) · `DROPPED` removed from scope (link decision)

## Milestones

| Milestone | Status | Tasks done |
|-----------|--------|------------|
| M0 Project scaffold | WIP | 4 / 6 |
| M1 Folder access | TODO | 0 / 6 |
| M2 Status list | TODO | 0 / 5 |
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
| T0.5 | Manifest permissions and label | TODO | Merged manifest has no INTERNET, but Media3 exoplayer adds ACCESS_NETWORK_STATE and WAKE_LOCK. Decide whether to remove them with `tools:node="remove"`; check playback still works without them |
| T0.6 | Unit test + lint wiring | TODO | |

### M1 — Folder access

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T1.1 | Models | TODO | |
| T1.2 | `StatusSourceStore` | TODO | |
| T1.3 | Tree validation + classification | TODO | |
| T1.4 | Initial-URI builder | TODO | |
| T1.5 | Onboarding screen | TODO | |
| T1.6 | Start destination logic | TODO | |

### M2 — Status list

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T2.1 | `StatusRepository.load()` | TODO | |
| T2.2 | `StatusesViewModel` + `UiState` | TODO | |
| T2.3 | Grid UI with tabs and thumbnails | TODO | |
| T2.4 | Pull-to-refresh + reload on resume | TODO | |
| T2.5 | Empty and lost-permission states | TODO | |

### M3 — Viewer

| ID | Task | Status | Notes |
|----|------|--------|-------|
| T3.1 | Pager viewer route | TODO | |
| T3.2 | Zoomable image | TODO | |
| T3.3 | Video player page | TODO | |
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
| T5.5 | Launcher icon (bookmark in ring) | TODO | Name already decided: Keepsake (D-009) |
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

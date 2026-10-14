# Implementation Plan — Keepsake (Android status saver)

Status: Draft v1 (2026-10-13). Live progress lives in `tracker/STATUS.md`, not here.
This file defines *what* each task is and *when it is done*.

Task IDs are stable. Never renumber; append new tasks (e.g. `T2.7`) instead.

## M0 — Project scaffold

Goal: an empty app that builds, runs, and follows system light/dark.

| ID | Task | Done when |
|----|------|-----------|
| T0.1 | Create Gradle project: Kotlin DSL, version catalog, `:app` module, package `com.moorthy.statussaver`, minSdk 26, compileSdk/targetSdk latest stable | `./gradlew assembleDebug` succeeds |
| T0.2 | Add `.gitignore` for Android/Gradle/IDE files and Gradle wrapper | Clean `git status` after a build |
| T0.3 | Compose + Material 3 theme with dynamic color and system dark mode; `MainActivity` edge-to-edge with placeholder screen | App launches; theme flips with system setting |
| T0.4 | Add dependencies: Navigation Compose, Lifecycle ViewModel Compose, DataStore, Coil 3 (+ video), Media3 ExoPlayer/UI | Builds; versions recorded in `libs.versions.toml` |
| T0.5 | Manifest: no `INTERNET`; `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`; app label "Keepsake" | Merged manifest checked (`processDebugManifest` output) |
| T0.6 | Unit test + lint wiring | `testDebugUnitTest` and `lintDebug` run green |

## M1 — Folder access (FR1, FR8 core)

Goal: user can grant the statuses folder once and the app remembers it.

| ID | Task | Done when |
|----|------|-----------|
| T1.1 | Models: `StatusSourceKind` (WHATSAPP, BUSINESS), `MediaType`, `StatusItem` | Compiles; used by later tasks |
| T1.2 | `StatusSourceStore`: persist tree URIs in DataStore; validate against `persistedUriPermissions` | Unit test for store logic with fake |
| T1.3 | Tree validation + classification from document id (`.Statuses`, WhatsApp vs Business) | Unit tests cover both apps, both path styles, wrong folder |
| T1.4 | Initial-URI builder per API level and source kind | Unit tests for generated document ids |
| T1.5 | Onboarding screen: explanation, "Grant access" for WhatsApp and Business, wrong-folder error | On device: picker opens at `.Statuses`, grant persists across app restart |
| T1.6 | Start destination logic: onboarding vs statuses | Fresh install → onboarding; after grant → statuses |

## M2 — Status list (FR2)

Goal: grid shows current statuses from all granted sources.

| ID | Task | Done when |
|----|------|-----------|
| T2.1 | `StatusRepository.load()`: single children query per source, MIME filter, merge, newest first | Unit tests on mapping/filter/sort; on device lists real statuses |
| T2.2 | `StatusesViewModel` with `UiState` (loading, content, empty, needsAccess) | Unit tests with fake repository |
| T2.3 | Grid UI with Images/Videos tabs, Coil thumbnails, video badge | Smooth scroll with 100+ items on device |
| T2.4 | Pull-to-refresh and reload on resume | New status viewed in WhatsApp appears after returning |
| T2.5 | Empty and lost-permission states | Revoking permission routes to onboarding |

## M3 — Viewer (FR3)

| ID | Task | Done when |
|----|------|-----------|
| T3.1 | Viewer route with `HorizontalPager` over current tab's items | Swipe moves between items; index preserved on rotation |
| T3.2 | Zoomable image (pinch, double-tap) | Works on device |
| T3.3 | Video page with Media3 player: play/pause, seek, mute; pause/release on leave | No audio continues after leaving; no player leak |
| T3.4 | Handle file vanished (expired) | Shows message and returns to refreshed list |

## M4 — Save, saved indicator, share (FR4, FR5, FR6)

| ID | Task | Done when |
|----|------|-----------|
| T4.1 | `SavedMediaRepository.save()` API 29+ MediaStore path with `IS_PENDING` | Instrumented test: file visible in MediaStore under `Pictures/Keepsake` |
| T4.2 | API 26–28 path with runtime `WRITE_EXTERNAL_STORAGE` + media scan | Verified on API 28 emulator |
| T4.3 | Saved-names flow and idempotent save (I3, I4) | Saving twice yields one file and "Already saved" |
| T4.4 | Save action in viewer + snackbar; check mark on tiles and viewer | Visible on device |
| T4.5 | Multi-select in grid with "Save selected" and result summary | Saves N items; summary counts correct |
| T4.6 | Share via `ACTION_SEND` with URI grant | Shares to another app on device |

## M5 — Settings and polish (FR7, FR8, NFRs)

| ID | Task | Done when |
|----|------|-----------|
| T5.1 | `SettingsStore` + settings screen: theme mode System/Light/Dark | Choice persists; applied without restart |
| T5.2 | Access management: show granted sources, re-pick, revoke | Revoke releases persisted permission |
| T5.3 | About section: non-affiliation disclaimer, responsibility note, version | Visible in settings |
| T5.4 | Accessibility pass: content descriptions, touch targets, TalkBack, font scale 200% | Manual checklist recorded in STATUS notes |
| T5.5 | Adaptive launcher icon: bookmark ribbon inside a circle, plus monochrome layer for themed icons (D-009) | Icon shows on launcher, including Android 13+ themed icons |
| T5.6 | Performance check vs NFR2/NFR3 | Measured numbers recorded in STATUS notes |
| T5.7 | Onboarding picker hint matches the folder picker of each Android version (pending check from T4.2) | Hint text checked against the picker on API 28, 29 and 35 |

## M6 — Release

| ID | Task | Done when |
|----|------|-----------|
| T6.1 | Decide distribution channel (resolves OQ1) | Entry in DECISIONS.md |
| T6.2 | Release build: R8/minify, resource shrinking, signing config (keystore outside repo, backed up; the same key must sign every update or sideloaded upgrades fail) | `assembleRelease` produces signed APK < 10 MB |
| T6.3 | Release manual test pass (design.md §8 checklist) on real device | Checklist recorded |
| T6.4 | Store listing text, screenshots, privacy policy (no data collected). Deferred: only needed if moving to Play Store (D-008) | Ready to upload |
| T6.5 | Sideload install notes: how to install the APK and allow "install unknown apps" | Short section in README, tested on a real device |

## Risks

| ID | Risk | Mitigation |
|----|------|------------|
| R1 | WhatsApp changes its statuses folder location | Path constants in one place; user can pick any `.Statuses` folder manually |
| R2 | After reinstall (API 29+), saved indicator misses old saves; MediaStore may create `name (1)` | Accepted for v1; revisit if users complain |
| R3 | Some OEM file pickers ignore `EXTRA_INITIAL_URI`; the picker also hides `.Statuses` (dot folder) unless "Show hidden files" is on | Onboarding shows the path text, and the wrong-folder message tells the user to turn on "Show hidden files" (checked on the API 35 picker in T1.5) |
| R4 | Play policy on apps related to other apps' content | No "WhatsApp" branding; disclaimer; no network |

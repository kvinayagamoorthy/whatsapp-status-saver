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

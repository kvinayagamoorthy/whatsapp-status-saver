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

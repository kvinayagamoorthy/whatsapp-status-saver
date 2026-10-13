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
